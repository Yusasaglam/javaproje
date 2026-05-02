# Türk Bankası — Tam Kod Rehberi
### Her dosya, her satır, Java bilmeyenler için açıklamalı

---

## ÖNCE: Java'da Bilmeniz Gereken Temel Kavramlar

Java kodu okurken şu kavramları bilmek yeterli:

| Kavram | Ne demek |
|---|---|
| `class` | Bir şeyin tarifi / kalıbı. "Hesap" sınıfı, bir hesabın nasıl olduğunu tanımlar. |
| `object` | O kalıptan üretilen gerçek nesne. HSP000001 nolu hesap bir object'tir. |
| `extends` | Kalıtım. "A extends B" → A, B'nin tüm özelliklerini alır + kendi özelliklerini ekler. |
| `implements` | Sözleşme uygulamak. "A implements B" → A, B'nin söylediği metodları yazmak zorunda. |
| `interface` | Sadece method isimleri olan sözleşme. İçinde kod yok, sadece "şu metodlar olmalı" diyor. |
| `abstract` | Yarı tamamlanmış. Doğrudan kullanılamaz, önce alt sınıf tamamlamalı. |
| `private` | Sadece bu sınıf içinden erişilebilir. |
| `public` | Her yerden erişilebilir. |
| `static` | Nesne oluşturmadan çağrılabilir. `Math.abs()` gibi. |
| `final` | Değiştirilemez. `final` bir değişken bir kez atanır, sonra değişmez. |
| `void` | Metot bir şey döndürmüyor. |
| `return` | Metottan bir değer döndür ve çık. |
| `new` | Yeni nesne oluştur. `new Customer(...)` → yeni müşteri nesnesi. |
| `this` | "Bu sınıfın kendisi." `this.ad = ad` → sınıfın `ad` alanına parametre `ad`'ı ata. |
| `List<T>` | Sıralı liste. `List<Account>` = Account listesi. |
| `Map<K,V>` | Anahtar-değer çifti. `Map<String, Integer>` = yazı → sayı eşleştirmesi. |
| `Set<T>` | Tekrarsız küme. Aynı eleman iki kez eklenemez. |
| `@Override` | "Bu metodu üst sınıftan alıp yeniden yazıyorum" notu. |
| `throws` | Bu metot hata fırlatabilir. |
| `try/catch` | Hata yakala ve işle. |
| `instanceof` | "Bu nesne şu türden mi?" kontrolü. |
| `enum` | Sabit seçenekler kümesi. `enum Renk { KIRMIZI, YESIL, MAVI }` |
| `//` veya `/* */` | Yorum satırı, Java çalıştırmaz, açıklama için. |

---

## BÖLÜM 1: Main.java — Uygulamanın Başlangıç Noktası

```java
import javafx.application.Application;  // JavaFX ana sınıfını dahil et
import javafx.stage.Stage;              // Pencere (Stage = sahne/sahne taşıyıcısı)
import ui.GirisEkrani;                  // Bizim yazdığımız giriş ekranı sınıfı

public class Main extends Application { // Main sınıfı, JavaFX Application'dan kalıtım alır
                                         // Bu sayede JavaFX uygulaması olarak çalışır

    @Override
    public void start(Stage stage) {     // JavaFX bu metodu otomatik çağırır
                                         // stage = ana pencere
        new GirisEkrani(stage);          // Giriş ekranını oluştur ve göster
    }

    public static void main(String[] args) { // Program buradan başlar
        launch(args);                         // JavaFX runtime'ı başlat, start() çağırılır
    }
}
```

**Ne yapıyor:** Tek görevi JavaFX'i başlatmak ve `GirisEkrani`'nı açmak. Başka iş mantığı yok.

---

## BÖLÜM 2: model/ Paketi — Veri Sınıfları

Bu paketteki sınıflar **veri taşır**. Bir müşterinin adı, hesabın bakiyesi gibi bilgileri saklar. Çok karmaşık iş mantığı içermezler.

---

### 2.1 IRiskCalculatable.java — Risk Arayüzü

```java
package model;

public interface IRiskCalculatable {        // Arayüz (interface) = sözleşme
    boolean yuksekRiskMi(double miktar);    // Bu metod mutlaka var olmalı
    double getRiskPuani();                  // Bu metod da mutlaka var olmalı
}
```

**Ne yapıyor:** "Risk hesaplayabilen her sınıf şu iki metodu yazmalı" diyen bir kural belgesi. `Account` ve `RiskEngine` bu kurala uyar. Hiçbir kod içermez, sadece metodların **adını ve parametrelerini** belirtir.

---

### 2.2 Account.java — Tüm Hesapların Şablonu

```java
package model;

import java.io.Serializable;  // Diske kaydedilebilmek için gerekli
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")   // Uyarıyı sustur (serialVersionUID eksikliği)
public abstract class Account   // abstract = doğrudan kullanılamaz, alt sınıf gerekir
        implements Serializable,      // Diske kaydedilebilir
                   IRiskCalculatable { // Risk hesaplama sözleşmesine uyar

    private static final long serialVersionUID = 1L; // Kayıt/yükleme için sürüm numarası

    // ── Alanlar (fields) ─────────────────────────────────────────────────────
    protected String            hesapId;    // "HSP000001" gibi benzersiz kimlik
    protected String            sahibiId;   // Hesabın sahibi müşterinin ID'si
    protected double            bakiye;     // Hesaptaki para (₺ veya döviz)
    protected List<Transaction> islemler;   // Bu hesaptaki tüm işlemlerin listesi

    // ── Kurucu (constructor) ──────────────────────────────────────────────────
    public Account(String hesapId, String sahibiId, double bakiye) {
        this.hesapId  = hesapId;             // Gelen hesapId'yi sınıf alanına ata
        this.sahibiId = sahibiId;            // Gelen sahibiId'yi sınıf alanına ata
        this.bakiye   = bakiye;              // Gelen bakiyeyi sınıf alanına ata
        this.islemler = new ArrayList<>();   // Boş işlem listesi oluştur
    }

    // ── Abstract metod: alt sınıf yazmak zorunda ──────────────────────────────
    public abstract String getHesapTuru();   // "VADESİZ", "VADELİ" vb. döner
                                             // Her hesap türü kendisi yazar

    // ── Para işlemleri ────────────────────────────────────────────────────────
    public void paraYatir(double miktar) {
        bakiye += miktar;    // Bakiyeye miktar ekle
    }

    public boolean paraCek(double miktar) {
        if (miktar > bakiye) return false;  // Yeterli bakiye yoksa false döner
        bakiye -= miktar;                   // Bakiyeden miktar düş
        return true;                        // Başarılı
    }

    public void islemEkle(Transaction islem) {
        islemler.add(islem);  // İşlem geçmişine ekle
    }

    // ── Okuma metodları (getters) ────────────────────────────────────────────
    public String            getHesapId()    { return hesapId; }
    public String            getSahibiId()   { return sahibiId; }
    public double            getBakiye()     { return bakiye; }
    public List<Transaction> getIslemler()   { return islemler; }

    // ── IRiskCalculatable sözleşmesini uygula ────────────────────────────────
    @Override
    public boolean yuksekRiskMi(double miktar) {
        return miktar > 50_000;   // 50 binden fazlaysa yüksek risk
    }

    @Override
    public double getRiskPuani() {
        if (bakiye <= 0)       return 80.0;  // Bakiye sıfır veya negatifse yüksek risk
        if (bakiye < 1_000)    return 60.0;
        if (bakiye < 10_000)   return 40.0;
        return 20.0;                         // Bakiye iyiyse düşük risk
    }
}
```

**Ne yapıyor:** `CheckingAccount`, `SavingsAccount`, `DovizHesabi`, `KrediHesabi` — hepsi bu sınıfı genişletir. Ortak alanları (`hesapId`, `bakiye` vb.) ve ortak metodları (`paraYatir`, `paraCek`) burada tanımlanmış, tekrar tekrar yazmaya gerek yok.

---

### 2.3 CheckingAccount.java — Vadesiz Hesap

```java
package model;

public class CheckingAccount extends Account { // Account'tan kalıtım alır

    private static final long serialVersionUID = 1L;

    // Kurucu: sadece üst sınıfa (Account) yönlendir
    public CheckingAccount(String hesapId, String sahibiId, double bakiye) {
        super(hesapId, sahibiId, bakiye);  // Account'un kurucusunu çağır
    }

    @Override
    public String getHesapTuru() {
        return "VADESİZ";  // Bu hesap türünün adı
    }
}
```

**Ne yapıyor:** En basit hesap. Account'tan her şeyi alır, sadece türünü "VADESİZ" olarak belirtir. Faiz yok, özel özellik yok.

---

### 2.4 SavingsAccount.java — Vadeli Hesap

```java
package model;

public class SavingsAccount extends Account {

    private static final long serialVersionUID = 1L;
    private double faizOrani;  // Yıllık faiz oranı (0.03 = %3)

    public SavingsAccount(String hesapId, String sahibiId,
                          double bakiye, double faizOrani) {
        super(hesapId, sahibiId, bakiye);  // Account'u başlat
        this.faizOrani = faizOrani;         // Faiz oranını kaydet
    }

    // Faiz uygula: bakiyeye faiz ekle
    public void faizUygula() {
        bakiye += bakiye * faizOrani;  // Örn: 10000 * 0.03 = 300 ₺ eklenir
    }

    @Override
    public String getHesapTuru() { return "VADELİ"; }

    public double getFaizOrani()            { return faizOrani; }
    public void   setFaizOrani(double oran) { this.faizOrani = oran; }
}
```

**Ne yapıyor:** Faiz tutabilen hesap. Admin "Vadeli Hesaplara Faiz Uygula" dediğinde `faizUygula()` çağrılır ve bakiye artar.

---

### 2.5 KrediHesabi.java — Kredi Hesabı

