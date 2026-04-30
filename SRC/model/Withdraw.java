package model;

public class Withdraw extends Transaction {

    private static final long serialVersionUID = 1L;

    private String kaynakHesapId;

    public Withdraw(String islemId, double miktar, String kaynakHesapId) {
        super(islemId, miktar);
        this.kaynakHesapId = kaynakHesapId;
    }

    @Override
    public String getTur() { return "PARA_CEKME"; }

    public String getKaynakHesapId() { return kaynakHesapId; }
}
