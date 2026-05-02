package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public abstract class Account implements Serializable, IRiskCalculatable {

    private static final long serialVersionUID = 1L;

    protected String          hesapId;
    protected String          sahibiId;
    protected double          bakiye;
    protected List<Transaction> islemler;

    public Account(String hesapId, String sahibiId, double bakiye) {
        this.hesapId  = hesapId;
        this.sahibiId = sahibiId;
        this.bakiye   = bakiye;
        this.islemler = new ArrayList<>();
    }

    public abstract String getHesapTuru();

    public void paraYatir(double miktar) { bakiye += miktar; }

    public boolean paraCek(double miktar) {
        if (miktar > bakiye) return false;
        bakiye -= miktar;
        return true;
    }

    public void islemEkle(Transaction islem) { islemler.add(islem); }

    public String          getHesapId()  { return hesapId; }
    public String          getSahibiId() { return sahibiId; }
    public double          getBakiye()   { return bakiye; }
    public List<Transaction> getIslemler() { return islemler; }

    // ── IRiskCalculatable ──────────────────────────────────────────────────────
    @Override
    public boolean yuksekRiskMi(double miktar) { return miktar > 50_000; }

    @Override
    public double getRiskPuani() {
        if (bakiye <= 0)        return 80.0;
        if (bakiye < 1_000)    return 60.0;
        if (bakiye < 10_000)   return 40.0;
        return 20.0;
    }
}
