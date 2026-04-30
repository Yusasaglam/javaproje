package model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class SupheSebebi implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String        hesapId;
    private final String        musteriId;
    private final String        sebep;
    private final double        ilgiliMiktar;
    private final LocalDateTime zaman;

    public SupheSebebi(String hesapId, String musteriId, String sebep, double ilgiliMiktar) {
        this.hesapId      = hesapId;
        this.musteriId    = musteriId;
        this.sebep        = sebep;
        this.ilgiliMiktar = ilgiliMiktar;
        this.zaman        = LocalDateTime.now();
    }

    public String        getHesapId()      { return hesapId; }
    public String        getMusteriId()    { return musteriId; }
    public String        getSebep()        { return sebep; }
    public double        getIlgiliMiktar() { return ilgiliMiktar; }
    public LocalDateTime getZaman()        { return zaman; }
}
