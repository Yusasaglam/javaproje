package model;

public class SavingsAccount extends Account {

    private static final long serialVersionUID = 1L;

    private double faizOrani;

    public SavingsAccount(String hesapId, String sahibiId, double bakiye, double faizOrani) {
        super(hesapId, sahibiId, bakiye);
        this.faizOrani = faizOrani;
    }

    public void faizUygula() { bakiye += bakiye * faizOrani; }

    @Override
    public String getHesapTuru() { return "VADELİ"; }

    public double getFaizOrani()              { return faizOrani; }
    public void   setFaizOrani(double oran)   { this.faizOrani = oran; }
}
