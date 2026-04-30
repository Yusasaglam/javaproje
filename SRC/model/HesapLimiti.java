package model;

import java.io.Serializable;

public class HesapLimiti implements Serializable {

    private static final long serialVersionUID = 1L;

    private double gunlukCekimLimiti;
    private double gunlukTransferLimiti;
    private double tekIslemCekimLimiti;
    private double tekIslemTransferLimiti;

    public HesapLimiti(double gunlukCekimLimiti, double gunlukTransferLimiti,
                       double tekIslemCekimLimiti, double tekIslemTransferLimiti) {
        this.gunlukCekimLimiti      = gunlukCekimLimiti;
        this.gunlukTransferLimiti   = gunlukTransferLimiti;
        this.tekIslemCekimLimiti    = tekIslemCekimLimiti;
        this.tekIslemTransferLimiti = tekIslemTransferLimiti;
    }

    public double getGunlukCekimLimiti()       { return gunlukCekimLimiti; }
    public double getGunlukTransferLimiti()    { return gunlukTransferLimiti; }
    public double getTekIslemCekimLimiti()     { return tekIslemCekimLimiti; }
    public double getTekIslemTransferLimiti()  { return tekIslemTransferLimiti; }

    public void setGunlukCekimLimiti(double v)      { this.gunlukCekimLimiti = v; }
    public void setGunlukTransferLimiti(double v)   { this.gunlukTransferLimiti = v; }
    public void setTekIslemCekimLimiti(double v)    { this.tekIslemCekimLimiti = v; }
    public void setTekIslemTransferLimiti(double v) { this.tekIslemTransferLimiti = v; }
}