```java
package model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class KrediHesabi extends Account {

    private static final long serialVersionUID = 2L;

    private double    krediLimiti;     // Maksimum borçlanma sınırı
    private double    faizOrani;       // Aylık faiz (0.02 = %2/ay)
    private LocalDate sonOdemeTarihi;  // Son ödeme veya açılış tarihi

    public KrediHesabi(String hesapId, String sahibiId,
                       double krediLimiti, double faizOrani) {
        super(hesapId, sahibiId, 0.0); // Başlangıç bakiyesi SIFIR
                                        // Bakiye negatife gidecek (borç)
        this.krediLimiti    = krediLimiti;
        this.faizOrani      = faizOrani;
        this.sonOdemeTarihi = LocalDate.now();
    }

    // Kredi çekimi: bakiye negatife gider, ama limitin altına inemez
    @Override
    public boolean paraCek(double miktar) {
        if (bakiye - miktar < -krediLimiti) return false;  // Limit aşılır mı?
        bakiye -= miktar;  // Örn: bakiye=0, miktar=1000 → bakiye=-1000 (1000₺ borç)
        return true;
    }

    // Borç öde
    public double krediOde(double odeme) {
        if (bakiye >= 0 || odeme <= 0) return 0; // Borç yoksa ödeme yapma
        double gercekOdeme = Math.min(odeme, -bakiye); // Borçtan fazla ödeme olmaz
        bakiye += gercekOdeme;   // Örn: bakiye=-1000, ödeme=500 → bakiye=-500
        sonOdemeTarihi = LocalDate.now();
        return gercekOdeme;
    }

    // Aylık faiz: 30 günden uzun süre ödeme yapılmamışsa faiz yap
    public boolean aylikFaizUygula() {
        if (bakiye >= 0) return false;  // Borç yoksa faiz yok
        long gecenGun = ChronoUnit.DAYS.between(sonOdemeTarihi, LocalDate.now());
        if (gecenGun < 30) return false;  // 30 gün dolmamış
        bakiye += bakiye * faizOrani;     // Örn: bakiye=-1000, faiz=0.02 → bakiye=-1020
        return true;
    }

    public double kalanKredi()      { return krediLimiti + bakiye; } // Kalan kullanılabilir limit
    public double getBorcMiktari()  { return bakiye < 0 ? -bakiye : 0; } // Borcun pozitif değeri

    @Override public String getHesapTuru()  { return "KREDİ"; }
    public double getKrediLimiti()          { return krediLimiti; }
    public double getFaizOrani()            { return faizOrani; }

    public void setKrediLimiti(double l)    { this.krediLimiti = l; }
    public void setFaizOrani(double o)      { this.faizOrani   = o; }

    // Kredi hesabı için yüksek risk eşiği daha düşük (20K)
    @Override public boolean yuksekRiskMi(double miktar) { return miktar > 20_000; }

    // Risk puanı: kullanılan kredi oranına göre
    @Override
    public double getRiskPuani() {
        if (krediLimiti <= 0) return 100.0;
        double kullanilanOran = bakiye < 0 ? (-bakiye / krediLimiti) : 0;
        return Math.min(100.0, kullanilanOran * 100.0); // %80 kullandıysa puan 80
    }
}
```

**Ne yapıyor:** Bakiyesi negatife gidebilen özel hesap. 1000₺ kredi çekince bakiye -1000₺ olur. `kalanKredi()` ne kadar daha çekebileceğini söyler. 30 gün ödeme yoksa faiz işler.

---

### 2.6 DovizHesabi.java — Döviz Hesabı

```java
package model;

public class DovizHesabi extends Account {

    private static final long serialVersionUID = 1L;

    public enum ParaBirimi { USD, EUR, GBP }  // 3 döviz türü

    private final ParaBirimi paraBirimi;   // Hangi döviz? (değişmez)
    private double           dovizKuru;    // 1 döviz = kaç TL (değişebilir)

    public DovizHesabi(String hesapId, String sahibiId,
                       double bakiyeYabanciPara,  // Bakiye dolar/euro/sterlin
                       ParaBirimi paraBirimi,
                       double dovizKuru) {
        super(hesapId, sahibiId, bakiyeYabanciPara);
        this.paraBirimi = paraBirimi;
        this.dovizKuru  = dovizKuru;
    }

    // TL karşılığını hesapla
    public double getBakiyeTL() {
        return bakiye * dovizKuru; // 100$ × 32.5 = 3250₺
    }

    // Kur güncelle
    public void dovizKuruGuncelle(double yeniKur) {
        this.dovizKuru = yeniKur;
    }

    @Override
    public String getHesapTuru() {
        return "DÖVİZ-" + paraBirimi.name();  // "DÖVİZ-USD", "DÖVİZ-EUR" vb.
    }

    public ParaBirimi getParaBirimi() { return paraBirimi; }
    public double     getDovizKuru()  { return dovizKuru; }

    // Para birimi sembolü
    public String getParaBirimiSimgesi() {
        switch (paraBirimi) {
            case USD: return "$";
            case EUR: return "€";
            case GBP: return "£";
            default:  return "?";
        }
    }
}
```

**Ne yapıyor:** Bakiyeyi dolar/euro/sterlin cinsinden tutar. `getBakiyeTL()` güncel kur ile TL karşılığını hesaplar. Admin kuru güncellediğinde bakiye değişmez ama TL karşılığı değişir.

---

### 2.7 Transaction.java — İşlem Şablonu

```java
package model;

import java.io.Serializable;
import java.time.LocalDateTime;

public abstract class Transaction implements Serializable {  // abstract: doğrudan kullanılamaz

    private static final long serialVersionUID = 1L;

    // İşlem durumları
    public enum Durum {
        PENDING,   // Bekliyor
        APPROVED,  // Onaylandı
        REJECTED,  // Reddedildi
        FLAGGED    // İşaretlendi (şüpheli)
    }

    protected String        islemId;  // "TRX00000001"
    protected double        miktar;   // İşlem tutarı
    protected LocalDateTime zaman;    // Ne zaman yapıldı
    private   Durum         durum;    // Mevcut durum

    public Transaction(String islemId, double miktar) {
        this.islemId = islemId;
        this.miktar  = miktar;
        this.zaman   = LocalDateTime.now();  // Şu an
        this.durum   = Durum.PENDING;        // Başlangıçta bekliyor
    }

    public abstract String getTur();  // Alt sınıf yazar: "PARA_YATIRMA" vb.

    public String        getIslemId() { return islemId; }
    public double        getMiktar()  { return miktar; }
    public LocalDateTime getZaman()   { return zaman; }
    public Durum         getDurum()   { return durum != null ? durum : Durum.PENDING; }
    public void          setDurum(Durum durum) { this.durum = durum; }
}
```

---

### 2.8 Deposit.java — Para Yatırma

```java
package model;

public class Deposit extends Transaction {  // Transaction'dan kalıtım

    private static final long serialVersionUID = 1L;
    private String hedefHesapId;  // Paraya yatırılan hesap

    public Deposit(String islemId, double miktar, String hedefHesapId) {
        super(islemId, miktar);    // Transaction kurucusu
        this.hedefHesapId = hedefHesapId;
    }

    @Override
    public String getTur() { return "PARA_YATIRMA"; }

    public String getHedefHesapId() { return hedefHesapId; }
}
```

### 2.9 Withdraw.java — Para Çekme

```java
package model;

public class Withdraw extends Transaction {

    private static final long serialVersionUID = 1L;
    private String kaynakHesapId;  // Paradan çekildiği hesap

    public Withdraw(String islemId, double miktar, String kaynakHesapId) {
        super(islemId, miktar);
        this.kaynakHesapId = kaynakHesapId;
    }

    @Override
    public String getTur() { return "PARA_CEKME"; }

    public String getKaynakHesapId() { return kaynakHesapId; }
}
```

### 2.10 Transfer.java — Transfer

```java
package model;

public class Transfer extends Transaction {

    private static final long serialVersionUID = 1L;
    private final String gondereciHesapId;  // Gönderen hesap
    private final String aliciHesapId;      // Alan hesap

    public Transfer(String islemId, double miktar,
                    String gondereciHesapId, String aliciHesapId) {
        super(islemId, miktar);
        this.gondereciHesapId = gondereciHesapId;
        this.aliciHesapId     = aliciHesapId;
    }

    @Override
    public String getTur() { return "TRANSFER"; }

    public String getGondereciHesapId() { return gondereciHesapId; }
    public String getAliciHesapId()     { return aliciHesapId; }
}
```

**Ne yapıyor (üç sınıf birlikte):** Her işlem türü ayrı bir sınıf. `Deposit` para yatırmayı, `Withdraw` para çekmeyi, `Transfer` transferi temsil eder. İşlem geçmişinde hangi türde işlem olduğunu `getTur()` ile anlıyoruz.

---

### 2.11 Customer.java — Müşteri

```java
package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    private String         musteriId;  // "MUS00001"
    private String         ad;          // "Ahmet Yılmaz"
    private String         eposta;      // "ahmet@ornek.com"
    private List<Account>  hesaplar;    // Bu müşteriye ait hesaplar listesi

    public Customer(String musteriId, String ad, String eposta) {
        this.musteriId = musteriId;
        this.ad        = ad;
        this.eposta    = eposta;
        this.hesaplar  = new ArrayList<>();  // Başlangıçta hesap yok
    }

    // Yeni hesap ekle
    public void hesapEkle(Account hesap) {
        hesaplar.add(hesap);
    }

    // Okuma/yazma metodları
    public String         getMusteriId()  { return musteriId; }
    public String         getAd()         { return ad; }
    public String         getEposta()     { return eposta; }
    public List<Account>  getHesaplar()   { return hesaplar; }  // Listeye doğrudan erişim

    public void setAd(String ad)          { this.ad = ad; }
    public void setEposta(String eposta)  { this.eposta = eposta; }
}
```

**Ne yapıyor:** Bir banka müşterisinin verilerini tutar. Dikkat: `getHesaplar()` listeyi doğrudan döndürür — dışarıdan `list.remove(hesap)` ile hesap silinebilir.

---

### 2.12 Kullanici.java — Giriş Yapan Kişi

```java
package model;

import java.io.Serializable;

public class Kullanici implements Serializable {

    private static final long serialVersionUID = 1L;

    // Kullanıcı rolleri
    public enum Rol { YONETICI, MUSTERI }

    private String  kullaniciAdi;         // Giriş adı ("admin", "ahmet")
    private String  sifre;                // SHA-256 hashlenmiş şifre
    private Rol     rol;                  // YONETICI veya MUSTERI
    private String  musteriId;            // Bağlı müşteri ID'si (admin için null)
    private int     basarisizGirisSayisi; // 3'e ulaşınca hesap kilitlenir
    private boolean engelliMi;            // true = giriş yapamaz (kilitli)
    private boolean pasifMi;              // true = giriş yapamaz (devre dışı)

    public Kullanici(String kullaniciAdi, String sifre, Rol rol, String musteriId) {
        this.kullaniciAdi = kullaniciAdi;
        this.sifre        = sifre;
        this.rol          = rol;
        this.musteriId    = musteriId;
        this.basarisizGirisSayisi = 0;
        this.engelliMi    = false;
        this.pasifMi      = false;
    }

    // Hesap yönetim metodları
    public void basarisizGirisArtir() { basarisizGirisSayisi++; }
    public void girisBasarisizSayisiniSifirla() { basarisizGirisSayisi = 0; }
    public void engelleHesap()  { engelliMi = true; }
    public void engelKaldir()   { engelliMi = false; basarisizGirisSayisi = 0; }
    public void pasifYap()      { pasifMi = true; }
    public void aktifYap()      { pasifMi = false; }

    // Okuma metodları
    public String  getKullaniciAdi()          { return kullaniciAdi; }
    public String  getSifre()                 { return sifre; }
    public void    setSifre(String sifre)     { this.sifre = sifre; }
    public Rol     getRol()                   { return rol; }
    public String  getMusteriId()             { return musteriId; }
    public int     getBasarisizGirisSayisi()  { return basarisizGirisSayisi; }
    public boolean isEngelliMi()              { return engelliMi; }
    public boolean isPasifMi()                { return pasifMi; }
}
```

**Ne yapıyor:** `Customer`'dan tamamen ayrı bir sınıf. `Customer` = banka müşterisi (hesapları, parası var). `Kullanici` = uygulamaya giriş yapan kişi (kullanıcı adı, şifre). `musteriId` ile ikisi birbirine bağlanır. Admin'in `musteriId`'si `null`'dur — adminin banka hesabı yok.

---

### 2.13 HesapLimiti.java — İşlem Limitleri

