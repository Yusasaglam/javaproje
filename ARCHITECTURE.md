\# PROJECT ARCHITECTURE



\## LAYERS



\### Model Layer

Contains:

\- Customer

\- abstract Account

\- abstract Transaction



Subclasses:

\- CheckingAccount

\- SavingsAccount

\- Deposit

\- Withdraw

\- Transfer



Collections:

\- List<Transaction>

\- Map<String, Customer>

\- Set<String>



\---



\### Service Layer

Contains:

\- BankController

\- RiskEngine



Responsibilities:

\- Business logic

\- Validation

\- Risk calculation

\- Exception handling



\---



\### UI Layer

Contains:

\- MainFrame

\- AccountPanel

\- TransferPanel



Responsibilities:

\- User interaction

\- Event handling (ActionListener)



\---



\### Persistence

Contains:

\- FileLogger



Responsibilities:

\- Write to file

\- Read from file

\- Later: Serialization



\---



\## DESIGN PRINCIPLES

\- Layered architecture must be preserved

\- MVC pattern must be respected

\- No direct UI → Model interaction

\- All logic goes through Service layer



\## NAMING CONVENTION

\- Turkish domain terms used for class, method, and variable names

\- Model classes: Musteri, Hesap, Islem, BankaState

\- Service classes: BankaKontrolcu, RiskMotoru

\- UI classes: AnaPencere, HesapPaneli, TransferPaneli

\- Persistence classes: DosyaKaydedici, DosyaOkuyucu, DosyaYazici, DosyaCozumleyici

\- Methods follow Turkish verb naming: paraYatir, paraCek, transferYap, musteriOlustur, hesapOlustur

\- Variables follow Turkish noun naming: musteriId, hesapId, bakiye, hesapTuru, islemSayaci

\- Java syntax, keywords, and library names remain in English

