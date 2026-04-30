package service;

import model.Kullanici;
import persistence.FileLogger;

import java.util.HashMap;
import java.util.Map;

public class KimlikDogrulama {

    private static final int MAKS_BASARISIZ_GIRIS = 3;

    private final Map<String, Kullanici> kullanicilar;
    private final FileLogger kaydedici;

    public KimlikDogrulama(FileLogger kaydedici) {
        this.kullanicilar = new HashMap<>();
        this.kaydedici = kaydedici;
        varsayilanKullanicilariYukle();
    }

    private void varsayilanKullanicilariYukle() {
        kullanicilar.put("admin", new Kullanici("admin", "admin123", Kullanici.Rol.YONETICI, null));
    }

    public Kullanici girisYap(String kullaniciAdi, String sifre) {
        Kullanici kullanici = kullanicilar.get(kullaniciAdi);
        if (kullanici == null) {
            kaydedici.kaydet("GIRIS_BASARISIZ: Kullanici bulunamadi - " + kullaniciAdi);
            return null;
        }
        if (kullanici.isEngelliMi()) {
            kaydedici.kaydet("GIRIS_ENGELLENDI: " + kullaniciAdi);
            return null;
        }
        if (kullanici.isPasifMi()) {
            kaydedici.kaydet("GIRIS_PASIF_HESAP: " + kullaniciAdi);
            return null;
        }
        if (!kullanici.getSifre().equals(sifre)) {
            kullanici.basarisizGirisArtir();
            kaydedici.kaydet("GIRIS_BASARISIZ: " + kullaniciAdi
                    + " (" + kullanici.getBasarisizGirisSayisi() + ". deneme)");
            if (kullanici.getBasarisizGirisSayisi() >= MAKS_BASARISIZ_GIRIS) {
                kullanici.engelleHesap();
                kaydedici.kaydet("HESAP_ENGELLENDI: " + kullaniciAdi);
            }
            return null;
        }
        kullanici.girisBasarisizSayisiniSifirla();
        kaydedici.kaydet("GIRIS_BASARILI: " + kullaniciAdi);
        return kullanici;
    }

    public void kullaniciEkle(Kullanici kullanici) {
        kullanicilar.put(kullanici.getKullaniciAdi(), kullanici);
        kaydedici.kaydet("KULLANICI_OLUSTURULDU: " + kullanici.getKullaniciAdi()
                + " ROL=" + kullanici.getRol());
    }

    public boolean engelliMi(String kullaniciAdi) {
        Kullanici k = kullanicilar.get(kullaniciAdi);
        return k != null && k.isEngelliMi();
    }

    public boolean pasifMi(String kullaniciAdi) {
        Kullanici k = kullanicilar.get(kullaniciAdi);
        return k != null && k.isPasifMi();
    }

    public void kullanicilariYukle(Map<String, Kullanici> yuklenenler) {
        kullanicilar.clear();
        varsayilanKullanicilariYukle();
        for (Map.Entry<String, Kullanici> giris : yuklenenler.entrySet()) {
            kullanicilar.put(giris.getKey(), giris.getValue());
        }
        kaydedici.kaydet("KULLANICILAR_YUKLENDI: " + kullanicilar.size() + " kullanici");
    }

    public Map<String, Kullanici> getKullanicilar() {
        return kullanicilar;
    }
}