```java
package model;

import java.io.Serializable;

public class HesapLimiti implements Serializable {

    private static final long serialVersionUID = 1L;

    private double gunlukCekimLimiti;       // Günde toplam çekilebilecek max
    private double gunlukTransferLimiti;    // Günde toplam transfer max
    private double tekIslemCekimLimiti;     // Tek seferde çekilebilecek max
    private double tekIslemTransferLimiti;  // Tek seferde transfer max

    public HesapLimiti(double gunlukCekim, double gunlukTransfer,
                       double tekCekim, double tekTransfer) {
        this.gunlukCekimLimiti      = gunlukCekim;
        this.gunlukTransferLimiti   = gunlukTransfer;
        this.tekIslemCekimLimiti    = tekCekim;
        this.tekIslemTransferLimiti = tekTransfer;
    }

    // Okuma metodları
    public double getGunlukCekimLimiti()      { return gunlukCekimLimiti; }
    public double getGunlukTransferLimiti()   { return gunlukTransferLimiti; }
    public double getTekIslemCekimLimiti()    { return tekIslemCekimLimiti; }
    public double getTekIslemTransferLimiti() { return tekIslemTransferLimiti; }

    // Yazma metodları
    public void setGunlukCekimLimiti(double v)      { gunlukCekimLimiti = v; }
    public void setGunlukTransferLimiti(double v)   { gunlukTransferLimiti = v; }
    public void setTekIslemCekimLimiti(double v)    { tekIslemCekimLimiti = v; }
    public void setTekIslemTransferLimiti(double v) { tekIslemTransferLimiti = v; }
}
```

**Ne yapıyor:** Sadece 4 sayı tutar. Her hesabın özel limiti olabilir; yoksa `RiskEngine`'deki varsayılan limitler kullanılır.

---

### 2.14 SupheSebebi.java — Şüphe Gerekçesi

```java
package model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class SupheSebebi implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String        hesapId;       // Hangi hesap şüpheli?
    private final String        musteriId;     // O hesabın sahibi kim?
    private final String        sebep;         // Neden şüpheli? ("Büyük işlem: 75,000 ₺")
    private final double        ilgiliMiktar;  // Şüpheyi tetikleyen tutar
    private final LocalDateTime zaman;         // Ne zaman şüpheli işaretlendi?

    public SupheSebebi(String hesapId, String musteriId,
                       String sebep, double ilgiliMiktar) {
        this.hesapId      = hesapId;
        this.musteriId    = musteriId;
        this.sebep        = sebep;
        this.ilgiliMiktar = ilgiliMiktar;
        this.zaman        = LocalDateTime.now();  // Şu anı kaydet
    }

    public String        getHesapId()      { return hesapId; }
    public String        getMusteriId()    { return musteriId; }
    public String        getSebep()        { return sebep; }
    public double        getIlgiliMiktar() { return ilgiliMiktar; }
    public LocalDateTime getZaman()        { return zaman; }
}
```

**Ne yapıyor:** Bir hesap şüpheli işaretlendiğinde nedenini belgeler. "Risk & Limitler" sekmesindeki şüpheli hesap tablosunda bu bilgiler gösterilir.

---

### 2.15 BankState.java — Sistem Anlık Görüntüsü

```java
package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("serial")
public class BankState implements Serializable {

    private static final long serialVersionUID = 3L;  // 3L: 3. versiyon

    // Kaydedilecek her şey bu sınıfta toplanır
    private final List<Customer>             musteriler;
    private final List<Account>              hesaplar;
    private final Map<String, Kullanici>     kullanicilar;     // kulAdi → Kullanici
    private final Set<String>                suphelihHesaplar; // Şüpheli hesap ID'leri
    private final Map<String, SupheSebebi>   supheSebebleri;   // hesapId → sebep
    private final Map<String, HesapLimiti>   hesapLimitleri;   // hesapId → limit
    private final Map<String, Integer>       riskSkorlari;     // hesapId → skor
    private final Map<String, LocalDate>     skorGuncelleme;   // hesapId → son tarih
    private final int musteriSayaci;  // Sonraki müşteri ID için
    private final int hesapSayaci;    // Sonraki hesap ID için
    private final int islemSayaci;    // Sonraki işlem ID için

    // Tüm alanları alan büyük kurucu
    public BankState(List<Customer> musteriler, List<Account> hesaplar,
                     Map<String, Kullanici> kullanicilar, ...) {
        this.musteriler  = musteriler;
        // ... diğer alanlar atanır
    }

    // Sadece okuma metodları (getter'lar)
    public List<Customer>           getMusteriler()       { return musteriler; }
    public List<Account>            getHesaplar()         { return hesaplar; }
    public Map<String, Kullanici>   getKullanicilar()     { return kullanicilar; }
    // ... diğer getter'lar
}
```

**Ne yapıyor:** `banka_durumu.dat` dosyasına yazılan ve oradan okunan tek nesnedir. Sistemi kapatıp açtığında tüm veriler bu sayede geri yüklenir. `serialVersionUID = 3L` → dosyayı eski bir sürümle kaydetmişseniz yeni sürümle okuyamazsınız.

---

### 2.16 YetersizBakiyeException.java ve RiskLimitiAsildiException.java — Özel Hatalar

```java
// YetersizBakiyeException.java
package model;

public class YetersizBakiyeException extends RuntimeException {
    // RuntimeException'dan kalıtım → try/catch zorunlu değil ama işlenebilir

    private final double mevcutBakiye;   // Hesapta ne kadar var?
    private final double istenenMiktar;  // Ne kadar çekilmek isteniyordu?

    public YetersizBakiyeException(double mevcutBakiye, double istenenMiktar) {
        super(String.format("Yetersiz bakiye: mevcut=%.2f ₺, istenen=%.2f ₺",
                mevcutBakiye, istenenMiktar));
        // super(...) → üst sınıfa (RuntimeException) hata mesajını ilet
        this.mevcutBakiye  = mevcutBakiye;
        this.istenenMiktar = istenenMiktar;
    }

    public double getMevcutBakiye()  { return mevcutBakiye; }
    public double getIstenenMiktar() { return istenenMiktar; }
}

// RiskLimitiAsildiException.java — benzer yapı
public class RiskLimitiAsildiException extends RuntimeException {
    private final double limit;
    private final double istenenMiktar;

    public RiskLimitiAsildiException(String sebep, double limit, double istenenMiktar) {
        super(String.format("%s (limit: %.2f ₺, istenen: %.2f ₺)",
                sebep, limit, istenenMiktar));
        this.limit         = limit;
        this.istenenMiktar = istenenMiktar;
    }

    public double getLimit()         { return limit; }
    public double getIstenenMiktar() { return istenenMiktar; }
}
```

**Ne yapıyor:** Normal `return false` yerine fırlatılan özel hatalar. UI katmanı `catch` bloğuyla yakalar ve kullanıcıya anlamlı mesaj gösterir ("Yetersiz bakiye! Mevcut: 500 ₺"). Hata içinde tutar bilgisi de olduğundan "Mevcut: X ₺" gibi detaylı mesaj verilebilir.

---

## BÖLÜM 3: service/ Paketi — İş Mantığı

---

### 3.1 Repository.java ve Depo Sınıfları

```java
// Repository.java — Generic arayüz (şablon)
package service;

import java.util.List;

public interface Repository<T, ID> {
    // T = depolanan tür (Customer, Account vb.)
    // ID = kimlik türü (String)

    void    kaydet(T entity);       // Ekle veya güncelle
    T       idIleGetir(ID id);      // ID ile bul, yoksa null
    List<T> hepsiniGetir();         // Tüm kayıtları listele
    void    sil(ID id);             // ID ile sil
    void    temizle();              // Hepsini sil
}

// CustomerRepository.java — Müşteri deposu
public class CustomerRepository implements Repository<Customer, String> {

    // HashMap: anahtar=musteriId, değer=Customer nesnesi
    // HashMap ile erişim çok hızlı: O(1) — ne kadar büyük olursa olsun
    private final Map<String, Customer> depo = new HashMap<>();

    @Override
    public void kaydet(Customer musteri) {
        depo.put(musteri.getMusteriId(), musteri); // ID → Customer ekle/güncelle
    }

    @Override
    public Customer idIleGetir(String id) {
        return depo.get(id);  // ID ile ara, yoksa null
    }

    @Override
    public List<Customer> hepsiniGetir() {
        return new ArrayList<>(depo.values()); // Tüm Customer'ları liste yap
    }

    @Override
    public void sil(String id) { depo.remove(id); }

    @Override
    public void temizle()      { depo.clear(); }
}
```

`AccountRepository` ve `TransactionRepository` de tamamen aynı yapıda, sadece tür farklı.

**Ne yapıyor:** Bellek içi "veritabanı". `HashMap` hızlı arama için kullanılır. Gerçek bir SQL veritabanı kullanmak isteseydiniz, sadece bu sınıfları değiştirirdiniz — geri kalan kod hiç değişmezdi. Bu, Repository deseninin güzelliği.

---

### 3.2 IBankService.java — Servis Sözleşmesi

```java
package service;

import model.*;
import java.util.List;

public interface IBankService {
    // Müşteri ve hesap işlemleri
    Customer musteriOlustur(String ad, String eposta);
    Account  hesapOlustur(String musteriId, String tur, double baslangicBakiye);
    boolean  paraYatir(String hesapId, double miktar);
    boolean  paraCek(String hesapId, double miktar);
    boolean  transferYap(String kaynakId, String hedefId, double miktar);

    // Sorgulama
    Customer       getMusteri(String musteriId);
    Account        getHesap(String hesapId);
    List<Customer> tumMusteriler();
    List<Account>  tumHesaplar();
    boolean        hesapMusteriyeAitMi(String hesapId, String musteriId);
    List<Account>  musteriHesaplari(String musteriId);

    // Limit işlemleri
    void        limitGuncelle(String hesapId, HesapLimiti limit);
    HesapLimiti getHesapLimiti(String hesapId);
    double      getGunlukCekimLimiti(String hesapId);
    double      getGunlukTransferLimiti(String hesapId);

    // Şüpheli hesap işlemleri
    SupheSebebi       supheSebebiGetir(String hesapId);
    List<SupheSebebi> tumSupheSebebleri();
}
```

**Ne yapıyor:** `BankController`'ın hangi metodları dışa açması gerektiğini tanımlar. UI sınıfları teorik olarak bu arayüz üzerinden çalışır — `BankController`'ı başka bir implemetasyon ile değiştirmenizi kolaylaştırır.

---

### 3.3 HashUtil.java — Şifre Güvenliği

```java
package service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class HashUtil {  // final = bu sınıftan kalıtım alınamaz

    private HashUtil() {}  // private kurucu = nesne oluşturulamaz, sadece static metodlar

    // "admin123" → "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a"
    public static String sha256(String metin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // Java'nın dahili SHA-256 algoritması
            byte[] bytes = md.digest(metin.getBytes(StandardCharsets.UTF_8));
            // bytes = [0x24, 0x0b, ...] gibi ham veri
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));  // Her byte'ı 2 hex karaktere çevir
            }
            return sb.toString();  // 64 karakterli hex string
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 desteklenmiyor", e);
        }
    }

    // SHA-256 çıktısı mı? 64 karakterli küçük harf hex mi?
    public static boolean hashMi(String s) {
        return s != null && s.length() == 64 && s.matches("[0-9a-f]+");
    }
}
```

