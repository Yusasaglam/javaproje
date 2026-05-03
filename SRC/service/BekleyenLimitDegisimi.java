package service;

import model.HesapLimiti;
import java.io.Serializable;
import java.time.LocalDateTime;

public class BekleyenLimitDegisimi implements Serializable {
    private static final long serialVersionUID = 1L;

    public final String        hesapId;
    public final HesapLimiti   yeniLimit;
    public final LocalDateTime talepZamani;
    public final LocalDateTime aktivasyonZamani;

    private static final int GECIKME_SAAT = 24;

    public BekleyenLimitDegisimi(String hesapId, HesapLimiti yeniLimit) {
        this.hesapId          = hesapId;
        this.yeniLimit        = yeniLimit;
        this.talepZamani      = LocalDateTime.now();
        this.aktivasyonZamani = talepZamani.plusHours(GECIKME_SAAT);
    }

    public boolean aktifMi() {
        return LocalDateTime.now().isAfter(aktivasyonZamani);
    }

    public long kalanDakika() {
        return java.time.temporal.ChronoUnit.MINUTES.between(
                LocalDateTime.now(), aktivasyonZamani);
    }
}
