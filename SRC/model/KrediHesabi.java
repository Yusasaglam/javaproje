package model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class KrediHesabi extends Account {

    private static final long serialVersionUID = 2L;

    private double    krediLimiti;
    private double    faizOrani;          // aylık faiz (ör. 0.02 = %2)
    private LocalDate sonOdemeTarihi;     // son ödeme veya hesap açılış tarihi

    public KrediHesabi(String hesapId, String sahibiId,
                       double krediLimiti, double faizOrani) {
        super(hesapId, sahibiId, 0.0);
        this.krediLimiti      = krediLimiti;
        this.faizOrani        = faizOrani;
        this.sonOdemeTarihi   = LocalDate.now();
    }

    /** Kredi çekimi: bakiye negatif limitin altına inemez. */
    @Override
    public boolean paraCek(double miktar) {
        if (bakiye - miktar < -krediLimiti) return false;
        bakiye -= miktar;
        return true;
    }

    /**
     * Mevcut borcu öder. Kaynaktan zaten düşülmüş olmalı;
     * bu method yalnızca kredi hesabındaki borcu azaltır.
     */
    public double krediOde(double odeme) {
        if (bakiye >= 0 || odeme <= 0) return 0;
        double gercekOdeme = Math.min(odeme, -bakiye); // borcu aşamaz
        bakiye += gercekOdeme;
        sonOdemeTarihi = LocalDate.now();
        return gercekOdeme;
    }

    /** Aylık faiz — 30 günden uzun süre ödeme yapılmamışsa uygular. */
    public boolean aylikFaizUygula() {
        if (bakiye >= 0) return false;
        long gecenGun = sonOdemeTarihi != null
                ? ChronoUnit.DAYS.between(sonOdemeTarihi, LocalDate.now()) : 999;
        if (gecenGun < 30) return false;
        bakiye += bakiye * faizOrani;
        return true;
    }

    /** Kalan kullanılabilir kredi. */
    public double kalanKredi() { return krediLimiti + bakiye; }

    /** Toplam borcun (negatif bakiyenin mutlak değeri). */
    public double getBorcMiktari() { return bakiye < 0 ? -bakiye : 0; }

    /** Son ödeme tarihinden bu yana geçen gün sayısı. */
    public long getOdenmeyenGunSayisi() {
        if (sonOdemeTarihi == null) return 999;
        return ChronoUnit.DAYS.between(sonOdemeTarihi, LocalDate.now());
    }

    @Override
    public String getHesapTuru() { return "KREDİ"; }

    public double    getKrediLimiti()    { return krediLimiti; }
    public double    getFaizOrani()      { return faizOrani; }
    public LocalDate getSonOdemeTarihi() { return sonOdemeTarihi; }

    public void setKrediLimiti(double limit) { this.krediLimiti = limit; }
    public void setFaizOrani(double oran)    { this.faizOrani   = oran;  }

    @Override
    public boolean yuksekRiskMi(double miktar) { return miktar > 20_000; }

    @Override
    public double getRiskPuani() {
        if (krediLimiti <= 0) return 100.0;
        double kullanilanOran = bakiye < 0 ? (-bakiye / krediLimiti) : 0;
        return Math.min(100.0, kullanilanOran * 100.0);
    }
}