**Ne yapıyor:** Şifreleri tek yönlü dönüşümle saklar. "admin123" → hash yaptık → hash'ten "admin123"'ü geri bulamayız. Giriş yapınca girilen şifre tekrar hash'lenir ve kayıtlı hash ile karşılaştırılır.

---

### 3.4 RiskDinleyici.java, RiskOlayTuru.java, RiskOlayi.java — Observer Deseni

```java
// RiskDinleyici.java
package service;

@FunctionalInterface  // Tek metodlu arayüz, lambda ile kullanılabilir
public interface RiskDinleyici {
    void onRiskOlayi(RiskOlayi olay);  // Risk olayı gelince bu çağrılır
}

// RiskOlayTuru.java — Hangi tür risk olayı?
public enum RiskOlayTuru {
    SUPHELI_ISLEM,       // Şüpheli hesaptan işlem girişimi
    YUKSEK_TUTAR,        // 50K üzeri işlem
    GECE_MODU_ISLEM,     // Gece saatinde büyük işlem
    ANI_BAKIYE_DUSUSU,   // Bakiyenin %95'ini çekme
    COK_FAZLA_ISLEM,     // 5 dakikada 10+ işlem
    HESAP_DONDURULDU,    // Risk skoru 86'yı aştı
    SUPHELI_KALDIRILDI   // Admin şüpheyi kaldırdı
}

// RiskOlayi.java — Olayın detayları
public class RiskOlayi {
    private final String        hesapId;   // Hangi hesap?
    private final String        musteriId; // Hangi müşteri?
    private final RiskOlayTuru  tur;       // Ne tür olay?
    private final double        miktar;    // Kaç ₺?
    private final String        mesaj;     // Açıklama
    private final LocalDateTime zaman;     // Ne zaman?

    public RiskOlayi(String hesapId, String musteriId,
                     RiskOlayTuru tur, double miktar, String mesaj) {
        this.hesapId   = hesapId;
        this.musteriId = musteriId;
        this.tur       = tur;
        this.miktar    = miktar;
        this.mesaj     = mesaj;
        this.zaman     = LocalDateTime.now();
    }
    // ... getter'lar
}
```

**Ne yapıyor (Observer deseni):** `BankController` risk tespiti yapınca `RiskOlayi` oluşturur ve kayıtlı tüm `RiskDinleyici`'lere gönderir. `YoneticiPaneli` ve `MusteriPaneli` birer dinleyici kaydeder — risk olayı gelince ekranda toast bildirimi görünür. Böylece `BankController` UI hakkında hiçbir şey bilmek zorunda kalmaz.

---

### 3.5 KimlikDogrulama.java — Giriş Yönetimi

```java
package service;

import model.Kullanici;
import persistence.FileLogger;
import java.util.HashMap;
import java.util.Map;

public class KimlikDogrulama {

    private static final int MAKS_BASARISIZ_GIRIS = 3;  // 3 yanlışta kilitler

    private final Map<String, Kullanici> kullanicilar;  // kulAdi → Kullanici
    private final FileLogger kaydedici;

    public KimlikDogrulama(FileLogger kaydedici) {
        this.kullanicilar = new HashMap<>();
        this.kaydedici    = kaydedici;
        varsayilanKullanicilariYukle();  // Admin hesabını ekle
    }

    // Her başlatmada admin'i ekle
    private void varsayilanKullanicilariYukle() {
        String hashliSifre = HashUtil.sha256("admin123");
        // "admin" kullanıcısını yönetici olarak ekle, müşteri ID'si yok (null)
        kullanicilar.put("admin",
            new Kullanici("admin", hashliSifre, Kullanici.Rol.YONETICI, null));
    }

    // Giriş yap
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

        // Eski düz metin şifre mi, hash'li mi?
        String saklananSifre = kullanici.getSifre();
        boolean eslesme;
        if (HashUtil.hashMi(saklananSifre)) {
            // Hash varsa: girilen şifreyi hash'le ve karşılaştır
            eslesme = saklananSifre.equals(HashUtil.sha256(sifre));
        } else {
            // Hash yoksa (eski sistem): düz metin karşılaştır
            eslesme = saklananSifre.equals(sifre);
            if (eslesme) {
                // İlk başarılı girişte hash'e çevir (otomatik geçiş)
                kullanici.setSifre(HashUtil.sha256(sifre));
            }
        }

        if (!eslesme) {
            kullanici.basarisizGirisArtir();
            kaydedici.kaydet("GIRIS_BASARISIZ: " + kullaniciAdi
                    + " (" + kullanici.getBasarisizGirisSayisi() + ". deneme)");
            // 3. yanlışta kilitler
            if (kullanici.getBasarisizGirisSayisi() >= MAKS_BASARISIZ_GIRIS) {
                kullanici.engelleHesap();
                kaydedici.kaydet("HESAP_ENGELLENDI: " + kullaniciAdi);
            }
            return null;
        }

        kullanici.girisBasarisizSayisiniSifirla();  // Başarılı → sayacı sıfırla
        kaydedici.kaydet("GIRIS_BASARILI: " + kullaniciAdi);
        return kullanici;
    }

    // Yeni kullanıcı ekle (şifre otomatik hash'lenir)
    public void kullaniciEkle(Kullanici kullanici) {
        if (!HashUtil.hashMi(kullanici.getSifre())) {
            kullanici.setSifre(HashUtil.sha256(kullanici.getSifre())); // Hash'le
        }
        kullanicilar.put(kullanici.getKullaniciAdi(), kullanici);
        kaydedici.kaydet("KULLANICI_OLUSTURULDU: " + kullanici.getKullaniciAdi());
    }

    // Diskten yüklenen kullanıcıları içe aktar
    public void kullanicilariYukle(Map<String, Kullanici> yuklenenler) {
        kullanicilar.clear();
        varsayilanKullanicilariYukle();  // Admin her zaman var
        for (Map.Entry<String, Kullanici> giris : yuklenenler.entrySet()) {
            kullanicilar.put(giris.getKey(), giris.getValue());
        }
    }

    // Admin: eski şifre sormadan sıfırla
    public boolean sifreSifirla(String kullaniciAdi, String yeniSifre) {
        Kullanici k = kullanicilar.get(kullaniciAdi);
        if (k == null) return false;
        k.setSifre(HashUtil.sha256(yeniSifre));  // Hash'le ve kaydet
        return true;
    }

    // Müşteri: eski şifre doğrulandıktan sonra değiştir
    public boolean sifreDegistir(String kullaniciAdi, String eskiSifre, String yeniSifre) {
        Kullanici k = kullanicilar.get(kullaniciAdi);
        if (k == null) return false;
        String sakli = k.getSifre();
        // Eski şifre doğru mu?
        boolean dogru = HashUtil.hashMi(sakli)
                ? sakli.equals(HashUtil.sha256(eskiSifre))
                : sakli.equals(eskiSifre);
        if (!dogru) return false;
        k.setSifre(HashUtil.sha256(yeniSifre));
        return true;
    }

    public boolean engelliMi(String kulAdi) {
        Kullanici k = kullanicilar.get(kulAdi);
        return k != null && k.isEngelliMi();
    }

    public boolean pasifMi(String kulAdi) {
        Kullanici k = kullanicilar.get(kulAdi);
        return k != null && k.isPasifMi();
    }

    public Map<String, Kullanici> getKullanicilar() { return kullanicilar; }
}
```

---

### 3.6 RiskEngine.java — Risk Motoru

