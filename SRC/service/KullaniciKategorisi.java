package service;

public enum KullaniciKategorisi {
    BIREYSEL(10, 10),  // eşik=10 işlem/5dk, 1.ihlal ceza=+10
    KURUMSAL(50,  5),  // eşik=50 işlem/5dk, 1.ihlal ceza=+5
    PREMIUM (25,  7);  // eşik=25 işlem/5dk, 1.ihlal ceza=+7

    public final int velocityEsigi;
    public final int baseCeza;

    KullaniciKategorisi(int velocityEsigi, int baseCeza) {
        this.velocityEsigi = velocityEsigi;
        this.baseCeza      = baseCeza;
    }
}
