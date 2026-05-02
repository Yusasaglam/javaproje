# Türk Bankası — Kullanım ve Kurulum Kılavuzu

---

## 1. Gereksinimler

| Gereksinim | Sürüm | Konum |
|---|---|---|
| Java (JDK) | 21 (Temurin 21.0.11) | `C:\Java\jdk-21` veya PATH'te |
| JavaFX SDK | 21 | `C:\Java\javafx-sdk-21` |
| VS Code | Herhangi | — |
| Java Extension Pack | VS Code eklentisi | Extension Marketplace |

> **Önemli:** JavaFX SDK sürümü Java sürümüyle eşleşmelidir.  
> Java 21 → JavaFX 21 SDK kullanılmalıdır.  
> `C:\Java\javafx-sdk-21\lib` klasörünün var olduğundan emin olun.

---

## 2. Proje Yapısı

```
java_proje/
├── SRC/
│   ├── Main.java                    ← Uygulama giriş noktası
│   ├── model/                       ← Veri modelleri (Customer, Account, vb.)
│   │   ├── Account.java
│   │   ├── BankState.java           ← Kayıt/yükleme için tüm sistem durumu
│   │   ├── CheckingAccount.java     ← Vadesiz hesap
│   │   ├── Customer.java
│   │   ├── Deposit.java
│   │   ├── DovizHesabi.java         ← USD/EUR/GBP döviz hesabı
│   │   ├── HesapLimiti.java         ← Günlük/tek işlem limitleri
│   │   ├── IRiskCalculatable.java   ← Risk arayüzü
│   │   ├── KrediHesabi.java         ← Kredi hesabı
│   │   ├── Kullanici.java           ← Giriş kullanıcısı (admin/müşteri)
│   │   ├── RiskLimitiAsildiException.java
│   │   ├── SavingsAccount.java      ← Vadeli hesap
│   │   ├── SupheSebebi.java
│   │   ├── Transaction.java
│   │   ├── Transfer.java
│   │   ├── Withdraw.java
│   │   └── YetersizBakiyeException.java
│   ├── service/                     ← İş mantığı
│   │   ├── AccountRepository.java
│   │   ├── BankController.java      ← Ana kontrolcü — tüm işlemler buradan geçer
│   │   ├── CustomerRepository.java
│   │   ├── HashUtil.java            ← SHA-256 şifre hash'i
│   │   ├── IBankService.java        ← Servis arayüzü
│   │   ├── KimlikDogrulama.java     ← Giriş/kimlik doğrulama
│   │   ├── Repository.java          ← Generic depo sınıfı
│   │   ├── RiskDinleyici.java       ← Observer arayüzü (risk olayları)
│   │   ├── RiskEngine.java          ← Risk motoru (limitler, skorlar)
│   │   ├── RiskOlayTuru.java
│   │   ├── RiskOlayi.java
│   │   └── TransactionRepository.java
│   ├── persistence/                 ← Kayıt/yükleme
│   │   ├── FileLogger.java          ← banka_kayit.txt'e yazar
│   │   └── Serializer.java          ← Java serialization ile .dat dosyası
│   └── ui/                          ← JavaFX arayüzleri
│       ├── GirisEkrani.java          ← Login ekranı
│       ├── MainFrame.java            ← Ana pencere çerçevesi
│       ├── MusteriPaneli.java        ← Müşteri paneli (7 sekme)
│       ├── UITema.java               ← UI yardımcı sınıfı, stil fonksiyonları
│       ├── YoneticiPaneli.java       ← Admin paneli (6 sekme)
│       └── banka.css                 ← Stil dosyası
├── bin/                             ← Derlenmiş .class dosyaları
├── .vscode/
│   └── launch.json                  ← VS Code çalıştırma konfigürasyonu
├── banka_durumu.dat                 ← (otomatik oluşur) Kayıtlı sistem durumu
└── banka_kayit.txt                  ← (otomatik oluşur) İşlem log dosyası
```

---

## 3. Derleme (Compile)

### VS Code ile (Önerilen)

VS Code'da projeyi açıkken **F5** veya sağ üstteki **▶ Run** butonuna basın.  
`.vscode/launch.json` dosyası doğru ayarlanmıştır, otomatik derleyip çalıştırır.

### Manuel Komut Satırı

**PowerShell** veya **Komut İstemi**'nde proje ana dizininden:

```powershell
# Önce bin klasörünü oluştur (ilk kez)
New-Item -ItemType Directory -Force -Path bin\ui

# Derle
javac --module-path "C:\Java\javafx-sdk-21\lib" `
      --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.swing `
      -d bin `
      SRC\Main.java SRC\model\*.java SRC\service\*.java SRC\persistence\*.java SRC\ui\*.java

# CSS dosyasını bin'e kopyala
copy SRC\ui\banka.css bin\ui\banka.css
```

### Manuel Çalıştırma

```powershell
java --module-path "C:\Java\javafx-sdk-21\lib" `
     --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.swing `
     -cp bin Main