```java
package service;

import model.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class RiskEngine implements IRiskCalculatable {

    // ── Sabit limitler ──────────────────────────────────────────────────────
    static final double VARSAYILAN_MAKS_YATIRMA    = 100_000.0; // Tek yatırma max
    static final double VARSAYILAN_MAKS_CEKIM      =  50_000.0; // Tek çekim max
    static final double VARSAYILAN_MAKS_TRANSFER   =  50_000.0; // Tek transfer max
    static final double YUKSEK_RISK_ESIGI          =  50_000.0; // Bu kadar = yüksek risk
    static final double VARSAYILAN_GUNLUK_CEKIM    =  20_000.0; // Günlük çekim max
    static final double VARSAYILAN_GUNLUK_TRANSFER =  30_000.0; // Günlük transfer max

    private static final double MIN_MIKTAR         =      0.01; // Minimum işlem
    private static final int    GUNLUK_ISLEM_ESIGI =        30; // Günde 30+ işlem = şüpheli
    private static final int    KISA_SURE_ISLEM_ESIGI =     10; // 5 dk'da 10+ = velocity
    private static final long   KISA_SURE_DAKIKA   =         5L;// 5 dakika

    // Gece modu: 01:00–06:00 arası büyük işlemler şüpheli
    private static final LocalTime GECE_BASLANGIC  = LocalTime.of(1, 0);
    private static final LocalTime GECE_BITIS      = LocalTime.of(6, 0);
    private static final double    GECE_MODU_ESIGI = 10_000.0;

    // Ani düşüş: bakiyenin %95'ini tek çekimde çekmek şüpheli
    private static final double ANI_DUSUS_ORANI    =      0.95;

    // ── Risk Skoru eşikleri ─────────────────────────────────────────────────
    public static final int SKOR_IZLEME = 31;  // 🟡 31+ = izleniyor
    public static final int SKOR_RISKLI = 61;  // 🟠 61+ = riskli
    public static final int SKOR_DONDUR = 86;  // 🔴 86+ = otomatik dondur

    // Simülasyon için gece modunu simüle etme
    private boolean simuleGeceModuAktif = false;
    public void setSimuleGeceModu(boolean aktif) { simuleGeceModuAktif = aktif; }

    // ── Risk verileri (bellekte tutulan) ────────────────────────────────────
    private final Map<String, Integer>   riskSkorlari   = new HashMap<>(); // hesapId → skor
    private final Map<String, LocalDate> skorGuncelleme = new HashMap<>(); // hesapId → tarih

    // Günlük çekim takibi
    private final Map<String, Double>    gunlukCekimler          = new HashMap<>();
    private final Map<String, LocalDate> gunlukCekimTarihleri    = new HashMap<>();
    // Günlük transfer takibi
    private final Map<String, Double>    gunlukTransferler       = new HashMap<>();
    private final Map<String, LocalDate> gunlukTransferTarihleri = new HashMap<>();
    // Günlük işlem sayısı
    private final Map<String, Integer>   gunlukIslemSayisi       = new HashMap<>();
    private final Map<String, LocalDate> gunlukIslemTarihleri    = new HashMap<>();
    // Özel limitler
    private final Map<String, HesapLimiti> ozelLimitler          = new HashMap<>();
    // Velocity (5 dakikada kaç işlem)
    private final Map<String, List<LocalDateTime>> kisaVadeliIslemler = new HashMap<>();

    // ── Geçerlilik kontrolleri ───────────────────────────────────────────────

    // Para yatırma geçerli mi?
    public boolean paraYatirmaGecerliMi(double miktar) {
        return miktar >= MIN_MIKTAR && miktar <= VARSAYILAN_MAKS_YATIRMA;
    }

    // Para çekme geçerli mi?
    public boolean paraCekmeGecerliMi(Account hesap, double miktar) {
        if (miktar < MIN_MIKTAR) return false;
        // Kredi hesabı için kalan kredi limiti kontrolü
        if (hesap instanceof KrediHesabi) {
            if (((KrediHesabi) hesap).kalanKredi() < miktar) return false;
        } else {
            if (hesap.getBakiye() < miktar) return false;  // Bakiye yeterli mi?
        }
        // Özel limit var mı? yoksa varsayılan kullan
        HesapLimiti limit = ozelLimitler.get(hesap.getHesapId());
        double tekMax    = limit != null ? limit.getTekIslemCekimLimiti()  : VARSAYILAN_MAKS_CEKIM;
        double gunlukMax = limit != null ? limit.getGunlukCekimLimiti()    : VARSAYILAN_GUNLUK_CEKIM;
        if (miktar > tekMax) return false;  // Tek işlem limiti aşıldı mı?
        return bugunCekilen(hesap.getHesapId()) + miktar <= gunlukMax; // Günlük limit?
    }

    // ── Gece Modu ────────────────────────────────────────────────────────────

    public boolean geceModuMu() {
        if (simuleGeceModuAktif) return true;  // Simülasyon için her zaman gece
        LocalTime simdi = LocalTime.now();
        return !simdi.isBefore(GECE_BASLANGIC) && simdi.isBefore(GECE_BITIS);
        // 01:00 ≤ şimdi < 06:00 ise gece modu
    }

    public boolean geceModuRisklimi(double miktar) {
        return geceModuMu() && miktar >= GECE_MODU_ESIGI; // Gece + 10K₺ = riskli
    }

    // ── Ani Bakiye Düşüşü ────────────────────────────────────────────────────

    public boolean aniDususVarMi(double bakiye, double miktar) {
        if (bakiye <= 0) return false;
        return miktar / bakiye >= ANI_DUSUS_ORANI; // %95 veya fazlası mı?
    }

    // ── Velocity Check (kısa sürede çok işlem) ───────────────────────────────

    public boolean kisaVadeliCokIslemMi(String hesapId) {
        List<LocalDateTime> zamanlar = kisaVadeliIslemler.get(hesapId);
        if (zamanlar == null) return false;
        LocalDateTime esik = LocalDateTime.now().minusMinutes(KISA_SURE_DAKIKA); // 5 dk önce
        long sonIslemler = zamanlar.stream()
                .filter(z -> z.isAfter(esik))  // Son 5 dakikadakiler
                .count();
        return sonIslemler >= KISA_SURE_ISLEM_ESIGI; // 10 veya fazla mı?
    }

    // ── Kayıt metodları ───────────────────────────────────────────────────────

    public void cekimKaydet(String hesapId, double miktar) {
        guncelle(hesapId, miktar, gunlukCekimler, gunlukCekimTarihleri);
        islemKaydet(hesapId);
    }

    public void transferKaydet(String hesapId, double miktar) {
        guncelle(hesapId, miktar, gunlukTransferler, gunlukTransferTarihleri);
        islemKaydet(hesapId);
    }

    private void islemKaydet(String hesapId) {
        LocalDate bugun = LocalDate.now();
        // Gün değiştiyse sayacı sıfırla
        if (!bugun.equals(gunlukIslemTarihleri.get(hesapId))) {
            gunlukIslemSayisi.put(hesapId, 0);
            gunlukIslemTarihleri.put(hesapId, bugun);
        }
        gunlukIslemSayisi.merge(hesapId, 1, Integer::sum); // +1 ekle

        // Sliding window: eski kayıtları temizle, yeni zamanı ekle
        LocalDateTime simdi = LocalDateTime.now();
        LocalDateTime esik  = simdi.minusMinutes(KISA_SURE_DAKIKA * 2);
        kisaVadeliIslemler
            .computeIfAbsent(hesapId, k -> new ArrayList<>())
            .removeIf(z -> z.isBefore(esik));  // 10 dk'dan eski kayıtları sil
        kisaVadeliIslemler.get(hesapId).add(simdi);
    }

    // ── Risk Skoru ────────────────────────────────────────────────────────────

    public int getRiskSkoru(String hesapId) {
        LocalDate bugun  = LocalDate.now();
        LocalDate sonGun = skorGuncelleme.getOrDefault(hesapId, bugun);
        // Gün farkı kadar -5 puan (her geçen temiz gün skoru düşürür)
        if (sonGun.isBefore(bugun)) {
            long gunFarki = java.time.temporal.ChronoUnit.DAYS.between(sonGun, bugun);
            int mevcut = riskSkorlari.getOrDefault(hesapId, 0);
            riskSkorlari.put(hesapId, Math.max(0, mevcut - (int)(gunFarki * 5)));
            skorGuncelleme.put(hesapId, bugun);
        }
        return riskSkorlari.getOrDefault(hesapId, 0);
    }

    public void skorEkle(String hesapId, int puan) {
        int mevcut = getRiskSkoru(hesapId);
        riskSkorlari.put(hesapId, Math.min(100, mevcut + puan)); // Max 100
        skorGuncelleme.put(hesapId, LocalDate.now());
    }
    // ... diğer metodlar
}
```

---

### 3.7 BankController.java — Her Şeyin Merkezi ⭐

Bu dosya çok uzun (~550 satır). Temel bölümleri açıklıyoruz:

```java
package service;
// ... import'lar

public class BankController implements IBankService {

    private static final String KAYIT_DOSYASI = "banka_durumu.dat";

    // ── Geri alınabilir işlem ────────────────────────────────────────────────
    public static class BekleyenIslem {
        public final String islemId;
        public final String kaynakId;
        public final String hedefId;    // Transfer için hedef hesap
        public final double miktar;
        public final String tip;        // "CEKIM" veya "TRANSFER"
        private final LocalDateTime zaman;

        public boolean geriAlinabilirMi() {
            // Oluşturulduğundan 3 dakika geçmemiş mi?
            return LocalDateTime.now().isBefore(zaman.plusMinutes(3));
        }

        public long kalanSaniye() {
            long gecen = ChronoUnit.SECONDS.between(zaman, LocalDateTime.now());
            return Math.max(0, 180 - gecen); // 180 saniye - geçen süre
        }
    }

    // ── Tüm alanlar ──────────────────────────────────────────────────────────
    private final CustomerRepository    musteriDeposu;
    private final AccountRepository     hesapDeposu;
    private final TransactionRepository islemDeposu;
    private final Set<String>           suphelihHesaplar;   // Dondurulmuş hesap ID'leri
    private final Map<String, SupheSebebi> supheSebebleri;  // Şüphe gerekçeleri
    private final RiskEngine            riskMotoru;
    private final FileLogger            kaydedici;          // banka_kayit.txt
    private final KimlikDogrulama       kimlikDogrulama;
    private final List<RiskDinleyici>   dinleyiciler;       // Observer listesi
    private final Map<String, BekleyenIslem> bekleyenIslemler; // Geri alma için
    private final Set<String>           demoMusteri;        // Demo müşteri ID'leri
    private int musteriSayaci, hesapSayaci, islemSayaci;    // ID üretim sayaçları

    // ── Kurucu ───────────────────────────────────────────────────────────────
    public BankController(KimlikDogrulama kimlikDogrulama) {
        this.musteriDeposu    = new CustomerRepository();
        this.hesapDeposu      = new AccountRepository();
        this.islemDeposu      = new TransactionRepository();
        this.suphelihHesaplar = new HashSet<>();
        this.supheSebebleri   = new HashMap<>();
        this.riskMotoru       = new RiskEngine();
        this.kaydedici        = new FileLogger();
        this.kimlikDogrulama  = kimlikDogrulama;
        this.dinleyiciler     = new CopyOnWriteArrayList<>(); // Thread-safe liste
        this.bekleyenIslemler = new LinkedHashMap<>();        // Sıralı Map
        this.demoMusteri      = new HashSet<>();
    }

    // ── Müşteri oluştur ──────────────────────────────────────────────────────
    @Override
    public Customer musteriOlustur(String ad, String eposta) {
        String id = String.format("MUS%05d", ++musteriSayaci);
        // ++musteriSayaci: önce artır, sonra kullan
        // String.format("MUS%05d", 1) → "MUS00001"
        Customer musteri = new Customer(id, ad, eposta);
        musteriDeposu.kaydet(musteri);
        kaydedici.kaydet("MUSTERI_OLUSTURULDU: " + id + " | " + ad + " | " + eposta);
        otomatikKaydet();  // No-op (kasıtlı devre dışı)
        return musteri;
    }

    // ── Hesap oluştur (Factory metodu) ───────────────────────────────────────
    @Override
    public Account hesapOlustur(String musteriId, String tur, double baslangicBakiye) {
        Customer musteri = musteriDeposu.idIleGetir(musteriId);
        if (musteri == null) return null;  // Müşteri yoksa null döner
        String hesapId = String.format("HSP%06d", ++hesapSayaci);

        Account hesap;
        switch (tur) {  // Türe göre doğru alt sınıfı oluştur
            case "VADELİ": case "VADELI":
                hesap = new SavingsAccount(hesapId, musteriId, baslangicBakiye, 0.03);
                break;
            case "DÖVİZ-USD":
                hesap = new DovizHesabi(hesapId, musteriId, baslangicBakiye,
                        DovizHesabi.ParaBirimi.USD, 32.5); // Başlangıç kur 32.5
                break;
            case "DÖVİZ-EUR":
                hesap = new DovizHesabi(hesapId, musteriId, baslangicBakiye,
                        DovizHesabi.ParaBirimi.EUR, 35.2);
                break;
            case "DÖVİZ-GBP":
                hesap = new DovizHesabi(hesapId, musteriId, baslangicBakiye,
                        DovizHesabi.ParaBirimi.GBP, 41.0);
                break;
            case "KREDİ": case "KREDI":
                hesap = new KrediHesabi(hesapId, musteriId,
                        baslangicBakiye > 0 ? baslangicBakiye : 10_000.0, 0.02);
                break;
            default:  // "VADESİZ" ve bilinmeyenler
                hesap = new CheckingAccount(hesapId, musteriId, baslangicBakiye);
        }

        hesapDeposu.kaydet(hesap);    // Depoya ekle
        musteri.hesapEkle(hesap);     // Müşterinin hesap listesine ekle
        kaydedici.kaydet("HESAP_OLUSTURULDU: " + hesapId + " | " + tur);
        return hesap;
    }

    // ── Para yatır ───────────────────────────────────────────────────────────
    @Override
    public boolean paraYatir(String hesapId, double miktar) {
        Account hesap = hesapDeposu.idIleGetir(hesapId);
        if (hesap == null || miktar <= 0) return false;

        if (!riskMotoru.paraYatirmaGecerliMi(miktar)) {
            kaydedici.kaydet("PARA_YATIRMA_REDDEDILDI: " + hesapId);
            return false;  // 100K üzeri yatırma reddi
        }

        String islemId = String.format("TRX%08d", ++islemSayaci);
        hesap.paraYatir(miktar);              // Bakiyeye ekle
        Transaction islem = new Deposit(islemId, miktar, hesapId);
        hesap.islemEkle(islem);               // Hesabın geçmişine ekle
        islemDeposu.kaydet(islem);            // Genel depoya ekle
        riskMotoru.yatirmaKaydet(hesapId);    // Velocity sayacını güncelle
        kaydedici.kaydet("PARA_YATIRILDI: " + islemId + " | " + hesapId + " | +" + miktar);
        islemSonrasiRiskKontrol(hesapId, hesap.getSahibiId(), 0.0, miktar);
        return true;
    }

    // ── Para çek ─────────────────────────────────────────────────────────────
    @Override
    public boolean paraCek(String hesapId, double miktar) {
        Account hesap = hesapDeposu.idIleGetir(hesapId);
        if (hesap == null || miktar <= 0) return false;

        // Şüpheli hesaptan çekim yapılamaz
        if (suphelihHesaplar.contains(hesapId)) {
            kaydedici.kaydet("PARA_CEKME_ENGELLENDI_SUPHELI: " + hesapId);
            return false;
        }

        // Limit kontrolü — aşılırsa exception fırlatır
        if (!riskMotoru.paraCekmeGecerliMi(hesap, miktar)) {
            kaydedici.kaydet("PARA_CEKME_REDDEDILDI: " + hesapId);
            throw new model.RiskLimitiAsildiException(
                    "Günlük çekim limiti aşıldı",
                    riskMotoru.getGunlukCekimLimit(hesapId), miktar);
        }

        String islemId = String.format("TRX%08d", ++islemSayaci);
        if (!hesap.paraCek(miktar)) {
            throw new model.YetersizBakiyeException(hesap.getBakiye(), miktar);
        }

        riskMotoru.cekimKaydet(hesapId, miktar);     // Günlük toplamı güncelle
        Transaction islem = new Withdraw(islemId, miktar, hesapId);
        hesap.islemEkle(islem);
        islemDeposu.kaydet(islem);
        kaydedici.kaydet("PARA_CEKILDI: " + islemId + " | " + hesapId + " | -" + miktar);

        // 3 dakika geri alma penceresi için kaydet
        sonBekleyenIslem = new BekleyenIslem(islemId, hesapId, null, miktar, "CEKIM");
        bekleyenIslemler.put(islemId, sonBekleyenIslem);

        islemSonrasiRiskKontrol(hesapId, hesap.getSahibiId(),
                hesap.getBakiye() + miktar, miktar); // bakiyeOncesi = sonraki + miktar
        return true;
    }

    // ── Risk kontrolü (her işlem sonrası) ────────────────────────────────────
    private void islemSonrasiRiskKontrol(String hesapId, String musteriId,
                                          double bakiyeOncesi, double miktar) {
        // Her kural tetiklenirse skor ekle
        if (riskMotoru.yuksekRiskMi(miktar)) {
            riskMotoru.skorEkle(hesapId, 20);  // Büyük işlem +20 puan
            riskYayinla(hesapId, musteriId, RiskOlayTuru.YUKSEK_TUTAR, miktar,
                    "Büyük tutarlı işlem");
        }
        if (riskMotoru.aniDususVarMi(bakiyeOncesi, miktar)) {
            riskMotoru.skorEkle(hesapId, 15);  // Ani düşüş +15 puan
            riskYayinla(...);
        }
        if (riskMotoru.geceModuRisklimi(miktar)) {
            riskMotoru.skorEkle(hesapId, 15);  // Gece modu +15 puan
            riskYayinla(...);
        }
        if (riskMotoru.kisaVadeliCokIslemMi(hesapId)) {
            riskMotoru.skorEkle(hesapId, 30);  // Velocity +30 puan
            riskYayinla(...);
        }

        // Eşik aşıldıysa otomatik dondur
        if (!suphelihHesaplar.contains(hesapId)) {
            int skor = riskMotoru.getRiskSkoru(hesapId);
            if (skor >= RiskEngine.SKOR_DONDUR) {  // 86 ve üzeri
                isaretleSebeple(hesapId, musteriId,
                        "Risk skoru " + skor + " eşiği aştı — otomatik donduruldu", miktar);
            }
        }
    }

    // ── Kaydetme/Yükleme ─────────────────────────────────────────────────────
    public void durumKaydet(String dosyaYolu) {
        BankState durum = new BankState(
                musteriDeposu.hepsiniGetir(),
                hesapDeposu.hepsiniGetir(),
                kimlikDogrulama.getKullanicilar(),
                new HashSet<>(suphelihHesaplar),
                new HashMap<>(supheSebebleri),
                new HashMap<>(riskMotoru.getOzelLimitler()),
                new HashMap<>(riskMotoru.getRiskSkorlari()),
                new HashMap<>(riskMotoru.getSkorGuncelleme()),
                musteriSayaci, hesapSayaci, islemSayaci);
        Serializer.serialize(durum, dosyaYolu);  // Diske yaz
        kaydedici.kaydet("DURUM_KAYDEDILDI: " + dosyaYolu);
    }

    public void durumYukle(String dosyaYolu) {
        Object obj = Serializer.deserialize(dosyaYolu);  // Diskten oku
        if (!(obj instanceof BankState)) return;  // Geçersiz dosya
        BankState durum = (BankState) obj;

        // Önce temizle
        musteriDeposu.temizle();
        hesapDeposu.temizle();
        suphelihHesaplar.clear();

        // Sonra yükle
        for (Customer m : durum.getMusteriler()) musteriDeposu.kaydet(m);
        for (Account h : durum.getHesaplar()) {
            hesapDeposu.kaydet(h);
            for (Transaction t : h.getIslemler()) islemDeposu.kaydet(t);
        }
        if (durum.getKullanicilar() != null)
            kimlikDogrulama.kullanicilariYukle(durum.getKullanicilar());
        // ... diğer alanlar

        // Sayaçları geri yükle (ID çakışması olmaması için kritik)
        musteriSayaci = durum.getMusteriSayaci();
        hesapSayaci   = durum.getHesapSayaci();
        islemSayaci   = durum.getIslemSayaci();
    }

    private void otomatikKaydet() { /* Kasıtlı boş — kayıt manuel */ }
}
```

