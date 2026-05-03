package service;

import model.Kullanici;
import persistence.FileLogger;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
        // Admin şifresi SHA-256 ile hash'lenerek saklanır
        String hashliSifre = HashUtil.sha256("admin123");
        kullanicilar.put("admin", new Kullanici("admin", hashliSifre, Kullanici.Rol.YONETICI, null));
    }

    public Kullanici girisYap(String kullaniciAdi, String sifre) {
        Kullanici kullanici = kullanicilar.get(kullaniciAdi);
        if (kullanici == null) {
            kaydedici.kaydet("GIRIS_BASARISIZ: Kullanici bulunamadi - " + kullaniciAdi);
            return null;
        }
        if (kullanici.isEngelliMi()) {
            LocalDateTime saati = kullanici.getEngellemeSaati();
            if (saati != null && LocalDateTime.now().isAfter(saati.plusMinutes(10))) {
                kullanici.engelKaldir();
                kaydedici.kaydet("ENGEL_OTOMATIK_KALDIRILDI: " + kullaniciAdi);
            } else {
                kaydedici.kaydet("GIRIS_ENGELLENDI: " + kullaniciAdi);
                return null;
            }
        }
        if (kullanici.isPasifMi()) {
            kaydedici.kaydet("GIRIS_PASIF_HESAP: " + kullaniciAdi);
            return null;
        }

        // Otomatik geçiş: düz metin şifre ise hash'e çevir
        String storedSifre = kullanici.getSifre();
        boolean eslesme;
        if (HashUtil.hashMi(storedSifre)) {
            eslesme = storedSifre.equals(HashUtil.sha256(sifre));
        } else {
            eslesme = storedSifre.equals(sifre);
            if (eslesme) {
                kullanici.setSifre(HashUtil.sha256(sifre));
                kaydedici.kaydet("SIFRE_HASHLE_GUNCELLENDI: " + kullaniciAdi);
            }
        }

        if (!eslesme) {
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

    /** Yeni kullanıcı ekler — şifre otomatik olarak SHA-256 ile hash'lenir. */
    public void kullaniciEkle(Kullanici kullanici) {
        // Düz metin şifre geldiyse hash'le
        if (!HashUtil.hashMi(kullanici.getSifre())) {
            kullanici.setSifre(HashUtil.sha256(kullanici.getSifre()));
        }
        kullanicilar.put(kullanici.getKullaniciAdi(), kullanici);
        kaydedici.kaydet("KULLANICI_OLUSTURULDU: " + kullanici.getKullaniciAdi()
                + " ROL=" + kullanici.getRol());
    }

    public boolean engelliMi(String kullaniciAdi) {
        Kullanici k = kullanicilar.get(kullaniciAdi);
        return k != null && k.isEngelliMi();
    }

    public boolean kullaniciVarMi(String kullaniciAdi) {
        return kullanicilar.containsKey(kullaniciAdi);
    }

    public long kalanBeklemeSaniyesi(String kullaniciAdi) {
        Kullanici k = kullanicilar.get(kullaniciAdi);
        if (k == null || !k.isEngelliMi() || k.getEngellemeSaati() == null) return 0;
        long kalan = ChronoUnit.SECONDS.between(LocalDateTime.now(), k.getEngellemeSaati().plusMinutes(10));
        return Math.max(0, kalan);
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

    /** Admin şifre sıfırlama — eski şifre doğrulaması YOK. */
    public boolean sifreSifirla(String kullaniciAdi, String yeniSifre) {
        Kullanici k = kullanicilar.get(kullaniciAdi);
        if (k == null) return false;
        k.setSifre(HashUtil.sha256(yeniSifre));
        kaydedici.kaydet("SIFRE_SIFIRLANDI: " + kullaniciAdi);
        return true;
    }

    /**
     * Mevcut şifreyi doğrulayıp yeni şifreyi SHA-256 olarak kaydeder.
     * @return true → başarılı, false → mevcut şifre hatalı veya kullanıcı yok
     */
    public boolean sifreDegistir(String kullaniciAdi, String eskiSifre, String yeniSifre) {
        Kullanici k = kullanicilar.get(kullaniciAdi);
        if (k == null) return false;
        String sakli = k.getSifre();
        boolean dogru = HashUtil.hashMi(sakli)
            ? sakli.equals(HashUtil.sha256(eskiSifre))
            : sakli.equals(eskiSifre);
        if (!dogru) return false;
        k.setSifre(HashUtil.sha256(yeniSifre));
        kaydedici.kaydet("SIFRE_DEGISTIRILDI: " + kullaniciAdi);
        return true;
    }
}
