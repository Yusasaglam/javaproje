package service;

import java.time.LocalDateTime;

public class RiskOlayi {

    private final String       hesapId;
    private final String       musteriId;
    private final RiskOlayTuru tur;
    private final double       miktar;
    private final String       mesaj;
    private final LocalDateTime zaman;

    public RiskOlayi(String hesapId, String musteriId,
                     RiskOlayTuru tur, double miktar, String mesaj) {
        this.hesapId   = hesapId;
        this.musteriId = musteriId;
        this.tur       = tur;
        this.miktar    = miktar;
        this.mesaj     = mesaj;
        this.zaman     = LocalDateTime.now();
    }

    public String       getHesapId()   { return hesapId; }
    public String       getMusteriId() { return musteriId; }
    public RiskOlayTuru getTur()       { return tur; }
    public double       getMiktar()    { return miktar; }
    public String       getMesaj()     { return mesaj; }
    public LocalDateTime getZaman()   { return zaman; }
}
