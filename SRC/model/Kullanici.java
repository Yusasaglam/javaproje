package model;

import java.io.Serializable;

public class Kullanici implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Rol { YONETICI, MUSTERI }

    private String kullaniciAdi;
    private String sifre;
    private Rol rol;
    private String musteriId;
    private int basarisizGirisSayisi;
    private boolean engelliMi;
    private boolean pasifMi;

    public Kullanici(String kullaniciAdi, String sifre, Rol rol, String musteriId) {
        this.kullaniciAdi = kullaniciAdi;
        this.sifre = sifre;
        this.rol = rol;
        this.musteriId = musteriId;
        this.basarisizGirisSayisi = 0;
        this.engelliMi = false;
        this.pasifMi   = false;
    }

    public void basarisizGirisArtir() { basarisizGirisSayisi++; }
    public void girisBasarisizSayisiniSifirla() { basarisizGirisSayisi = 0; }
    public void engelleHesap() { engelliMi = true; }
    public void engelKaldir() { engelliMi = false; basarisizGirisSayisi = 0; }
    public void pasifYap()     { pasifMi = true; }
    public void aktifYap()     { pasifMi = false; }

    public String getKullaniciAdi() { return kullaniciAdi; }
    public String getSifre() { return sifre; }
    public void   setSifre(String sifre) { this.sifre = sifre; }
    public Rol getRol() { return rol; }
    public String getMusteriId() { return musteriId; }
    public int getBasarisizGirisSayisi() { return basarisizGirisSayisi; }
    public boolean isEngelliMi() { return engelliMi; }
    public boolean isPasifMi()   { return pasifMi; }
}
