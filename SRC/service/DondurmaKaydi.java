package service;

import java.io.Serializable;
import java.time.LocalDateTime;

public class DondurmaKaydi implements Serializable {

    private static final long serialVersionUID = 1L;

    public final String        hesapId;
    public final DondurmaSecegi sekil;
    public final LocalDateTime  dondurmaZamani;
    public final String        sebep;

    public DondurmaKaydi(String hesapId, DondurmaSecegi sekil,
                          LocalDateTime dondurmaZamani, String sebep) {
        this.hesapId       = hesapId;
        this.sekil         = sekil;
        this.dondurmaZamani = dondurmaZamani;
        this.sebep         = sebep;
    }
}
