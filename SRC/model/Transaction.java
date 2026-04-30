package model;

import java.io.Serializable;
import java.time.LocalDateTime;

public abstract class Transaction implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String islemId;
    protected double miktar;
    protected LocalDateTime zaman;

    public Transaction(String islemId, double miktar) {
        this.islemId = islemId;
        this.miktar = miktar;
        this.zaman = LocalDateTime.now();
    }

    public abstract String getTur();

    public String getIslemId() { return islemId; }
    public double getMiktar() { return miktar; }
    public LocalDateTime getZaman() { return zaman; }
}
