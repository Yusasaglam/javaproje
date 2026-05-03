# Türk Bankası Yönetim Sistemi

Java tabanlı, katmanlı mimarili masaüstü bankacılık uygulaması.

## Özellikler

- Kullanıcı girişi ve rol tabanlı yetkilendirme (Yönetici / Müşteri)
- Müşteri ve hesap yönetimi (vadesiz, vadeli, döviz, kredi)
- Para yatırma, çekme, transfer ve kredi ödeme işlemleri
- İşlem geçmişi, filtreleme ve PDF export
- **Risk motoru** — 7 kurallı event-tabanlı skorlama sistemi:
  - Hızlı hesap boşaltma (kart hırsızlığı tespiti)
  - Gece saati çekimi (01:00–06:00)
  - Ani bakiye boşaltma (%90 eşiği)
  - Yapılandırma / structuring (kara para aklama tespiti)
  - Müşteri velocity (hesap ele geçirilme tespiti)
  - Günlük işlem limiti (bot tespiti)
  - Yeni alıcıya büyük transfer (account takeover tespiti)
- Otomatik dondurma (skor ≥86) ve otomatik çözme (skor <20)
- Müşteri limit değişim talebi (24 saat gecikme + SMS simülasyonu)
- İşlem geri alma (3 dakika penceresi)
- Bot simülasyonu — izole modda, gerçek verileri etkilemez
- Aktivite log sistemi (her işlem kayıt altına alınır)
- Otomatik durum kaydetme ve yükleme (Java serileştirme)
- Dosya tabanlı işlem kaydı (`banka_kayit.txt`)

## Mimari

```
SRC/
├── Main.java
├── model/          → Veri modelleri (Account, Customer, Transaction, BankState, ...)
├── service/        → İş mantığı (BankController, RiskEngine, KimlikDogrulama, ...)
├── persistence/    → Kalıcılık (FileLogger, Serializer)
└── ui/             → JavaFX arayüzü (GirisEkrani, MainFrame, YoneticiPaneli, MusteriPaneli)
```

## Katmanlar

| Katman | Paket | Sorumluluk |
|---|---|---|
| Model | `model` | Soyut ve somut varlık sınıfları, istisnalar |
| Servis | `service` | İş kuralları, risk kontrolü, kimlik doğrulama, log |
| Kalıcılık | `persistence` | Dosya kaydı ve Java serileştirme |
| Arayüz | `ui` | JavaFX GUI, yalnızca servis katmanını çağırır |

## Kullanım

### Derleme

```powershell
javac --module-path "C:/Users/tunce/OneDrive/Desktop/javafx-sdk-26.0.1/lib" `
      --add-modules javafx.controls,javafx.fxml `
      -encoding UTF-8 -cp SRC -d out `
      SRC/model/*.java SRC/service/*.java SRC/ui/*.java SRC/Main.java
```

### Çalıştırma

```powershell
java --module-path "C:/Users/tunce/OneDrive/Desktop/javafx-sdk-26.0.1/lib" `
     --add-modules javafx.controls,javafx.fxml `
     -cp out Main
```

### VS Code ile (Önerilen)

F5 — `.vscode/launch.json` otomatik derleyip çalıştırır.

### Varsayılan Giriş

| Kullanıcı Adı | Şifre | Rol |
|---|---|---|
| admin | admin123 | Yönetici |

## Teknolojiler

- Java 25 (JDK — Eclipse Temurin)
- JavaFX 26.0.1 (GUI)
- Java Serialization (durum kaydetme, `serialVersionUID 7L`)
- Collections Framework, Generics
- MVC mimarisi
- Observer tasarım deseni (risk olayları)
- Event-sourced risk skorlama (decay tabanlı)

## İsimlendirme

Sınıf, metot ve değişken isimlerinde Türkçe alan terimleri kullanılır. Java sözdizimi ve kütüphane isimleri İngilizce kalır.

## Takım

- Eren
- Eray
- Yusa
