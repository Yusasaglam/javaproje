package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    private String musteriId;
    private String ad;
    private String eposta;
    private List<Account> hesaplar;

    public Customer(String musteriId, String ad, String eposta) {
        this.musteriId = musteriId;
        this.ad = ad;
        this.eposta = eposta;
        this.hesaplar = new ArrayList<>();
    }

    public void hesapEkle(Account hesap) { hesaplar.add(hesap); }

    public String getMusteriId() { return musteriId; }
    public String getAd() { return ad; }
    public String getEposta() { return eposta; }
    public List<Account> getHesaplar() { return hesaplar; }

    public void setAd(String ad) { this.ad = ad; }
    public void setEposta(String eposta) { this.eposta = eposta; }
}
