package service;

import java.io.Serializable;
import java.time.LocalDate;

public class MusteriRiskProfili implements Serializable {

    private static final long serialVersionUID = 1L;

    public final String    musteriId;
    public       int       musteriSkoru;
    public       LocalDate skorGuncelleme;

    public MusteriRiskProfili(String musteriId) {
        this.musteriId      = musteriId;
        this.musteriSkoru   = 0;
        this.skorGuncelleme = LocalDate.now();
    }
}
