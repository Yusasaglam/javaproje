package model;

public class RiskLimitiAsildiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final double limit;
    private final double istenenMiktar;

    public RiskLimitiAsildiException(String sebep, double limit, double istenenMiktar) {
        super(String.format("%s (limit: %.2f ₺, istenen: %.2f ₺)", sebep, limit, istenenMiktar));
        this.limit         = limit;
        this.istenenMiktar = istenenMiktar;
    }

    public double getLimit()         { return limit; }
    public double getIstenenMiktar() { return istenenMiktar; }
}
