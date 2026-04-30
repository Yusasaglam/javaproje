package ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.FileChooser;
import model.*;
import service.BankController;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MusteriPaneli extends BorderPane {

    private final Kullanici      kullanici;
    private final BankController kontrolcu;

    // Hesaplarım
    private TableView<ObservableList<String>> hesapTablo;

    // Para Yatır
    private ComboBox<String> pyHesapCombo;
    private TextField        pyMiktarField;
    private Label            pyDurum;

    // Para Çek
    private ComboBox<String> pcHesapCombo;
    private TextField        pcMiktarField;
    private Label            pcDurum, pcLimitLabel;

    // Transfer
    private ComboBox<String> trKaynakCombo;
    private TextField        trHedefField, trMiktarField;
    private Label            trDurum, trLimitLabel;

    // İşlem Geçmişi
    private ComboBox<String> gecmisCombo;
    private DatePicker       baslangicTarih, bitisTarih;
    private TableView<ObservableList<String>> islemTablo;

    // Profilim
    private TextField profilAdField, profilEpostaField;

    @SuppressWarnings("this-escape")
    public MusteriPaneli(Kullanici kullanici, BankController kontrolcu) {
        this.kullanici  = kullanici;
        this.kontrolcu  = kontrolcu;
        setStyle("-fx-background-color: #f0f3f9;");
        setPadding(new Insets(8));
        bilesimleriBaslat();
    }

    private void bilesimleriBaslat() {
        TabPane sekmeler = new TabPane();
        sekmeler.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab t1 = new Tab("  Hesaplarım  ",    hesaplarimSekme());
        Tab t2 = new Tab("  Para Yatır  ",    paraYatirSekme());
        Tab t3 = new Tab("  Para Çek  ",      paraCekSekme());
        Tab t4 = new Tab("  Transfer  ",      transferSekme());
        Tab t5 = new Tab("  İşlem Geçmişi  ", islemGecmisiSekme());
        Tab t6 = new Tab("  Profilim  ",      profilimSekme());

        sekmeler.getTabs().addAll(t1, t2, t3, t4, t5, t6);

        sekmeler.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n == t1) hesaplarimYenile();
            else if (n == t2 || n == t3 || n == t4) combolarYenile();
            else if (n == t5) gecmisComboYenile();
            else if (n == t6) profilYukle();
        });

        setCenter(sekmeler);
    }

    // ── Hesaplarım ────────────────────────────────────────────────────────────
    private ScrollPane hesaplarimSekme() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(14));
        panel.setStyle("-fx-background-color: #f0f3f9;");

        Customer musteri = kontrolcu.getMusteri(kullanici.getMusteriId());
        String ad = musteri != null ? musteri.getAd() : kullanici.getKullaniciAdi();

        VBox hosKart = UITema.kart("Hesap Özeti");
        Label hosLabel = new Label("Hoş geldiniz,  " + ad);
        hosLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        hosLabel.setStyle("-fx-text-fill: #163264;");
        hosKart.getChildren().add(hosLabel);

        hesapTablo = UITema.tablo("Hesap No", "Tür", "Bakiye", "Durum");
        hesapTablo.setPrefHeight(280);

        // Durum sütununu renklendir
        @SuppressWarnings("unchecked")
        TableColumn<ObservableList<String>, String> durumSutun =
            (TableColumn<ObservableList<String>, String>) hesapTablo.getColumns().get(3);
        durumSutun.setCellFactory(col -> new TableCell<ObservableList<String>, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setFont(Font.font("System", FontWeight.BOLD, 12));
                setStyle("ŞÜPHELİ".equals(item)
                    ? "-fx-text-fill: #af1414;"
                    : "-fx-text-fill: #146418;");
            }
        });

        Button yenileBtn = UITema.normalButon("Yenile");
        yenileBtn.setOnAction(e -> hesaplarimYenile());
        HBox btnRow = new HBox(yenileBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(8, 0, 0, 0));

        VBox tabloKart = UITema.kart("Hesaplarım");
        VBox.setVgrow(hesapTablo, Priority.ALWAYS);
        tabloKart.getChildren().addAll(hesapTablo, btnRow);
        VBox.setVgrow(tabloKart, Priority.ALWAYS);

        panel.getChildren().addAll(hosKart, tabloKart);
        hesaplarimYenile();

        ScrollPane sp = new ScrollPane(panel);
        sp.setFitToWidth(true); sp.setStyle("-fx-background-color: transparent;");
        return sp;
    }

    // ── Para Yatır ────────────────────────────────────────────────────────────
    private StackPane paraYatirSekme() {
        pyHesapCombo  = new ComboBox<>(); pyHesapCombo.setMaxWidth(Double.MAX_VALUE);
        pyMiktarField = UITema.alan();
        pyDurum       = UITema.durumLabel();

        VBox kart = UITema.kart("Para Yatırma");
        kart.setMaxWidth(360);
        Button btn = UITema.anaButon("Para Yatır");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> paraYatir());
        kart.getChildren().addAll(
            UITema.etiket("Hesabınız:"), pyHesapCombo,
            UITema.etiket("Miktar (₺):"), pyMiktarField,
            UITema.bilgiLabel("Maksimum tek işlem: 100,000.00 ₺"),
            btn, pyDurum
        );
        VBox.setMargin(btn, new Insets(14, 0, 0, 0));

        StackPane sp = new StackPane(kart);
        sp.setStyle("-fx-background-color: #f0f3f9;");
        sp.setPadding(new Insets(40));
        combolarYenile();
        return sp;
    }

    // ── Para Çek ──────────────────────────────────────────────────────────────
    private StackPane paraCekSekme() {
        pcHesapCombo  = new ComboBox<>(); pcHesapCombo.setMaxWidth(Double.MAX_VALUE);
        pcMiktarField = UITema.alan();
        pcDurum       = UITema.durumLabel();
        pcLimitLabel  = UITema.bilgiLabel("–");

        VBox kart = UITema.kart("Para Çekme");
        kart.setMaxWidth(360);
        Button btn = UITema.anaButon("Para Çek");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> paraCek());
        pcHesapCombo.setOnAction(e -> limitGuncelle(pcHesapCombo, pcLimitLabel, true));
        kart.getChildren().addAll(
            UITema.etiket("Hesabınız:"), pcHesapCombo,
            UITema.etiket("Miktar (₺):"), pcMiktarField,
            pcLimitLabel, btn, pcDurum
        );
        VBox.setMargin(btn, new Insets(14, 0, 0, 0));

        StackPane sp = new StackPane(kart);
        sp.setStyle("-fx-background-color: #f0f3f9;");
        sp.setPadding(new Insets(40));
        combolarYenile();
        return sp;
    }

    // ── Transfer ──────────────────────────────────────────────────────────────
    private StackPane transferSekme() {
        trKaynakCombo = new ComboBox<>(); trKaynakCombo.setMaxWidth(Double.MAX_VALUE);
        trHedefField  = UITema.alan();
        trMiktarField = UITema.alan();
        trDurum       = UITema.durumLabel();
        trLimitLabel  = UITema.bilgiLabel("–");

        VBox kart = UITema.kart("Para Transferi");
        kart.setMaxWidth(360);
        Button btn = UITema.anaButon("Transfer Yap");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> transferYap());
        trKaynakCombo.setOnAction(e -> limitGuncelle(trKaynakCombo, trLimitLabel, false));
        kart.getChildren().addAll(
            UITema.etiket("Kaynak Hesabınız:"), trKaynakCombo,
            UITema.etiket("Hedef Hesap No:"), trHedefField,
            UITema.etiket("Miktar (₺):"), trMiktarField,
            trLimitLabel, btn, trDurum
        );
        VBox.setMargin(btn, new Insets(14, 0, 0, 0));

        StackPane sp = new StackPane(kart);
        sp.setStyle("-fx-background-color: #f0f3f9;");
        sp.setPadding(new Insets(40));
        combolarYenile();
        return sp;
    }

    // ── İşlem Geçmişi ─────────────────────────────────────────────────────────
    private ScrollPane islemGecmisiSekme() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(14));
        panel.setStyle("-fx-background-color: #f0f3f9;");

        VBox filtrKart = UITema.kart("Hesap ve Tarih Filtresi");
        gecmisCombo = new ComboBox<>(); gecmisCombo.setMaxWidth(Double.MAX_VALUE);
        baslangicTarih = new DatePicker(); baslangicTarih.setMaxWidth(Double.MAX_VALUE);
        bitisTarih     = new DatePicker(); bitisTarih.setMaxWidth(Double.MAX_VALUE);

        GridPane grid = new GridPane(); grid.setHgap(12); grid.setVgap(8);
        grid.add(UITema.etiket("Hesap:"), 0, 0);              grid.add(gecmisCombo, 1, 0);
        grid.add(UITema.etiket("Başlangıç Tarihi:"), 0, 1);   grid.add(baslangicTarih, 1, 1);
        grid.add(UITema.etiket("Bitiş Tarihi:"), 0, 2);       grid.add(bitisTarih, 1, 2);
        ColumnConstraints cc0 = new ColumnConstraints(); cc0.setMinWidth(140);
        ColumnConstraints cc1 = new ColumnConstraints(); cc1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(cc0, cc1);

        Button getirBtn  = UITema.anaButon("Geçmişi Getir");
        Button csvBtn    = UITema.normalButon("CSV Olarak Kaydet");
        Button temizleBtn = UITema.normalButon("Tarihi Temizle");
        getirBtn.setOnAction(e -> islemGecmisiniYukle());
        csvBtn.setOnAction(e -> csvOlarakKaydet());
        temizleBtn.setOnAction(e -> { baslangicTarih.setValue(null); bitisTarih.setValue(null); });
        HBox btnRow = new HBox(8, temizleBtn, csvBtn, getirBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        filtrKart.getChildren().addAll(grid, btnRow);

        islemTablo = UITema.tablo("Tarih / Saat", "İşlem Türü", "Miktar");
        islemTablo.setPrefHeight(320);

        // İşlem türü renklendir
        @SuppressWarnings("unchecked")
        TableColumn<ObservableList<String>, String> turSutun =
            (TableColumn<ObservableList<String>, String>) islemTablo.getColumns().get(1);
        turSutun.setCellFactory(col -> new TableCell<ObservableList<String>, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setFont(Font.font("System", FontWeight.BOLD, 12));
                if (item.contains("Yatırma"))      setStyle("-fx-text-fill: #146418;");
                else if (item.contains("Çekme"))   setStyle("-fx-text-fill: #af1414;");
                else if (item.contains("Transfer"))setStyle("-fx-text-fill: #235299;");
                else setStyle("");
            }
        });

        VBox tabloKart = UITema.kart("İşlem Geçmişi");
        VBox.setVgrow(islemTablo, Priority.ALWAYS);
        tabloKart.getChildren().add(islemTablo);

        panel.getChildren().addAll(filtrKart, tabloKart);
        gecmisComboYenile();

        ScrollPane sp = new ScrollPane(panel);
        sp.setFitToWidth(true); sp.setStyle("-fx-background-color: transparent;");
        return sp;
    }

    // ── Profilim ──────────────────────────────────────────────────────────────
    private StackPane profilimSekme() {
        profilAdField     = UITema.alan();
        profilEpostaField = UITema.alan();

        VBox kart = UITema.kart("Müşteri Bilgilerimi Güncelle");
        kart.setMaxWidth(400);

        Button kaydetBtn = UITema.anaButon("Bilgileri Güncelle");
        kaydetBtn.setMaxWidth(Double.MAX_VALUE);
        kaydetBtn.setOnAction(e -> profilGuncelle());
        VBox.setMargin(kaydetBtn, new Insets(16, 0, 0, 0));

        kart.getChildren().addAll(
            UITema.etiket("Ad Soyad:"), profilAdField,
            UITema.etiket("E-posta:"),  profilEpostaField,
            kaydetBtn
        );

        StackPane sp = new StackPane(kart);
        sp.setStyle("-fx-background-color: #f0f3f9;");
        sp.setPadding(new Insets(40));
        profilYukle();
        return sp;
    }

    // ── İşlem mantığı ─────────────────────────────────────────────────────────
    private void paraYatir() {
        String id = seciliHesapId(pyHesapCombo);
        if (id == null) { UITema.durumGoster(pyDurum, "Hesap seçilmedi.", false); return; }
        if (!kontrolcu.hesapMusteriyeAitMi(id, kullanici.getMusteriId())) {
            UITema.durumGoster(pyDurum, "Bu hesap size ait değil.", false); return;
        }
        try {
            double m = Double.parseDouble(pyMiktarField.getText().trim());
            if (kontrolcu.paraYatir(id, m)) {
                UITema.durumGoster(pyDurum, "Para yatırma başarılı: " + tl(m), true);
                pyMiktarField.clear(); combolarYenile(); hesaplarimYenile();
            } else UITema.durumGoster(pyDurum, "İşlem başarısız. 100,000 ₺ sınırı aşılıyor olabilir.", false);
        } catch (NumberFormatException e) { UITema.durumGoster(pyDurum, "Geçersiz miktar.", false); }
    }

    private void paraCek() {
        String id = seciliHesapId(pcHesapCombo);
        if (id == null) { UITema.durumGoster(pcDurum, "Hesap seçilmedi.", false); return; }
        if (!kontrolcu.hesapMusteriyeAitMi(id, kullanici.getMusteriId())) {
            UITema.durumGoster(pcDurum, "Bu hesap size ait değil.", false); return;
        }
        try {
            double m = Double.parseDouble(pcMiktarField.getText().trim());
            if (kontrolcu.paraCek(id, m)) {
                UITema.durumGoster(pcDurum, "Para çekme başarılı: " + tl(m), true);
                pcMiktarField.clear(); combolarYenile(); hesaplarimYenile();
            } else UITema.durumGoster(pcDurum, "Başarısız. Yetersiz bakiye, limit aşımı veya şüpheli hesap.", false);
        } catch (NumberFormatException e) { UITema.durumGoster(pcDurum, "Geçersiz miktar.", false); }
    }

    private void transferYap() {
        String kId = seciliHesapId(trKaynakCombo), hId = trHedefField.getText().trim();
        if (kId == null) { UITema.durumGoster(trDurum, "Kaynak hesap seçilmedi.", false); return; }
        if (!kontrolcu.hesapMusteriyeAitMi(kId, kullanici.getMusteriId())) {
            UITema.durumGoster(trDurum, "Kaynak hesap size ait değil.", false); return;
        }
        if (hId.isEmpty()) { UITema.durumGoster(trDurum, "Hedef hesap no giriniz.", false); return; }
        if (kId.equals(hId)) { UITema.durumGoster(trDurum, "Kaynak ve hedef aynı olamaz.", false); return; }
        try {
            double m = Double.parseDouble(trMiktarField.getText().trim());
            if (kontrolcu.transferYap(kId, hId, m)) {
                UITema.durumGoster(trDurum, "Transfer başarılı: " + tl(m), true);
                trHedefField.clear(); trMiktarField.clear(); combolarYenile(); hesaplarimYenile();
            } else UITema.durumGoster(trDurum, "Transfer başarısız. Limit/bakiye/hedef bulunamadı.", false);
        } catch (NumberFormatException e) { UITema.durumGoster(trDurum, "Geçersiz miktar.", false); }
    }

    private void islemGecmisiniYukle() {
        if (islemTablo == null) return;
        islemTablo.getItems().clear();
        String id = seciliHesapId(gecmisCombo);
        if (id == null) return;
        if (!kontrolcu.hesapMusteriyeAitMi(id, kullanici.getMusteriId())) {
            UITema.hata("Hata", "Bu hesap size ait değil."); return;
        }
        Account hesap = kontrolcu.getHesap(id);
        if (hesap == null) return;
        LocalDate bas = baslangicTarih.getValue();
        LocalDate bit = bitisTarih.getValue();

        List<Transaction> islemler = hesap.getIslemler();
        if (islemler.isEmpty()) { UITema.bilgi("Bilgi", "Bu hesaba ait işlem bulunamadı."); return; }
        for (Transaction i : islemler) {
            LocalDate tarih = i.getZaman().toLocalDate();
            if (bas != null && tarih.isBefore(bas)) continue;
            if (bit != null && tarih.isAfter(bit))  continue;
            String zaman = i.getZaman().toString().replace("T", " ");
            if (zaman.length() > 19) zaman = zaman.substring(0, 19);
            UITema.satirEkle(islemTablo, zaman, islemTurTr(i.getTur()), tl(i.getMiktar()));
        }
    }

    private void csvOlarakKaydet() {
        if (islemTablo == null || islemTablo.getItems().isEmpty()) {
            UITema.uyari("Uyarı", "Önce işlem geçmişini getirin."); return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("CSV Dosyası Kaydet");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Dosyası", "*.csv"));
        fc.setInitialFileName("islem_gecmisi.csv");
        File dosya = fc.showSaveDialog(getScene().getWindow());
        if (dosya == null) return;
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(dosya), "UTF-8"))) {
            pw.println("Tarih/Saat,İşlem Türü,Miktar");
            for (ObservableList<String> satir : islemTablo.getItems()) {
                pw.println("\"" + satir.get(0) + "\",\"" + satir.get(1) + "\",\"" + satir.get(2) + "\"");
            }
            UITema.bilgi("Başarılı", "CSV dosyası kaydedildi:\n" + dosya.getAbsolutePath());
        } catch (IOException ex) {
            UITema.hata("Hata", "Dosya kaydedilemedi: " + ex.getMessage());
        }
    }

    private void profilYukle() {
        if (profilAdField == null) return;
        String mId = kullanici.getMusteriId();
        if (mId == null) { profilAdField.setPromptText("Müşteri bağlantısı yok"); return; }
        Customer m = kontrolcu.getMusteri(mId);
        if (m == null) return;
        profilAdField.setText(m.getAd());
        profilEpostaField.setText(m.getEposta());
    }

    private void profilGuncelle() {
        String mId = kullanici.getMusteriId();
        if (mId == null) { UITema.uyari("Uyarı", "Bu hesaba bağlı müşteri kaydı yok."); return; }
        Customer m = kontrolcu.getMusteri(mId);
        if (m == null) return;
        String yeniAd = profilAdField.getText().trim();
        String yeniEp = profilEpostaField.getText().trim();
        if (yeniAd.isEmpty() || yeniEp.isEmpty()) { UITema.uyari("Uyarı", "Ad ve e-posta boş olamaz."); return; }
        m.setAd(yeniAd);
        m.setEposta(yeniEp);
        kontrolcu.durumKaydet("banka_durumu.dat");
        UITema.bilgi("Başarılı", "Bilgileriniz güncellendi.");
    }

    // ── Yenileme ──────────────────────────────────────────────────────────────
    private void hesaplarimYenile() {
        if (hesapTablo == null) return;
        hesapTablo.getItems().clear();
        String mId = kullanici.getMusteriId();
        if (mId == null) return;
        for (Account h : kontrolcu.musteriHesaplari(mId)) {
            String durum = kontrolcu.suphelihMi(h.getHesapId()) ? "ŞÜPHELİ" : "Normal";
            UITema.satirEkle(hesapTablo, h.getHesapId(), h.getHesapTuru(), tl(h.getBakiye()), durum);
        }
    }

    private void combolarYenile() {
        String mId = kullanici.getMusteriId();
        if (mId == null) return;
        List<Account> hesaplar = kontrolcu.musteriHesaplari(mId);
        for (ComboBox<String> combo : Arrays.asList(pyHesapCombo, pcHesapCombo, trKaynakCombo)) {
            if (combo == null) continue;
            String secili = combo.getValue();
            combo.getItems().clear();
            for (Account h : hesaplar)
                combo.getItems().add(h.getHesapId() + " – " + h.getHesapTuru() + " – " + tl(h.getBakiye()));
            if (secili != null) combo.setValue(secili);
        }
        if (pcHesapCombo != null && pcLimitLabel != null) limitGuncelle(pcHesapCombo, pcLimitLabel, true);
        if (trKaynakCombo != null && trLimitLabel != null) limitGuncelle(trKaynakCombo, trLimitLabel, false);
    }

    private void gecmisComboYenile() {
        if (gecmisCombo == null) return;
        String mId = kullanici.getMusteriId();
        if (mId == null) return;
        String secili = gecmisCombo.getValue();
        gecmisCombo.getItems().clear();
        for (Account h : kontrolcu.musteriHesaplari(mId))
            gecmisCombo.getItems().add(h.getHesapId() + " – " + h.getHesapTuru());
        if (secili != null) gecmisCombo.setValue(secili);
    }

    private void limitGuncelle(ComboBox<String> combo, Label label, boolean cekim) {
        String id = seciliHesapId(combo);
        if (id == null) { label.setText("–"); return; }
        double kalan = cekim ? kontrolcu.kalanCekimLimiti(id)    : kontrolcu.kalanTransferLimiti(id);
        double max   = cekim ? kontrolcu.getGunlukCekimLimiti(id): kontrolcu.getGunlukTransferLimiti(id);
        String tip   = cekim ? "Günlük çekim" : "Günlük transfer";
        label.setText(tip + " kalan: " + tl(kalan) + " / " + tl(max));
        label.setStyle(kalan < max * 0.2
            ? "-fx-text-fill: #af4b00; -fx-font-size: 11;"
            : "-fx-text-fill: #163264; -fx-font-size: 11;");
    }

    // ── Yardımcılar ───────────────────────────────────────────────────────────
    private static String seciliHesapId(ComboBox<String> combo) {
        String s = combo.getValue();
        return (s == null || s.isEmpty()) ? null : s.split(" – ")[0].trim();
    }

    private static String islemTurTr(String tur) {
        switch (tur) {
            case "PARA_YATIRMA": return "Para Yatırma";
            case "PARA_CEKME":   return "Para Çekme";
            case "TRANSFER":     return "Transfer";
            default:             return tur;
        }
    }

    private static String tl(double m) {
        return String.format(Locale.US, "%,.2f ₺", m);
    }
}
