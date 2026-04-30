package service;

import model.*;
import persistence.FileLogger;
import persistence.Serializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BankController implements IBankService {

    private static final String KAYIT_DOSYASI = "banka_durumu.dat";

    private final CustomerRepository       musteriDeposu;
    private final AccountRepository        hesapDeposu;
    private final TransactionRepository    islemDeposu;
    private final Set<String>              suphelihHesaplar;
    private final Map<String, SupheSebebi> supheSebebleri;
    private final RiskEngine               riskMotoru;
    private final FileLogger               kaydedici;
    private final KimlikDogrulama          kimlikDogrulama;
    private int islemSayaci;
    private int musteriSayaci;
    private int hesapSayaci;

    public BankController(KimlikDogrulama kimlikDogrulama) {
        this.musteriDeposu   = new CustomerRepository();
        this.hesapDeposu     = new AccountRepository();
        this.islemDeposu     = new TransactionRepository();
        this.suphelihHesaplar = new HashSet<>();
        this.supheSebebleri  = new HashMap<>();
        this.riskMotoru      = new RiskEngine();
        this.kaydedici       = new FileLogger();
        this.kimlikDogrulama = kimlikDogrulama;
    }

    // ── Müşteri / Hesap ───────────────────────────────────────────────────────

    @Override
    public Customer musteriOlustur(String ad, String eposta) {
        String id = String.format("MUS%05d", ++musteriSayaci);
        Customer musteri = new Customer(id, ad, eposta);
        musteriDeposu.kaydet(musteri);
        kaydedici.kaydet("MUSTERI_OLUSTURULDU: " + id + " | " + ad + " | " + eposta);
        otomatikKaydet();
        return musteri;
    }

    @Override
    public Account hesapOlustur(String musteriId, String tur, double baslangicBakiye) {
        Customer musteri = musteriDeposu.idIleGetir(musteriId);
        if (musteri == null) return null;
        String hesapId = String.format("HSP%06d", ++hesapSayaci);
        Account hesap;
        if ("VADELİ".equals(tur) || "VADELI".equals(tur)) {
            hesap = new SavingsAccount(hesapId, musteriId, baslangicBakiye, 0.03);
        } else {
            hesap = new CheckingAccount(hesapId, musteriId, baslangicBakiye);
        }
        hesapDeposu.kaydet(hesap);
        musteri.hesapEkle(hesap);
        kaydedici.kaydet("HESAP_OLUSTURULDU: " + hesapId + " | " + tur + " | " + musteriId);
        otomatikKaydet();
        return hesap;
    }

    // ── İşlemler ──────────────────────────────────────────────────────────────

    @Override
    public boolean paraYatir(String hesapId, double miktar) {
        Account hesap = hesapDeposu.idIleGetir(hesapId);
        if (hesap == null || miktar <= 0) return false;
        if (!riskMotoru.paraYatirmaGecerliMi(miktar)) {
            kaydedici.kaydet("PARA_YATIRMA_REDDEDILDI: " + hesapId + " miktar=" + miktar);
            return false;
        }
        String islemId = String.format("TRX%08d", ++islemSayaci);
        hesap.paraYatir(miktar);
        Transaction islem = new Deposit(islemId, miktar, hesapId);
        hesap.islemEkle(islem);
        islemDeposu.kaydet(islem);
        riskMotoru.yatirmaKaydet(hesapId);
        kaydedici.kaydet("PARA_YATIRILDI: " + islemId + " | " + hesapId + " | +" + miktar);
        islemSonrasiRiskKontrol(hesapId, hesap.getSahibiId(), miktar);
        otomatikKaydet();
        return true;
    }

    @Override
    public boolean paraCek(String hesapId, double miktar) {
        Account hesap = hesapDeposu.idIleGetir(hesapId);
        if (hesap == null || miktar <= 0) return false;
        if (suphelihHesaplar.contains(hesapId)) {
            kaydedici.kaydet("PARA_CEKME_ENGELLENDI_SUPHELI: " + hesapId);
            return false;
        }
        if (!riskMotoru.paraCekmeGecerliMi(hesap, miktar)) {
            kaydedici.kaydet("PARA_CEKME_REDDEDILDI: " + hesapId + " miktar=" + miktar);
            return false;
        }
        String islemId = String.format("TRX%08d", ++islemSayaci);
        if (!hesap.paraCek(miktar)) return false;
        riskMotoru.cekimKaydet(hesapId, miktar);
        Transaction islem = new Withdraw(islemId, miktar, hesapId);
        hesap.islemEkle(islem);
        islemDeposu.kaydet(islem);
        kaydedici.kaydet("PARA_CEKILDI: " + islemId + " | " + hesapId + " | -" + miktar);
        if (riskMotoru.yuksekRiskMi(miktar)) {
            isaretleSebeple(hesapId, hesap.getSahibiId(), "Büyük tutarlı para çekimi", miktar);
        } else {
            islemSonrasiRiskKontrol(hesapId, hesap.getSahibiId(), miktar);
        }
        otomatikKaydet();
        return true;
    }

    @Override
    public boolean transferYap(String kaynakId, String hedefId, double miktar) {
        Account kaynak = hesapDeposu.idIleGetir(kaynakId);
        Account hedef  = hesapDeposu.idIleGetir(hedefId);
        if (kaynak == null || hedef == null || miktar <= 0) return false;
        if (suphelihHesaplar.contains(kaynakId)) {
            kaydedici.kaydet("TRANSFER_ENGELLENDI_SUPHELI: " + kaynakId);
            return false;
        }
        if (!riskMotoru.transferGecerliMi(kaynak, miktar)) {
            kaydedici.kaydet("TRANSFER_REDDEDILDI: " + kaynakId + " -> " + hedefId + " miktar=" + miktar);
            return false;
        }
        if (!kaynak.paraCek(miktar)) return false;
        hedef.paraYatir(miktar);
        String islemId = String.format("TRX%08d", ++islemSayaci);
        riskMotoru.transferKaydet(kaynakId, miktar);
        Transfer islem = new Transfer(islemId, miktar, kaynakId, hedefId);
        kaynak.islemEkle(islem);
        hedef.islemEkle(islem);
        islemDeposu.kaydet(islem);
        kaydedici.kaydet("TRANSFER_YAPILDI: " + islemId + " | " + kaynakId + " -> " + hedefId + " | " + miktar);
        if (riskMotoru.yuksekRiskMi(miktar)) {
            isaretleSebeple(kaynakId, kaynak.getSahibiId(), "Büyük tutarlı transfer", miktar);
        } else {
            islemSonrasiRiskKontrol(kaynakId, kaynak.getSahibiId(), miktar);
        }
        otomatikKaydet();
        return true;
    }

    private void islemSonrasiRiskKontrol(String hesapId, String musteriId, double miktar) {
        if (!suphelihHesaplar.contains(hesapId) && riskMotoru.cokFazlaIslemMi(hesapId)) {
            isaretleSebeple(hesapId, musteriId,
                    "Kısa sürede çok fazla işlem (" + riskMotoru.bugunIslemSayisi(hesapId) + " işlem/gün)",
                    miktar);
        }
    }

    // ── Şüpheli hesap yönetimi ────────────────────────────────────────────────

    public void hesapIsaretle(String hesapId) {
        Account h = hesapDeposu.idIleGetir(hesapId);
        isaretleSebeple(hesapId, h != null ? h.getSahibiId() : null,
                "Yönetici tarafından şüpheli işaretlendi", 0);
        otomatikKaydet();
    }

    private void isaretleSebeple(String hesapId, String musteriId, String sebep, double miktar) {
        suphelihHesaplar.add(hesapId);
        supheSebebleri.put(hesapId, new SupheSebebi(hesapId, musteriId, sebep, miktar));
        kaydedici.kaydet("HESAP_SUPHELI: " + hesapId + " | " + sebep);
    }

    public void isaretKaldir(String hesapId) {
        suphelihHesaplar.remove(hesapId);
        supheSebebleri.remove(hesapId);
        kaydedici.kaydet("SUPHELI_KALDIRILDI: " + hesapId);
        otomatikKaydet();
    }

    public boolean suphelihMi(String hesapId) {
        return suphelihHesaplar.contains(hesapId);
    }

    @Override
    public SupheSebebi supheSebebiGetir(String hesapId) {
        return supheSebebleri.get(hesapId);
    }

    @Override
    public List<SupheSebebi> tumSupheSebebleri() {
        return new ArrayList<>(supheSebebleri.values());
    }

    // ── Limit yönetimi ────────────────────────────────────────────────────────

    @Override
    public void limitGuncelle(String hesapId, HesapLimiti limit) {
        riskMotoru.limitGuncelle(hesapId, limit);
        kaydedici.kaydet("LIMIT_GUNCELLENDI: " + hesapId);
        otomatikKaydet();
    }

    @Override
    public HesapLimiti getHesapLimiti(String hesapId) {
        return riskMotoru.getHesapLimiti(hesapId);
    }

    @Override
    public double getGunlukCekimLimiti(String hesapId) {
        return riskMotoru.getGunlukCekimLimit(hesapId);
    }

    @Override
    public double getGunlukTransferLimiti(String hesapId) {
        return riskMotoru.getGunlukTransferLimit(hesapId);
    }

    public double kalanCekimLimiti(String hesapId) { return riskMotoru.kalanCekimLimiti(hesapId); }
    public double kalanTransferLimiti(String hesapId) { return riskMotoru.kalanTransferLimiti(hesapId); }
    public HesapLimiti varsayilanLimit()             { return riskMotoru.varsayilanLimit(); }

    // ── Sorgulama ─────────────────────────────────────────────────────────────

    @Override public Customer       getMusteri(String id) { return musteriDeposu.idIleGetir(id); }
    @Override public Account        getHesap(String id)   { return hesapDeposu.idIleGetir(id); }
    @Override public List<Customer> tumMusteriler()       { return musteriDeposu.hepsiniGetir(); }
    @Override public List<Account>  tumHesaplar()         { return hesapDeposu.hepsiniGetir(); }

    @Override
    public boolean hesapMusteriyeAitMi(String hesapId, String musteriId) {
        Account h = hesapDeposu.idIleGetir(hesapId);
        return h != null && musteriId != null && musteriId.equals(h.getSahibiId());
    }

    @Override
    public List<Account> musteriHesaplari(String musteriId) {
        List<Account> sonuc = new ArrayList<>();
        for (Account h : hesapDeposu.hepsiniGetir())
            if (musteriId.equals(h.getSahibiId())) sonuc.add(h);
        return sonuc;
    }

    // ── Kalıcılık ─────────────────────────────────────────────────────────────

    private void otomatikKaydet() { durumKaydet(KAYIT_DOSYASI); }

    public void durumKaydet(String dosyaYolu) {
        BankState durum = new BankState(
                musteriDeposu.hepsiniGetir(),
                hesapDeposu.hepsiniGetir(),
                kimlikDogrulama.getKullanicilar(),
                new HashSet<>(suphelihHesaplar),
                new HashMap<>(supheSebebleri),
                new HashMap<>(riskMotoru.getOzelLimitler()),
                musteriSayaci, hesapSayaci, islemSayaci);
        Serializer.serialize(durum, dosyaYolu);
        kaydedici.kaydet("DURUM_KAYDEDILDI: " + dosyaYolu);
    }

    public void durumYukle(String dosyaYolu) {
        Object obj = Serializer.deserialize(dosyaYolu);
        if (!(obj instanceof BankState)) return;
        BankState durum = (BankState) obj;

        musteriDeposu.temizle();
        hesapDeposu.temizle();
        islemDeposu.temizle();
        suphelihHesaplar.clear();
        supheSebebleri.clear();

        for (Customer m : durum.getMusteriler()) musteriDeposu.kaydet(m);
        for (Account h : durum.getHesaplar()) {
            hesapDeposu.kaydet(h);
            for (Transaction t : h.getIslemler()) islemDeposu.kaydet(t);
        }
        if (durum.getSuphelihHesaplar() != null) suphelihHesaplar.addAll(durum.getSuphelihHesaplar());
        if (durum.getSupheSebebleri()   != null) supheSebebleri.putAll(durum.getSupheSebebleri());
        if (durum.getHesapLimitleri()   != null) riskMotoru.ozelLimitleriYukle(durum.getHesapLimitleri());
        if (durum.getKullanicilar()     != null) kimlikDogrulama.kullanicilariYukle(durum.getKullanicilar());

        musteriSayaci = durum.getMusteriSayaci();
        hesapSayaci   = durum.getHesapSayaci();
        islemSayaci   = durum.getIslemSayaci();
        kaydedici.kaydet("DURUM_YUKLENDI: " + dosyaYolu);
    }

    public FileLogger getKaydedici() { return kaydedici; }
}
