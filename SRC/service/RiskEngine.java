package service;

import model.Account;
import model.HesapLimiti;
import model.IRiskCalculatable;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RiskEngine implements IRiskCalculatable {

    static final double VARSAYILAN_MAKS_YATIRMA    = 100_000.0;
    static final double VARSAYILAN_MAKS_CEKIM      =  50_000.0;
    static final double VARSAYILAN_MAKS_TRANSFER   =  50_000.0;
    static final double YUKSEK_RISK_ESIGI          =  10_000.0;
    static final double VARSAYILAN_GUNLUK_CEKIM    =  20_000.0;
    static final double VARSAYILAN_GUNLUK_TRANSFER =  30_000.0;
    private static final double MIN_MIKTAR         =      0.01;
    private static final int    GUNLUK_ISLEM_ESIGI =        15;

    private final Map<String, Double>      gunlukCekimler          = new HashMap<>();
    private final Map<String, LocalDate>   gunlukCekimTarihleri    = new HashMap<>();
    private final Map<String, Double>      gunlukTransferler       = new HashMap<>();
    private final Map<String, LocalDate>   gunlukTransferTarihleri = new HashMap<>();
    private final Map<String, Integer>     gunlukIslemSayisi       = new HashMap<>();
    private final Map<String, LocalDate>   gunlukIslemTarihleri    = new HashMap<>();
    private final Map<String, HesapLimiti> ozelLimitler            = new HashMap<>();

    // ── Geçerlilik kontrolleri ────────────────────────────────────────────────

    public boolean paraYatirmaGecerliMi(double miktar) {
        return miktar >= MIN_MIKTAR && miktar <= VARSAYILAN_MAKS_YATIRMA;
    }

    public boolean paraCekmeGecerliMi(Account hesap, double miktar) {
        if (miktar < MIN_MIKTAR || hesap.getBakiye() < miktar) return false;
        HesapLimiti limit  = ozelLimitler.get(hesap.getHesapId());
        double tekMax      = limit != null ? limit.getTekIslemCekimLimiti()  : VARSAYILAN_MAKS_CEKIM;
        double gunlukMax   = limit != null ? limit.getGunlukCekimLimiti()    : VARSAYILAN_GUNLUK_CEKIM;
        if (miktar > tekMax) return false;
        return bugunCekilen(hesap.getHesapId()) + miktar <= gunlukMax;
    }

    public boolean transferGecerliMi(Account kaynak, double miktar) {
        if (miktar < MIN_MIKTAR || kaynak.getBakiye() < miktar) return false;
        HesapLimiti limit  = ozelLimitler.get(kaynak.getHesapId());
        double tekMax      = limit != null ? limit.getTekIslemTransferLimiti() : VARSAYILAN_MAKS_TRANSFER;
        double gunlukMax   = limit != null ? limit.getGunlukTransferLimiti()   : VARSAYILAN_GUNLUK_TRANSFER;
        if (miktar > tekMax) return false;
        return bugunTransfer(kaynak.getHesapId()) + miktar <= gunlukMax;
    }

    @Override public boolean yuksekRiskMi(double miktar) { return miktar >= YUKSEK_RISK_ESIGI; }
    @Override public double  getRiskPuani()              { return YUKSEK_RISK_ESIGI / VARSAYILAN_MAKS_TRANSFER; }

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
}
