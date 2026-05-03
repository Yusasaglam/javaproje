# MİMARİ — Türk Bankası Yönetim Sistemi

---

## KATMANLAR

### Model Katmanı (`model/`)

Veriyi tutar, iş mantığı içermez. Tümü `Serializable`.

**Hesap hiyerarşisi:**
- `Account` (abstract) ← `CheckingAccount`, `SavingsAccount`, `DovizHesabi`, `KrediHesabi`

**İşlem hiyerarşisi:**
- `Transaction` (abstract) ← `Deposit`, `Withdraw`, `Transfer`

**Diğer modeller:**
- `Customer`, `Kullanici`, `HesapLimiti`, `SupheSebebi`
- `BankState` — serileştirilen tek nesne (serialVersionUID 7L), tüm sistemi taşır

**Arayüzler / İstisnalar:**
- `IRiskCalculatable` — `yuksekRiskMi()`, `getRiskPuani()`
- `YetersizBakiyeException`, `RiskLimitiAsildiException`

---

### Servis Katmanı (`service/`)

İş mantığının tamamı burada. UI doğrudan model'e erişmez.

**Ana kontrolcü:**
- `BankController` — tüm işlemler (paraYatir, paraCek, transferYap, krediOde), kalıcılık, observer yönetimi

**Risk motoru:**
- `RiskEngine` — 7 kural, event-tabanlı skorlama, decay sistemi
- `RiskOlayKaydi` — her risk olayının kaydı (puan, ağırlık, islemId, zaman)
- `IslemRiskAgirlik` — işlem türüne göre çarpan ve günlük decay değerleri (enum)
- `MusteriRiskProfili` — müşteri seviye risk skoru (%25 bulaşma modeli)
- `DondurmaKaydi` — hesap dondurma kaydı (OTOMATIK / MANUEL)
- `DondurmaSecegi` — dondurma türü enum
- `KullaniciKategorisi` — müşteri kategorisi (BIREYSEL / KURUMSAL), adaptif velocity eşikleri

**Log sistemi:**
- `ActivityLog` — tek bir aktivite kaydı (islem tipi, risk delta, kaynak: GERCEK/DEMO)
- `AktiviteLogServisi` — log listesi yönetimi

**Diğer servisler:**
- `KimlikDogrulama` — SHA-256 hash, giriş, hesap kilitleme, rol yönetimi
- `HashUtil` — SHA-256 yardımcısı (static)
- `BekleyenLimitDegisimi` — 24 saatlik limit değişim talebi
- `Repository<T,ID>` (generic interface) ← `AccountRepository`, `CustomerRepository`, `TransactionRepository`
- `RiskDinleyici` (@FunctionalInterface), `RiskOlayi`, `RiskOlayTuru`
- `IBankService` — BankController'ın dışa açtığı arayüz

---

### Kalıcılık Katmanı (`persistence/`)

- `FileLogger` — `banka_kayit.txt`'e zaman damgalı log yazar; bot modunda sessiz
- `Serializer` — `BankState`'i `.dat` dosyasına yazar / okur

---

### Arayüz Katmanı (`ui/`)

- `GirisEkrani` — login ekranı; `BankController` burada oluşturulur, `durumYukle()` çağrılır
- `MainFrame` — üst başlık şeridi; role göre `YoneticiPaneli` veya `MusteriPaneli` açar
- `YoneticiPaneli` — 6 sekmeli yönetici paneli
- `MusteriPaneli` — 8 sekmeli müşteri paneli (Limitlerimi Yönet sekmesi dahil)
- `UITema` — buton, kart, alert, toast yardımcı static metodları

---

## TASARIM DESENLERİ

| Desen | Nerede |
|---|---|
| **MVC** | model / service / ui katman ayrımı |
| **Repository** | `AccountRepository`, `CustomerRepository`, `TransactionRepository` |
| **Observer** | `RiskDinleyici` — risk olayları UI'ya event olarak iletilir |
| **Template Method** | `Account` abstract — `paraCek/paraYatir` şablon, `getHesapTuru` alt sınıfta |
| **Strategy** | `IRiskCalculatable` — her hesap türü kendi risk puanını hesaplar |
| **Factory** | `BankController.hesapOlustur()` — switch ile doğru alt sınıf |
| **DTO** | `BankState`, `RiskOlayi`, `ActivityLog` — katmanlar arası veri taşıma |
| **Event Sourcing** | `RiskOlayKaydi` listesi — skor event'lerden türetilir, decay uygulanır |

---

## TEMEL KURALLAR

- UI katmanı doğrudan model'e erişemez — her şey `BankController` üzerinden geçer
- Risk skoru `RiskOlayKaydi` event log'undan hesaplanır; doğrudan saklanmaz
- Bot simülasyonu sırasında `botModuAktif = true` → `otomatikKaydet()` çalışmaz
- Demo müşteriler `durumYukle()` sırasında otomatik temizlenir
- `BankState.serialVersionUID` yapısal değişikliklerde artırılmalıdır (şu an: 7L)
- Türkçe alan terimleri sınıf/metot/değişken isimlerinde kullanılır; Java keyword'leri İngilizce
