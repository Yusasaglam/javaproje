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
import java.util.concurrent.CopyOnWriteArrayList;

public class BankController implements IBankService {

    private static final String KAYIT_DOSYASI = "banka_durumu.dat";

    // ── Geri alınabilir işlem kaydı ───────────────────────────────────────────
    public static class BekleyenIslem {
        public final String islemId;
        public final String kaynakId;
        public final String hedefId;   // transfer için hedef, diğerleri null
        public final double miktar;
        public final String tip;       // "CEKIM" | "TRANSFER"
        private final java.time.LocalDateTime zaman;

        public BekleyenIslem(String islemId, String kaynakId, String hedefId,
                              double miktar, String tip) {
            this.islemId  = islemId;
            this.kaynakId = kaynakId;
            this.hedefId  = hedefId;
            this.miktar   = miktar;
            this.tip      = tip;
            this.zaman    = java.time.LocalDateTime.now();
        }

        public boolean geriAlinabilirMi() {
            return java.time.LocalDateTime.now().isBefore(zaman.plusMinutes(3));
        }

        public long kalanSaniye() {
            long gecen = java.time.temporal.ChronoUnit.SECONDS.between(
                    zaman, java.time.LocalDateTime.now());
            return Math.max(0, 180 - gecen);
        }
    }

