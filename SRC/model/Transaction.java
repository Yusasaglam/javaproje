package model;

import java.io.Serializable;
import java.time.LocalDateTime;

public abstract class Transaction implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Durum { PENDING, APPROVED, REJECTED, FLAGGED }

    protected String        islemId;
    protected double        miktar;
    protected LocalDateTime zaman;
    private   Durum         durum;

    public Transaction(String islemId, double miktar) {
        this.islemId = islemId;
        this.miktar  = miktar;
        this.zaman   = LocalDateTime.now();
        this.durum   = Durum.PENDING;
    }

    public abstract String getTur();

    public String        getIslemId() { return islemId; }
    public double        getMiktar()  { return miktar; }
    public LocalDateTime getZaman()   { return zaman; }

    public Durum getDurum()            { return durum != null ? durum : Durum.PENDING; }
    public void  setDurum(Durum durum) { this.durum = durum; }
}
