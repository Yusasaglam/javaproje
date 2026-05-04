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
    private final Set<String>              demoMusteri;
    private final AktiviteLogServisi       logServisi;
    private final Map<String, BekleyenLimitDegisimi> bekleyenLimitler;
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
        this.demoMusteri      = new HashSet<>();
        this.logServisi       = new AktiviteLogServisi();
        this.bekleyenLimitler = new HashMap<>();
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
        logEkle(id, null, ActivityLog.IslemTipi.MUSTERI_OLUSTURMA, 0, 0, 0, 0, "", "Yeni müşteri: " + ad);
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
        logEkle(musteriId, hesapId, ActivityLog.IslemTipi.HESAP_OLUSTURMA, baslangicBakiye,
                0, 0, 0, "", "Hesap oluşturuldu: " + tur);
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
        if (suphelihHesaplar.contains(hesapId)) {
            kaydedici.kaydet("PARA_YATIRMA_ENGELLENDI_SUPHELI: " + hesapId);
            return false;
        }
        bekleyenLimitleriKontrolEt(hesapId);
        if (!riskMotoru.paraYatirmaGecerliMi(miktar)) {
            kaydedici.kaydet("PARA_YATIRMA_REDDEDILDI: " + hesapId + " miktar=" + miktar);
            return false;
        }
        int riskOncesi = getRiskSkoru(hesapId);
        String islemId = String.format("TRX%08d", ++islemSayaci);
        hesap.paraYatir(miktar);
        Transaction islem = new Deposit(islemId, miktar, hesapId);
        hesap.islemEkle(islem);
        islemDeposu.kaydet(islem);
        riskMotoru.yatirmaKaydet(hesapId, hesap.getSahibiId());
        kaydedici.kaydet("PARA_YATIRILDI: " + islemId + " | " + hesapId + " | +" + miktar);
        String kurallar = islemSonrasiRiskKontrol(hesapId, hesap.getSahibiId(), 0.0, miktar, islemId, IslemRiskAgirlik.PARA_YATIRMA, null);
        int riskSonrasi = getRiskSkoru(hesapId);
        logEkle(hesap.getSahibiId(), hesapId, ActivityLog.IslemTipi.PARA_YATIRMA, miktar,
                riskOncesi, riskSonrasi - riskOncesi, riskSonrasi, kurallar,
                String.format("Para yatırma: %,.2f ₺", miktar));
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
        bekleyenLimitleriKontrolEt(hesapId);
        if (!riskMotoru.paraCekmeGecerliMi(hesap, miktar)) {
            kaydedici.kaydet("PARA_CEKME_REDDEDILDI: " + hesapId + " miktar=" + miktar);
            throw new model.RiskLimitiAsildiException(
                    "Günlük çekim limiti aşıldı",
                    riskMotoru.getGunlukCekimLimit(hesapId), miktar);
        }
        boolean suphelihOncesi = suphelihHesaplar.contains(hesapId);
        int riskOncesi = getRiskSkoru(hesapId);
        String islemId = String.format("TRX%08d", ++islemSayaci);
        if (!hesap.paraCek(miktar)) {
            throw new model.YetersizBakiyeException(hesap.getBakiye(), miktar);
        }
        riskMotoru.cekimKaydet(hesapId, miktar, hesap.getSahibiId());
        Transaction islem = new Withdraw(islemId, miktar, hesapId);
        hesap.islemEkle(islem);
        islemDeposu.kaydet(islem);
        kaydedici.kaydet("PARA_CEKILDI: " + islemId + " | " + hesapId + " | -" + miktar);
        sonBekleyenIslem = null;
        String kurallar = islemSonrasiRiskKontrol(hesapId, hesap.getSahibiId(), hesap.getBakiye() + miktar, miktar, islemId, IslemRiskAgirlik.NAKIT_CEKIM, null);
        // Eğer bu işlem hesabı dondurdu ise parayı iade et ve işlemi iptal et
        if (!suphelihOncesi && suphelihHesaplar.contains(hesapId)) {
            hesap.paraYatir(miktar);
            hesap.getIslemler().remove(islem);
            kaydedici.kaydet("PARA_CEKME_BLOKE: " + islemId + " | risk dondurma tetiklendi, işlem iptal");
            otomatikKaydet();
            return false;
        }
        int riskSonrasi = getRiskSkoru(hesapId);
        logEkle(hesap.getSahibiId(), hesapId, ActivityLog.IslemTipi.PARA_CEKME, miktar,
                riskOncesi, riskSonrasi - riskOncesi, riskSonrasi, kurallar,
                String.format("Para çekme: %,.2f ₺", miktar));
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
        bekleyenLimitleriKontrolEt(kaynakId);
        if (!riskMotoru.transferGecerliMi(kaynak, miktar)) {
            kaydedici.kaydet("TRANSFER_REDDEDILDI: " + kaynakId + " -> " + hedefId + " miktar=" + miktar);
            throw new model.RiskLimitiAsildiException(
                    "Transfer limiti aşıldı",
                    riskMotoru.getGunlukTransferLimit(kaynakId), miktar);
        }
        boolean suphelihOncesi = suphelihHesaplar.contains(kaynakId);
        int riskOncesi = getRiskSkoru(kaynakId);
        double bakiyeOncesi = kaynak.getBakiye();
        if (!kaynak.paraCek(miktar)) {
            throw new model.YetersizBakiyeException(kaynak.getBakiye(), miktar);
        }
        // Para kaynaktan düşer ama hedefe henüz GİTMEZ — onay bekler
        String islemId = String.format("TRX%08d", ++islemSayaci);
        riskMotoru.transferKaydet(kaynakId, miktar, kaynak.getSahibiId());
        Transfer islem = new Transfer(islemId, miktar, kaynakId, hedefId);
        kaynak.islemEkle(islem);
        islemDeposu.kaydet(islem);
        kaydedici.kaydet("TRANSFER_BEKLEMEDE: " + islemId + " | " + kaynakId + " -> " + hedefId + " | " + miktar);
        sonBekleyenIslem = new BekleyenIslem(islemId, kaynakId, hedefId, miktar, "BEKLEYEN_TRANSFER");
        bekleyenIslemler.put(islemId, sonBekleyenIslem);
        String kurallar = islemSonrasiRiskKontrol(kaynakId, kaynak.getSahibiId(), bakiyeOncesi, miktar, islemId, IslemRiskAgirlik.DIS_TRANSFER, hedefId);
        // Risk dondurma tetiklendiyse parayı kaynağa iade et (hedefe hiç gitmedi)
        if (!suphelihOncesi && suphelihHesaplar.contains(kaynakId)) {
            kaynak.paraYatir(miktar);
            riskMotoru.transferGeriAl(kaynakId, miktar);
            kaynak.getIslemler().remove(islem);
            bekleyenIslemler.remove(islemId);
            sonBekleyenIslem = null;
            kaydedici.kaydet("TRANSFER_BLOKE: " + islemId + " | risk dondurma tetiklendi, işlem iptal");
            otomatikKaydet();
            return false;
        }
        int riskSonrasi = getRiskSkoru(kaynakId);
        logEkle(kaynak.getSahibiId(), kaynakId, ActivityLog.IslemTipi.TRANSFER, miktar,
                riskOncesi, riskSonrasi - riskOncesi, riskSonrasi, kurallar,
                String.format("Transfer beklemede: %,.2f ₺ → %s", miktar, hedefId));
        otomatikKaydet();
        return true;
    }

    private String islemSonrasiRiskKontrol(String hesapId, String musteriId,
                                            double bakiyeOncesi, double miktar,
                                            String islemId, IslemRiskAgirlik agirlik,
                                            String hedefHesapId) {
        List<String> tetiklenen = new ArrayList<>();
        Account hesap = hesapDeposu.idIleGetir(hesapId);

        // ── KURAL 1: Kart hırsızlığı / hesap ele geçirme — 10 dk'da 3+ çekim/transfer VE toplam ≥25K
        // Sıradan maaş çekimi (tek seferlik büyük çekim) bunu TETIKLEMEZ.
        // Tetiklemesi için hırsız gibi art arda birden fazla işlem gerekir.
        boolean k1Tetiklendi = false;
        if ((agirlik == IslemRiskAgirlik.NAKIT_CEKIM || agirlik == IslemRiskAgirlik.DIS_TRANSFER)
                && riskMotoru.hizliBoşaltmaMi(hesapId)) {
            riskMotoru.skorEkle(islemId, hesapId, musteriId, 40, agirlik);
            riskYayinla(hesapId, musteriId, RiskOlayTuru.COK_FAZLA_ISLEM, miktar,
                    String.format("Hızlı hesap boşaltma: 10 dk'da %d çekim, toplam %.0f ₺",
                            riskMotoru.kisaVadeliCekimAdedi(hesapId), riskMotoru.kisaVadeliToplamCekim(hesapId)));
            tetiklenen.add("Hızlı Boşaltma");
            k1Tetiklendi = true;
        }

        // ── KURAL 2: Gece saati çekimi — uyurken çalınan kart ──────────────────
        // 01:00-06:00 arası ≥5K: normal insan bu saatte ATM'ye gitmez. Gece başına bir kez tetiklenir.
        if (riskMotoru.geceModuTetiklensinMi(hesapId, miktar)) {
            riskMotoru.skorEkle(islemId, hesapId, musteriId, 20, agirlik);
            riskYayinla(hesapId, musteriId, RiskOlayTuru.GECE_MODU_ISLEM, miktar,
                    "Gece saati (01:00-06:00) yüksek tutarlı işlem — çalınan kart şüphesi");
            tetiklenen.add("Gece Çekimi");
        }

        // ── KURAL 3: Ani hesap boşaltma — tek işlemde bakiyenin %90'ı, min 10K ─
        // K1 zaten tetiklendiyse bu aynı olayın tekrar sayılması demektir — atla.
        // K1 = art arda çoklu işlem, K3 = tek seferlik büyük çekim; farklı senaryolar.
        if (!k1Tetiklendi && riskMotoru.aniDususVarMi(bakiyeOncesi, miktar)) {
            riskMotoru.skorEkle(islemId, hesapId, musteriId, 25, agirlik);
            riskYayinla(hesapId, musteriId, RiskOlayTuru.ANI_BAKIYE_DUSUSU, miktar,
                    String.format("Bakiyenin %%%.0f'i tek işlemde çekildi (%.0f ₺ / %.0f ₺)",
                            riskMotoru.getAniDususOrani() * 100, miktar, bakiyeOncesi));
            tetiklenen.add("Ani Boşalma");
        }

        // ── KURAL 4: Yapılandırma — kara para aklama tekniği ───────────────────
        // Eşiğin hemen altında (12.750-14.999 ₺) 3+ ardışık işlem.
        // Normal harcama bu kalıbı oluşturmaz.
        if (hesap != null && riskMotoru.yapilandirmaVarMi(hesap)) {
            riskMotoru.skorEkle(islemId, hesapId, musteriId, 20, IslemRiskAgirlik.DAVRANISSAL);
            riskYayinla(hesapId, musteriId, RiskOlayTuru.YUKSEK_TUTAR, miktar,
                    "Yapılandırma şüphesi: eşik altında ardışık işlem kalıbı");
            tetiklenen.add("Yapılandırma");
        }

        // ── KURAL 5: Müşteri velocity — hesap ele geçirildi senaryosu ──────────
        // Eşimi aşım miktarına göre kademeli ceza: 1-5 aşım→+20, 6-10→+35, 10+→+50. 5 dk'da bir kez.
        if (riskMotoru.k5TetiklensinMi(musteriId)) {
            long asim = riskMotoru.kisaVadeliMusteriIslemSayisi(musteriId)
                        - riskMotoru.getKullaniciKategorisi(musteriId).velocityEsigi;
            int ceza = asim <= 5 ? 20 : asim <= 10 ? 35 : 50;
            riskMotoru.skorEkle(islemId, hesapId, musteriId, ceza, IslemRiskAgirlik.DAVRANISSAL);
            riskYayinla(hesapId, musteriId, RiskOlayTuru.COK_FAZLA_ISLEM, miktar,
                    "Anormal işlem hızı: 5 dk'da " + riskMotoru.kisaVadeliMusteriIslemSayisi(musteriId) + " işlem");
            tetiklenen.add("Anormal Hız");
        }

        // ── KURAL 6: Günlük işlem sayısı — bot/otomatik saldırı ────────────────
        if (riskMotoru.cokFazlaIslemMi(hesapId)) {
            riskMotoru.skorEkle(islemId, hesapId, musteriId, 20, IslemRiskAgirlik.DAVRANISSAL);
            riskYayinla(hesapId, musteriId, RiskOlayTuru.COK_FAZLA_ISLEM, miktar,
                    "Günde " + riskMotoru.bugunIslemSayisi(hesapId) + " işlem — bot şüphesi");
            tetiklenen.add("Günlük Limit");
        }

        // ── KURAL 7: Yeni alıcıya büyük transfer — hesap ele geçirme tespiti ────
        // Daha önce para gönderilmemiş hesaba büyük para gidiyorsa şüpheli.
        // aliciKaydet BURADA ÇAĞRILMAZ — transfer onaylanınca transferOnayla() çağırır.
        // Böylece iptal edilen transferler alıcıyı "bilinen" yapmaz.
        if (hedefHesapId != null && riskMotoru.yeniAliciMi(musteriId, hedefHesapId)) {
            if (miktar >= RiskEngine.YENI_ALICI_BUYUK_ESIK) {
                riskMotoru.skorEkle(islemId, hesapId, musteriId, 30, IslemRiskAgirlik.DIS_TRANSFER);
                riskYayinla(hesapId, musteriId, RiskOlayTuru.YUKSEK_TUTAR, miktar,
                        String.format("Bilinmeyen hesaba büyük transfer: %.0f ₺", miktar));
                tetiklenen.add("Yeni Alıcı Büyük Transfer");
            } else if (miktar >= RiskEngine.YENI_ALICI_ORTA_ESIK) {
                riskMotoru.skorEkle(islemId, hesapId, musteriId, 12, IslemRiskAgirlik.DIS_TRANSFER);
                tetiklenen.add("Yeni Alıcı");
            }
        }

        // ── Otomatik dondurma ────────────────────────────────────────────────────
        if (!suphelihHesaplar.contains(hesapId)) {
            int skor = riskMotoru.getRiskSkoru(hesapId, musteriId);
            if (skor >= RiskEngine.SKOR_DONDUR) {
                isaretleSebeple(hesapId, musteriId,
                        "Risk skoru " + skor + "/100 — otomatik donduruldu", miktar,
                        DondurmaSecegi.OTOMATIK);
                tetiklenen.add("Oto. Dondurma");
            }
        }
        return String.join(", ", tetiklenen);
    }

    // ── Şüpheli hesap yönetimi ────────────────────────────────────────────────

    public void hesapIsaretle(String hesapId) {
        Account h = hesapDeposu.idIleGetir(hesapId);
        String musteriId = h != null ? h.getSahibiId() : null;
        int skor = getRiskSkoru(hesapId);
        isaretleSebeple(hesapId, musteriId, "Yönetici tarafından şüpheli işaretlendi", 0, DondurmaSecegi.MANUEL);
        logEkle(musteriId, hesapId, ActivityLog.IslemTipi.HESAP_DONDURMA, 0,
                skor, 0, skor, "MANUEL", "Yönetici tarafından donduruldu");
        otomatikKaydet();
    }

    private void isaretleSebeple(String hesapId, String musteriId, String sebep, double miktar, DondurmaSecegi sekil) {
        boolean zatenSupheli = suphelihHesaplar.contains(hesapId);
        suphelihHesaplar.add(hesapId);
        supheSebebleri.put(hesapId, new SupheSebebi(hesapId, musteriId, sebep, miktar));
        riskMotoru.dondurmaKaydet(hesapId, sekil, sebep);
        kaydedici.kaydet("HESAP_SUPHELI: " + hesapId + " | " + sebep);
        if (!zatenSupheli) {
            riskYayinla(hesapId, musteriId, RiskOlayTuru.HESAP_DONDURULDU, miktar,
                    "Hesap donduruldu: " + sebep);
        }
    }

    public void isaretKaldir(String hesapId) {
        Account h = hesapDeposu.idIleGetir(hesapId);
        String musteriId = h != null ? h.getSahibiId() : null;
        int skor = getRiskSkoru(hesapId);
        suphelihHesaplar.remove(hesapId);
        supheSebebleri.remove(hesapId);
        riskMotoru.dondurmaKaldir(hesapId);
        logEkle(musteriId, hesapId, ActivityLog.IslemTipi.HESAP_COZ, 0,
                skor, 0, skor, "", "Dondurma kaldırıldı");
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

    public void basarisizGirisKaydet(String kullaniciAdi) {
        Kullanici k = kimlikDogrulama.getKullanicilar().get(kullaniciAdi);
        if (k == null || k.getMusteriId() == null) return;
        riskMotoru.basarisizGirisKaydet(k.getMusteriId());
    }

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

    // ── Silme işlemleri ───────────────────────────────────────────────────────

    public boolean musteriSil(String musteriId) {
        Customer m = musteriDeposu.idIleGetir(musteriId);
        if (m == null) return false;
        for (Account h : new ArrayList<>(m.getHesaplar())) {
            hesapDeposu.sil(h.getHesapId());
            suphelihHesaplar.remove(h.getHesapId());
            supheSebebleri.remove(h.getHesapId());
        }
        musteriDeposu.sil(musteriId);
        demoMusteri.remove(musteriId);
        String kulAdiSil = null;
        for (Kullanici k : kimlikDogrulama.getKullanicilar().values()) {
            if (musteriId.equals(k.getMusteriId())) { kulAdiSil = k.getKullaniciAdi(); break; }
        }
        if (kulAdiSil != null) kimlikDogrulama.getKullanicilar().remove(kulAdiSil);
        kaydedici.kaydet("MUSTERI_SILINDI: " + musteriId);
        durumKaydet(KAYIT_DOSYASI);
        return true;
    }

    public boolean hesapSil(String hesapId) {
        Account h = hesapDeposu.idIleGetir(hesapId);
        if (h == null) return false;
        Customer m = musteriDeposu.idIleGetir(h.getSahibiId());
        if (m != null) m.getHesaplar().remove(h);
        hesapDeposu.sil(hesapId);
        suphelihHesaplar.remove(hesapId);
        supheSebebleri.remove(hesapId);
        kaydedici.kaydet("HESAP_SILINDI: " + hesapId);
        durumKaydet(KAYIT_DOSYASI);
        return true;
    }

    // ── Demo veri takibi ──────────────────────────────────────────────────────

    public void demoMusteriIsaretle(String musteriId) { demoMusteri.add(musteriId); }
    public boolean isDemoMusteri(String musteriId)    { return demoMusteri.contains(musteriId); }

    // ── Kalıcılık ─────────────────────────────────────────────────────────────

    private volatile boolean botModuAktif = false;

    public void botModuBaslat() { botModuAktif = true; }
    public void botModuBitir()  { botModuAktif = false; }

    private void otomatikKaydet() {
        if (!botModuAktif) durumKaydet(KAYIT_DOSYASI);
    }

    public void durumKaydet(String dosyaYolu) {
        BankState durum = new BankState(
                musteriDeposu.hepsiniGetir(),
                hesapDeposu.hepsiniGetir(),
                kimlikDogrulama.getKullanicilar(),
                new HashSet<>(suphelihHesaplar),
                new HashMap<>(supheSebebleri),
                new HashMap<>(riskMotoru.getOzelLimitler()),
                new HashSet<>(demoMusteri),
                new ArrayList<>(riskMotoru.getOlayKayitlari()),
                new HashMap<>(riskMotoru.getMusteriProfilleri()),
                new HashMap<>(riskMotoru.getDondurmaKayitlari()),
                new HashMap<>(riskMotoru.getKullaniciKategorileri()),
                new ArrayList<>(logServisi.getLoglar()),
                new HashMap<>(riskMotoru.getBilinenAlicilar()),
                new HashMap<>(bekleyenLimitler),
                new HashMap<>(riskMotoru.getGunlukTransferler()),
                new HashMap<>(riskMotoru.getGunlukTransferTarihleri()),
                new HashMap<>(riskMotoru.getGunlukCekimler()),
                new HashMap<>(riskMotoru.getGunlukCekimTarihleri()),
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
        riskMotoru.riskVerileriniYukle(
                durum.getRiskOlayKayitlari(),
                durum.getMusteriProfilleri(),
                durum.getDondurmaKayitlari(),
                durum.getKullaniciKategorileri());
        logServisi.loklarYukle(durum.getAktiviteLoglari());
        riskMotoru.alicilariYukle(durum.getBilinenAlicilar());
        bekleyenLimitler.clear();
        if (durum.getBekleyenLimitler() != null) bekleyenLimitler.putAll(durum.getBekleyenLimitler());
        riskMotoru.gunlukVerileriYukle(
                durum.getGunlukTransferler(),
                durum.getGunlukTransferTarihleri(),
                durum.getGunlukCekimler(),
                durum.getGunlukCekimTarihleri());

        demoMusteri.clear();
        if (durum.getDemoMusteriler() != null) demoMusteri.addAll(durum.getDemoMusteriler());

        // Demo müşterileri ve hesaplarını temizle — her açılışta taze başlar
        Set<String> demoHesapIdleri = new HashSet<>();
        for (String demId : new HashSet<>(demoMusteri)) {
            Customer dm = musteriDeposu.idIleGetir(demId);
            if (dm != null) {
                for (Account dh : new ArrayList<>(dm.getHesaplar())) {
                    demoHesapIdleri.add(dh.getHesapId());
                    hesapDeposu.sil(dh.getHesapId());
                    suphelihHesaplar.remove(dh.getHesapId());
                    supheSebebleri.remove(dh.getHesapId());
                }
            }
            musteriDeposu.sil(demId);
            String kulAdiSil = null;
            for (Kullanici k : kimlikDogrulama.getKullanicilar().values()) {
                if (demId.equals(k.getMusteriId())) { kulAdiSil = k.getKullaniciAdi(); break; }
            }
            if (kulAdiSil != null) kimlikDogrulama.getKullanicilar().remove(kulAdiSil);
        }
        if (!demoHesapIdleri.isEmpty()) riskMotoru.hesapRiskOlaylariniSil(demoHesapIdleri);
        riskMotoru.musteriProfilleriniSil(new HashSet<>(demoMusteri));
        demoMusteri.clear();
        // Demo kaynaklı aktivite loglarını da temizle
        final Set<String> demoIdleri = new HashSet<>(demoHesapIdleri);
        demoIdleri.addAll(demoMusteri);  // empty at this point but kept for clarity
        logServisi.loklarYukle(logServisi.getLoglar().stream()
                .filter(l -> l.kaynak != ActivityLog.Kaynak.DEMO)
                .collect(java.util.stream.Collectors.toList()));

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
        if (kh.getBakiye() >= 0) return 0;
        if (kaynak.getBakiye() < miktar) throw new model.YetersizBakiyeException(kaynak.getBakiye(), miktar);
        bekleyenLimitleriKontrolEt(kaynakHesapId);
        int riskOncesi = getRiskSkoru(kaynakHesapId);
        kaynak.paraCek(miktar);
        double gercekOdeme = kh.krediOde(miktar);
        String islemId = String.format("TRX%08d", ++islemSayaci);
        Transaction cekimIslem = new Withdraw(islemId, gercekOdeme, kaynakHesapId);
        Transaction yatirmaIslem = new Deposit(islemId, gercekOdeme, krediHesapId);
        kaynak.islemEkle(cekimIslem);
        hedef.islemEkle(yatirmaIslem);
        islemDeposu.kaydet(cekimIslem);
        islemDeposu.kaydet(yatirmaIslem);
        kaydedici.kaydet("KREDI_ODEME: " + islemId + " | " + kaynakHesapId
                + " -> " + krediHesapId + " | " + gercekOdeme);
        String kurallar = islemSonrasiRiskKontrol(kaynakHesapId, kaynak.getSahibiId(), kaynak.getBakiye() + miktar, miktar, islemId, IslemRiskAgirlik.KREDI_CEKIM, null);
        int riskSonrasi = getRiskSkoru(kaynakHesapId);
        logEkle(kaynak.getSahibiId(), kaynakHesapId, ActivityLog.IslemTipi.KREDI_ODEME, gercekOdeme,
                riskOncesi, riskSonrasi - riskOncesi, riskSonrasi, kurallar,
                String.format("Kredi ödemesi: %,.2f ₺ → %s", gercekOdeme, krediHesapId));
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

    public int getRiskSkoru(String hesapId) {
        Account h = hesapDeposu.idIleGetir(hesapId);
        String musteriId = h != null ? h.getSahibiId() : null;
        int skor = riskMotoru.getRiskSkoru(hesapId, musteriId);
        if (skor < RiskEngine.SKOR_OTOMATIK_COZ && suphelihHesaplar.contains(hesapId)
                && riskMotoru.getDondurmaSecegi(hesapId) == DondurmaSecegi.OTOMATIK) {
            suphelihHesaplar.remove(hesapId);
            supheSebebleri.remove(hesapId);
            riskMotoru.dondurmaKaldir(hesapId);
            kaydedici.kaydet("OTOMATIK_DONDURMA_COZULDU: " + hesapId + " skor=" + skor);
        }
        return skor;
    }
    public String getRiskSeviyesi(String hesapId) { return riskMotoru.getRiskSeviyesi(hesapId); }
    public void   skorEkleDemo(String hesapId, int puan)              { riskMotoru.skorEkle(hesapId, puan); }
    public boolean geceModuMu()                                       { return riskMotoru.geceModuMu(); }
    public boolean kisaVadeliCokIslemMi(String hesapId)               { return riskMotoru.kisaVadeliCokIslemMi(hesapId); }
    public boolean kisaVadeliCokIslemMiMusteri(String musteriId)      { return riskMotoru.kisaVadeliCokIslemMiMusteri(musteriId); }
    public boolean cokFazlaIslemMi(String hesapId)                    { return riskMotoru.cokFazlaIslemMi(hesapId); }
    public int     bugunIslemSayisi(String hesapId)                   { return riskMotoru.bugunIslemSayisi(hesapId); }
    public long    kisaVadeliIslemSayisi(String hesapId)              { return riskMotoru.kisaVadeliIslemSayisi(hesapId); }
    public long    kisaVadeliMusteriIslemSayisi(String musteriId)     { return riskMotoru.kisaVadeliMusteriIslemSayisi(musteriId); }
    public double  getYuksekRiskEsigi()                               { return riskMotoru.getYuksekRiskEsigi(); }
    public DondurmaSecegi      getDondurmaSecegi(String hesapId)      { return riskMotoru.getDondurmaSecegi(hesapId); }
    public void kullaniciKategorisiAta(String musteriId, KullaniciKategorisi kat) { riskMotoru.kullaniciKategorisiAta(musteriId, kat); }
    public KullaniciKategorisi getKullaniciKategorisi(String musteriId) { return riskMotoru.getKullaniciKategorisi(musteriId); }

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
            if ("BEKLEYEN_TRANSFER".equals(bekleyen.tip)) {
                // Para zaten hedefe gitmedi — sadece kaynağa iade et
                Account kaynak = hesapDeposu.idIleGetir(bekleyen.kaynakId);
                if (kaynak != null) {
                    kaynak.paraYatir(bekleyen.miktar);
                    riskMotoru.transferGeriAl(bekleyen.kaynakId, bekleyen.miktar);
                    riskMotoru.islemRiskOlaylariniIptalEt(islemId);
                    int skorSonra = getRiskSkoru(bekleyen.kaynakId);
                    logEkle(kaynak.getSahibiId(), bekleyen.kaynakId, ActivityLog.IslemTipi.GERI_AL,
                            bekleyen.miktar, skorSonra, 0, skorSonra, "Transfer iptal",
                            String.format("Bekleyen transfer iptal edildi: %,.2f ₺", bekleyen.miktar));
                }
            } else if ("TRANSFER".equals(bekleyen.tip)) {
                Account kaynak = hesapDeposu.idIleGetir(bekleyen.kaynakId);
                Account hedef  = hesapDeposu.idIleGetir(bekleyen.hedefId);
                if (kaynak != null && hedef != null && hedef.getBakiye() >= bekleyen.miktar) {
                    hedef.paraCek(bekleyen.miktar);
                    kaynak.paraYatir(bekleyen.miktar);
                    riskMotoru.transferGeriAl(bekleyen.kaynakId, bekleyen.miktar);
                    riskMotoru.islemRiskOlaylariniIptalEt(islemId);
                    int skorSonra = getRiskSkoru(bekleyen.kaynakId);
                    logEkle(kaynak.getSahibiId(), bekleyen.kaynakId, ActivityLog.IslemTipi.GERI_AL,
                            bekleyen.miktar, skorSonra, 0, skorSonra, "Transfer iptali",
                            String.format("Transfer geri alındı: %,.2f ₺", bekleyen.miktar));
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

    /** Bekleyen transferi onaylar — para şimdi hedefe gönderilir. */
    public BekleyenIslem transferOnayla(String islemId) {
        BekleyenIslem bekleyen = bekleyenIslemler.get(islemId);
        if (bekleyen == null || !"BEKLEYEN_TRANSFER".equals(bekleyen.tip)) return null;
        try {
            Account kaynak = hesapDeposu.idIleGetir(bekleyen.kaynakId);
            Account hedef  = hesapDeposu.idIleGetir(bekleyen.hedefId);
            if (kaynak == null || hedef == null) return null;
            hedef.paraYatir(bekleyen.miktar);
            hedef.islemEkle(new Transfer(islemId, bekleyen.miktar, bekleyen.kaynakId, bekleyen.hedefId));
            // Transfer onaylandı → alıcıyı şimdi kaydet (iptal edilseydi kaydetmeyecektik)
            riskMotoru.aliciKaydet(kaynak.getSahibiId(), bekleyen.hedefId);
            bekleyenIslemler.remove(islemId);
            if (sonBekleyenIslem != null && sonBekleyenIslem.islemId.equals(islemId))
                sonBekleyenIslem = null;
            kaydedici.kaydet("TRANSFER_ONAYLANDI: " + islemId + " | " + bekleyen.kaynakId + " -> " + bekleyen.hedefId + " | " + bekleyen.miktar);
            int skor = getRiskSkoru(bekleyen.kaynakId);
            logEkle(kaynak.getSahibiId(), bekleyen.kaynakId, ActivityLog.IslemTipi.TRANSFER,
                    bekleyen.miktar, skor, 0, skor, "",
                    String.format("Transfer onaylandı: %,.2f ₺ → %s", bekleyen.miktar, bekleyen.hedefId));
            otomatikKaydet();
            return bekleyen;
        } catch (Exception e) {
            return null;
        }
    }

    // ── Aktivite loglama ──────────────────────────────────────────────────────

    private void logEkle(String musteriId, String hesapId, ActivityLog.IslemTipi tip,
                         double miktar, int riskOncesi, int riskDelta, int riskSonrasi,
                         String kural, String aciklama) {
        ActivityLog.Kaynak kaynak = (musteriId != null && isDemoMusteri(musteriId))
                ? ActivityLog.Kaynak.DEMO
                : ActivityLog.Kaynak.GERCEK;
        logServisi.kaydet(new ActivityLog(musteriId, hesapId, tip, miktar,
                riskOncesi, riskDelta, riskSonrasi, kural, aciklama, kaynak));
    }

    public AktiviteLogServisi getLogServisi() { return logServisi; }

    // ── Bekleyen limit yönetimi ───────────────────────────────────────────────

    public String limitDegisimTalep(String hesapId, HesapLimiti yeniLimit) {
        bekleyenLimitler.put(hesapId, new BekleyenLimitDegisimi(hesapId, yeniLimit));
        logEkle(getHesap(hesapId) != null ? getHesap(hesapId).getSahibiId() : null,
                hesapId, ActivityLog.IslemTipi.LIMIT_DEGISIMI, 0, 0, 0, 0,
                "", "Limit değişim talebi oluşturuldu (24s sonra aktif)");
        otomatikKaydet();
        return "📱 SMS gönderildi. Yeni limitler 24 saat sonra otomatik olarak aktif olacak.";
    }

    public BekleyenLimitDegisimi getBekleyenLimit(String hesapId) {
        return bekleyenLimitler.get(hesapId);
    }

    private void bekleyenLimitleriKontrolEt(String hesapId) {
        BekleyenLimitDegisimi bekleyen = bekleyenLimitler.get(hesapId);
        if (bekleyen != null && bekleyen.aktifMi()) {
            riskMotoru.limitGuncelle(hesapId, bekleyen.yeniLimit);
            bekleyenLimitler.remove(hesapId);
            Account h = hesapDeposu.idIleGetir(hesapId);
            logEkle(h != null ? h.getSahibiId() : null, hesapId,
                    ActivityLog.IslemTipi.LIMIT_DEGISIMI, 0, 0, 0, 0,
                    "", "Yeni limitler aktif edildi");
            kaydedici.kaydet("BEKLEYEN_LIMIT_AKTIF: " + hesapId);
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
