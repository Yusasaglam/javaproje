package model;

public class CheckingAccount extends Account {

    private static final long serialVersionUID = 1L;

    public CheckingAccount(String hesapId, String sahibiId, double bakiye) {
        super(hesapId, sahibiId, bakiye);
    }

    @Override
    public String getHesapTuru() { return "VADESİZ"; }
}
