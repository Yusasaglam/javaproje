package model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("serial")
public class BankState implements Serializable {

    private static final long serialVersionUID = 2L;

    private final List<Customer>             musteriler;
    private final List<Account>              hesaplar;
    private final Map<String, Kullanici>     kullanicilar;
    private final Set<String>                suphelihHesaplar;
    private final Map<String, SupheSebebi>   supheSebebleri;
    private final Map<String, HesapLimiti>   hesapLimitleri;
    private final int musteriSayaci;
    private final int hesapSayaci;
    private final int islemSayaci;

    public BankState(List<Customer> musteriler, List<Account> hesaplar,
                     Map<String, Kullanici> kullanicilar, Set<String> suphelihHesaplar,
                     Map<String, SupheSebebi> supheSebebleri, Map<String, HesapLimiti> hesapLimitleri,
                     int musteriSayaci, int hesapSayaci, int islemSayaci) {
        this.musteriler      = musteriler;
        this.hesaplar        = hesaplar;
        this.kullanicilar    = kullanicilar;
        this.suphelihHesaplar = suphelihHesaplar;
        this.supheSebebleri  = supheSebebleri;
        this.hesapLimitleri  = hesapLimitleri;
        this.musteriSayaci   = musteriSayaci;
        this.hesapSayaci     = hesapSayaci;
        this.islemSayaci     = islemSayaci;
    }

    public List<Customer>           getMusteriler()       { return musteriler; }
    public List<Account>            getHesaplar()         { return hesaplar; }
    public Map<String, Kullanici>   getKullanicilar()     { return kullanicilar; }
    public Set<String>              getSuphelihHesaplar() { return suphelihHesaplar; }
    public Map<String, SupheSebebi> getSupheSebebleri()   { return supheSebebleri; }
    public Map<String, HesapLimiti> getHesapLimitleri()   { return hesapLimitleri; }
    public int getMusteriSayaci()                         { return musteriSayaci; }
    public int getHesapSayaci()                           { return hesapSayaci; }
    public int getIslemSayaci()                           { return islemSayaci; }
}