---

## BÖLÜM 4: persistence/ Paketi — Diske Kayıt

---

### 4.1 FileLogger.java — Log Dosyası

```java
package persistence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.time.LocalDateTime;

public class FileLogger {

    private static final String LOG_DOSYASI = "banka_kayit.txt"; // Sabit dosya adı
    private boolean sessiz = false; // true = yazmayı sustur

    public void setSessiz(boolean s) { this.sessiz = s; }

    // Dosyaya tek satır ekle
    public void kaydet(String mesaj) {
        if (sessiz) return;  // Susturulmuşsa hiçbir şey yapma
        try (java.io.FileWriter fw = new java.io.FileWriter(LOG_DOSYASI, true);
             // true = mevcut dosyaya ekle (üzerine yazma)
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write("[" + LocalDateTime.now() + "] " + mesaj);
            // Örn: [2025-01-15T14:32:05.123] PARA_YATIRILDI: TRX00000001 | HSP000001
            bw.newLine();  // Satır sonu
        } catch (IOException e) {
            System.err.println("Kayıt hatası: " + e.getMessage());
        }
    }

    // Tüm log dosyasını string olarak oku
    public String kayitlariOku() {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new java.io.FileReader(LOG_DOSYASI))) {
            String satir;
            while ((satir = br.readLine()) != null) {
                sb.append(satir).append("\n"); // Her satırı ekle
            }
        } catch (IOException e) {
            return "";  // Dosya yoksa boş döner
        }
        return sb.toString();
    }
}
```

**Ne yapıyor:** Her `kaydet()` çağrısında dosyayı açar, satır ekler, kapatır. `sessiz` modu bot simülasyonu sırasında gerçek log kirlenmesini önler.

---

### 4.2 Serializer.java — Nesneyi Diske Yaz/Oku

```java
package persistence;

import java.io.*;

public class Serializer {

    // Herhangi bir nesneyi .dat dosyasına yaz
    public static void serialize(Object nesne, String dosyaYolu) {
        try (ObjectOutputStream oos =
                new ObjectOutputStream(new FileOutputStream(dosyaYolu))) {
            // ObjectOutputStream: Java nesnelerini binary formata çevirir
            oos.writeObject(nesne);
            // BankState nesnesi binary olarak dosyaya yazılır
        } catch (IOException e) {
            System.err.println("Serileştirme hatası: " + e.getMessage());
        }
    }

    // .dat dosyasından nesneyi geri oku
    public static Object deserialize(String dosyaYolu) {
        try (ObjectInputStream ois =
                new ObjectInputStream(new FileInputStream(dosyaYolu))) {
            // ObjectInputStream: binary formatı Java nesnesine çevirir
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Seri çözme hatası: " + e.getMessage());
            return null;  // Hata varsa null döner
        }
    }
}
```

**Ne yapıyor:** Java'nın yerleşik serileştirme mekanizmasını kullanır. `Serializable` olan her nesne binary olarak dosyaya yazılabilir ve geri okunabilir. Bu sayede tüm sistem durumu tek bir `.dat` dosyasında saklanır.

---

## BÖLÜM 5: ui/ Paketi — Görsel Arayüz

---

### 5.1 UITema.java — Tüm Buton ve Bileşen Fabrikası

