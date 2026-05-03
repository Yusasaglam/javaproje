package service;

import model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class RiskEngine implements IRiskCalculatable {

    // ── Limit sabitleri ───────────────────────────────────────────────────────
    static final double VARSAYILAN_MAKS_YATIRMA    = 100_000.0;
    static final double VARSAYILAN_MAKS_CEKIM      =  50_000.0;
    static final double VARSAYILAN_MAKS_TRANSFER   =  50_000.0;
    static final double YUKSEK_RISK_ESIGI          =  15_000.0;
    static final double VARSAYILAN_GUNLUK_CEKIM    = 100_000.0;
    static final double VARSAYILAN_GUNLUK_TRANSFER = 100_000.0;
    private static final double MIN_MIKTAR             =      0.01;
    private static final int    GUNLUK_ISLEM_ESIGI     =        30;
    private static final long   KISA_SURE_DAKIKA       =         5L;
    private static final LocalTime GECE_BASLANGIC      = LocalTime.of(1, 0);
    private static final LocalTime GECE_BITIS          = LocalTime.of(6, 0);
    private static final double    GECE_MODU_ESIGI     =  5_000.0;  // gece 01-06 arası ≥5K şüpheli
    private static final double    ANI_DUSUS_ORANI     =      0.90;  // bakiyenin %90'ı tek işlemde
    private static final double    ANI_DUSUS_MIN       =  10_000.0;  // küçük hesaplar etkilenmesin
    // Hızlı boşaltma: kart çalındı senaryosu — kısa sürede çok çekim
    static final int               HIZLI_BOSALTMA_SAYI    = 3;        // 10 dk'da kaç çekim
    static final double            HIZLI_BOSALTMA_MIKTAR  = 25_000.0; // toplam çekim eşiği
    private static final long      HIZLI_BOSALTMA_DAKIKA  = 10L;
    // Yapılandırma: eşiğin %85-99'u arasında 3+ işlem → kara para aklama
    private static final double    YAPILANDIRMA_ORAN   =      0.85;
    private static final int       YAPILANDIRMA_ESIK   =         3;
    // Müşteri alt sınırı: düşürüldü — bir hesap diğerlerini az etkilesin
    private static final double    MUSTERI_ALT_SINIR   =      0.35;

    // ── Risk skoru eşikleri ───────────────────────────────────────────────────
    public static final int SKOR_IZLEME     = 31;
    public static final int SKOR_RISKLI     = 61;
    public static final int SKOR_DONDUR     = 86;
    public static final int SKOR_OTOMATIK_COZ = 20;

    private boolean simuleGeceModuAktif = false;
    public void setSimuleGeceModu(boolean aktif) { this.simuleGeceModuAktif = aktif; }

    // ── Event log (sayaç yerine olay kaydı) ──────────────────────────────────
    private final List<RiskOlayKaydi> olayKayitlari = new ArrayList<>();

    // ── Müşteri profilleri (hibrit model) ────────────────────────────────────
    private final Map<String, MusteriRiskProfili> musteriProfilleri = new HashMap<>();

    // ── Dondurma kayıtları ────────────────────────────────────────────────────
    private final Map<String, DondurmaKaydi> dondurmaKayitlari = new HashMap<>();

    // ── Kullanıcı kategorileri (adaptif velocity) ─────────────────────────────
    private final Map<String, KullaniciKategorisi> kullaniciKategorileri = new HashMap<>();

    // ── Günlük izler ──────────────────────────────────────────────────────────
    private final Map<String, Double>    gunlukCekimler          = new HashMap<>();
    private final Map<String, LocalDate> gunlukCekimTarihleri    = new HashMap<>();
    private final Map<String, Double>    gunlukTransferler       = new HashMap<>();
    private final Map<String, LocalDate> gunlukTransferTarihleri = new HashMap<>();
    private final Map<String, Integer>   gunlukIslemSayisi       = new HashMap<>();
    private final Map<String, LocalDate> gunlukIslemTarihleri    = new HashMap<>();
    private final Map<String, Integer>   bugunVelocityIhlali     = new HashMap<>();
    private final Map<String, HesapLimiti> ozelLimitler          = new HashMap<>();

    // ── Velocity sliding-window ───────────────────────────────────────────────
    private final Map<String, List<LocalDateTime>> kisaVadeliIslemler        = new HashMap<>();
    private final Map<String, List<LocalDateTime>> kisaVadeliMusteriIslemler = new HashMap<>();
    // [0]=epochMilli, [1]=miktar — kısa vadeli çekim penceresi (velocity + boşaltma tespiti)
    private final Map<String, List<double[]>>      kisaVadeliCekimler        = new HashMap<>();
    // Yeni alıcı tespiti: müşteri daha önce para göndermediği hesaba büyük transfer
    private final Map<String, Set<String>>         bilinenAlicilar           = new HashMap<>();
    static final double YENI_ALICI_BUYUK_ESIK =  30_000.0;  // ≥30K → yüksek risk
    static final double YENI_ALICI_ORTA_ESIK  =  10_000.0;  // 10K-30K → orta risk

    // Yapılandırma — 24 saatlik cooldown (çifte sayım önleme)
    private final Map<String, LocalDateTime>       sonYapilandirmaTespiti    = new HashMap<>();
    private static final int YAPILANDIRMA_COOLDOWN_SAAT = 24;
    // Günlük limit — günde yalnızca bir kez ceza (çifte sayım önleme)
    private final Map<String, LocalDate>           gunlukLimitTespiti        = new HashMap<>();

    // ── Geçerlilik kontrolleri ────────────────────────────────────────────────

    public boolean paraYatirmaGecerliMi(double miktar) {
        return miktar >= MIN_MIKTAR && miktar <= VARSAYILAN_MAKS_YATIRMA;
    }

    public boolean paraCekmeGecerliMi(Account hesap, double miktar) {
        if (miktar < MIN_MIKTAR) return false;
        if (hesap instanceof KrediHesabi) {
            if (((KrediHesabi) hesap).kalanKredi() < miktar) return false;
        } else {
            if (hesap.getBakiye() < miktar) return false;
        }
        HesapLimiti limit  = ozelLimitler.get(hesap.getHesapId());
        double tekMax      = limit != null ? limit.getTekIslemCekimLimiti()  : VARSAYILAN_MAKS_CEKIM;
        double gunlukMax   = limit != null ? limit.getGunlukCekimLimiti()    : VARSAYILAN_GUNLUK_CEKIM;
        if (miktar > tekMax) return false;
        return bugunCekilen(hesap.getHesapId()) + miktar <= gunlukMax;
    }

    public boolean transferGecerliMi(Account kaynak, double miktar) {
        if (miktar < MIN_MIKTAR) return false;
        if (kaynak instanceof KrediHesabi) {
            if (((KrediHesabi) kaynak).kalanKredi() < miktar) return false;
        } else {
            if (kaynak.getBakiye() < miktar) return false;
        }
        HesapLimiti limit  = ozelLimitler.get(kaynak.getHesapId());
        double tekMax      = limit != null ? limit.getTekIslemTransferLimiti() : VARSAYILAN_MAKS_TRANSFER;
        double gunlukMax   = limit != null ? limit.getGunlukTransferLimiti()   : VARSAYILAN_GUNLUK_TRANSFER;
        if (miktar > tekMax) return false;
        return bugunTransfer(kaynak.getHesapId()) + miktar <= gunlukMax;
    }

    @Override public boolean yuksekRiskMi(double miktar) { return miktar >= YUKSEK_RISK_ESIGI; }
    @Override public double  getRiskPuani()              { return YUKSEK_RISK_ESIGI / VARSAYILAN_MAKS_TRANSFER; }

    // ── Kural kontrolleri ─────────────────────────────────────────────────────

    public boolean geceModuMu() {
        if (simuleGeceModuAktif) return true;
        LocalTime t = LocalTime.now();
        return !t.isBefore(GECE_BASLANGIC) && t.isBefore(GECE_BITIS);
    }

    public boolean geceModuRisklimi(double miktar) {
        return geceModuMu() && miktar >= GECE_MODU_ESIGI;
    }

    public boolean aniDususVarMi(double bakiye, double miktar) {
        if (bakiye <= 0 || miktar < ANI_DUSUS_MIN) return false;
        return miktar / bakiye >= ANI_DUSUS_ORANI;
    }

    /** Son 10 işleme bakarak yapılandırma (structuring) tespiti yapar. */
    /** Yapılandırma tespiti — aynı kalıp 24 saat içinde tekrar cezalandırılmaz. */
    public boolean yapilandirmaVarMi(Account hesap) {
        String hesapId = hesap.getHesapId();
        // 24 saatlik cooldown: aynı kalıbı tekrar tekrar sayma
        LocalDateTime sonTespit = sonYapilandirmaTespiti.get(hesapId);
        if (sonTespit != null &&
                sonTespit.isAfter(LocalDateTime.now().minusHours(YAPILANDIRMA_COOLDOWN_SAAT))) {
            return false;
        }
        List<Transaction> islemler = hesap.getIslemler();
        if (islemler == null || islemler.size() < YAPILANDIRMA_ESIK) return false;
        int baslangic = Math.max(0, islemler.size() - 10);
        double alt = YUKSEK_RISK_ESIGI * YAPILANDIRMA_ORAN;
        long supheli = islemler.subList(baslangic, islemler.size()).stream()
            .filter(t -> "PARA_CEKME".equals(t.getTur()) || "TRANSFER".equals(t.getTur()))
            .filter(t -> t.getMiktar() >= alt && t.getMiktar() < YUKSEK_RISK_ESIGI)
            .count();
        if (supheli >= YAPILANDIRMA_ESIK) {
            sonYapilandirmaTespiti.put(hesapId, LocalDateTime.now());
            return true;
        }
        return false;
    }

    // ── Velocity ──────────────────────────────────────────────────────────────

    public boolean kisaVadeliCokIslemMi(String hesapId) {
        List<LocalDateTime> z = kisaVadeliIslemler.get(hesapId);
        if (z == null) return false;
        LocalDateTime esik = LocalDateTime.now().minusMinutes(KISA_SURE_DAKIKA);
        return z.stream().filter(t -> t.isAfter(esik)).count() >= 10;
    }

    public long kisaVadeliIslemSayisi(String hesapId) {
        List<LocalDateTime> z = kisaVadeliIslemler.get(hesapId);
        if (z == null) return 0;
        LocalDateTime esik = LocalDateTime.now().minusMinutes(KISA_SURE_DAKIKA);
        return z.stream().filter(t -> t.isAfter(esik)).count();
    }

    /** Müşterinin tüm hesaplarında adaptif eşikle velocity kontrolü. */
    public boolean kisaVadeliCokIslemMiMusteri(String musteriId) {
        KullaniciKategorisi kat = kullaniciKategorileri
            .getOrDefault(musteriId, KullaniciKategorisi.BIREYSEL);
        List<LocalDateTime> z = kisaVadeliMusteriIslemler.get(musteriId);
        if (z == null) return false;
        LocalDateTime esik = LocalDateTime.now().minusMinutes(KISA_SURE_DAKIKA);
        return z.stream().filter(t -> t.isAfter(esik)).count() >= kat.velocityEsigi;
    }

    public long kisaVadeliMusteriIslemSayisi(String musteriId) {
        List<LocalDateTime> z = kisaVadeliMusteriIslemler.get(musteriId);
        if (z == null) return 0;
        LocalDateTime esik = LocalDateTime.now().minusMinutes(KISA_SURE_DAKIKA);
        return z.stream().filter(t -> t.isAfter(esik)).count();
    }

    /** Kademeli ceza: 1.ihlal +baseCeza, 2.ihlal ×2, 3.+ ×3. */
    public int velocityCezasi(String musteriId) {
        KullaniciKategorisi kat = kullaniciKategorileri
            .getOrDefault(musteriId, KullaniciKategorisi.BIREYSEL);
        int ihlal = bugunVelocityIhlali.merge(musteriId, 1, Integer::sum);
        return Math.min(30, kat.baseCeza * ihlal);
    }

    // ── Kayıt metodları ───────────────────────────────────────────────────────

    public void cekimKaydet(String hesapId, double miktar, String musteriId) {
        guncelle(hesapId, miktar, gunlukCekimler, gunlukCekimTarihleri);
        islemKaydet(hesapId, musteriId);
        long simdi = System.currentTimeMillis();
        long pencere = KISA_SURE_DAKIKA * 2 * 60_000L;
        List<double[]> c = kisaVadeliCekimler.computeIfAbsent(hesapId, k -> new ArrayList<>());
        c.removeIf(e -> simdi - (long) e[0] > pencere);
        c.add(new double[]{simdi, miktar});
    }

    /** Son 10 dakikada toplam çekilen miktar (boşaltma tespiti için). */
    public double kisaVadeliToplamCekim(String hesapId) {
        List<double[]> c = kisaVadeliCekimler.get(hesapId);
        if (c == null) return 0;
        long esik = System.currentTimeMillis() - HIZLI_BOSALTMA_DAKIKA * 60_000L;
        return c.stream().filter(e -> (long) e[0] >= esik).mapToDouble(e -> e[1]).sum();
    }

    /** Son 10 dakikada çekim adedi. */
    public long kisaVadeliCekimAdedi(String hesapId) {
        List<double[]> c = kisaVadeliCekimler.get(hesapId);
        if (c == null) return 0;
        long esik = System.currentTimeMillis() - HIZLI_BOSALTMA_DAKIKA * 60_000L;
        return c.stream().filter(e -> (long) e[0] >= esik).count();
    }

    /** Kart hırsızlığı tespiti: 10 dk'da 3+ çekim VE toplam ≥25K. */
    public boolean hizliBoşaltmaMi(String hesapId) {
        return kisaVadeliCekimAdedi(hesapId) >= HIZLI_BOSALTMA_SAYI
                && kisaVadeliToplamCekim(hesapId) >= HIZLI_BOSALTMA_MIKTAR;
    }

    public void transferKaydet(String hesapId, double miktar, String musteriId) {
        guncelle(hesapId, miktar, gunlukTransferler, gunlukTransferTarihleri);
        islemKaydet(hesapId, musteriId);
    }

    public void transferGeriAl(String hesapId, double miktar) {
        if (LocalDate.now().equals(gunlukTransferTarihleri.get(hesapId))) {
            double mevcut = gunlukTransferler.getOrDefault(hesapId, 0.0);
            gunlukTransferler.put(hesapId, Math.max(0.0, mevcut - miktar));
        }
    }

    public void yatirmaKaydet(String hesapId, String musteriId) {
        islemKaydet(hesapId, musteriId);
    }

    private void islemKaydet(String hesapId, String musteriId) {
        LocalDate bugun = LocalDate.now();
        if (!bugun.equals(gunlukIslemTarihleri.get(hesapId))) {
            gunlukIslemSayisi.put(hesapId, 0);
            gunlukIslemTarihleri.put(hesapId, bugun);
        }
        gunlukIslemSayisi.merge(hesapId, 1, Integer::sum);

        LocalDateTime simdi = LocalDateTime.now();
        LocalDateTime esik  = simdi.minusMinutes(KISA_SURE_DAKIKA * 2);

        kisaVadeliIslemler
            .computeIfAbsent(hesapId, k -> new ArrayList<>())
            .removeIf(z -> z.isBefore(esik));
        kisaVadeliIslemler.get(hesapId).add(simdi);

        if (musteriId != null) {
            kisaVadeliMusteriIslemler
                .computeIfAbsent(musteriId, k -> new ArrayList<>())
                .removeIf(z -> z.isBefore(esik));
            kisaVadeliMusteriIslemler.get(musteriId).add(simdi);
        }
    }

    // ── Puan ekleme (event log tabanlı) ──────────────────────────────────────

    /**
     * İşlem bazlı puan ekler. Geri alındığında islemId üzerinden iptal edilebilir.
     * Gerçek ağırlık: bazPuan × agirlik.carpan
     */
    public void skorEkle(String islemId, String hesapId, String musteriId,
                          int bazPuan, IslemRiskAgirlik agirlik) {
        int efektif = Math.min(100, (int)(bazPuan * agirlik.carpan));
        olayKayitlari.add(new RiskOlayKaydi(
            UUID.randomUUID().toString(), islemId,
            hesapId, musteriId, efektif, agirlik,
            LocalDateTime.now()
        ));
        // Müşteri skoruna %25 bulaşma — bir hesap diğerlerini az etkilesin
        if (musteriId != null) {
            MusteriRiskProfili p = musteriProfilleri
                .computeIfAbsent(musteriId, MusteriRiskProfili::new);
            p.musteriSkoru = Math.min(100, p.musteriSkoru + (int)(efektif * 0.25));
            p.skorGuncelleme = LocalDate.now();
        }
    }

    /** Geriye dönük uyumluluk — islemId bilinmediğinde (admin/demo çağrıları). */
    public void skorEkle(String hesapId, int puan) {
        olayKayitlari.add(new RiskOlayKaydi(
            UUID.randomUUID().toString(), "ANONIM",
            hesapId, null, puan, IslemRiskAgirlik.DAVRANISSAL,
            LocalDateTime.now()
        ));
    }

    // ── Geri alma — event iptal ───────────────────────────────────────────────

    /** Transfer geri alındığında o işleme ait tüm risk olaylarını pasif yapar. */
    public void islemRiskOlaylariniIptalEt(String islemId) {
        for (RiskOlayKaydi e : olayKayitlari) {
            if (e.islemId.equals(islemId) && e.aktif) {
                e.aktif = false;
                if (e.musteriId != null) {
                    MusteriRiskProfili p = musteriProfilleri.get(e.musteriId);
                    if (p != null)
                        p.musteriSkoru = Math.max(0, p.musteriSkoru - (int)(e.puan * 0.5));
                }
            }
        }
    }

    // ── Skor sorgulama ────────────────────────────────────────────────────────

    /** Hesap skorunu event log üzerinden hesaplar, müşteri alt sınırını uygular. */
    public int getRiskSkoru(String hesapId, String musteriId) {
        LocalDate bugun = LocalDate.now();
        int hesapSkoru = 0;
        for (RiskOlayKaydi e : olayKayitlari) {
            if (!e.hesapId.equals(hesapId) || !e.aktif) continue;
            long gun    = ChronoUnit.DAYS.between(e.olusturmaTarihi.toLocalDate(), bugun);
            int  decay  = (int)(gun * e.agirlik.gunlukDecay);
            hesapSkoru += Math.max(0, e.puan - decay);
        }
        hesapSkoru = Math.min(100, hesapSkoru);

        // Müşteri alt sınırı: hesap skoru hiçbir zaman müşteri skorunun %60'ından düşük olamaz
        if (musteriId != null) {
            MusteriRiskProfili p = musteriProfilleri.get(musteriId);
            if (p != null) {
                int altSinir = (int)(musteri_skoruHesapla(p) * MUSTERI_ALT_SINIR);
                hesapSkoru = Math.max(hesapSkoru, altSinir);
            }
        }
        return Math.min(100, hesapSkoru);
    }

    /** Geriye dönük uyumluluk — musteriId bilinmiyorsa. */
    public int getRiskSkoru(String hesapId) {
        return getRiskSkoru(hesapId, null);
    }

    private int musteri_skoruHesapla(MusteriRiskProfili p) {
        LocalDate bugun = LocalDate.now();
        if (p.skorGuncelleme != null && p.skorGuncelleme.isBefore(bugun)) {
            long gun = ChronoUnit.DAYS.between(p.skorGuncelleme, bugun);
            p.musteriSkoru   = Math.max(0, p.musteriSkoru - (int)(gun * 3));
            p.skorGuncelleme = bugun;
        }
        return p.musteriSkoru;
    }

    public String getRiskSeviyesi(String hesapId) {
        int s = getRiskSkoru(hesapId);
        if (s >= SKOR_DONDUR) return "DONDURULDU";
        if (s >= SKOR_RISKLI) return "RİSKLİ";
        if (s >= SKOR_IZLEME) return "İZLENİYOR";
        return "GÜVENLİ";
    }

    public String getRiskRengi(String hesapId) {
        int s = getRiskSkoru(hesapId);
        if (s >= SKOR_DONDUR) return "#af1414";
        if (s >= SKOR_RISKLI) return "#af4b00";
        if (s >= SKOR_IZLEME) return "#c8a000";
        return "#146418";
    }

    // ── Dondurma yönetimi ─────────────────────────────────────────────────────

    public void dondurmaKaydet(String hesapId, DondurmaSecegi sekil, String sebep) {
        dondurmaKayitlari.put(hesapId,
            new DondurmaKaydi(hesapId, sekil, LocalDateTime.now(), sebep));
    }

    public void dondurmaKaldir(String hesapId) {
        dondurmaKayitlari.remove(hesapId);
    }

    public DondurmaSecegi getDondurmaSecegi(String hesapId) {
        DondurmaKaydi k = dondurmaKayitlari.get(hesapId);
        return k != null ? k.sekil : null;
    }

    // ── Sorgulama metodları ───────────────────────────────────────────────────

    /** Günlük işlem eşiği aşıldıysa true döner — günde yalnızca bir kez ceza verilir. */
    public boolean cokFazlaIslemMi(String hesapId) {
        LocalDate bugun = LocalDate.now();
        if (!bugun.equals(gunlukIslemTarihleri.get(hesapId))) return false;
        if (gunlukIslemSayisi.getOrDefault(hesapId, 0) < GUNLUK_ISLEM_ESIGI) return false;
        if (bugun.equals(gunlukLimitTespiti.get(hesapId))) return false; // bugün zaten ceza verildi
        gunlukLimitTespiti.put(hesapId, bugun);
        return true;
    }

    public int bugunIslemSayisi(String hesapId) {
        if (!LocalDate.now().equals(gunlukIslemTarihleri.get(hesapId))) return 0;
        return gunlukIslemSayisi.getOrDefault(hesapId, 0);
    }

    public double bugunCekilen(String hesapId) {
        return bugunMiktar(hesapId, gunlukCekimler, gunlukCekimTarihleri);
    }

    public double bugunTransfer(String hesapId) {
        return bugunMiktar(hesapId, gunlukTransferler, gunlukTransferTarihleri);
    }

    public double kalanCekimLimiti(String hesapId) {
        return Math.max(0.0, getGunlukCekimLimit(hesapId) - bugunCekilen(hesapId));
    }

    public double kalanTransferLimiti(String hesapId) {
        return Math.max(0.0, getGunlukTransferLimit(hesapId) - bugunTransfer(hesapId));
    }

    public double getGunlukCekimLimit(String hesapId) {
        HesapLimiti l = ozelLimitler.get(hesapId);
        return l != null ? l.getGunlukCekimLimiti() : VARSAYILAN_GUNLUK_CEKIM;
    }

    public double getGunlukTransferLimit(String hesapId) {
        HesapLimiti l = ozelLimitler.get(hesapId);
        return l != null ? l.getGunlukTransferLimiti() : VARSAYILAN_GUNLUK_TRANSFER;
    }

    // ── Limit yönetimi ────────────────────────────────────────────────────────

    public void limitGuncelle(String hesapId, HesapLimiti limit) {
        ozelLimitler.put(hesapId, limit);
    }

    public HesapLimiti getHesapLimiti(String hesapId) { return ozelLimitler.get(hesapId); }

    public HesapLimiti varsayilanLimit() {
        return new HesapLimiti(
            VARSAYILAN_GUNLUK_CEKIM, VARSAYILAN_GUNLUK_TRANSFER,
            VARSAYILAN_MAKS_CEKIM,   VARSAYILAN_MAKS_TRANSFER);
    }

    public Map<String, HesapLimiti> getOzelLimitler() {
        return Collections.unmodifiableMap(ozelLimitler);
    }

    public void ozelLimitleriYukle(Map<String, HesapLimiti> limitler) {
        ozelLimitler.clear();
        if (limitler != null) ozelLimitler.putAll(limitler);
    }

    // ── Kullanıcı kategorisi ─────────────────────────────────────────────────

    public void kullaniciKategorisiAta(String musteriId, KullaniciKategorisi kat) {
        kullaniciKategorileri.put(musteriId, kat);
    }

    public KullaniciKategorisi getKullaniciKategorisi(String musteriId) {
        return kullaniciKategorileri.getOrDefault(musteriId, KullaniciKategorisi.BIREYSEL);
    }

    // ── Kalıcılık ─────────────────────────────────────────────────────────────

    // ── Yeni alıcı tespiti ────────────────────────────────────────────────────

    /** Bu müşteri bu hesaba daha önce hiç transfer yapmadıysa true döner. */
    public boolean yeniAliciMi(String musteriId, String hedefHesapId) {
        if (musteriId == null || hedefHesapId == null) return false;
        Set<String> alicilar = bilinenAlicilar.get(musteriId);
        return alicilar == null || !alicilar.contains(hedefHesapId);
    }

    /** Transfer sonrası hedef hesabı bilinen alıcılar listesine ekler. */
    public void aliciKaydet(String musteriId, String hedefHesapId) {
        if (musteriId == null || hedefHesapId == null) return;
        bilinenAlicilar.computeIfAbsent(musteriId, k -> new HashSet<>()).add(hedefHesapId);
    }

    public Map<String, Set<String>> getBilinenAlicilar() {
        return Collections.unmodifiableMap(bilinenAlicilar);
    }

    public void alicilariYukle(Map<String, Set<String>> yuklenecek) {
        bilinenAlicilar.clear();
        if (yuklenecek != null) yuklenecek.forEach(
            (k, v) -> bilinenAlicilar.put(k, new HashSet<>(v)));
    }

    // ── Kalıcılık ─────────────────────────────────────────────────────────────

    public List<RiskOlayKaydi> getOlayKayitlari() {
        return Collections.unmodifiableList(olayKayitlari);
    }

    public Map<String, MusteriRiskProfili> getMusteriProfilleri() {
        return Collections.unmodifiableMap(musteriProfilleri);
    }

    public Map<String, DondurmaKaydi> getDondurmaKayitlari() {
        return Collections.unmodifiableMap(dondurmaKayitlari);
    }

    public Map<String, KullaniciKategorisi> getKullaniciKategorileri() {
        return Collections.unmodifiableMap(kullaniciKategorileri);
    }

    public void hesapRiskOlaylariniSil(Set<String> hesapIdleri) {
        olayKayitlari.removeIf(e -> hesapIdleri.contains(e.hesapId));
        dondurmaKayitlari.keySet().removeIf(hesapIdleri::contains);
    }

    public void musteriProfilleriniSil(Set<String> musteriIdleri) {
        for (String mid : musteriIdleri) musteriProfilleri.remove(mid);
        kullaniciKategorileri.keySet().removeIf(musteriIdleri::contains);
    }

    public void riskVerileriniYukle(List<RiskOlayKaydi> olaylar,
                                     Map<String, MusteriRiskProfili> profiller,
                                     Map<String, DondurmaKaydi> dondurma,
                                     Map<String, KullaniciKategorisi> kategoriler) {
        olayKayitlari.clear();
        if (olaylar    != null) olayKayitlari.addAll(olaylar);
        musteriProfilleri.clear();
        if (profiller  != null) musteriProfilleri.putAll(profiller);
        dondurmaKayitlari.clear();
        if (dondurma   != null) dondurmaKayitlari.putAll(dondurma);
        kullaniciKategorileri.clear();
        if (kategoriler != null) kullaniciKategorileri.putAll(kategoriler);
    }

    // ── İç yardımcılar ───────────────────────────────────────────────────────

    private void guncelle(String id, double miktar,
                           Map<String, Double> miktarlar,
                           Map<String, LocalDate> tarihler) {
        LocalDate bugun = LocalDate.now();
        if (!bugun.equals(tarihler.get(id))) {
            miktarlar.put(id, 0.0);
            tarihler.put(id, bugun);
        }
        miktarlar.merge(id, miktar, Double::sum);
    }

    private double bugunMiktar(String id,
                                Map<String, Double> miktarlar,
                                Map<String, LocalDate> tarihler) {
        if (!LocalDate.now().equals(tarihler.get(id))) return 0.0;
        return miktarlar.getOrDefault(id, 0.0);
    }

    // ── Getter'lar ────────────────────────────────────────────────────────────

    public double getYuksekRiskEsigi()      { return YUKSEK_RISK_ESIGI; }
    public double getMaksParaYatirma()      { return VARSAYILAN_MAKS_YATIRMA; }
    public double getMaksParaCekme()        { return VARSAYILAN_MAKS_CEKIM; }
    public double getMaksTransfer()         { return VARSAYILAN_MAKS_TRANSFER; }
    public double getGunlukCekimLimiti()    { return VARSAYILAN_GUNLUK_CEKIM; }
    public double getGunlukTransferLimiti() { return VARSAYILAN_GUNLUK_TRANSFER; }
    public double getGeceModuEsigi()        { return GECE_MODU_ESIGI; }
    public double getAniDususOrani()        { return ANI_DUSUS_ORANI; }

    // Artık kullanılmayan ama derleme uyumluluğu için bırakılan stubs
    public Map<String, Integer>   getRiskSkorlari()   { return Collections.emptyMap(); }
    public Map<String, LocalDate> getSkorGuncelleme() { return Collections.emptyMap(); }
    public void riskSkorlariniYukle(Map<String, Integer> s, Map<String, LocalDate> t) { /* yeni sistem: riskVerileriniYukle */ }
}
