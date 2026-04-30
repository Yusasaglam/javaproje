\# PROJECT STRUCTURE



src/

&#x20;├── model/

&#x20;│    ├── Customer.java          (Musteri)

&#x20;│    ├── Account.java           (Hesap - abstract)

&#x20;│    ├── CheckingAccount.java   (VadesizHesap)

&#x20;│    ├── SavingsAccount.java    (VadeliHesap)

&#x20;│    ├── Transaction.java       (Islem - abstract)

&#x20;│    ├── Deposit.java           (ParaYatirma)

&#x20;│    ├── Withdraw.java          (ParaCekme)

&#x20;│    ├── Transfer.java          (Transfer)

&#x20;│    ├── BankState.java         (BankaDurumu)

&#x20;│    ├── IRiskCalculatable.java (IRiskHesaplanabilir)

&#x20;│

&#x20;├── service/

&#x20;│    ├── IBankService.java          (IBankaServisi)

&#x20;│    ├── BankController.java        (BankaKontrolcu)

&#x20;│    ├── RiskEngine.java            (RiskMotoru)

&#x20;│    ├── Repository.java            (Depo - generic interface)

&#x20;│    ├── CustomerRepository.java    (MusteriDeposu)

&#x20;│    ├── AccountRepository.java     (HesapDeposu)

&#x20;│    ├── TransactionRepository.java (IslemDeposu)

&#x20;│

&#x20;├── ui/

&#x20;│    ├── MainFrame.java    (AnaPencere)

&#x20;│    ├── AccountPanel.java (HesapPaneli)

&#x20;│    ├── TransferPanel.java (TransferPaneli)

&#x20;│

&#x20;├── persistence/

&#x20;│    ├── FileLogger.java  (DosyaKaydedici)

&#x20;│    ├── FileReader.java  (DosyaOkuyucu)

&#x20;│    ├── FileWriter.java  (DosyaYazici)

&#x20;│    ├── FileParser.java  (DosyaCozumleyici)

&#x20;│    ├── Serializer.java  (Serileştirici)

&#x20;│

&#x20;├── Main.java



\## NAMING NOTE

Turkish equivalents shown in parentheses.

File names remain in English until rename is explicitly requested.

New files must use Turkish class/method/variable names.

