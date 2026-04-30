package model;

public class Transfer extends Transaction {

    private static final long serialVersionUID = 1L;

    private final String gondereciHesapId;
    private final String aliciHesapId;

    public Transfer(String islemId, double miktar, String gondereciHesapId, String aliciHesapId) {
        super(islemId, miktar);
        this.gondereciHesapId = gondereciHesapId;
        this.aliciHesapId = aliciHesapId;
    }

    @Override
    public String getTur() { return "TRANSFER"; }

    public String getGondereciHesapId() { return gondereciHesapId; }
    public String getAliciHesapId() { return aliciHesapId; }
}