```

---

## 4. İlk Açılış ve Giriş

Uygulama başladığında giriş ekranı açılır.

### Varsayılan Admin Hesabı

| Kullanıcı Adı | Şifre |
|---|---|
| `admin` | `admin123` |

> Admin hesabı her açılışta otomatik oluşturulur — silmek mümkün değildir.

### Kayıtlı Durum Yükleme

Proje klasöründe `banka_durumu.dat` dosyası varsa, uygulama önceki kayıtlı durumu (müşteriler, hesaplar, kullanıcılar, risk skorları) otomatik olarak yükler.

---

## 5. Yönetici (Admin) Paneli

Admin girişi yapıldıktan sonra **6 sekmeli** yönetici paneli açılır.

---

### 5.1 Müşteri Yönetimi

**Yeni Müşteri Oluştur:**
1. **Ad Soyad** alanına isim girin
2. **E-posta** alanına e-posta girin
3. **Kullanıcı Adı** → müşterinin giriş yapacağı kullanıcı adı (benzersiz olmalı)
4. **Şifre** → müşterinin giriş şifresi (en az 1 karakter)
5. **Müşteri Oluştur** butonuna basın

> Müşteri oluşturulunca sistem durumu **otomatik kaydedilir** (`banka_durumu.dat`).

**Müşteri Silme:**
- Tablodan bir müşteri seçin → **🗑 Müşteri Sil** butonuna basın
- Onay dialogu çıkar → Evet ise müşteri, tüm hesapları ve giriş hesabı birlikte silinir
- Silinen müşterinin giriş hesabı **PASİF** yapılır (giriş yapamaz)

---

### 5.2 Hesap Yönetimi

**Yeni Hesap Oluştur:**
1. **Müşteri** açılır listesinden müşteri seçin
2. **Hesap Türü** seçin:
   - `VADESİZ` → Vadesiz TL hesabı
   - `VADELİ` → Vadeli TL hesabı (%3 yıllık faiz, değiştirilebilir)
   - `DÖVİZ-USD` → Dolar hesabı (1 USD = 32.50 ₺ başlangıç kuru)
   - `DÖVİZ-EUR` → Euro hesabı (1 EUR = 35.20 ₺ başlangıç kuru)
   - `DÖVİZ-GBP` → Sterlin hesabı (1 GBP = 41.00 ₺ başlangıç kuru)
   - `KREDİ` → Kredi hesabı (girilen değer kredi limiti olur, %2 aylık faiz)
3. **Başlangıç Bakiyesi** girin (DÖVİZ için yabancı para cinsinden, KREDİ için limit)
4. **Hesap Oluştur** butonuna basın

> Hesap oluşturulunca sistem durumu **otomatik kaydedilir**.

**Diğer İşlemler:**
- **⚠ Şüpheli İşaretle** → Seçili hesabı şüpheli olarak işaretler, işlemler engellenir
- **✓ Şüpheyi Kaldır** → Şüpheli işareti kaldırır
- **✎ Faiz / Kur Güncelle** → Vadeli hesap faiz oranını veya döviz kurunu günceller
- **🗑 Hesap Sil** → Seçili hesabı siler

> Faiz/kur güncellemeleri için **"Durumu Kaydet"** butonuna basmak gerekir.

---

### 5.3 İşlemler

Admin olarak tüm hesaplara para yatırma, çekme ve transfer yapılabilir.

| İşlem | Açıklama |
|---|---|
| **Para Yatır** | Hesap seçin → miktar girin → Para Yatır |
| **Para Çek** | Hesap seçin → miktar girin → Para Çek |
| **Transfer** | Kaynak hesap seçin → hedef hesap numarası girin → miktar girin |

**Varsayılan Limitler:**

| Limit Türü | Değer |
|---|---|
| Maksimum tek para yatırma | 100,000 ₺ |
| Maksimum tek çekim | 50,000 ₺ |
| Maksimum tek transfer | 50,000 ₺ |
| Günlük çekim toplamı | 20,000 ₺ |
| Günlük transfer toplamı | 30,000 ₺ |

---

### 5.4 Kullanıcı Yönetimi

**Kullanıcı Şifresi Sıfırlama (Admin):**
1. **Kullanıcı Adı** alanına hedef kullanıcı adını girin
2. **Yeni Şifre** alanına en az 6 karakterli yeni şifre girin
3. **Şifreyi Sıfırla** butonuna basın → otomatik kaydedilir

**Müşteri Profili Düzenleme:**
1. Açılır listeden müşteri seçin (ad/eposta alanları otomatik dolar)
2. Değerleri düzenleyin
3. **Profili Güncelle** butonuna basın
4. Değişikliğin kalıcı olması için **"Durumu Kaydet"** butonuna basın

**Kullanıcı Durum Yönetimi (Tablo):**
- **🔓 Engeli Kaldır** → 3 başarısız girişten kilitlenmiş hesabı açar
- **⏸ Pasif Yap** → Hesabı geçici olarak devre dışı bırakır (giriş yapamaz)
- **▶ Aktif Yap** → Pasif veya engelli hesabı yeniden aktif eder

> Tüm durum değişiklikleri **otomatik kaydedilir**.

---

### 5.5 Raporlar

| Bölüm | Açıklama |
|---|---|
| **İstatistik Kartları** | Toplam müşteri, hesap, bakiye, şüpheli hesap sayıları |
| **Hesap Türü Dağılımı** | Pasta grafik |
| **En Yüksek Bakiyeli Hesaplar** | Animasyonlu çubuk grafik (Top 5) |
| **Vadeli Hesaplara Faiz Uygula** | Tüm vadeli hesaplara faiz ekler |
| **Kredi Hesaplarına Faiz Uygula** | Tüm kredi hesaplarına aylık faiz ekler |
| **🤖 Bot Simülasyonu (30 İşlem)** | Demo risk senaryosunu çalıştırır (bkz. Bölüm 7) |
| **📋 Deneme Sonuçları** | Simülasyon tamamlandıktan sonra aktif olur |
| **📦 Demo Veri Yükle** | 4 demo müşteri ve hesap oluşturur (yalnızca boş sistemde) |
| **Durumu Kaydet** | Tüm sistemi `banka_durumu.dat` dosyasına kaydeder |

> **Not:** Faiz güncellemeleri, kur değişiklikleri, limit ayarları — bunlar için **"Durumu Kaydet"** butonuna basmak gerekir.

---

### 5.6 Risk & Limitler

**Hesap Limiti Güncelleme:**
1. Açılır listeden hesap seçin → mevcut limitler otomatik dolar
2. Günlük çekim, günlük transfer, tek çekim, tek transfer limitlerini değiştirin
3. **Limiti Kaydet** butonuna basın
4. Kalıcı olması için **"Durumu Kaydet"** butonuna basın

**Şüpheli Hesaplar Tablosu:**
- Sistem tarafından otomatik işaretlenmiş şüpheli hesaplar ve gerekçeleri görünür
- **✓ Şüpheyi Kaldır** ile seçili hesabın şüpheli işareti kaldırılabilir

---

## 6. Müşteri Paneli

Müşteri olarak giriş yapıldığında **7 sekmeli** müşteri paneli açılır.  
Her müşteri yalnızca kendi hesaplarını görebilir.

---

### 6.1 Hesaplarım

- Tüm hesapların listesi: hesap numarası, tür, bakiye, ek bilgi, durum
- Her hesabın risk durumu renkli gösterilir:
  - 🟢 **GÜVENLİ** (skor 0–30)
  - 🟡 **İZLENİYOR** (skor 31–60)
  - 🟠 **RİSKLİ** (skor 61–85)
  - 🔴 **ŞÜPHELİ** (skor 86–100, hesap dondurulur)
- Üst kısımda: TL Varlık, Döviz (TL karşılığı), Kredi Borcu, Net Varlık özeti

---

### 6.2 Para Yatır

1. Hesap seçin
2. Miktar girin (maksimum tek seferlik: 100,000 ₺)
3. **Para Yatır** butonuna basın

---

### 6.3 Para Çek

1. Hesap seçin
2. Miktar girin
3. **Para Çek** butonuna basın

**Kısıtlamalar:**
- Şüpheli işaretli hesaptan çekim yapılamaz
- Günlük çekim limiti aşılamaz (varsayılan 20,000 ₺)
- Tek işlem limiti aşılamaz (varsayılan 50,000 ₺)
- Bakiye yetersizse işlem reddedilir

**3 Dakika İçinde Geri Alma:**
- Son çekim işlemi 3 dakika içinde iptal edilebilir
- **Geri Al** butonu çıkar ve kalan süreyi gösterir

---

### 6.4 Transfer

1. Kaynak hesabı seçin
2. **Hedef Hesap No** alanına alıcı hesap numarasını girin (örn: `HSP000001`)
3. Miktar girin
4. **Transfer Yap** butonuna basın

**Kısıtlamalar:**
- Şüpheli hesaptan transfer yapılamaz
- Şüpheli hesaba transfer yapılamaz
- Günlük transfer limiti aşılamaz (varsayılan 30,000 ₺)
- Tek işlem limiti aşılamaz (varsayılan 50,000 ₺)

**3 Dakika İçinde Geri Alma:** Son transfer 3 dakika içinde iptal edilebilir.

---

### 6.5 İşlem Geçmişi

1. Hesap seçin
2. İsteğe bağlı filtreler: işlem türü, tarih aralığı, metin arama
3. **Ara / Filtrele** butonuna basın
4. **PDF'e Aktar** ile hesap özeti PDF olarak kaydedilebilir

---

### 6.6 Kredi Yönetimi

**Kredi Kullanımı:**
1. Kredi hesabı seçin
2. Miktar girin (kalan kredi limitini aşamaz)
3. **Krediyi Kullan** butonuna basın

**Kredi Ödeme:**
1. Ödeme yapılacak kaynak hesap seçin
2. Ödeme yapılacak kredi hesabı seçin
3. Miktar girin
4. **Ödeme Yap** butonuna basın

---

### 6.7 Profilim

**Şifre Değiştirme:**
1. Mevcut şifreyi girin
2. Yeni şifre girin (en az 6 karakter)
3. Yeni şifreyi tekrar girin
4. **Şifreyi Değiştir** butonuna basın → otomatik kaydedilir

**Profil Bilgileri:** Ad, e-posta görüntülenir (düzenleme admin panelinden yapılır).

---

## 7. Bot Simülasyonu (30 İşlem)

**Raporlar** sekmesindeki **"🤖 Bot Simülasyonu"** butonu otomatik bir risk yolculuğu çalıştırır.

### Nasıl Çalışır

- Sistemdeki hesaplar üzerinde 30 önceden belirlenmiş işlem gerçekleştirir
- En düşük riskli vadesiz hesap "test hesabı" olarak seçilir
- 4 aşamada risk seviyesi yükseltilir:

| Aşama | İşlem No | Risk Seviyesi | Açıklama |
|---|---|---|---|
| Aşama 1 | 1–6 | 🟢 GÜVENLİ | Küçük normal işlemler |
| Aşama 2 | 7–12 | 🟡 İZLENİYOR | Büyük tutarlar (≥50K), gece modu |
| Aşama 3 | 13–22 | 🟠 RİSKLİ | Ani bakiye düşüşleri, velocity artışı |
| Aşama 4 | 23–30 | 🔴 ŞÜPHELİ | Risk skoru 86'yı aşınca otomatik dondurma |

### Risk Puanlama Sistemi

| Tetikleyici | Puan |
|---|---|
| Büyük işlem (≥ 50,000 ₺) | +20 |
| Gece modu işlem (01:00–06:00, ≥ 10,000 ₺) | +15 |
| Ani bakiye düşüşü (bakiyenin %95'i) | +15 |
| Velocity: 5 dakikada ≥ 10 işlem | +30 |
| Her temiz geçen gün | -5 (otomatik) |

### Risk Eşikleri

| Skor | Durum |
|---|---|
| 0–30 | 🟢 GÜVENLİ |
| 31–60 | 🟡 İZLENİYOR |
| 61–85 | 🟠 RİSKLİ |
| 86–100 | 🔴 ŞÜPHELİ — hesap otomatik dondurulur |

### Önemli Notlar

- Simülasyon çalışırken **işlem logları** (`banka_kayit.txt`) sessiz moda alınır; gerçek kayıtlar kirlenmez
- Simülasyon **kaydedilmez** — uygulama kapatılıp açılınca simülasyon verileri sıfırlanır
- Sonuçları görmek için simülasyon bittikten sonra **"📋 Deneme Sonuçları"** butonuna basın

---

## 8. Demo Veri

**Raporlar** sekmesindeki **"📦 Demo Veri Yükle"** butonu 4 hazır müşteri oluşturur.

> Yalnızca sistem **boşken** (hiç müşteri yokken) kullanılabilir.

| Kullanıcı Adı | Şifre | Risk Seviyesi | Skor |
|---|---|---|---|
| `ahmet` | `Ahmet123` | 🟢 GÜVENLİ | 0 |
| `fatma` | `Fatma123` | 🟡 İZLENİYOR | 45 |
| `mehmet` | `Mehmet123` | 🟠 RİSKLİ | 75 |
| `zeynep` | `Zeynep123` | 🔴 ŞÜPHELİ | 90+ (dondurulmuş) |

**Demo veri kaydedilmez.** Uygulama kapatılıp açılınca bu müşteriler silinir.  
Admin hesabı kalıcıdır: `admin / admin123`

---

## 9. Veri Kalıcılığı (Kayıt/Yükleme)

### Ne Zaman Otomatik Kaydedilir

| İşlem | Otomatik Kayıt |
|---|---|
| Yeni müşteri oluşturma | ✅ Evet |
| Yeni hesap oluşturma | ✅ Evet |
| Müşteri silme | ✅ Evet |
| Hesap silme | ✅ Evet |
| Kullanıcı engel/pasif/aktif ayarı | ✅ Evet |
| Şifre sıfırlama (admin) | ✅ Evet |
| Şifre değiştirme (müşteri) | ✅ Evet |
| Faiz oranı güncelleme | ❌ Manuel kayıt gerekir |
| Döviz kuru güncelleme | ❌ Manuel kayıt gerekir |
| Limit güncelleme | ❌ Manuel kayıt gerekir |
| Müşteri profili güncelleme | ❌ Manuel kayıt gerekir |
| Para yatır/çek/transfer | ❌ Manuel kayıt gerekir |
| Bot simülasyonu | ❌ Kaydedilmez (kasıtlı) |
| Demo veri | ❌ Kaydedilmez (kasıtlı) |

### Manuel Kayıt

**Raporlar** sekmesindeki **"Durumu Kaydet"** butonu her zaman manuel kayıt yapar.

### Kayıt Dosyaları

| Dosya | İçerik |
|---|---|
| `banka_durumu.dat` | Java serialization — müşteriler, hesaplar, kullanıcılar, risk skorları, limitler |
| `banka_kayit.txt` | Metin log — tüm işlemlerin zaman damgalı kaydı |

Her iki dosya da proje ana dizininde (`java_proje/`) otomatik oluşturulur.

---

## 10. Güvenlik

### Şifre Depolama

Tüm şifreler **SHA-256** ile hash'lenerek saklanır — düz metin tutulmaz.  
Eski düz metin şifreler ilk girişte otomatik olarak hash'e çevrilir.

### Hesap Kilitleme

3 başarısız giriş denemesinden sonra hesap otomatik kilitlenir.  
Admin, "Kullanıcı Yönetimi → Engeli Kaldır" ile açabilir.

### Rol Sistemi

| Rol | Erişim |
|---|---|
| `YONETICI` | Tüm müşterilerin hesapları, sistem yönetimi |
| `MUSTERI` | Yalnızca kendi hesapları |

---

## 11. Sık Karşılaşılan Sorunlar

### VS Code'da Kırmızı Çizgiler (IDE Hataları)

JavaFX sınıfları (`Button`, `VBox`, `Label` vb.) için IDE hata gösterebilir.  
Bu **gerçek derleme hatası değildir** — Language Server'ın JavaFX'i tanımamasından kaynaklanır.  
Uygulama **F5 ile sorunsuz çalışır**.

Tamamen gidermek için: `Ctrl+Shift+P → Java: Clean Language Server Workspace`

### `banka_durumu.dat` Bozulursa

Dosyayı silin ve uygulamayı yeniden açın. Sistem temizden başlar (yalnızca admin hesabı kalır).

### JavaFX Bulunamadı Hatası

`launch.json` dosyasındaki `--module-path` yolunun `C:\Java\javafx-sdk-21\lib` klasörüne doğru işaret ettiğini kontrol edin.

### `class file has wrong version` Hatası

JavaFX SDK ile Java JDK sürümleri uyuşmuyor demektir.  
Java 21 → JavaFX SDK **21** kullanılmalıdır (25 değil).

---

## 12. Kısa Kullanım Özeti

```
1. Projeyi VS Code'da aç
2. F5 ile çalıştır
3. Giriş: admin / admin123
4. [Opsiyonel] Raporlar → Demo Veri Yükle
5. Müşteri Yönetimi → Yeni müşteri oluştur (kullanıcı adı + şifre gir)
6. Hesap Yönetimi → Yeni hesap oluştur
7. İşlemler → Para yatır / çek / transfer
8. Risk & Limitler → Limitleri ayarla, şüpheli hesapları yönet
9. Raporlar → Bot Simülasyonu çalıştır
10. Raporlar → Durumu Kaydet (kalıcı kayıt için)
```

---

## 13. Kod Yapısı — Hangi Dosya Ne İşe Yarar

Bu bölüm projedeki her Java dosyasını, ne yaptığını ve diğer dosyalarla nasıl bağlandığını açıklar.

---

### 13.1 Giriş Noktası

---

#### `Main.java` — Uygulamayı Başlatır

```
Paket : (varsayılan)
Görevi: JavaFX uygulamasını başlatır
```

JavaFX'in `Application` sınıfından türemiştir. `start()` metodu `GirisEkrani`'nı oluşturur, `main()` ise `launch(args)` ile JavaFX runtime'ı ayağa kaldırır. Başka iş mantığı içermez.

**Kullandığı sınıflar:** `GirisEkrani`

---

### 13.2 model/ Paketi — Veri Yapıları

Bu paketteki sınıflar yalnızca veri tutar ve doğrudan iş mantığı içermez. Tamamı `Serializable` arayüzünü uygular — böylece diske kaydedilebilirler.

---

#### `Account.java` — Tüm Hesap Türlerinin Ana Sınıfı (abstract)

```
Paket  : model
Tür    : abstract class
Kalıtım: Serializable, IRiskCalculatable
```

Tüm hesap türlerinin ortak alanlarını ve metodlarını tanımlar:

| Alan / Metot | Açıklama |
|---|---|
| `hesapId` | HSP000001 formatında benzersiz kimlik |
| `sahibiId` | Hesabın sahibi müşterinin MUS00001 ID'si |
| `bakiye` | Hesaptaki para miktarı |
| `islemler` | Bu hesapta gerçekleşen tüm işlemlerin listesi |
| `paraYatir(miktar)` | Bakiyeye miktar ekler |
| `paraCek(miktar)` | Bakiye yeterliyse düşer, yeterli değilse false döner |
| `getHesapTuru()` | **abstract** — alt sınıf "VADESİZ", "VADELİ" vb. döner |
| `yuksekRiskMi(miktar)` | 50,000 ₺ üzerindeyse true |
| `getRiskPuani()` | Bakiyeye göre 20–80 arası puan döner |

**Alt sınıflar:** `CheckingAccount`, `SavingsAccount`, `DovizHesabi`, `KrediHesabi`

---

#### `CheckingAccount.java` — Vadesiz Hesap

```
Paket  : model
Tür    : class
Kalıtım: Account
```

En basit hesap türü. `Account`'tan başka hiçbir şey eklemez; sadece `getHesapTuru()` metodunu `"VADESİZ"` döndürecek şekilde uygular.

---

#### `SavingsAccount.java` — Vadeli Hesap

```
Paket  : model
Tür    : class
Kalıtım: Account
```

Faiz oranı (%3 başlangıç) tutan vadeli hesap.

| Metot | Açıklama |
|---|---|
| `faizUygula()` | `bakiye += bakiye * faizOrani` — admin tetikler |
| `getFaizOrani()` | Mevcut yıllık faiz oranını döner |
| `setFaizOrani(oran)` | Faiz oranını değiştirir |

---

#### `KrediHesabi.java` — Kredi Hesabı

```
Paket  : model
Tür    : class
Kalıtım: Account
```

Bakiyesi negatif gidebilen özel hesap. Başlangıç bakiyesi her zaman 0'dır; kredi limiti ayrı tutulur.

| Alan / Metot | Açıklama |
|---|---|
| `krediLimiti` | Çekilebilecek maksimum borç miktarı |
| `faizOrani` | Aylık faiz (varsayılan %2) |
| `paraCek(miktar)` | Bakiye `−krediLimiti`'nin altına inemez |
| `krediOde(odeme)` | Borcu azaltır; fazla ödeme kabul edilmez |
| `aylikFaizUygula()` | Son ödemeden 30+ gün geçmişse faiz ekler |
| `kalanKredi()` | `krediLimiti + bakiye` (bakiye negatif olduğundan kalan limit) |
| `getBorcMiktari()` | Bakiyenin mutlak değeri (borç) |
| `getRiskPuani()` | Kullanılan kredi oranına göre 0–100 puan |
| `yuksekRiskMi(miktar)` | 20,000 ₺ üzerindeyse true (normal hesaptan düşük eşik) |

---

#### `DovizHesabi.java` — Döviz Hesabı (USD/EUR/GBP)

```
Paket  : model
Tür    : class
Kalıtım: Account
```

Bakiyesi yabancı para cinsinden tutulur. `Account`'un `bakiye` alanı dolar/euro/sterlin miktarıdır.

| Alan / Metot | Açıklama |
|---|---|
| `paraBirimi` | `USD`, `EUR` veya `GBP` (enum) |
| `dovizKuru` | 1 yabancı para = kaç TL |
| `getBakiyeTL()` | `bakiye * dovizKuru` — TL karşılığı |
| `dovizKuruGuncelle(yeniKur)` | Kuru günceller |
| `getParaBirimiSimgesi()` | `$`, `€` veya `£` döner |

---

#### `Transaction.java` — Tüm İşlem Türlerinin Ana Sınıfı (abstract)

```
Paket  : model
Tür    : abstract class
Kalıtım: Serializable
```

Her işlemin ortak bilgilerini tutar:

| Alan | Açıklama |
|---|---|
| `islemId` | TRX00000001 formatında kimlik |
| `miktar` | İşlem tutarı |
| `zaman` | İşlemin gerçekleştiği `LocalDateTime` |
| `durum` | `PENDING`, `APPROVED`, `REJECTED`, `FLAGGED` |

**Alt sınıflar:** `Deposit`, `Withdraw`, `Transfer`

---

#### `Deposit.java` — Para Yatırma İşlemi

`Transaction`'dan türer. `hedefHesapId` alanı taşır. `getTur()` → `"PARA_YATIRMA"`

#### `Withdraw.java` — Para Çekme İşlemi

`Transaction`'dan türer. `kaynakHesapId` alanı taşır. `getTur()` → `"PARA_CEKME"`

#### `Transfer.java` — Transfer İşlemi

`Transaction`'dan türer. `gondereciHesapId` ve `aliciHesapId` alanlarını taşır. `getTur()` → `"TRANSFER"`

---

#### `Customer.java` — Müşteri

```
Paket  : model
Tür    : class
Kalıtım: Serializable
```

Bir bankacılık müşterisini temsil eder.

| Alan | Açıklama |
|---|---|
| `musteriId` | MUS00001 formatında benzersiz kimlik |
| `ad` | Ad soyad |
| `eposta` | E-posta adresi |
| `hesaplar` | Müşteriye ait hesapların listesi (`List<Account>`) |

`hesapEkle(hesap)` ile yeni hesap bağlanır. Liste doğrudan erişilebilir (`getHesaplar()`) — silme işlemlerinde `remove()` ile hesap listeden çıkarılır.

---

#### `Kullanici.java` — Giriş Kullanıcısı

```
Paket  : model
Tür    : class
Kalıtım: Serializable
```

Uygulamaya giriş yapan kişiyi temsil eder. **`Customer`'dan ayrıdır** — `musteriId` alanıyla bankacılık müşterisine bağlanır.

| Alan | Açıklama |
|---|---|
| `kullaniciAdi` | Giriş adı |
| `sifre` | SHA-256 hash'lenmiş şifre |
| `rol` | `YONETICI` veya `MUSTERI` |
| `musteriId` | Bağlı müşteri kimliği (admin için `null`) |
| `basarisizGirisSayisi` | Başarısız giriş sayacı (3'e ulaşınca kilitler) |
| `engelliMi` | 3 başarısız girişten sonra `true` olur |
| `pasifMi` | Admin tarafından devre dışı bırakılabilir |

Metodlar: `engelleHesap()`, `engelKaldir()`, `pasifYap()`, `aktifYap()`, `basarisizGirisArtir()`

---

#### `HesapLimiti.java` — İşlem Limitleri

```
Paket  : model
Tür    : class
Kalıtım: Serializable
```

Bir hesaba ait 4 limit değerini tutar:

| Alan | Açıklama |
|---|---|
| `gunlukCekimLimiti` | Günde toplam çekilebilecek maksimum (varsayılan 20,000 ₺) |
| `gunlukTransferLimiti` | Günde toplam transfer maksimum (varsayılan 30,000 ₺) |
| `tekIslemCekimLimiti` | Tek seferde çekilebilecek maksimum (varsayılan 50,000 ₺) |
| `tekIslemTransferLimiti` | Tek seferde transfer maksimum (varsayılan 50,000 ₺) |

---

#### `SupheSebebi.java` — Şüpheli İşaret Kaydı

```
Paket  : model
Tür    : class
Kalıtım: Serializable
```

Bir hesabın neden şüpheli işaretlendiğini belgeler:

| Alan | Açıklama |
|---|---|
| `hesapId` | Şüpheli işaretlenen hesap |
| `musteriId` | Hesabın sahibi |
| `sebep` | Metin açıklama (ör. "Büyük işlem: 75,000 ₺") |
| `ilgiliMiktar` | Şüpheyi tetikleyen tutar |
| `zaman` | Şüpheli işaretlenme zamanı |

---

#### `BankState.java` — Sistem Durumu Anlık Görüntüsü

```
Paket  : model
Tür    : class
Kalıtım: Serializable
```

`banka_durumu.dat` dosyasına kaydedilen ve oradan yüklenen tek nesnedir. İçinde tüm sistem durumu bulunur:

```
BankState içeriği:
├── List<Customer>             musteriler
├── List<Account>              hesaplar
├── Map<String, Kullanici>     kullanicilar
├── Set<String>                suphelihHesaplar
├── Map<String, SupheSebebi>   supheSebebleri
├── Map<String, HesapLimiti>   hesapLimitleri
├── Map<String, Integer>       riskSkorlari
├── Map<String, LocalDate>     skorGuncelleme
├── int                        musteriSayaci
├── int                        hesapSayaci
└── int                        islemSayaci
```

Sayaçlar ID üretimi için kritiktir — yüklenmezse ID'ler sıfırdan başlar, çakışma olur.

---

#### `IRiskCalculatable.java` — Risk Arayüzü

```
Paket  : model
Tür    : interface
```

`Account` ve `RiskEngine` tarafından uygulanan iki metodlu arayüz:

```java
boolean yuksekRiskMi(double miktar);  // Bu tutar yüksek riskli mi?
double  getRiskPuani();               // Hesabın/durumun risk puanı (0-100)
```

`Account` bu arayüzü uygular; her hesap türü risk eşiğini kendine özgü belirler (ör. `KrediHesabi` için eşik 20,000 ₺, normal hesap için 50,000 ₺).

---

#### `YetersizBakiyeException.java` — Bakiye Hatası

```
Paket  : model
Tür    : RuntimeException
```

Para çekme veya transfer sırasında bakiye yetersiz olduğunda fırlatılır. `mevcutBakiye` ve `istenenMiktar` bilgilerini taşır — UI'da kullanıcıya "Yetersiz bakiye: mevcut X ₺" mesajı gösterilir.

---

#### `RiskLimitiAsildiException.java` — Limit Hatası

```
Paket  : model
Tür    : RuntimeException
```

Günlük veya tek işlem limiti aşıldığında fırlatılır. `limit` ve `istenenMiktar` bilgilerini taşır.

---

### 13.3 service/ Paketi — İş Mantığı

---

#### `IBankService.java` — Servis Arayüzü

```
Paket  : service
Tür    : interface
```

`BankController`'ın dışa açtığı temel işlemleri tanımlayan sözleşme:

```
musteriOlustur()     hesapOlustur()      paraYatir()
paraCek()            transferYap()       getMusteri()
getHesap()           tumMusteriler()     tumHesaplar()
hesapMusteriyeAitMi() musteriHesaplari() limitGuncelle()
getHesapLimiti()     supheSebebiGetir()  tumSupheSebebleri()
```

`MusteriPaneli` ve `YoneticiPaneli` teorik olarak bu arayüz üzerinden çalışır; `BankController` bu arayüzü uygular.

---

#### `Repository.java` — Generic Depo Arayüzü

```
Paket  : service
Tür    : interface (generic)
```

CRUD işlemlerini tanımlayan generic sözleşme:

```java
void    kaydet(T entity)
T       idIleGetir(ID id)
List<T> hepsiniGetir()
void    sil(ID id)
void    temizle()
```

`AccountRepository`, `CustomerRepository`, `TransactionRepository` bu arayüzü uygular. Her biri içinde `HashMap<String, T>` tutar.

---

#### `AccountRepository.java` — Hesap Deposu

`Repository<Account, String>` uygular. İçindeki `HashMap<String, Account>` hesap ID'sine göre hızlı erişim sağlar. `BankController` içinde `hesapDeposu` adıyla kullanılır.

#### `CustomerRepository.java` — Müşteri Deposu

`Repository<Customer, String>` uygular. Müşteri ID'sine göre indekslenmiş `HashMap`. `BankController` içinde `musteriDeposu` adıyla kullanılır.

#### `TransactionRepository.java` — İşlem Deposu

`Repository<Transaction, String>` uygular. İşlem ID'sine göre indekslenmiş `HashMap`. `BankController` içinde `islemDeposu` adıyla kullanılır.

---

#### `BankController.java` — Ana Kontrolcü ⭐

```
Paket     : service
Tür       : class
Uygulanan : IBankService
```

Tüm iş mantığının merkezi. UI katmanı bu sınıf üzerinden her şeyi yapar.

**Alanlar:**

| Alan | Tür | Görevi |
|---|---|---|
| `musteriDeposu` | `CustomerRepository` | Müşterileri depolar |
| `hesapDeposu` | `AccountRepository` | Hesapları depolar |
| `islemDeposu` | `TransactionRepository` | İşlemleri depolar |
| `suphelihHesaplar` | `Set<String>` | Şüpheli hesap ID'leri |
| `supheSebebleri` | `Map<String, SupheSebebi>` | Şüphe gerekçeleri |
| `riskMotoru` | `RiskEngine` | Risk hesaplamaları |
| `kaydedici` | `FileLogger` | Log dosyasına yazar |
| `kimlikDogrulama` | `KimlikDogrulama` | Kullanıcı işlemleri |
| `dinleyiciler` | `List<RiskDinleyici>` | Observer listesi |
| `bekleyenIslemler` | `Map<String, BekleyenIslem>` | Geri alınabilir işlemler (3 dk) |
| `demoMusteri` | `Set<String>` | Demo müşteri ID'leri ([DEMO] etiketi için) |
| `musteriSayaci` | `int` | MUS ID üretimi |
| `hesapSayaci` | `int` | HSP ID üretimi |
| `islemSayaci` | `int` | TRX ID üretimi |

**Temel Metodlar:**

| Metot | Görevi |
|---|---|
| `musteriOlustur(ad, eposta)` | Yeni Customer oluşturur, ID üretir, depoya kaydeder |
| `hesapOlustur(musteriId, tur, bakiye)` | Türe göre doğru Account alt sınıfını oluşturur |
| `paraYatir(hesapId, miktar)` | Risk kontrolü → bakiyeye ekler → log → risk skoru günceller |
| `paraCek(hesapId, miktar)` | Şüpheli kontrol → limit kontrol → bakiye düşer → geri alma kaydı |
| `transferYap(kaynakId, hedefId, miktar)` | Her iki hesap için risk kontrol → çift taraflı bakiye işlemi |
| `hesapIsaretle(hesapId)` | Hesabı şüpheli işaretler, SupheSebebi oluşturur, risk olayı yayınlar |
| `isaretKaldir(hesapId)` | Şüpheli işareti kaldırır |
| `musteriSil(musteriId)` | Müşteri + hesaplar + Kullanici pasifleştirilir |
| `hesapSil(hesapId)` | Hesap silindi, Customer listesinden de çıkarılır |
| `durumKaydet(dosyaYolu)` | BankState oluşturur, Serializer ile diske yazar |
| `durumYukle(dosyaYolu)` | Diskten BankState okur, tüm depoları doldurur |
| `islemSonrasiRiskKontrol(...)` | Her işlem sonrası RiskEngine'i çağırır, gerekirse dondurur |
| `islemGeriAl(islemId)` | 3 dakika içindeyse çekim/transferi tersine çevirir |

**İç sınıf `BekleyenIslem`:** Her çekim ve transferin geri alınabilirlik durumunu tutar. `geriAlinabilirMi()` 3 dakika dolup dolmadığını kontrol eder.

---

#### `KimlikDogrulama.java` — Kimlik Doğrulama

```
Paket  : service
Tür    : class
```

Kullanıcı girişini ve hesap yönetimini yönetir.

| Metot | Görevi |
|---|---|
| `girisYap(ad, sifre)` | Hash karşılaştırır → başarısız sayacı artırır → 3'te kilitler |
| `kullaniciEkle(kullanici)` | Düz metin şifreyi otomatik hash'ler, listeye ekler |
| `kullanicilariYukle(map)` | Diskten yüklenen kullanıcıları içe aktarır, admin'i korur |
| `sifreSifirla(kulAdi, yeniSifre)` | Admin: eski şifre sormadan sıfırlar, hash'ler |
| `sifreDegistir(kulAdi, eski, yeni)` | Müşteri: eski şifre doğrulandıktan sonra değiştirir |
| `engelliMi(kulAdi)` / `pasifMi(kulAdi)` | Hesap durumu sorgusu |

`varsayilanKullanicilariYukle()`: Her başlangıçta `admin/admin123` hesabını oluşturur (hash'lenmiş).

**Otomatik şifre geçişi:** Eski sistemden gelen düz metin şifreler, ilk başarılı girişte SHA-256 hash'e otomatik dönüştürülür.

---

#### `RiskEngine.java` — Risk Motoru ⭐

```
Paket     : service
Tür       : class
Uygulanan : IRiskCalculatable
```

Tüm risk hesaplamalarının merkezi. Günlük limitler, velocity check, gece modu, risk skoru burada hesaplanır.

**Sabit Limitler:**

```
Para yatırma maks  : 100,000 ₺
Para çekme maks    :  50,000 ₺
Transfer maks      :  50,000 ₺
Günlük çekim       :  20,000 ₺
Günlük transfer    :  30,000 ₺
Gece modu eşiği    :  10,000 ₺ (01:00–06:00)
Ani düşüş oranı    :  %95 (bakiyenin %95'ini tek çekimde çekmek)
Velocity eşiği     :  5 dakikada 10 işlem
```

**Risk Skoru Eşikleri:**

```
SKOR_IZLEME = 31   → 🟡 İzleniyor
SKOR_RISKLI = 61   → 🟠 Riskli
SKOR_DONDUR = 86   → 🔴 Otomatik dondur
```

**Puan ekleme kaynaklarına göre:**

| Tetikleyici | Puan | Metot |
|---|---|---|
| Büyük işlem ≥ 50K ₺ | +20 | `islemSonrasiRiskKontrol` |
| Gece modu ≥ 10K ₺ | +15 | `geceModuRisklimi()` |
| Ani düşüş ≥ %95 | +15 | `aniDususVarMi()` |
| Velocity ≥ 10/5dk | +30 | `kisaVadeliCokIslemMi()` |
| Her temiz geçen gün | −5 | `getRiskSkoru()` içinde otomatik |

**Günlük sıfırlama:** `gunlukCekimler`, `gunlukTransferler` tarih değişince otomatik sıfırlanır.

**Sliding window (velocity):** Son 5 dakikanın işlem zamanları `kisaVadeliIslemler` listesinde tutulur; 10 dakika geçmiş kayıtlar otomatik temizlenir.

---

#### `HashUtil.java` — SHA-256 Hash Yardımcısı

```
Paket  : service
Tür    : final class (instantiate edilemez)
```

İki static metod:

```java
sha256(metin)   // String → 64 karakterli hex hash
hashMi(s)       // 64 karakterli hex mi? → önceden hash'lenmiş mi kontrolü
```

Java'nın `java.security.MessageDigest` sınıfını kullanır. `KimlikDogrulama` şifre saklamak ve doğrulamak için bu sınıfı kullanır.

---

#### `RiskDinleyici.java` — Observer Arayüzü

```
Paket  : service
Tür    : @FunctionalInterface
```

Observer (Gözlemci) tasarım deseni için arayüz. Tek metodu `onRiskOlayi(RiskOlayi olay)`. Lambda ile kullanılabilir. `YoneticiPaneli` ve `MusteriPaneli` birer `RiskDinleyici` lambda'sı oluşturup `BankController.dinleyiciEkle()` ile kaydeder.

---

#### `RiskOlayi.java` — Risk Olayı Verisi

```
Paket  : service
Tür    : class
```

`RiskDinleyici`'ye iletilen olay nesnesi. Hangi hesapta, hangi müşteride, ne tür bir risk olayı yaşandığını ve tutarını taşır.

---

#### `RiskOlayTuru.java` — Risk Olay Türleri

```
Paket  : service
Tür    : enum
```

```java
SUPHELI_ISLEM       // Şüpheli hesaptan işlem girişimi
YUKSEK_TUTAR        // 50K ₺ üzeri işlem
GECE_MODU_ISLEM     // Gece saatinde büyük işlem
ANI_BAKIYE_DUSUSU   // Bakiyenin %95'ini çekme
COK_FAZLA_ISLEM     // 5 dakikada 10+ işlem
HESAP_DONDURULDU    // Risk skoru 86'yı aştı
SUPHELI_KALDIRILDI  // Admin şüpheyi kaldırdı
```

---

### 13.4 persistence/ Paketi — Veri Saklama

---

#### `FileLogger.java` — Log Dosyasına Yazar

```
Paket  : persistence
Tür    : class
```

Her çağrıda `banka_kayit.txt`'e bir satır ekler. Satır formatı:

```
[2025-01-15T14:32:05.123] PARA_YATIRILDI: TRX00000001 | HSP000001 | +5000.0
```

`setSessiz(true)` ile log yazımı susturulabilir — bot simülasyonu sırasında gerçek log kirlenmez.

| Metot | Görevi |
|---|---|
| `kaydet(mesaj)` | Zaman damgasıyla dosyaya yazar (`sessiz` modda atlar) |
| `kayitlariOku()` | Tüm log dosyasını string olarak döner (UI'da göstermek için) |
| `setSessiz(boolean)` | Log yazımını açar/kapatır |

---

#### `Serializer.java` — Java Serileştirme

```
Paket  : persistence
Tür    : class
```

İki static metod:

```java
serialize(nesne, dosyaYolu)    // Object → .dat dosyası (ObjectOutputStream)
deserialize(dosyaYolu)         // .dat dosyası → Object (ObjectInputStream)
```

`BankController.durumKaydet()` ve `durumYukle()` bu sınıfı kullanır. Kaydedilen nesne bir `BankState` örneğidir.

---

### 13.5 ui/ Paketi — Arayüz

---

#### `GirisEkrani.java` — Login Ekranı

```
Paket  : ui
Tür    : class
```

Uygulamanın giriş kapısı. Constructor'da:
1. `FileLogger` oluşturur
2. `KimlikDogrulama` oluşturur (admin otomatik eklenir)
3. `BankController` oluşturur
4. `banka_durumu.dat` varsa `durumYukle()` çağırır
5. JavaFX ekranını gösterir

Sol panel: marka görselleşmesi (gradient, halkalar, özellik listesi).  
Sağ panel: kullanıcı adı + şifre formu.

Başarılı girişte `MainFrame` açılır ve `GirisEkrani` kaybolur.

---

#### `MainFrame.java` — Ana Pencere Çerçevesi

```
Paket  : ui
Tür    : class
```

Üst başlık şeridini (banka logosu, kullanıcı bilgisi, çıkış butonu) oluşturur. Kullanıcı rolüne göre:
- `YONETICI` → `YoneticiPaneli`
- `MUSTERI` → `MusteriPaneli`

Çıkış butonu: mevcut sahneyi kapatır, `GirisEkrani`'nı yeniden açar (tüm sistem sıfırlanmaz, `BankController` yeniden yüklenir).

---

#### `YoneticiPaneli.java` — Yönetici Paneli

```
Paket  : ui
Tür    : class
Kalıtım: BorderPane (JavaFX)
```

Admin kullanıcıların gördüğü 6 sekmeli panel. Her sekme kendi `private` metodunda oluşturulur:

| Metod | Sekme |
|---|---|
| `musteriSekme()` | Müşteri oluşturma formu + müşteri listesi tablosu |
| `hesapSekme()` | Hesap oluşturma formu + hesap listesi tablosu |
| `islemlerSekme()` | Para yatır / çek / transfer blokları |
| `kullaniciSekme()` | Şifre sıfırlama, profil düzenleme, kullanıcı listesi |
| `raporlarSekme()` | İstatistik kartları, grafikler, bot sim butonu |
| `riskLimitlerSekme()` | Limit formu + şüpheli hesap tablosu |

**Önemli iç metodlar:**

| Metod | Görevi |
|---|---|
| `musteriOlustur()` | Hem `Customer` hem `Kullanici` oluşturur, durumu kaydeder |
| `hesapOlustur()` | Hesap oluşturur, durumu kaydeder |
| `botSimulasyonuCalistir(botBtn)` | Ayrı thread'de 30 işlem çalıştırır, log'u susturur |
| `demoVeriYukle()` | 4 hazır müşteri + hesap oluşturur, kaydedilmez |
| `musterileriYenile()` | Demo müşterilere `[DEMO]` etiketi ekleyerek tabloyu günceller |
| `riskDinleyiciKaydet()` | Observer: herhangi risk olayında toast bildirimi gösterir |

---

#### `MusteriPaneli.java` — Müşteri Paneli

```
Paket  : ui
Tür    : class
Kalıtım: BorderPane (JavaFX)
```

Müşteri kullanıcıların gördüğü 7 sekmeli panel. Müşteri yalnızca `kullanici.getMusteriId()`'sine ait hesapları görür.

| Metod | Sekme |
|---|---|
| `hesaplarimSekme()` | Hesap tablosu + varlık özeti kartları |
| `paraYatirSekme()` | Para yatırma formu |
| `paraCekSekme()` | Para çekme + geri alma butonu |
| `transferSekme()` | Transfer formu + geri alma butonu |
| `islemGecmisiSekme()` | Filtrelenebilir işlem geçmişi + PDF export |
| `krediSekme()` | Kredi kullanım + ödeme formları |
| `profilimSekme()` | Şifre değiştirme, profil bilgileri |

**Observer kaydı:** Constructor'da `BankController.dinleyiciEkle()` ile kayıt olur — kendi müşteri ID'siyle ilgili risk olaylarını dinler, toast bildirimi gösterir.

---

#### `UITema.java` — UI Yardımcı Sınıfı

```
Paket  : ui
Tür    : class (static metodlar)
```

Tüm UI bileşenlerinin tutarlı görünmesini sağlar. Tekrarlanan bileşen oluşturma kodunu merkezi bir yerde toplar.

| Metod | Görevi |
|---|---|
| `kart(baslik)` | Başlıklı beyaz kart (VBox) oluşturur |
| `alan()` | Standart stillendirilmiş TextField |
| `etiket(metin)` | Standart Label |
| `anaButon(metin)` | Mavi ana buton |
| `normalButon(metin)` | Gri normal buton |
| `tehlikeButon(metin)` | Kırmızı silme/uyarı butonu |
| `tablo(sutunlar...)` | TableView oluşturur, sütunları bağlar |
| `satirEkle(tablo, degerler...)` | Tabloya satır ekler |
| `bilgi/uyari/hata(baslik, mesaj)` | Alert dialog gösterir |
| `onay(baslik, mesaj)` | Onay dialog (true/false döner) |
| `durumGoster(label, mesaj, basarili)` | Yeşil/kırmızı durum mesajı |
| `toast(scene, mesaj, basarili)` | 3 saniye görünen geçici bildirim |
| `CSS_YOLU` | `banka.css` stil dosyasının path'i |

---

#### `banka.css` — Stil Dosyası

Tüm JavaFX bileşenlerinin görsel stilini tanımlar. Önemli CSS sınıfları:

| CSS Sınıfı | Kullanıldığı Yer |
|---|---|
| `.giris-kart` | GirisEkrani sağ panel kartı |
| `.giris-alan` | GirisEkrani text alanları |
| `.giris-buton` | GirisEkrani giriş butonu |
| `.istat-kart` | Raporlar sekmesi istatistik kartları |

---

### 13.6 Sınıf İlişki Haritası

```
                    ┌─────────────┐
                    │    Main     │
                    └──────┬──────┘
                           │ başlatır
                    ┌──────▼──────┐
                    │ GirisEkrani │
                    └──────┬──────┘
                           │ oluşturur
          ┌────────────────┼────────────────┐
          │                │                │
   ┌──────▼──────┐  ┌──────▼──────┐  ┌─────▼──────┐
   │  FileLogger │  │  KimlikDo-  │  │  BankCon-  │
   │             │  │  grulama    │  │  troller   │
   └─────────────┘  └──────┬──────┘  └─────┬──────┘
                           │               │
                    ┌──────▼──────┐        │  içerir
                    │  Kullanici  │  ┌─────┴──────────────────┐
                    └─────────────┘  │                        │
                                     │  ┌──────────────┐      │
                                     │  │ CustomerRepo │      │
                                     │  ├──────────────┤      │
                                     │  │ AccountRepo  │      │
                                     │  ├──────────────┤      │
                                     │  │ Transaction  │      │
                                     │  │    Repo      │      │
                                     │  ├──────────────┤      │
                                     │  │ RiskEngine   │      │
                                     │  ├──────────────┤      │
                                     │  │ FileLogger   │      │
                                     │  └──────────────┘      │
                                     └────────────────────────┘
                                                │
                    ┌──────────────┬────────────┘
                    │              │
             ┌──────▼──────┐ ┌────▼──────────┐
             │ Yonetici-   │ │  Musteri-     │
             │ Paneli      │ │  Paneli       │
             └──────┬──────┘ └───────────────┘
                    │
              ┌─────┴──────────────┐
              │                    │
       ┌──────▼──────┐    ┌────────▼────────┐
       │   Customer  │    │    Account      │ (abstract)
       │  ┌─────────┐│    │ ┌─────────────┐ │
       │  │hesaplar ││    │ │CheckingAcc. │ │
       │  └────┬────┘│    │ ├─────────────┤ │
       └───────┼─────┘    │ │SavingsAcc.  │ │
               │          │ ├─────────────┤ │
               └──────────► │DovizHesabi  │ │
                            │ ├─────────────┤ │
                            │ │KrediHesabi  │ │
                            │ └─────────────┘ │
                            └─────────────────┘
```

---

### 13.7 Kullanılan Tasarım Desenleri (Design Patterns)

| Desen | Nerede Kullanıldı | Açıklama |
|---|---|---|
| **MVC** | Genel mimari | Model (`model/`), View (`ui/`), Controller (`service/`) katmanları |
| **Repository** | `CustomerRepository`, `AccountRepository`, `TransactionRepository` | Veri erişimini soyutlar; depolama detayı iş mantığından ayrılır |
| **Observer** | `RiskDinleyici` / `BankController` / paneller | Risk olayları UI'ya event olarak iletilir, sıkı bağımlılık yok |
| **Template Method** | `Account` abstract sınıfı | `paraYatir`, `paraCek` şablonu sabit; `getHesapTuru` alt sınıfta |
| **Strategy** | `IRiskCalculatable` | Her hesap türü kendi risk puanını farklı hesaplar |
| **Singleton-benzeri** | `HashUtil` | Static metodlar, instantiate edilemez |
| **DTO (Data Transfer Object)** | `BankState`, `RiskOlayi` | Katmanlar arası veri taşıma nesneleri |
| **Factory yöntemi** | `BankController.hesapOlustur()` | `switch` ile türe göre doğru `Account` alt sınıfı oluşturulur |

---

*OOP Bankacılık Sistemi — Java 21 + JavaFX 21*