```java
package ui;
// ... JavaFX import'ları

class UITema {

    // CSS dosyasının konumunu bul (bin/ klasörüne kopyalandıktan sonra)
    static final String CSS_YOLU;
    static {
        java.net.URL url = UITema.class.getResource("banka.css");
        CSS_YOLU = url != null ? url.toExternalForm() : "";
    }

    // Renk sabitleri (hex kodları)
    static final String HEX_BIRINCIL  = "#163264"; // Lacivert
    static final String HEX_BASARILI  = "#146418"; // Yeşil
    static final String HEX_HATA      = "#af1414"; // Kırmızı
    static final String HEX_UYARI     = "#af4b00"; // Turuncu

    // ── Buton fabrikası ─────────────────────────────────────────────────────
    // Mavi ana buton (oluştur, kaydet vb.)
    static Button anaButon(String metin) {
        Button b = new Button(metin);
        b.getStyleClass().add("buton-ana");  // CSS'deki .buton-ana stili uygulanır
        return b;
    }

    // Gri normal buton (yenile, listele vb.)
    static Button normalButon(String metin) {
        Button b = new Button(metin);
        b.getStyleClass().add("buton-normal");
        return b;
    }

    // Kırmızı tehlike butonu (sil, engelle vb.)
    static Button tehlikeButon(String metin) {
        Button b = new Button(metin);
        b.getStyleClass().add("buton-tehlike");
        return b;
    }

    // ── Form elemanları ──────────────────────────────────────────────────────
    static TextField alan() {
        TextField tf = new TextField();
        tf.getStyleClass().add("alan");
        tf.setMaxWidth(Double.MAX_VALUE);  // Tam genişlik
        return tf;
    }

    static Label etiket(String metin) {
        Label l = new Label(metin);
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: #37415a;");
        return l;
    }

    // ── Kart paneli ──────────────────────────────────────────────────────────
    // Başlıklı beyaz kutu
    static VBox kart(String baslik) {
        VBox kart = new VBox(10);  // İçindeki elemanlar arası 10 px boşluk
        kart.getStyleClass().add("kart");
        kart.setPadding(new Insets(14, 16, 14, 16)); // Üst/sağ/alt/sol içi boşluk
        if (baslik != null && !baslik.isEmpty()) {
            Label bl = new Label(baslik);
            bl.setFont(Font.font("System", FontWeight.BOLD, 12));
            bl.setStyle("-fx-text-fill: #163264;");
            kart.getChildren().addAll(bl, new Separator()); // Başlık + çizgi
        }
        return kart;
    }

    // ── Tablo fabrikası ──────────────────────────────────────────────────────
    // Dinamik sütunlu tablo oluşturur
    static TableView<ObservableList<String>> tablo(String... sutunlar) {
        TableView<ObservableList<String>> tv = new TableView<>();
        for (int i = 0; i < sutunlar.length; i++) {
            final int idx = i;
            TableColumn<ObservableList<String>, String> col =
                    new TableColumn<>(sutunlar[i]);
            // i. sütun i. indeksteki string'i gösterir
            col.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                    idx < data.getValue().size() ? data.getValue().get(idx) : ""));
            tv.getColumns().add(col);
        }
        tv.setColumnResizePolicy(
            TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        // Son sütun kalan alanı doldurur
        tv.setPlaceholder(new Label("Veri bulunamadı"));
        return tv;
    }

    // Tabloya satır ekle
    static void satirEkle(TableView<ObservableList<String>> tv, String... degerler) {
        tv.getItems().add(FXCollections.observableArrayList(degerler));
        // String... = istediğin kadar String parametresi
    }

    // ── Dialog/Bildirim metodları ────────────────────────────────────────────
    static void bilgi(String baslik, String mesaj) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); // Mavi "i" ikonlu dialog
        a.setTitle(baslik);
        a.setContentText(mesaj);
        a.showAndWait();  // Dialog kapanana kadar bekle
    }

    static void hata(String baslik, String mesaj) {
        Alert a = new Alert(Alert.AlertType.ERROR); // Kırmızı "x" ikonlu dialog
        // ...
    }

    static boolean onay(String baslik, String mesaj) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION); // Soru işaretli dialog
        a.setTitle(baslik);
        a.setContentText(mesaj);
        return a.showAndWait().filter(r -> r == ButtonType.OK).isPresent();
        // OK'a bastıysa true, iptal/kapat ise false
    }

    // Yeşil veya kırmızı durum mesajı göster
    static void durumGoster(Label label, String mesaj, boolean basarili) {
        label.setText(mesaj);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: " +
                (basarili ? HEX_BASARILI : HEX_HATA) + ";");
    }

    // ── Toast Bildirimi (sağ altta geçici mesaj) ─────────────────────────────
    static void toast(Scene sahne, String mesaj, Boolean basarili) {
        if (sahne == null) return;
        Platform.runLater(() -> {  // JavaFX UI thread'inde çalıştır
            // Renge göre yeşil/kırmızı/turuncu
            String renk = basarili == null ? HEX_UYARI
                         : basarili        ? HEX_BASARILI
                                          : HEX_HATA;

            Label label = new Label(mesaj);
            StackPane popup = new StackPane(label);
            popup.setOpacity(0);  // Başlangıçta görünmez

            // Sahnenin sağ altına yerleştir
            StackPane katman = (StackPane) sahne.lookup("#toastKatmani");
            // ... katman yönetimi

            // Animasyon: 250ms belir → 2500ms bekle → 450ms kaybol
            Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,        // Başlangıç: opacity=0
                    new KeyValue(popup.opacityProperty(), 0.0)),
                new KeyFrame(Duration.millis(250), // 250ms sonra: opacity=1
                    new KeyValue(popup.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(2750),// 2750ms'ye kadar görün
                    new KeyValue(popup.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(3200),// 3200ms sonra: opacity=0
                    new KeyValue(popup.opacityProperty(), 0.0))
            );
            tl.setOnFinished(e -> toastKatmani.getChildren().remove(popup)); // Kaldır
            tl.play();
        });
    }
}
```

---

### 5.2 GirisEkrani.java — Login Ekranı

```java
package ui;
// ... import'lar

public class GirisEkrani {

    private static final String DURUM_DOSYASI = "banka_durumu.dat";

    private final Stage           stage;            // Ana JavaFX penceresi
    private final BankController  kontrolcu;
    private final KimlikDogrulama kimlikDogrulama;

    private TextField     kullaniciAdiField;
    private PasswordField sifreField;
    private Label         mesajLabel;   // Hata mesajı için
    private VBox          girisKart;    // Giriş formu kartı

    public GirisEkrani(Stage stage) {
        this.stage = stage;
        FileLogger kaydedici = new FileLogger();
        this.kimlikDogrulama  = new KimlikDogrulama(kaydedici);
        this.kontrolcu        = new BankController(kimlikDogrulama);

        // Kayıtlı durum varsa yükle
        if (new File(DURUM_DOSYASI).exists()) {
            kontrolcu.durumYukle(DURUM_DOSYASI);
        }
        goster();  // Ekranı göster
    }

    private void goster() {
        HBox kok = new HBox();           // Sol-sağ iki panelli ana kutu
        kok.setPrefSize(1000, 640);

        Pane sol = solPanel();           // Marka görseli (lacivert panel)
        StackPane sag = sagPanel();      // Giriş formu (beyaz panel)

        HBox.setHgrow(sol, Priority.ALWAYS);  // Sol panel esner
        HBox.setHgrow(sag, Priority.ALWAYS);  // Sağ panel esner

        kok.getChildren().addAll(sol, sag);

        Scene scene = new Scene(kok, 1000, 640);
        scene.getStylesheets().add(UITema.CSS_YOLU);  // CSS uygula

        stage.setTitle("Türk Bankası – Giriş");
        stage.setScene(scene);
        stage.show();

        // İlk yükleme bittikten sonra kart yüksekliğini sabitle
        Platform.runLater(() -> {
            double h = girisKart.getHeight();
            if (h > 0) {
                girisKart.setMinHeight(h);
                girisKart.setMaxHeight(h);
                // Böylece hata mesajı çıkınca kart büyümez, kaymaz
            }
        });
    }

    // Giriş işlemi
    private void girisYap() {
        String ad    = kullaniciAdiField.getText().trim();
        String sifre = sifreField.getText().trim();

        if (ad.isEmpty() || sifre.isEmpty()) {
            hataGoster("⚠  Kullanıcı adı ve şifre boş bırakılamaz.");
            return;
        }

        Kullanici kullanici = kimlikDogrulama.girisYap(ad, sifre);

        if (kullanici == null) {
            // Neden başarısız? Detaylı mesaj göster
            String mesaj = kimlikDogrulama.engelliMi(ad)
                    ? "⛔  Hesap kilitlendi. Yönetici ile iletişime geçin."
                    : kimlikDogrulama.pasifMi(ad)
                    ? "⛔  Hesap pasif durumda."
                    : "✗  Hatalı kullanıcı adı veya şifre.";
            hataGoster(mesaj);
            sifreField.clear();  // Şifre alanını temizle
        } else {
            hataGizle();
            new MainFrame(stage, kullanici, kontrolcu, kimlikDogrulama);
            // Başarılı giriş: MainFrame açılır, bu ekran gizlenir
        }
    }
}
```

---

### 5.3 MainFrame.java — Ana Pencere Çerçevesi

```java
package ui;
// ... import'lar

public class MainFrame {

    public MainFrame(Stage stage, Kullanici kullanici,
                     BankController kontrolcu, KimlikDogrulama kimlikDogrulama) {

        boolean yonetici = kullanici.getRol() == Kullanici.Rol.YONETICI;

        // ── Üst başlık şeridi ─────────────────────────────────────────────
        HBox header = new HBox(0);
        header.setStyle("-fx-background-color: linear-gradient(to right, #080f2e, #163264);");
        // CSS gradient: soldan sağa koyu lacivert → mavi

        // Çıkış butonu
        Button cikisBtn = new Button("⏻  Çıkış");
        cikisBtn.setOnAction(e -> {
            stage.close();          // Mevcut pencereyi kapat
            new GirisEkrani(stage); // Giriş ekranını yeniden aç
            // NOT: BankController yeniden oluşturulur, dosyadan yüklenir
        });

        // ── İçerik: role göre farklı panel ───────────────────────────────
        Region icerik = yonetici
                ? new YoneticiPaneli(kontrolcu, kimlikDogrulama)
                : new MusteriPaneli(kullanici, kontrolcu);
        // Yönetici → 6 sekmeli admin paneli
        // Müşteri  → 7 sekmeli müşteri paneli

        BorderPane kok = new BorderPane();
        kok.setTop(topBar);    // Üst: başlık şeridi
        kok.setCenter(icerik); // Orta: panel

        Scene scene = new Scene(kok, 1150, 780);
        scene.getStylesheets().add(UITema.CSS_YOLU);
        stage.setScene(scene);
        stage.show();
    }
}
```

---

### 5.4 YoneticiPaneli.java — Admin Paneli (Özet)

Bu dosya ~1550 satır olduğu için tüm kodu göstermek yerine yapısını açıklıyoruz:

