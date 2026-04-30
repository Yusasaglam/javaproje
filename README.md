# Türk Bankası Yönetim Sistemi

Java tabanlı, katmanlı mimarili masaüstü bankacılık uygulaması.

## Özellikler

- Kullanıcı girişi ve rol tabanlı yetkilendirme (Yönetici / Müşteri)
- Müşteri ve hesap yönetimi
- Para yatırma, çekme ve transfer işlemleri
- İşlem geçmişi
- Risk motoru (günlük limit, şüpheli hesap tespiti)
- Otomatik durum kaydetme ve yükleme (serileştirme)
- Dosya tabanlı işlem kaydı

## Mimari

```
SRC/
├── Main.java
├── model/          → Veri modelleri (Account, Customer, Transaction, ...)
├── service/        → İş mantığı (BankController, RiskEngine, KimlikDogrulama, ...)
├── persistence/    → Kalıcılık (FileLogger, Serializer)
└── ui/             → Swing arayüzü (GirisEkrani, MainFrame, YoneticiPaneli, MusteriPaneli)
```

## Katmanlar

| Katman | Paket | Sorumluluk |
|---|---|---|
| Model | `model` | Soyut ve somut varlık sınıfları |
| Servis | `service` | İş kuralları, risk kontrolü, kimlik doğrulama |
| Kalıcılık | `persistence` | Dosya kaydı ve serileştirme |
| Arayüz | `ui` | Swing GUI, sadece servis katmanını çağırır |

## Kullanım

### Derleme

```bash
javac -d out -sourcepath SRC SRC/Main.java
```

### Çalıştırma

```bash
java -cp out Main
```

### Varsayılan Giriş

| Kullanıcı Adı | Şifre | Rol |
|---|---|---|
| admin | admin123 | Yönetici |

## Teknolojiler

- Java (Swing GUI)
- Java Serialization (durum kaydetme)
- Collections Framework
- Generics
- MVC mimarisi

## İsimlendirme

Sınıf, metot ve değişken isimlerinde Türkçe alan terimleri kullanılır. Java sözdizimi ve kütüphane isimleri İngilizce kalır.

## Takım

- Eren
- Eray
- Yusa
