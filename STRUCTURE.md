# PROJECT STRUCTURE

```
SRC/
├── Main.java                          ← Uygulama giriş noktası (JavaFX Application)
│
├── model/
│   ├── Account.java                   ← Tüm hesap türlerinin soyut ana sınıfı
│   ├── CheckingAccount.java           ← Vadesiz hesap
│   ├── SavingsAccount.java            ← Vadeli hesap (%3 faiz)
│   ├── DovizHesabi.java               ← Döviz hesabı (USD/EUR/GBP)
│   ├── KrediHesabi.java               ← Kredi hesabı
│   ├── Customer.java                  ← Müşteri (ad, eposta, hesap listesi)
│   ├── Kullanici.java                 ← Giriş kullanıcısı (rol, şifre hash)
│   ├── Transaction.java               ← Tüm işlem türlerinin soyut ana sınıfı
│   ├── Deposit.java                   ← Para yatırma işlemi
│   ├── Withdraw.java                  ← Para çekme işlemi
│   ├── Transfer.java                  ← Transfer işlemi
│   ├── HesapLimiti.java               ← Günlük/tek işlem limitleri
│   ├── SupheSebebi.java               ← Şüpheli işaret kaydı
│   ├── BankState.java                 ← Serileştirilen sistem durumu (serialVersionUID 7L)
│   ├── IRiskCalculatable.java         ← Risk arayüzü
│   ├── YetersizBakiyeException.java   ← Bakiye yetersizliği istisnası
│   └── RiskLimitiAsildiException.java ← Limit aşımı istisnası
│
├── service/
│   ├── IBankService.java              ← BankController'ın servis arayüzü
│   ├── BankController.java            ← Ana kontrolcü — tüm işlemler buradan geçer
│   ├── RiskEngine.java                ← Risk motoru (7 kural, event-tabanlı skorlama)
│   ├── KimlikDogrulama.java           ← Giriş, SHA-256 hash, hesap kilitleme
│   ├── HashUtil.java                  ← SHA-256 yardımcısı
│   ├── Repository.java                ← Generic CRUD arayüzü
│   ├── AccountRepository.java         ← Hesap deposu (HashMap tabanlı)
│   ├── CustomerRepository.java        ← Müşteri deposu
│   ├── TransactionRepository.java     ← İşlem deposu
│   ├── RiskDinleyici.java             ← Observer arayüzü (risk olayları)
│   ├── RiskOlayi.java                 ← Risk olayı verisi (hesapId, tür, miktar)
│   ├── RiskOlayTuru.java              ← Risk olay türleri enum
│   ├── RiskOlayKaydi.java             ← Event log kaydı (puan, decay, islemId)
│   ├── IslemRiskAgirlik.java          ← İşlem türü çarpan ve decay değerleri enum
│   ├── MusteriRiskProfili.java        ← Müşteri seviye risk skoru (bulaşma modeli)
│   ├── DondurmaKaydi.java             ← Dondurma kaydı (hesapId, zaman, sebep)
│   ├── DondurmaSecegi.java            ← Dondurma türü enum (OTOMATIK / MANUEL)
│   ├── KullaniciKategorisi.java       ← Müşteri kategorisi enum (BIREYSEL / KURUMSAL)
│   ├── ActivityLog.java               ← Aktivite log satırı (islem, risk delta)
│   ├── AktiviteLogServisi.java        ← Aktivite log servisi (kaydet, filtrele)
│   └── BekleyenLimitDegisimi.java     ← 24 saatlik limit değişim talebi
│
├── persistence/
│   ├── FileLogger.java                ← banka_kayit.txt'e zaman damgalı log yazar
│   └── Serializer.java                ← Java serileştirme (.dat dosyası)
│
└── ui/
    ├── GirisEkrani.java               ← Login ekranı (BankController burada oluşturulur)
    ├── MainFrame.java                 ← Ana pencere çerçevesi (rol bazlı panel seçimi)
    ├── YoneticiPaneli.java            ← Yönetici paneli (6 sekme)
    ├── MusteriPaneli.java             ← Müşteri paneli (8 sekme)
    ├── UITema.java                    ← UI yardımcı sınıfı (butonlar, kartlar, alertler)
    └── banka.css                      ← JavaFX stil dosyası
```

## Proje Kök Dizini

```
javaproje/
├── SRC/                 ← Kaynak kodlar
├── out/                 ← Derlenmiş .class dosyaları
├── lib/                 ← Kütüphane araçları
├── .vscode/
│   └── settings.json    ← JavaFX classpath, JDK yolu
├── banka_durumu.dat     ← (otomatik) Serileştirilmiş sistem durumu
└── banka_kayit.txt      ← (otomatik) İşlem log dosyası
```

## NAMING NOTE

Turkish equivalents shown in comments.
File names match class names exactly (PascalCase).
New files use Turkish class/method/variable names.
