package model;

public class Deposit extends Transaction {

    private static final long serialVersionUID = 1L;

    private String hedefHesapId;

    public Deposit(String islemId, double miktar, String hedefHesapId) {
        super(islemId, miktar);
        this.hedefHesapId = hedefHesapId;
    }

    @Override
    public String getTur() { return "PARA_YATIRMA"; }

    public String getHedefHesapId() { return hedefHesapId; }
}
