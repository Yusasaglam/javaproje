package model;

import service.*;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("serial")
public class BankState implements Serializable {

    private static final long serialVersionUID = 7L;

    private final List<Customer>             musteriler;
    private final List<Account>              hesaplar;
    private final Map<String, Kullanici>     kullanicilar;
    private final Set<String>                suphelihHesaplar;
    private final Map<String, SupheSebebi>   supheSebebleri;
    private final Map<String, HesapLimiti>   hesapLimitleri;
    private final Set<String>                demoMusteriler;

    // ── Risk engine state ──────────────────────────────────────────────────────
    private final List<RiskOlayKaydi>                  riskOlayKayitlari;
    private final Map<String, MusteriRiskProfili>      musteriProfilleri;
    private final Map<String, DondurmaKaydi>           dondurmaKayitlari;
    private final Map<String, KullaniciKategorisi>     kullaniciKategorileri;
    private final List<ActivityLog>                    aktiviteLoglari;
    private final Map<String, Set<String>>             bilinenAlicilar;

    // ── Bekleyen limit değişimleri ─────────────────────────────────────────────
    private final Map<String, BekleyenLimitDegisimi>   bekleyenLimitler;

    private final int musteriSayaci;
    private final int hesapSayaci;
    private final int islemSayaci;

    public BankState(List<Customer> musteriler, List<Account> hesaplar,
                     Map<String, Kullanici> kullanicilar, Set<String> suphelihHesaplar,
                     Map<String, SupheSebebi> supheSebebleri, Map<String, HesapLimiti> hesapLimitleri,
                     Set<String> demoMusteriler,
                     List<RiskOlayKaydi> riskOlayKayitlari,
                     Map<String, MusteriRiskProfili> musteriProfilleri,
                     Map<String, DondurmaKaydi> dondurmaKayitlari,
                     Map<String, KullaniciKategorisi> kullaniciKategorileri,
                     List<ActivityLog> aktiviteLoglari,
                     Map<String, Set<String>> bilinenAlicilar,
                     Map<String, BekleyenLimitDegisimi> bekleyenLimitler,
                     int musteriSayaci, int hesapSayaci, int islemSayaci) {
        this.musteriler            = musteriler;
        this.hesaplar              = hesaplar;
        this.kullanicilar          = kullanicilar;
        this.suphelihHesaplar      = suphelihHesaplar;
        this.supheSebebleri        = supheSebebleri;
        this.hesapLimitleri        = hesapLimitleri;
        this.demoMusteriler        = demoMusteriler;
        this.riskOlayKayitlari     = riskOlayKayitlari;
        this.musteriProfilleri     = musteriProfilleri;
        this.dondurmaKayitlari     = dondurmaKayitlari;
        this.kullaniciKategorileri  = kullaniciKategorileri;
        this.aktiviteLoglari       = aktiviteLoglari;
        this.bilinenAlicilar       = bilinenAlicilar;
        this.bekleyenLimitler      = bekleyenLimitler;
        this.musteriSayaci         = musteriSayaci;
        this.hesapSayaci           = hesapSayaci;
        this.islemSayaci           = islemSayaci;
    }

    public List<Customer>                         getMusteriler()            { return musteriler; }
    public List<Account>                          getHesaplar()              { return hesaplar; }
    public Map<String, Kullanici>                 getKullanicilar()          { return kullanicilar; }
    public Set<String>                            getSuphelihHesaplar()      { return suphelihHesaplar; }
    public Map<String, SupheSebebi>               getSupheSebebleri()        { return supheSebebleri; }
    public Map<String, HesapLimiti>               getHesapLimitleri()        { return hesapLimitleri; }
    public Set<String>                            getDemoMusteriler()        { return demoMusteriler; }
    public List<RiskOlayKaydi>                    getRiskOlayKayitlari()     { return riskOlayKayitlari; }
    public Map<String, MusteriRiskProfili>        getMusteriProfilleri()     { return musteriProfilleri; }
    public Map<String, DondurmaKaydi>             getDondurmaKayitlari()     { return dondurmaKayitlari; }
    public Map<String, KullaniciKategorisi>       getKullaniciKategorileri() { return kullaniciKategorileri; }
    public List<ActivityLog>                      getAktiviteLoglari()       { return aktiviteLoglari; }
    public Map<String, Set<String>>               getBilinenAlicilar()       { return bilinenAlicilar; }
    public Map<String, BekleyenLimitDegisimi>     getBekleyenLimitler()      { return bekleyenLimitler; }
    public int getMusteriSayaci()                                             { return musteriSayaci; }
    public int getHesapSayaci()                                               { return hesapSayaci; }
    public int getIslemSayaci()                                               { return islemSayaci; }
}