```java
package ui;
// ... import'lar

public class YoneticiPaneli extends BorderPane {

    private final BankController  kontrolcu;
    private final KimlikDogrulama kimlikDogrulama;

    // Müşteri sekmesi için alanlar
    private TextField adField, epostaField, musteriKulAdiField, musteriSifreField;
    private TableView<ObservableList<String>> musteriTablo;

    // Hesap sekmesi için alanlar
    private ComboBox<String> hesapMusteriCombo, hesapTuruCombo;
    private TextField baslangicBakiyeField;
    private TableView<ObservableList<String>> hesapTablo;

    // ... diğer alanlar

    public YoneticiPaneli(BankController kontrolcu, KimlikDogrulama kimlikDogrulama) {
        this.kontrolcu       = kontrolcu;
        this.kimlikDogrulama = kimlikDogrulama;
        bilesimleriBaslat();  // Tüm sekmeleri oluştur
        riskDinleyiciKaydet(); // Risk bildirimlerini dinle
    }

    // Risk olaylarını dinle → ekranda toast göster
    private void riskDinleyiciKaydet() {
        RiskDinleyici dinleyici = (RiskOlayi olay) ->
            Platform.runLater(() -> {
                String mesaj = "[" + olay.getTur().name() + "] "
                             + olay.getHesapId() + " — " + olay.getMesaj();
                UITema.toast(getScene(), mesaj, false);
            });
        kontrolcu.dinleyiciEkle(dinleyici);
    }

    private void bilesimleriBaslat() {
        TabPane sekmeler = new TabPane();
        sekmeler.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        // Sekmeler kapatılamaz

        Tab t1 = new Tab("  Müşteri Yönetimi  ",  musteriSekme());
        Tab t2 = new Tab("  Hesap Yönetimi  ",    hesapSekme());
        Tab t3 = new Tab("  İşlemler  ",           islemlerSekme());
        Tab t4 = new Tab("  Kullanıcı Yönetimi  ", kullaniciSekme());
        Tab t5 = new Tab("  Raporlar  ",            raporlarSekme());
        Tab t6 = new Tab("  Risk & Limitler  ",     riskLimitlerSekme());
        sekmeler.getTabs().addAll(t1, t2, t3, t4, t5, t6);

        // Sekme değişince tabloları güncelle
        sekmeler.getSelectionModel().selectedItemProperty()
            .addListener((obs, eski, yeni) -> {
                if (yeni == t5) raporlariYenile();
                if (yeni == t6) { hesapComboGuncelle(limitHesapCombo);
                                  limitlariYenile(); riskTablosunuYenile(); }
            });
        setCenter(sekmeler);
    }

    // Müşteri oluşturma mantığı
    private void musteriOlustur() {
        String ad    = adField.getText().trim();
        String ep    = epostaField.getText().trim();
        String kulAd = musteriKulAdiField.getText().trim();
        String sifre = musteriSifreField.getText().trim();

        // Doğrulama kontrolleri
        if (ad.isEmpty() || ep.isEmpty()) {
            UITema.uyari("Uyarı", "Ad ve e-posta zorunludur."); return;
        }
        if (kimlikDogrulama.getKullanicilar().containsKey(kulAd)) {
            UITema.hata("Hata", "Bu kullanıcı adı zaten alınmış: " + kulAd); return;
        }

        // 1. Bankacılık müşterisi oluştur
        Customer m = kontrolcu.musteriOlustur(ad, ep);
        // 2. Giriş hesabı oluştur ve bağla
        kimlikDogrulama.kullaniciEkle(
            new Kullanici(kulAd, sifre, Kullanici.Rol.MUSTERI, m.getMusteriId()));
        // 3. Diske kaydet
        kontrolcu.durumKaydet("banka_durumu.dat");

        // 4. Alanları temizle ve tabloları güncelle
        adField.clear(); epostaField.clear();
        musteriKulAdiField.clear(); musteriSifreField.clear();
        musterileriYenile();
        UITema.bilgi("Başarılı", "Müşteri oluşturuldu.\nGiriş: " + kulAd);
    }

    // Bot simülasyonu — ayrı thread'de 30 işlem
    private void botSimulasyonuCalistir(Button botBtn) {
        List<Account> hesaplar = kontrolcu.tumHesaplar();
        if (hesaplar.isEmpty()) {
            UITema.uyari("Uyarı", "Hesap gerekli."); return;
        }

        botBtn.setDisable(true);  // Butonu devre dışı bırak (çift tıklama önlemi)
        botSimLoglar = null;

        kontrolcu.getKaydedici().setSessiz(true); // Log'ları sustur

        new Thread(() -> {  // Yeni thread: UI donmasın
            List<String> L = new ArrayList<>(); // Sonuç satırları
            int basarili = 0, basarisiz = 0;

            // ... 30 işlem burada gerçekleşir

            final List<String> sonLoglar = L;
            Platform.runLater(() -> {  // UI güncellemesi JavaFX thread'inde olmalı
                kontrolcu.getKaydedici().setSessiz(false); // Log'ları aç
                raporlariYenile();
                botSimLoglar = sonLoglar;
                sonuclarBtn.setDisable(false);
                botBtn.setDisable(false);
                UITema.bilgi("Simülasyon Tamamlandı", "...");
            });
        }).start(); // Thread'i başlat
    }
}
```

---

### 5.5 MusteriPaneli.java — Müşteri Paneli (Özet)

```java
public class MusteriPaneli extends BorderPane {

    private final Kullanici      kullanici;  // Giriş yapan kullanıcı
    private final BankController kontrolcu;

    public MusteriPaneli(Kullanici kullanici, BankController kontrolcu) {
        this.kullanici  = kullanici;
        this.kontrolcu  = kontrolcu;
        bilesimleriBaslat();
        riskDinleyiciKaydet();
    }

    // Sadece kendi müşteri ID'siyle ilgili risk olaylarını dinle
    private void riskDinleyiciKaydet() {
        String musteriId = kullanici.getMusteriId();
        if (musteriId == null) return;
        RiskDinleyici dinleyici = (RiskOlayi olay) -> {
            if (!musteriId.equals(olay.getMusteriId())) return; // Başkasınınsa atla
            Platform.runLater(() -> {
                UITema.toast(getScene(), "Risk Uyarısı: " + olay.getMesaj(), false);
                hesaplarimYenile();  // Hesap durumunu güncelle
            });
        };
        kontrolcu.dinleyiciEkle(dinleyici);
    }

    // Hesap tablosunu güncelle
    private void hesaplarimYenile() {
        hesapTablo.getItems().clear();
        String mId = kullanici.getMusteriId();
        if (mId == null) return;

        double sumTL = 0, sumDoviz = 0, sumKredi = 0;

        // Sadece bu müşterinin hesapları
        for (Account h : kontrolcu.musteriHesaplari(mId)) {
            int    skor  = kontrolcu.getRiskSkoru(h.getHesapId());
            String durum;
            if      (kontrolcu.suphelihMi(h.getHesapId())) durum = "ŞÜPHELİ";
            else if (skor >= 61) durum = "RİSKLİ ("    + skor + ")";
            else if (skor >= 31) durum = "İZLENİYOR (" + skor + ")";
            else                 durum = "GÜVENLİ ("   + skor + ")";

            UITema.satirEkle(hesapTablo,
                h.getHesapId(), h.getHesapTuru(), bakiyeStr(h),
                hesapEkBilgi(h), durum);

            // TL toplamı hesapla
            if (h instanceof DovizHesabi)
                sumDoviz += ((DovizHesabi) h).getBakiyeTL();
            else if (h instanceof KrediHesabi) {
                if (h.getBakiye() < 0) sumKredi += -h.getBakiye();
            } else {
                sumTL += h.getBakiye();
            }
        }
        // Özet kartlarını güncelle
        toplamTLLabel.setText(tl(sumTL));
        toplamDovizLabel.setText(tl(sumDoviz));
        toplamKrediLabel.setText(tl(sumKredi));
        netVarlikLabel.setText(tl(sumTL + sumDoviz - sumKredi));
    }

    // Para çekme
    private void paraCek() {
        String id = seciliHesapId(pcHesapCombo);
        if (id == null) { ... return; }

        // Hesap bu müşteriye ait mi? Güvenlik kontrolü
        if (!kontrolcu.hesapMusteriyeAitMi(id, kullanici.getMusteriId())) {
            UITema.hata("Güvenlik", "Bu hesap size ait değil."); return;
        }

        try {
            double m = Double.parseDouble(pcMiktarField.getText().trim());
            if (kontrolcu.paraCek(id, m)) {
                UITema.durumGoster(pcDurum, "Para çekme başarılı: " + tl(m), true);
                pcMiktarField.clear();
                combolarYenile();
                hesaplarimYenile();
                geriAlButonunuGuncelle(); // 3 dakika geri alma butonunu aktif et
            }
        } catch (YetersizBakiyeException e) {
            UITema.durumGoster(pcDurum,
                "Yetersiz bakiye! Mevcut: " + tl(e.getMevcutBakiye()), false);
        } catch (RiskLimitiAsildiException e) {
            UITema.durumGoster(pcDurum, "Risk limiti aşıldı: " + e.getMessage(), false);
        }
    }
}
```

---

## BÖLÜM 6: Tüm Dosyaların Birbirine Bağlantısı

```
Uygulama başladığında:
Main.java
  └─► GirisEkrani (stage)
        ├─ FileLogger oluşturur (banka_kayit.txt)
        ├─ KimlikDogrulama oluşturur (admin hesabı eklenir)
        ├─ BankController oluşturur
        │     ├─ CustomerRepository (müşteri deposu)
        │     ├─ AccountRepository (hesap deposu)
        │     ├─ TransactionRepository (işlem deposu)
        │     ├─ RiskEngine (risk motoru)
        │     └─ FileLogger (log)
        ├─ banka_durumu.dat varsa durumYukle() çağrılır
        └─ Login ekranı gösterilir

Giriş yapılınca:
KimlikDogrulama.girisYap()
  └─► Başarılıysa MainFrame açılır
        ├─ Yönetici → YoneticiPaneli(kontrolcu, kimlikDogrulama)
        └─ Müşteri  → MusteriPaneli(kullanici, kontrolcu)

Müşteri oluştururken:
YoneticiPaneli.musteriOlustur()
  ├─ BankController.musteriOlustur(ad, ep) → Customer nesnesi
  ├─ KimlikDogrulama.kullaniciEkle(Kullanici) → login hesabı
  └─ BankController.durumKaydet() → Serializer → banka_durumu.dat

Para çekerken:
MusteriPaneli.paraCek()
  └─► BankController.paraCek(hesapId, miktar)
        ├─ suphelihHesaplar kontrolü
        ├─ RiskEngine.paraCekmeGecerliMi() → limit kontrolü
        ├─ Account.paraCek() → bakiye düşer
        ├─ RiskEngine.cekimKaydet() → günlük toplam güncellenir
        ├─ FileLogger.kaydet() → log satırı
        ├─ BekleyenIslem kaydı (3 dk geri alma için)
        └─ islemSonrasiRiskKontrol() → risk skoru güncellenebilir, hesap donabilir

Risk olayı tetiklenince:
BankController.riskYayinla()
  └─► tüm RiskDinleyici'lere RiskOlayi gönderilir
        ├─ YoneticiPaneli: toast bildirimi
        └─ MusteriPaneli: toast bildirimi (sadece kendi hesabıysa)
```

---

## BÖLÜM 7: Tasarım Desenleri — Neden Bu Şekilde Yazıldı?

### Kalıtım (Inheritance) — "Bir kez yaz, defalarca kullan"

```
Account (abstract)
├── CheckingAccount  → "VADESİZ"
├── SavingsAccount   → "VADELİ" + faizOrani
├── DovizHesabi      → bakiye = döviz, kur tutulur
└── KrediHesabi      → bakiye negatife gider, faiz işler
```

`paraYatir()` ve `paraCek()` sadece `Account`'ta yazılmış. Dört alt sınıf da bunu kullanır, tekrar yazmak gerekmez.

---

### Observer (Gözlemci) Deseni — "Haberdar et ama bağımlı olma"

```java
// BankController habercisi: "risk olayı var"
riskYayinla(hesapId, musteriId, tur, miktar, mesaj);

// YoneticiPaneli dinleyicisi: "haber geldi, toast göster"
kontrolcu.dinleyiciEkle((olay) -> UITema.toast(...));
```

`BankController`, `YoneticiPaneli` hakkında hiçbir şey bilmez. Sadece "olay var" der, dinleyenler kendi tepkilerini verir.

---

### Repository Deseni — "Depolama detayını gizle"

```java
musteriDeposu.kaydet(musteri);   // Nasıl sakladığını bilmek zorunda değiliz
musteriDeposu.idIleGetir("MUS00001");  // HashMap mı, SQL mi, fark etmez
```

Şu an `HashMap` kullanılıyor. Yarın SQL veritabanına geçmek istesek sadece `CustomerRepository.java`'yı değiştiririz, `BankController` hiç değişmez.

---

### Factory Metodu — "Doğru nesneyi oluştur"

```java
switch (tur) {
    case "VADELİ":    hesap = new SavingsAccount(...);  break;
    case "DÖVİZ-USD": hesap = new DovizHesabi(...);    break;
    case "KREDİ":     hesap = new KrediHesabi(...);    break;
    default:           hesap = new CheckingAccount(...);
}
```

`BankController.hesapOlustur()` türe bakarak doğru alt sınıfı üretir. Çağıran kod hangi alt sınıfın döneceğini bilmek zorunda değil.

---

*OOP Bankacılık Sistemi — Java 21 + JavaFX 21*
*Bu doküman projedeki tüm Java sınıflarını, kodlarını ve açıklamalarını içerir.*
