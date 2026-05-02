package model;

public class YetersizBakiyeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final double mevcutBakiye;
    private final double istenenMiktar;

    public YetersizBakiyeException(double mevcutBakiye, double istenenMiktar) {
        super(String.format("Yetersiz bakiye: mevcut=%.2f ₺, istenen=%.2f ₺",
                mevcutBakiye, istenenMiktar));
        this.mevcutBakiye  = mevcutBakiye;
        this.istenenMiktar = istenenMiktar;
    }

    public double getMevcutBakiye()  { return mevcutBakiye; }
    public double getIstenenMiktar() { return istenenMiktar; }
}