    private final CustomerRepository       musteriDeposu;
    private final AccountRepository        hesapDeposu;
    private final TransactionRepository    islemDeposu;
    private final Set<String>              suphelihHesaplar;
    private final Map<String, SupheSebebi> supheSebebleri;
    private final RiskEngine               riskMotoru;
    private final FileLogger               kaydedici;
    private final KimlikDogrulama          kimlikDogrulama;
    private final List<RiskDinleyici>      dinleyiciler;
    private final Map<String, BekleyenIslem> bekleyenIslemler;
    private BekleyenIslem sonBekleyenIslem;
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
        this.dinleyiciler    = new CopyOnWriteArrayList<>();
        this.bekleyenIslemler = new java.util.LinkedHashMap<>();
    }

    // ── Observer yönetimi ─────────────────────────────────────────────────────

    public void dinleyiciEkle(RiskDinleyici dinleyici) {
        dinleyiciler.add(dinleyici);
    }

    public void dinleyiciKaldir(RiskDinleyici dinleyici) {
        dinleyiciler.remove(dinleyici);
    }

    private void riskYayinla(String hesapId, String musteriId,
                              RiskOlayTuru tur, double miktar, String mesaj) {
        RiskOlayi olay = new RiskOlayi(hesapId, musteriId, tur, miktar, mesaj);
        for (RiskDinleyici d : dinleyiciler) d.onRiskOlayi(olay);
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
        switch (tur) {
            case "VADELİ": case "VADELI":
                hesap = new SavingsAccount(hesapId, musteriId, baslangicBakiye, 0.03);
                break;
            case "DÖVİZ-USD":
                hesap = new DovizHesabi(hesapId, musteriId, baslangicBakiye, DovizHesabi.ParaBirimi.USD, 32.5);
                break;
            case "DÖVİZ-EUR":
                hesap = new DovizHesabi(hesapId, musteriId, baslangicBakiye, DovizHesabi.ParaBirimi.EUR, 35.2);
                break;
            case "DÖVİZ-GBP":
                hesap = new DovizHesabi(hesapId, musteriId, baslangicBakiye, DovizHesabi.ParaBirimi.GBP, 41.0);
                break;
            case "KREDİ": case "KREDI":
                // baslangicBakiye kredi limiti olarak kullanılır
                hesap = new KrediHesabi(hesapId, musteriId,
                        baslangicBakiye > 0 ? baslangicBakiye : 10_000.0, 0.02);
                break;
            default:
                hesap = new CheckingAccount(hesapId, musteriId, baslangicBakiye);
        }
        hesapDeposu.kaydet(hesap);
        musteri.hesapEkle(hesap);
        kaydedici.kaydet("HESAP_OLUSTURULDU: " + hesapId + " | " + tur + " | " + musteriId);
        otomatikKaydet();
        return hesap;
    }

    /** Tüm vadeli (SavingsAccount) hesaplara faiz uygular. */
    public int faizUygula() {
        int sayac = 0;
        for (Account h : hesapDeposu.hepsiniGetir()) {
            if (h instanceof SavingsAccount) {
                ((SavingsAccount) h).faizUygula();
                kaydedici.kaydet("FAIZ_UYGULANDI: " + h.getHesapId()
                        + " yeni_bakiye=" + h.getBakiye());
                sayac++;
            }
        }
        if (sayac > 0) otomatikKaydet();
        return sayac;
    }

    /** Tüm kredi hesaplarına aylık faiz uygular. */
    public int krediAylikFaizUygula() {
        int sayac = 0;
        for (Account h : hesapDeposu.hepsiniGetir()) {
            if (h instanceof KrediHesabi) {
                ((KrediHesabi) h).aylikFaizUygula();
                kaydedici.kaydet("KREDI_FAIZ_UYGULANDI: " + h.getHesapId()
                        + " bakiye=" + h.getBakiye());
                sayac++;
            }
        }
        if (sayac > 0) otomatikKaydet();
        return sayac;
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
        // Para yatırmada bakiye düşüşü riski yok, bakiyeOncesi=0 geçilir
        islemSonrasiRiskKontrol(hesapId, hesap.getSahibiId(), 0.0, miktar);
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
            throw new model.RiskLimitiAsildiException(
                    "Günlük çekim limiti aşıldı",
                    riskMotoru.getGunlukCekimLimit(hesapId), miktar);
        }
        String islemId = String.format("TRX%08d", ++islemSayaci);
        if (!hesap.paraCek(miktar)) {
            throw new model.YetersizBakiyeException(hesap.getBakiye(), miktar);
        }
        riskMotoru.cekimKaydet(hesapId, miktar);
        Transaction islem = new Withdraw(islemId, miktar, hesapId);
        hesap.islemEkle(islem);
        islemDeposu.kaydet(islem);
        kaydedici.kaydet("PARA_CEKILDI: " + islemId + " | " + hesapId + " | -" + miktar);
        // Geri alma penceresi
        sonBekleyenIslem = new BekleyenIslem(islemId, hesapId, null, miktar, "CEKIM");
        bekleyenIslemler.put(islemId, sonBekleyenIslem);
        islemSonrasiRiskKontrol(hesapId, hesap.getSahibiId(), hesap.getBakiye() + miktar, miktar);
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
        if (suphelihHesaplar.contains(hedefId)) {
            kaydedici.kaydet("TRANSFER_REDDEDILDI_SUPHELI_HEDEF: " + kaynakId + " -> " + hedefId);
            return false;
        }
        if (!riskMotoru.transferGecerliMi(kaynak, miktar)) {
            kaydedici.kaydet("TRANSFER_REDDEDILDI: " + kaynakId + " -> " + hedefId + " miktar=" + miktar);
            throw new model.RiskLimitiAsildiException(
                    "Transfer limiti aşıldı",
                    riskMotoru.getGunlukTransferLimit(kaynakId), miktar);
        }
        double bakiyeOncesi = kaynak.getBakiye();
        if (!kaynak.paraCek(miktar)) {
            throw new model.YetersizBakiyeException(kaynak.getBakiye(), miktar);
        }
        hedef.paraYatir(miktar);
        String islemId = String.format("TRX%08d", ++islemSayaci);
        riskMotoru.transferKaydet(kaynakId, miktar);
        Transfer islem = new Transfer(islemId, miktar, kaynakId, hedefId);
        kaynak.islemEkle(islem);
        hedef.islemEkle(islem);
        islemDeposu.kaydet(islem);
        kaydedici.kaydet("TRANSFER_YAPILDI: " + islemId + " | " + kaynakId + " -> " + hedefId + " | " + miktar);
        // Geri alma penceresi
        sonBekleyenIslem = new BekleyenIslem(islemId, kaynakId, hedefId, miktar, "TRANSFER");
        bekleyenIslemler.put(islemId, sonBekleyenIslem);
        islemSonrasiRiskKontrol(kaynakId, kaynak.getSahibiId(), bakiyeOncesi, miktar);
        otomatikKaydet();
        return true;
    }

    private void islemSonrasiRiskKontrol(String hesapId, String musteriId,
                                          double bakiyeOncesi, double miktar) {
        // Her kural puan ekler — dondurma yok, eşik aşılınca otomatik dondurur
        if (riskMotoru.yuksekRiskMi(miktar)) {
            riskMotoru.skorEkle(hesapId, 20);
            riskYayinla(hesapId, musteriId, RiskOlayTuru.YUKSEK_TUTAR, miktar,
                    "Büyük tutarlı işlem (" + String.format("%.2f", miktar) + " ₺)");
        }
        if (riskMotoru.aniDususVarMi(bakiyeOncesi, miktar)) {
            riskMotoru.skorEkle(hesapId, 15);
            riskYayinla(hesapId, musteriId, RiskOlayTuru.ANI_BAKIYE_DUSUSU, miktar,
                    "Ani bakiye düşüşü — bakiyenin %" + (int)(riskMotoru.getAniDususOrani() * 100) + "'inden fazlası");
        }
        if (riskMotoru.geceModuRisklimi(miktar)) {
            riskMotoru.skorEkle(hesapId, 15);
            riskYayinla(hesapId, musteriId, RiskOlayTuru.GECE_MODU_ISLEM, miktar,
                    "Gece saatinde (01:00-06:00) yüksek tutarlı işlem");
        }
        if (riskMotoru.kisaVadeliCokIslemMi(hesapId)) {
            riskMotoru.skorEkle(hesapId, 30);
            riskYayinla(hesapId, musteriId, RiskOlayTuru.COK_FAZLA_ISLEM, miktar,
                    "5 dk içinde yüksek işlem sıklığı (" + riskMotoru.kisaVadeliIslemSayisi(hesapId) + " işlem)");
        }
        if (riskMotoru.cokFazlaIslemMi(hesapId)) {
            riskMotoru.skorEkle(hesapId, 25);
            riskYayinla(hesapId, musteriId, RiskOlayTuru.COK_FAZLA_ISLEM, miktar,
                    "Günlük çok fazla işlem (" + riskMotoru.bugunIslemSayisi(hesapId) + " işlem)");
        }
        // Eşik aşıldıysa otomatik dondur
        if (!suphelihHesaplar.contains(hesapId)) {
            int skor = riskMotoru.getRiskSkoru(hesapId);
            if (skor >= RiskEngine.SKOR_DONDUR) {
                isaretleSebeple(hesapId, musteriId,
                        "Risk skoru " + skor + "/100 eşiği aştı — otomatik donduruldu", miktar);
            }
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
        boolean zatenSupheli = suphelihHesaplar.contains(hesapId);
        suphelihHesaplar.add(hesapId);
        supheSebebleri.put(hesapId, new SupheSebebi(hesapId, musteriId, sebep, miktar));
        kaydedici.kaydet("HESAP_SUPHELI: " + hesapId + " | " + sebep);
        if (!zatenSupheli) {
            riskYayinla(hesapId, musteriId, RiskOlayTuru.HESAP_DONDURULDU, miktar,
                    "Hesap donduruldu: " + sebep);
        }
    }

    public void isaretKaldir(String hesapId) {
        Account h = hesapDeposu.idIleGetir(hesapId);
        String musteriId = h != null ? h.getSahibiId() : null;
        suphelihHesaplar.remove(hesapId);
        supheSebebleri.remove(hesapId);
        kaydedici.kaydet("SUPHELI_KALDIRILDI: " + hesapId);
        riskYayinla(hesapId, musteriId, RiskOlayTuru.SUPHELI_KALDIRILDI, 0,
                "Şüpheli işaret kaldırıldı: " + hesapId);
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

    /** Otomatik kayıt devre dışı — kullanıcı "Durumu Kaydet" butonuna basmalıdır. */
    private void otomatikKaydet() { /* kayıt manuel */ }

    public void durumKaydet(String dosyaYolu) {
        BankState durum = new BankState(
                musteriDeposu.hepsiniGetir(),
                hesapDeposu.hepsiniGetir(),
                kimlikDogrulama.getKullanicilar(),
                new HashSet<>(suphelihHesaplar),
                new HashMap<>(supheSebebleri),
                new HashMap<>(riskMotoru.getOzelLimitler()),
                new HashMap<>(riskMotoru.getRiskSkorlari()),
                new HashMap<>(riskMotoru.getSkorGuncelleme()),
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
        riskMotoru.riskSkorlariniYukle(durum.getRiskSkorlari(), durum.getSkorGuncelleme());

        musteriSayaci = durum.getMusteriSayaci();
        hesapSayaci   = durum.getHesapSayaci();
        islemSayaci   = durum.getIslemSayaci();
        kaydedici.kaydet("DURUM_YUKLENDI: " + dosyaYolu);
    }

    public FileLogger getKaydedici() { return kaydedici; }

    /** Vadeli hesabın faiz oranını günceller. */
    public boolean faizOraniGuncelle(String hesapId, double yeniOran) {
        Account h = hesapDeposu.idIleGetir(hesapId);
        if (!(h instanceof SavingsAccount)) return false;
        ((SavingsAccount) h).setFaizOrani(yeniOran);
        kaydedici.kaydet("FAIZ_ORANI_GUNCELLENDI: " + hesapId + " oran=" + yeniOran);
        otomatikKaydet();
        return true;
    }

    /** Döviz hesabının kurunu günceller. */
    public boolean dovizKuruGuncelle(String hesapId, double yeniKur) {
        Account h = hesapDeposu.idIleGetir(hesapId);
        if (!(h instanceof DovizHesabi)) return false;
        ((DovizHesabi) h).dovizKuruGuncelle(yeniKur);
        kaydedici.kaydet("DOVIZ_KURU_GUNCELLENDI: " + hesapId + " kur=" + yeniKur);
        otomatikKaydet();
        return true;
    }

    /** Kullanıcının şifresini değiştirir (mevcut şifre doğrulamasıyla). */
    public boolean sifreDegistir(String kullaniciAdi, String eskiSifre, String yeniSifre) {
        boolean sonuc = kimlikDogrulama.sifreDegistir(kullaniciAdi, eskiSifre, yeniSifre);
        if (sonuc) otomatikKaydet();
        return sonuc;
    }

    /**
     * Kaynak hesaptan kredi hesabına ödeme yapar.
     * @return ödenen gerçek miktar (borca göre kırpılır)
     */
    public double krediOde(String kaynakHesapId, String krediHesapId, double miktar) {
        Account kaynak = hesapDeposu.idIleGetir(kaynakHesapId);
        Account hedef  = hesapDeposu.idIleGetir(krediHesapId);
        if (kaynak == null || !(hedef instanceof KrediHesabi)) return -1;
        KrediHesabi kh = (KrediHesabi) hedef;
        if (kh.getBakiye() >= 0) return 0; // borç yok
        if (kaynak.getBakiye() < miktar) throw new model.YetersizBakiyeException(kaynak.getBakiye(), miktar);
        kaynak.paraCek(miktar);
        double gercekOdeme = kh.krediOde(miktar);
        String islemId = String.format("TRX%08d", ++islemSayaci);
        // İşlem geçmişine yaz: kaynak hesaptan çekim, kredi hesabına yatırma
        Transaction cekimIslem = new Withdraw(islemId, gercekOdeme, kaynakHesapId);
        Transaction yatirmaIslem = new Deposit(islemId, gercekOdeme, krediHesapId);
        kaynak.islemEkle(cekimIslem);
        hedef.islemEkle(yatirmaIslem);
        islemDeposu.kaydet(cekimIslem);
        islemDeposu.kaydet(yatirmaIslem);
        kaydedici.kaydet("KREDI_ODEME: " + islemId + " | " + kaynakHesapId
                + " -> " + krediHesapId + " | " + gercekOdeme);
        islemSonrasiRiskKontrol(kaynakHesapId, kaynak.getSahibiId(), kaynak.getBakiye() + miktar, miktar);
        return gercekOdeme;
    }

    /** Kullanıcının şifresini admin tarafından sıfırlar (eski şifre gerekmez). */
    public boolean sifreSifirla(String kullaniciAdi, String yeniSifre) {
        return kimlikDogrulama.sifreSifirla(kullaniciAdi, yeniSifre);
    }

    /** Kredi hesabının limitini günceller. */
    public boolean krediLimitiGuncelle(String hesapId, double yeniLimit) {
        Account h = hesapDeposu.idIleGetir(hesapId);
        if (!(h instanceof KrediHesabi)) return false;
        ((KrediHesabi) h).setKrediLimiti(yeniLimit);
        kaydedici.kaydet("KREDI_LIMITI_GUNCELLENDI: " + hesapId + " limit=" + yeniLimit);
        otomatikKaydet();
        return true;
    }

    /** Bot simülasyonu için gece modunu zorla açar/kapatır. */
    public void simuleGeceModuAktifEt(boolean aktif) { riskMotoru.setSimuleGeceModu(aktif); }

    // ── Risk Skoru ────────────────────────────────────────────────────────────

    public int    getRiskSkoru(String hesapId)    { return riskMotoru.getRiskSkoru(hesapId); }
    public String getRiskSeviyesi(String hesapId) { return riskMotoru.getRiskSeviyesi(hesapId); }
    public void   skorEkleDemo(String hesapId, int puan)         { riskMotoru.skorEkle(hesapId, puan); }
    public boolean geceModuMu()                                  { return riskMotoru.geceModuMu(); }
    public boolean kisaVadeliCokIslemMi(String hesapId)          { return riskMotoru.kisaVadeliCokIslemMi(hesapId); }
    public boolean cokFazlaIslemMi(String hesapId)               { return riskMotoru.cokFazlaIslemMi(hesapId); }
    public int     bugunIslemSayisi(String hesapId)              { return riskMotoru.bugunIslemSayisi(hesapId); }
    public long    kisaVadeliIslemSayisi(String hesapId)         { return riskMotoru.kisaVadeliIslemSayisi(hesapId); }

    /**
     * İşlem öncesi risk kontrolü — UI, kullanıcıya onay dialogu göstermek için kullanır.
     * @return boş liste = risk yok; dolu liste = kullanıcıya gösterilecek risk sebepleri
     */
    public java.util.List<String> riskKontrolEt(String hesapId, double miktar) {
        java.util.List<String> riskler = new java.util.ArrayList<>();
        int skor = riskMotoru.getRiskSkoru(hesapId);
        if (riskMotoru.yuksekRiskMi(miktar))
            riskler.add(String.format("Büyük tutarlı işlem: %,.0f ₺", miktar));
        if (riskMotoru.geceModuMu() && miktar >= 10_000)
            riskler.add("Gece saati (01:00-06:00) yüksek tutarlı işlem");
        if (riskMotoru.kisaVadeliCokIslemMi(hesapId))
            riskler.add("Son 5 dk içinde " + riskMotoru.kisaVadeliIslemSayisi(hesapId) + " işlem yapıldı");
        if (skor >= RiskEngine.SKOR_RISKLI)
            riskler.add("Hesap risk skoru yüksek: " + skor + "/100");
        return riskler;
    }

    // ── Geri Alma ─────────────────────────────────────────────────────────────

    /** Son başarılı işlemin geri alma kaydını döner (UI için). */
    public BekleyenIslem getSonBekleyenIslem() { return sonBekleyenIslem; }

    /**
     * İşlemi geri alır. 3 dakika içinde çağrılmalıdır.
     * @return geri alınan işlem; null ise süre dolmuş veya işlem bulunamadı
     */
    public BekleyenIslem geriAl(String islemId) {
        BekleyenIslem bekleyen = bekleyenIslemler.get(islemId);
        if (bekleyen == null || !bekleyen.geriAlinabilirMi()) return null;
        try {
            if ("CEKIM".equals(bekleyen.tip)) {
                Account h = hesapDeposu.idIleGetir(bekleyen.kaynakId);
                if (h != null) h.paraYatir(bekleyen.miktar);
            } else if ("TRANSFER".equals(bekleyen.tip)) {
                Account kaynak = hesapDeposu.idIleGetir(bekleyen.kaynakId);
                Account hedef  = hesapDeposu.idIleGetir(bekleyen.hedefId);
                if (kaynak != null && hedef != null && hedef.getBakiye() >= bekleyen.miktar) {
                    hedef.paraCek(bekleyen.miktar);
                    kaynak.paraYatir(bekleyen.miktar);
                }
            }
            bekleyenIslemler.remove(islemId);
            if (sonBekleyenIslem != null && sonBekleyenIslem.islemId.equals(islemId))
                sonBekleyenIslem = null;
            kaydedici.kaydet("ISLEM_GERI_ALINDI: " + islemId + " | " + bekleyen.tip + " | " + bekleyen.miktar);
            otomatikKaydet();
            return bekleyen;
        } catch (Exception e) {
            return null;
        }
    }

    /** Kredi hesabının aylık faiz oranını günceller. */
    public boolean krediFaizOraniGuncelle(String hesapId, double yeniOran) {
        Account h = hesapDeposu.idIleGetir(hesapId);
        if (!(h instanceof KrediHesabi)) return false;
        ((KrediHesabi) h).setFaizOrani(yeniOran);
        kaydedici.kaydet("KREDI_FAIZ_GUNCELLENDI: " + hesapId + " oran=" + yeniOran);
        otomatikKaydet();
        return true;
    }
}
