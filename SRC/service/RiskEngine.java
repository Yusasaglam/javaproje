package service;

import model.Account;
import model.HesapLimiti;
import model.IRiskCalculatable;
import model.KrediHesabi;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class RiskEngine implements IRiskCalculatable {

    static final double VARSAYILAN_MAKS_YATIRMA    = 100_000.0;
    static final double VARSAYILAN_MAKS_CEKIM      =  50_000.0;
    static final double VARSAYILAN_MAKS_TRANSFER   =  50_000.0;
    static final double YUKSEK_RISK_ESIGI          =  50_000.0;
    static final double VARSAYILAN_GUNLUK_CEKIM    =  20_000.0;
    static final double VARSAYILAN_GUNLUK_TRANSFER =  30_000.0;
    private static final double MIN_MIKTAR              =      0.01;
    private static final int    GUNLUK_ISLEM_ESIGI      =        30;
    // Sliding-window velocity: max işlem sayısı 5 dakikada
    private static final int    KISA_SURE_ISLEM_ESIGI   =        10;
    private static final long   KISA_SURE_DAKIKA        =         5L;
    // Gece modu: 01:00-06:00 arası şüpheli eşiği
    private static final LocalTime GECE_BASLANGIC       = LocalTime.of(1, 0);
    private static final LocalTime GECE_BITIS           = LocalTime.of(6, 0);
    private static final double    GECE_MODU_ESIGI      = 10_000.0;
    // Ani bakiye düşüş: bakiyenin bu oranını aşan tek çekim şüpheli
    private static final double    ANI_DUSUS_ORANI      =      0.95;

    // ── Risk Skoru eşikleri ───────────────────────────────────────────────────
    public static final int SKOR_IZLEME = 31;   // sarı  — izleniyor
    public static final int SKOR_RISKLI = 61;   // turuncu — onay dialogu
    public static final int SKOR_DONDUR = 86;   // kırmızı — otomatik dondur

    private boolean simuleGeceModuAktif = false;
    public void setSimuleGeceModu(boolean aktif) { this.simuleGeceModuAktif = aktif; }

    // ── Risk Skoru verileri ───────────────────────────────────────────────────
    private final Map<String, Integer>   riskSkorlari   = new HashMap<>();
    private final Map<String, LocalDate> skorGuncelleme = new HashMap<>();

    private final Map<String, Double>           gunlukCekimler          = new HashMap<>();
    private final Map<String, LocalDate>        gunlukCekimTarihleri    = new HashMap<>();
    private final Map<String, Double>           gunlukTransferler       = new HashMap<>();
    private final Map<String, LocalDate>        gunlukTransferTarihleri = new HashMap<>();
    private final Map<String, Integer>          gunlukIslemSayisi       = new HashMap<>();
    private final Map<String, LocalDate>        gunlukIslemTarihleri    = new HashMap<>();
    private final Map<String, HesapLimiti>      ozelLimitler            = new HashMap<>();
    // Sliding-window: hesapId → son işlem zamanları listesi
    private final Map<String, List<LocalDateTime>> kisaVadeliIslemler   = new HashMap<>();

    // ── Geçerlilik kontrolleri ────────────────────────────────────────────────

    public boolean paraYatirmaGecerliMi(double miktar) {
        return miktar >= MIN_MIKTAR && miktar <= VARSAYILAN_MAKS_YATIRMA;
    }

    public boolean paraCekmeGecerliMi(Account hesap, double miktar) {
        if (miktar < MIN_MIKTAR) return false;
        // KrediHesabi: bakiye negatife gidebilir; kalan kredi limitine göre kontrol
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

    // ── Gece Modu (Night Mode) ────────────────────────────────────────────────

    /** 01:00–06:00 arası gece işlemi mi? Simülasyon modunda her zaman true. */
    public boolean geceModuMu() {
        if (simuleGeceModuAktif) return true;
        LocalTime simdi = LocalTime.now();
        return !simdi.isBefore(GECE_BASLANGIC) && simdi.isBefore(GECE_BITIS);
    }

    /** Gece modunda şüpheli sayılacak eşiği aşıyor mu? */
    public boolean geceModuRisklimi(double miktar) {
        return geceModuMu() && miktar >= GECE_MODU_ESIGI;
    }

    // ── Ani Bakiye Düşüş Koruması ─────────────────────────────────────────────

    /** Tek işlemde bakiyenin %80'inden fazlası mı çekiliyor? */
    public boolean aniDususVarMi(double bakiye, double miktar) {
        if (bakiye <= 0) return false;
        return miktar / bakiye >= ANI_DUSUS_ORANI;
    }

    // ── Velocity Check (Sliding Window) ──────────────────────────────────────

    /** Son 5 dakika içinde çok fazla işlem var mı? */
    public boolean kisaVadeliCokIslemMi(String hesapId) {
        List<LocalDateTime> zamanlar = kisaVadeliIslemler.get(hesapId);
        if (zamanlar == null) return false;
        LocalDateTime esik = LocalDateTime.now().minusMinutes(KISA_SURE_DAKIKA);
        long sonIslemler = zamanlar.stream().filter(z -> z.isAfter(esik)).count();
        return sonIslemler >= KISA_SURE_ISLEM_ESIGI;
    }

    /** Son 5 dakikadaki işlem sayısını döner. */
    public long kisaVadeliIslemSayisi(String hesapId) {
        List<LocalDateTime> zamanlar = kisaVadeliIslemler.get(hesapId);
        if (zamanlar == null) return 0;
        LocalDateTime esik = LocalDateTime.now().minusMinutes(KISA_SURE_DAKIKA);
        return zamanlar.stream().filter(z -> z.isAfter(esik)).count();
    }

    // ── Kayıt metodları ───────────────────────────────────────────────────────

    public void cekimKaydet(String hesapId, double miktar) {
        guncelle(hesapId, miktar, gunlukCekimler, gunlukCekimTarihleri);
        islemKaydet(hesapId);
    }

    public void transferKaydet(String hesapId, double miktar) {
        guncelle(hesapId, miktar, gunlukTransferler, gunlukTransferTarihleri);
        islemKaydet(hesapId);
    }

    public void yatirmaKaydet(String hesapId) {
        islemKaydet(hesapId);
    }

    private void islemKaydet(String hesapId) {
        LocalDate bugun = LocalDate.now();
        if (!bugun.equals(gunlukIslemTarihleri.get(hesapId))) {
            gunlukIslemSayisi.put(hesapId, 0);
            gunlukIslemTarihleri.put(hesapId, bugun);
        }
        gunlukIslemSayisi.merge(hesapId, 1, Integer::sum);

        // Sliding-window kaydı (eski girişleri temizle)
        LocalDateTime simdi = LocalDateTime.now();
        LocalDateTime esik  = simdi.minusMinutes(KISA_SURE_DAKIKA * 2);
        kisaVadeliIslemler
            .computeIfAbsent(hesapId, k -> new ArrayList<>())
            .removeIf(z -> z.isBefore(esik));
        kisaVadeliIslemler.get(hesapId).add(simdi);
    }

    // ── Sorgulama metodları ───────────────────────────────────────────────────

    public boolean cokFazlaIslemMi(String hesapId) {
        if (!LocalDate.now().equals(gunlukIslemTarihleri.get(hesapId))) return false;
        return gunlukIslemSayisi.getOrDefault(hesapId, 0) >= GUNLUK_ISLEM_ESIGI;
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

    public HesapLimiti getHesapLimiti(String hesapId) {
        return ozelLimitler.get(hesapId);
    }

    public HesapLimiti varsayilanLimit() {
        return new HesapLimiti(
                VARSAYILAN_GUNLUK_CEKIM,
                VARSAYILAN_GUNLUK_TRANSFER,
                VARSAYILAN_MAKS_CEKIM,
                VARSAYILAN_MAKS_TRANSFER);
    }

    public Map<String, HesapLimiti> getOzelLimitler() {
        return Collections.unmodifiableMap(ozelLimitler);
    }

    public void ozelLimitleriYukle(Map<String, HesapLimiti> limitler) {
        ozelLimitler.clear();
        if (limitler != null) ozelLimitler.putAll(limitler);
    }

    // ── İç yardımcı metodlar ──────────────────────────────────────────────────

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

    public double getMaksParaYatirma()      { return VARSAYILAN_MAKS_YATIRMA; }
    public double getMaksParaCekme()        { return VARSAYILAN_MAKS_CEKIM; }
    public double getMaksTransfer()         { return VARSAYILAN_MAKS_TRANSFER; }
    public double getGunlukCekimLimiti()    { return VARSAYILAN_GUNLUK_CEKIM; }
    public double getGunlukTransferLimiti() { return VARSAYILAN_GUNLUK_TRANSFER; }
    public double getGeceModuEsigi()        { return GECE_MODU_ESIGI; }
    public double getAniDususOrani()        { return ANI_DUSUS_ORANI; }

    // ── Risk Skoru metodları ──────────────────────────────────────────────────

    /** Hesabın güncel risk skorunu döner; her geçen temiz gün -5 puan azalır. */
    public int getRiskSkoru(String hesapId) {
        LocalDate bugun = LocalDate.now();
        LocalDate sonGun = skorGuncelleme.getOrDefault(hesapId, bugun);
        if (sonGun.isBefore(bugun)) {
            long gunFarki = java.time.temporal.ChronoUnit.DAYS.between(sonGun, bugun);
            int mevcut = riskSkorlari.getOrDefault(hesapId, 0);
            riskSkorlari.put(hesapId, Math.max(0, mevcut - (int)(gunFarki * 5)));
            skorGuncelleme.put(hesapId, bugun);
        }
        return riskSkorlari.getOrDefault(hesapId, 0);
    }

    /** Hesabın risk skoruna puan ekler (max 100). */
    public void skorEkle(String hesapId, int puan) {
        int mevcut = getRiskSkoru(hesapId);
        riskSkorlari.put(hesapId, Math.min(100, mevcut + puan));
        skorGuncelleme.put(hesapId, LocalDate.now());
    }

    /** Risk seviyesini metin olarak döner: GÜVENLİ / İZLENİYOR / RİSKLİ / DONDURULDU */
    public String getRiskSeviyesi(String hesapId) {
        int s = getRiskSkoru(hesapId);
        if (s >= SKOR_DONDUR) return "DONDURULDU";
        if (s >= SKOR_RISKLI) return "RİSKLİ";
        if (s >= SKOR_IZLEME) return "İZLENİYOR";
        return "GÜVENLİ";
    }

    /** Risk seviyesine karşılık gelen hex renk kodunu döner. */
    public String getRiskRengi(String hesapId) {
        int s = getRiskSkoru(hesapId);
        if (s >= SKOR_DONDUR) return "#af1414";
        if (s >= SKOR_RISKLI) return "#af4b00";
        if (s >= SKOR_IZLEME) return "#c8a000";
        return "#146418";
    }

    public Map<String, Integer>   getRiskSkorlari()   { return Collections.unmodifiableMap(riskSkorlari); }
    public Map<String, LocalDate> getSkorGuncelleme() { return Collections.unmodifiableMap(skorGuncelleme); }

    public void riskSkorlariniYukle(Map<String, Integer> skorlar, Map<String, LocalDate> tarihler) {
        riskSkorlari.clear();
        if (skorlar  != null) riskSkorlari.putAll(skorlar);
        skorGuncelleme.clear();
        if (tarihler != null) skorGuncelleme.putAll(tarihler);
    }
}
