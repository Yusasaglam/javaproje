package ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.FileChooser;
import model.*;
import service.BankController;
import service.RiskDinleyici;
import service.RiskOlayi;

import java.io.*;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MusteriPaneli extends BorderPane {

    private final Kullanici      kullanici;
    private final BankController kontrolcu;

    // Hesaplarım
    private TableView<ObservableList<String>> hesapTablo;
    private Label toplamTLLabel, toplamDovizLabel, toplamKrediLabel, netVarlikLabel;

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
    private ComboBox<String> islemTuruCombo;
    private TextField        aramaField;
    private DatePicker       baslangicTarih, bitisTarih;
    private TableView<ObservableList<String>> islemTablo;

    // Profilim
    private TextField     profilAdField, profilEpostaField;
    private PasswordField eskiSifreField, yeniSifreField, yeniSifreTekrarField;
    private Label         sifreDurumLabel;

    // Geri alma
    private Button pcGeriAlBtn, trGeriAlBtn;
    private javafx.animation.Timeline geriAlTimeline;

    // Kredi Yönetimi
    private ComboBox<String> krediHesapCombo;
    private TextField        krediKullanimMiktarField;
    private Label            krediKullanimDurum, krediBilgiLabel;
    private ComboBox<String> krediOdemeKaynakCombo, krediOdemeHedefCombo;
    private TextField        krediOdemeMiktarField;
    private Label            krediOdemeDurum;

    @SuppressWarnings("this-escape")
    public MusteriPaneli(Kullanici kullanici, BankController kontrolcu) {
        this.kullanici  = kullanici;
        this.kontrolcu  = kontrolcu;
        setStyle("-fx-background-color: #f0f3f9;");
        setPadding(new Insets(8));
        bilesimleriBaslat();
        riskDinleyiciKaydet();
    }

    private void riskDinleyiciKaydet() {
        String musteriId = kullanici.getMusteriId();
        if (musteriId == null) return;
        RiskDinleyici dinleyici = (RiskOlayi olay) -> {
            if (!musteriId.equals(olay.getMusteriId())) return;
            Platform.runLater(() -> {
                UITema.toast(getScene(), "Risk Uyarısı: " + olay.getMesaj(), false);
                hesaplarimYenile();
            });
        };
        kontrolcu.dinleyiciEkle(dinleyici);
    }

    private void bilesimleriBaslat() {
        TabPane sekmeler = new TabPane();
        sekmeler.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab t1 = new Tab("  Hesaplarım  ",    hesaplarimSekme());
        Tab t2 = new Tab("  Para Yatır  ",    paraYatirSekme());
        Tab t3 = new Tab("  Para Çek  ",      paraCekSekme());
        Tab t4 = new Tab("  Transfer  ",      transferSekme());
        Tab t5 = new Tab("  İşlem Geçmişi  ", islemGecmisiSekme());
        Tab t6 = new Tab("  Kredi Yönetimi  ", krediSekme());
        Tab t7 = new Tab("  Profilim  ",      profilimSekme());

        sekmeler.getTabs().addAll(t1, t2, t3, t4, t5, t6, t7);
        sekmeler.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n == t1) hesaplarimYenile();
            else if (n == t2 || n == t3 || n == t4) combolarYenile();
            else if (n == t5) gecmisComboYenile();
            else if (n == t6) krediCombolariYenile();
            else if (n == t7) profilYukle();
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

        VBox hosKart = UITema.kart("Varlık Özeti");
        Label hosLabel = new Label("Hoş geldiniz,  " + ad);
        hosLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        hosLabel.setStyle("-fx-text-fill: #163264;");

        HBox varlikRow = new HBox(10);
        varlikRow.setFillHeight(true);
        toplamTLLabel    = varlikKarti(varlikRow, "TL Varlık",    "0,00 ₺", "#163264");
        toplamDovizLabel = varlikKarti(varlikRow, "Döviz (TL)",   "0,00 ₺", "#235299");
        toplamKrediLabel = varlikKarti(varlikRow, "Kredi Borcu",  "0,00 ₺", "#af1414");
        netVarlikLabel   = varlikKarti(varlikRow, "Net Varlık",   "0,00 ₺", "#146418");
        for (javafx.scene.Node n : varlikRow.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);

        hosKart.getChildren().addAll(hosLabel, varlikRow);

        hesapTablo = UITema.tablo("Hesap No", "Tür", "Bakiye", "Ek Bilgi", "Durum");
        hesapTablo.setPrefHeight(280);

        // Durum sütunu renklendirme (index 4)
        @SuppressWarnings("unchecked")
        TableColumn<ObservableList<String>, String> durumSutun =
            (TableColumn<ObservableList<String>, String>) hesapTablo.getColumns().get(4);
        durumSutun.setCellFactory(col -> new TableCell<ObservableList<String>, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setFont(Font.font("System", FontWeight.BOLD, 11));
                if      (item.startsWith("ŞÜPHELİ"))    setStyle("-fx-text-fill: #af1414;");
                else if (item.startsWith("RİSKLİ"))      setStyle("-fx-text-fill: #af4b00;");
                else if (item.startsWith("İZLENİYOR"))   setStyle("-fx-text-fill: #c8a000;");
                else                                      setStyle("-fx-text-fill: #146418;");
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
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color: transparent;");
        return sp;
    }

    private Label varlikKarti(HBox satir, String baslik, String deger, String renk) {
        VBox kart = new VBox(4);
        kart.getStyleClass().add("istat-kart");
        kart.setAlignment(Pos.CENTER);
        Label degerLabel = new Label(deger);
        degerLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        degerLabel.setStyle("-fx-text-fill: " + renk + ";");
        Label baslikLabel = new Label(baslik);
        baslikLabel.setStyle("-fx-text-fill: #808080; -fx-font-size: 11;");
        kart.getChildren().addAll(degerLabel, baslikLabel);
        satir.getChildren().add(kart);
        return degerLabel;
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
            UITema.etiket("Miktar:"), pyMiktarField,
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
        pcGeriAlBtn = UITema.tehlikeButon("↩ Geri Al (3:00)");
        pcGeriAlBtn.setMaxWidth(Double.MAX_VALUE);
        pcGeriAlBtn.setVisible(false);
        kart.getChildren().addAll(
            UITema.etiket("Hesabınız:"), pcHesapCombo,
            UITema.etiket("Miktar (₺):"), pcMiktarField,
            pcLimitLabel, btn, pcGeriAlBtn, pcDurum
        );
        VBox.setMargin(btn, new Insets(14, 0, 0, 0));
        VBox.setMargin(pcGeriAlBtn, new Insets(6, 0, 0, 0));

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
        trGeriAlBtn = UITema.tehlikeButon("↩ Geri Al (3:00)");
        trGeriAlBtn.setMaxWidth(Double.MAX_VALUE);
        trGeriAlBtn.setVisible(false);
        kart.getChildren().addAll(
            UITema.etiket("Kaynak Hesabınız:"), trKaynakCombo,
            UITema.etiket("Hedef Hesap No:"), trHedefField,
            UITema.etiket("Miktar (₺):"), trMiktarField,
            trLimitLabel, btn, trGeriAlBtn, trDurum
        );
        VBox.setMargin(btn, new Insets(14, 0, 0, 0));
        VBox.setMargin(trGeriAlBtn, new Insets(6, 0, 0, 0));

        StackPane sp = new StackPane(kart);
        sp.setStyle("-fx-background-color: #f0f3f9;");
        sp.setPadding(new Insets(40));
        combolarYenile();
        return sp;
    }

    // ── Kredi Yönetimi ────────────────────────────────────────────────────────
    private ScrollPane krediSekme() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(14));
        panel.setStyle("-fx-background-color: #f0f3f9;");

        krediHesapCombo           = new ComboBox<>(); krediHesapCombo.setMaxWidth(Double.MAX_VALUE);
        krediKullanimMiktarField  = UITema.alan();
        krediKullanimDurum        = UITema.durumLabel();
        krediBilgiLabel           = UITema.bilgiLabel("–");
        krediOdemeKaynakCombo     = new ComboBox<>(); krediOdemeKaynakCombo.setMaxWidth(Double.MAX_VALUE);
        krediOdemeHedefCombo      = new ComboBox<>(); krediOdemeHedefCombo.setMaxWidth(Double.MAX_VALUE);
        krediOdemeMiktarField     = UITema.alan();
        krediOdemeDurum           = UITema.durumLabel();

        // Kredi kullan kartı
        VBox kullanimKart = UITema.kart("Kredi Kullan (Limitten Para Çek)");
        kullanimKart.setMaxWidth(Double.MAX_VALUE);
        Button krediCekBtn = UITema.anaButon("Krediyi Kullan");
        krediCekBtn.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(krediCekBtn, new Insets(14, 0, 0, 0));
        krediCekBtn.setOnAction(e -> krediKullan());
        krediHesapCombo.setOnAction(e -> krediBilgiGuncelle());
        kullanimKart.getChildren().addAll(
            UITema.etiket("Kredi Hesabınız:"), krediHesapCombo,
            krediBilgiLabel,
            UITema.etiket("Çekmek İstediğiniz Tutar (₺):"), krediKullanimMiktarField,
            krediCekBtn, krediKullanimDurum
        );

        // Borç öde kartı
        VBox odemeKart = UITema.kart("Borç Öde (Başka Hesaptan)");
        odemeKart.setMaxWidth(Double.MAX_VALUE);
        Button odemeBtn = UITema.anaButon("Borcu Öde");
        odemeBtn.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(odemeBtn, new Insets(14, 0, 0, 0));
        odemeBtn.setOnAction(e -> krediOde());
        odemeKart.getChildren().addAll(
            UITema.etiket("Ödeme Yapılacak Kredi Hesabı:"), krediOdemeHedefCombo,
            UITema.etiket("Ödeme Kaynağı Hesap:"), krediOdemeKaynakCombo,
            UITema.etiket("Ödeme Tutarı (₺):"), krediOdemeMiktarField,
            odemeBtn, krediOdemeDurum
        );

        HBox kartiRow = new HBox(12, kullanimKart, odemeKart);
        kartiRow.setFillHeight(false);
        HBox.setHgrow(kullanimKart, Priority.ALWAYS);
        HBox.setHgrow(odemeKart, Priority.ALWAYS);

        panel.getChildren().add(kartiRow);
        krediCombolariYenile();

        ScrollPane sp = new ScrollPane(panel);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color: transparent;");
        return sp;
    }

    private void krediBilgiGuncelle() {
        String id = seciliHesapId(krediHesapCombo);
        if (id == null) { krediBilgiLabel.setText("–"); return; }
        Account h = kontrolcu.getHesap(id);
        if (!(h instanceof KrediHesabi)) { krediBilgiLabel.setText("–"); return; }
        KrediHesabi k = (KrediHesabi) h;
        double borc   = k.getBorcMiktari();
        double kalan  = k.kalanKredi();
        double limit  = k.getKrediLimiti();
        krediBilgiLabel.setText(String.format(java.util.Locale.US,
            "Limit: %,.0f ₺  |  Kullanılan: %,.0f ₺  |  Kalan: %,.0f ₺  |  Borç: %,.0f ₺",
            limit, limit - kalan, kalan, borc));
        krediBilgiLabel.setStyle(borc > 0
            ? "-fx-text-fill: #af1414; -fx-font-size: 11;"
            : "-fx-text-fill: #146418; -fx-font-size: 11;");
    }

    private void krediKullan() {
        String id = seciliHesapId(krediHesapCombo);
        if (id == null) { UITema.durumGoster(krediKullanimDurum, "Kredi hesabı seçilmedi.", false); return; }
        if (!kontrolcu.hesapMusteriyeAitMi(id, kullanici.getMusteriId())) {
            UITema.durumGoster(krediKullanimDurum, "Bu hesap size ait değil.", false); return;
        }
        Account h = kontrolcu.getHesap(id);
        if (!(h instanceof KrediHesabi)) { UITema.durumGoster(krediKullanimDurum, "Seçili hesap kredi hesabı değil.", false); return; }
        try {
            double m = Double.parseDouble(krediKullanimMiktarField.getText().trim());
            if (kontrolcu.paraCek(id, m)) {
                UITema.durumGoster(krediKullanimDurum, "Kredi kullanımı başarılı: " + tl(m), true);
                krediKullanimMiktarField.clear();
                krediCombolariYenile(); krediBilgiGuncelle(); hesaplarimYenile();
            } else UITema.durumGoster(krediKullanimDurum, "İşlem başarısız (hesap şüpheli olabilir).", false);
        } catch (RiskLimitiAsildiException e) {
            UITema.durumGoster(krediKullanimDurum, "Risk limiti: " + e.getMessage(), false);
        } catch (YetersizBakiyeException e) {
            UITema.durumGoster(krediKullanimDurum, "Kredi limiti aşılıyor! Kalan: " + tl(e.getMevcutBakiye()), false);
        } catch (NumberFormatException e) { UITema.durumGoster(krediKullanimDurum, "Geçersiz miktar.", false); }
    }

    private void krediOde() {
        String krediId  = seciliHesapId(krediOdemeHedefCombo);
        String kaynakId = seciliHesapId(krediOdemeKaynakCombo);
        if (krediId == null)  { UITema.durumGoster(krediOdemeDurum, "Kredi hesabı seçilmedi.", false); return; }
        if (kaynakId == null) { UITema.durumGoster(krediOdemeDurum, "Kaynak hesap seçilmedi.", false); return; }
        if (krediId.equals(kaynakId)) { UITema.durumGoster(krediOdemeDurum, "Kaynak ve kredi hesabı aynı olamaz.", false); return; }
        try {
            double m = Double.parseDouble(krediOdemeMiktarField.getText().trim());
            double gercekOdeme = kontrolcu.krediOde(kaynakId, krediId, m);
            if (gercekOdeme < 0) {
                UITema.durumGoster(krediOdemeDurum, "Geçersiz hesap seçimi.", false);
            } else if (gercekOdeme == 0) {
                UITema.durumGoster(krediOdemeDurum, "Bu kredi hesabında aktif borç bulunmuyor.", false);
            } else {
                UITema.durumGoster(krediOdemeDurum, "Ödeme başarılı: " + tl(gercekOdeme), true);
                krediOdemeMiktarField.clear();
                krediCombolariYenile(); hesaplarimYenile();
            }
        } catch (YetersizBakiyeException e) {
            UITema.durumGoster(krediOdemeDurum, "Yetersiz bakiye! Mevcut: " + tl(e.getMevcutBakiye()), false);
        } catch (NumberFormatException e) { UITema.durumGoster(krediOdemeDurum, "Geçersiz miktar.", false); }
    }

    private void krediCombolariYenile() {
        String mId = kullanici.getMusteriId();
        if (mId == null) return;
        List<Account> hesaplar = kontrolcu.musteriHesaplari(mId);

        // Kredi kullan combo — sadece kredi hesapları
        String secKredi = krediHesapCombo != null ? krediHesapCombo.getValue() : null;
        if (krediHesapCombo != null) {
            krediHesapCombo.getItems().clear();
            for (Account h : hesaplar)
                if (h instanceof KrediHesabi)
                    krediHesapCombo.getItems().add(h.getHesapId() + " – KREDİ – " + bakiyeStr(h));
            if (secKredi != null) krediHesapCombo.setValue(secKredi);
        }

        // Ödeme hedef combo — sadece kredi hesapları
        String secHedef = krediOdemeHedefCombo != null ? krediOdemeHedefCombo.getValue() : null;
        if (krediOdemeHedefCombo != null) {
            krediOdemeHedefCombo.getItems().clear();
            for (Account h : hesaplar)
                if (h instanceof KrediHesabi)
                    krediOdemeHedefCombo.getItems().add(h.getHesapId() + " – KREDİ – " + bakiyeStr(h));
            if (secHedef != null) krediOdemeHedefCombo.setValue(secHedef);
        }

        // Ödeme kaynak combo — kredi dışı tüm hesaplar
        String secKaynak = krediOdemeKaynakCombo != null ? krediOdemeKaynakCombo.getValue() : null;
        if (krediOdemeKaynakCombo != null) {
            krediOdemeKaynakCombo.getItems().clear();
            for (Account h : hesaplar)
                if (!(h instanceof KrediHesabi))
                    krediOdemeKaynakCombo.getItems().add(h.getHesapId() + " – " + h.getHesapTuru() + " – " + bakiyeStr(h));
            if (secKaynak != null) krediOdemeKaynakCombo.setValue(secKaynak);
        }

        krediBilgiGuncelle();
    }

    // ── İşlem Geçmişi ─────────────────────────────────────────────────────────
    private ScrollPane islemGecmisiSekme() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(14));
        panel.setStyle("-fx-background-color: #f0f3f9;");

        VBox filtrKart = UITema.kart("Hesap ve Filtre");
        gecmisCombo = new ComboBox<>(); gecmisCombo.setMaxWidth(Double.MAX_VALUE);
        baslangicTarih = new DatePicker(); baslangicTarih.setMaxWidth(Double.MAX_VALUE);
        bitisTarih     = new DatePicker(); bitisTarih.setMaxWidth(Double.MAX_VALUE);
        aramaField = UITema.alan(); aramaField.setPromptText("Miktar veya tür ile ara...");
        islemTuruCombo = new ComboBox<>(FXCollections.observableArrayList(
            "Tümü", "Para Yatırma", "Para Çekme", "Transfer"));
        islemTuruCombo.setValue("Tümü");
        islemTuruCombo.setMaxWidth(Double.MAX_VALUE);

        GridPane grid = new GridPane(); grid.setHgap(12); grid.setVgap(8);
        grid.add(UITema.etiket("Hesap:"), 0, 0);              grid.add(gecmisCombo, 1, 0);
        grid.add(UITema.etiket("İşlem Türü:"), 0, 1);         grid.add(islemTuruCombo, 1, 1);
        grid.add(UITema.etiket("Başlangıç Tarihi:"), 0, 2);   grid.add(baslangicTarih, 1, 2);
        grid.add(UITema.etiket("Bitiş Tarihi:"), 0, 3);       grid.add(bitisTarih, 1, 3);
        grid.add(UITema.etiket("Arama:"), 0, 4);              grid.add(aramaField, 1, 4);
        ColumnConstraints cc0 = new ColumnConstraints(); cc0.setMinWidth(140);
        ColumnConstraints cc1 = new ColumnConstraints(); cc1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(cc0, cc1);

        Button getirBtn   = UITema.anaButon("Geçmişi Getir");
        Button csvBtn     = UITema.normalButon("CSV Olarak Kaydet");
        Button temizleBtn = UITema.normalButon("Tarihi Temizle");
        getirBtn.setOnAction(e -> islemGecmisiniYukle());
        csvBtn.setOnAction(e -> csvOlarakKaydet());
        temizleBtn.setOnAction(e -> {
            baslangicTarih.setValue(null); bitisTarih.setValue(null);
            aramaField.clear(); islemTuruCombo.setValue("Tümü");
        });
        HBox btnRow = new HBox(8, temizleBtn, csvBtn, getirBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        filtrKart.getChildren().addAll(grid, btnRow);

        islemTablo = UITema.tablo("Tarih / Saat", "İşlem Türü", "Miktar");
        islemTablo.setPrefHeight(320);

        @SuppressWarnings("unchecked")
        TableColumn<ObservableList<String>, String> turSutun =
            (TableColumn<ObservableList<String>, String>) islemTablo.getColumns().get(1);
        turSutun.setCellFactory(col -> new TableCell<ObservableList<String>, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setFont(Font.font("System", FontWeight.BOLD, 12));
                if      (item.contains("Yatırma"))  setStyle("-fx-text-fill: #146418;");
                else if (item.contains("Çekme"))    setStyle("-fx-text-fill: #af1414;");
                else if (item.contains("Transfer")) setStyle("-fx-text-fill: #235299;");
                else setStyle("");
            }
        });

        VBox tabloKart = UITema.kart("İşlem Geçmişi");
        VBox.setVgrow(islemTablo, Priority.ALWAYS);
        tabloKart.getChildren().add(islemTablo);

        panel.getChildren().addAll(filtrKart, tabloKart);
        gecmisComboYenile();

        ScrollPane sp = new ScrollPane(panel);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color: transparent;");
        return sp;
    }

    // ── Profilim ──────────────────────────────────────────────────────────────
    private ScrollPane profilimSekme() {
        profilAdField        = UITema.alan();
        profilEpostaField    = UITema.alan();
        eskiSifreField       = UITema.sifreAlani();
        yeniSifreField       = UITema.sifreAlani();
        yeniSifreTekrarField = UITema.sifreAlani();
        sifreDurumLabel      = UITema.durumLabel();

        VBox panel = new VBox(12);
        panel.setPadding(new Insets(14));
        panel.setStyle("-fx-background-color: #f0f3f9;");

        // Bilgi güncelleme kartı
        VBox bilgiKart = UITema.kart("Müşteri Bilgilerimi Güncelle");
        bilgiKart.setMaxWidth(420);
        Button kaydetBtn = UITema.anaButon("Bilgileri Güncelle");
        kaydetBtn.setMaxWidth(Double.MAX_VALUE);
        kaydetBtn.setOnAction(e -> profilGuncelle());
        VBox.setMargin(kaydetBtn, new Insets(12, 0, 0, 0));
        bilgiKart.getChildren().addAll(
            UITema.etiket("Ad Soyad:"),  profilAdField,
            UITema.etiket("E-posta:"),   profilEpostaField,
            kaydetBtn
        );

        // Şifre değiştirme kartı
        VBox sifreKart = UITema.kart("Şifre Değiştir");
        sifreKart.setMaxWidth(420);
        Button sifreBtn = UITema.anaButon("Şifremi Değiştir");
        sifreBtn.setMaxWidth(Double.MAX_VALUE);
        sifreBtn.setOnAction(e -> sifreDegistir());
        VBox.setMargin(sifreBtn, new Insets(12, 0, 0, 0));
        sifreKart.getChildren().addAll(
            UITema.etiket("Mevcut Şifre:"),        eskiSifreField,
            UITema.etiket("Yeni Şifre:"),           yeniSifreField,
            UITema.etiket("Yeni Şifre (Tekrar):"), yeniSifreTekrarField,
            sifreBtn, sifreDurumLabel
        );

        HBox satir = new HBox(12, bilgiKart, sifreKart);
        satir.setFillHeight(false);
        HBox.setHgrow(bilgiKart, Priority.ALWAYS);
        HBox.setHgrow(sifreKart, Priority.ALWAYS);

        panel.getChildren().add(satir);
        profilYukle();

        ScrollPane sp = new ScrollPane(panel);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color: transparent;");
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
            java.util.List<String> riskler = kontrolcu.riskKontrolEt(id, m);
            if (!riskler.isEmpty() && !onayDiyalogu(riskler, m)) return;
            if (kontrolcu.paraCek(id, m)) {
                BankController.BekleyenIslem bekleyen = kontrolcu.getSonBekleyenIslem();
                UITema.durumGoster(pcDurum, "Para çekme başarılı: " + tl(m), true);
                if (bekleyen != null) geriAlBaslat(bekleyen, pcDurum, pcGeriAlBtn);
                pcMiktarField.clear(); combolarYenile(); hesaplarimYenile();
            } else UITema.durumGoster(pcDurum, "Başarısız: Hesap şüpheli olarak işaretlenmiş.", false);
        } catch (YetersizBakiyeException e) {
            UITema.durumGoster(pcDurum, "Yetersiz bakiye! Mevcut: " + tl(e.getMevcutBakiye()), false);
        } catch (RiskLimitiAsildiException e) {
            UITema.durumGoster(pcDurum, "Limit aşıldı: " + e.getMessage(), false);
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
            java.util.List<String> riskler = kontrolcu.riskKontrolEt(kId, m);
            if (!riskler.isEmpty() && !onayDiyalogu(riskler, m)) return;
            if (kontrolcu.transferYap(kId, hId, m)) {
                BankController.BekleyenIslem bekleyen = kontrolcu.getSonBekleyenIslem();
                UITema.durumGoster(trDurum, "Transfer başarılı: " + tl(m), true);
                if (bekleyen != null) geriAlBaslat(bekleyen, trDurum, trGeriAlBtn);
                trHedefField.clear(); trMiktarField.clear(); combolarYenile(); hesaplarimYenile();
            } else UITema.durumGoster(trDurum, "Transfer başarısız: Şüpheli hesap veya hedef bulunamadı.", false);
        } catch (YetersizBakiyeException e) {
            UITema.durumGoster(trDurum, "Yetersiz bakiye! Mevcut: " + tl(e.getMevcutBakiye()), false);
        } catch (RiskLimitiAsildiException e) {
            UITema.durumGoster(trDurum, "Limit aşıldı: " + e.getMessage(), false);
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

        LocalDate bas       = baslangicTarih.getValue();
        LocalDate bit       = bitisTarih.getValue();
        String    arama     = aramaField != null ? aramaField.getText().trim().toLowerCase() : "";
        String    seciliTur = islemTuruCombo != null ? islemTuruCombo.getValue() : "Tümü";

        List<Transaction> islemler = hesap.getIslemler();
        if (islemler.isEmpty()) { UITema.bilgi("Bilgi", "Bu hesaba ait işlem bulunamadı."); return; }

        int gosterilen = 0;
        for (Transaction i : islemler) {
            LocalDate tarih = i.getZaman().toLocalDate();
            if (bas != null && tarih.isBefore(bas)) continue;
            if (bit != null && tarih.isAfter(bit))  continue;
            String turTr     = islemTurTr(i.getTur());
            String miktarStr = tl(i.getMiktar());
            if (!"Tümü".equals(seciliTur) && !turTr.equals(seciliTur)) continue;
            if (!arama.isEmpty()
                    && !turTr.toLowerCase().contains(arama)
                    && !miktarStr.replace(",", "").contains(arama)) continue;
            String zaman = i.getZaman().toString().replace("T", " ");
            if (zaman.length() > 19) zaman = zaman.substring(0, 19);
            UITema.satirEkle(islemTablo, zaman, turTr, miktarStr);
            gosterilen++;
        }
        if (gosterilen == 0) UITema.bilgi("Bilgi", "Filtre kriterlerine uygun işlem bulunamadı.");
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
            for (ObservableList<String> satir : islemTablo.getItems())
                pw.println("\"" + satir.get(0) + "\",\"" + satir.get(1) + "\",\"" + satir.get(2) + "\"");
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
        m.setAd(yeniAd); m.setEposta(yeniEp);
        kontrolcu.durumKaydet("banka_durumu.dat");
        UITema.bilgi("Başarılı", "Bilgileriniz güncellendi.");
    }

    private void sifreDegistir() {
        String eski   = eskiSifreField.getText();
        String yeni   = yeniSifreField.getText();
        String tekrar = yeniSifreTekrarField.getText();
        if (eski.isEmpty() || yeni.isEmpty() || tekrar.isEmpty()) {
            UITema.durumGoster(sifreDurumLabel, "Tüm şifre alanları doldurulmalıdır.", false); return;
        }
        if (!yeni.equals(tekrar)) {
            UITema.durumGoster(sifreDurumLabel, "Yeni şifreler uyuşmuyor.", false); return;
        }
        if (yeni.length() < 6) {
            UITema.durumGoster(sifreDurumLabel, "Yeni şifre en az 6 karakter olmalıdır.", false); return;
        }
        if (kontrolcu.sifreDegistir(kullanici.getKullaniciAdi(), eski, yeni)) {
            UITema.durumGoster(sifreDurumLabel, "Şifreniz başarıyla güncellendi.", true);
            eskiSifreField.clear(); yeniSifreField.clear(); yeniSifreTekrarField.clear();
        } else {
            UITema.durumGoster(sifreDurumLabel, "Mevcut şifre hatalı.", false);
        }
    }

    // ── Yenileme ──────────────────────────────────────────────────────────────
    private void hesaplarimYenile() {
        if (hesapTablo == null) return;
        hesapTablo.getItems().clear();
        String mId = kullanici.getMusteriId();
        if (mId == null) return;

        double sumTL = 0, sumDoviz = 0, sumKredi = 0;
        for (Account h : kontrolcu.musteriHesaplari(mId)) {
            int    skor  = kontrolcu.getRiskSkoru(h.getHesapId());
            String durum;
            if      (kontrolcu.suphelihMi(h.getHesapId())) durum = "ŞÜPHELİ";
            else if (skor >= 61) durum = "RİSKLİ ("    + skor + ")";
            else if (skor >= 31) durum = "İZLENİYOR (" + skor + ")";
            else                 durum = "GÜVENLİ ("   + skor + ")";
            UITema.satirEkle(hesapTablo,
                h.getHesapId(), h.getHesapTuru(), bakiyeStr(h), hesapEkBilgi(h), durum);
            if (h instanceof DovizHesabi) {
                sumDoviz += ((DovizHesabi) h).getBakiyeTL();
            } else if (h instanceof KrediHesabi) {
                if (h.getBakiye() < 0) sumKredi += -h.getBakiye();
            } else {
                sumTL += h.getBakiye();
            }
        }
        double net = sumTL + sumDoviz - sumKredi;
        if (toplamTLLabel    != null) toplamTLLabel.setText(tl(sumTL));
        if (toplamDovizLabel != null) toplamDovizLabel.setText(tl(sumDoviz));
        if (toplamKrediLabel != null) toplamKrediLabel.setText(tl(sumKredi));
        if (netVarlikLabel   != null) {
            netVarlikLabel.setText(tl(net));
            netVarlikLabel.setStyle("-fx-text-fill: " + (net >= 0 ? "#146418" : "#af1414")
                + "; -fx-font-weight: bold; -fx-font-size: 16;");
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
                combo.getItems().add(h.getHesapId() + " – " + h.getHesapTuru() + " – " + bakiyeStr(h));
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
        double kalan = cekim ? kontrolcu.kalanCekimLimiti(id)     : kontrolcu.kalanTransferLimiti(id);
        double max   = cekim ? kontrolcu.getGunlukCekimLimiti(id) : kontrolcu.getGunlukTransferLimiti(id);
        String tip   = cekim ? "Günlük çekim" : "Günlük transfer";
        label.setText(tip + ": " + tl(kalan) + " / " + tl(max));
        String renk;
        if (kalan <= 0)          renk = "#af1414";       // kırmızı — limit bitti
        else if (kalan == max)   renk = "#146418";       // yeşil — limit hiç kullanılmamış
        else if (kalan < max * 0.2) renk = "#af4b00";   // turuncu — %20'den az kaldı
        else                     renk = "#163264";       // mavi — normal
        label.setStyle("-fx-text-fill: " + renk + "; -fx-font-size: 11; -fx-font-weight: bold;");
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

    static String bakiyeStr(Account h) {
        if (h instanceof DovizHesabi) {
            DovizHesabi d = (DovizHesabi) h;
            return String.format(Locale.US, "%,.2f %s", h.getBakiye(), d.getParaBirimiSimgesi());
        }
        return tl(h.getBakiye());
    }

    static String hesapEkBilgi(Account h) {
        if (h instanceof SavingsAccount) {
            return String.format("Faiz: %%%.1f / yıl", ((SavingsAccount) h).getFaizOrani() * 100);
        }
        if (h instanceof DovizHesabi) {
            DovizHesabi d = (DovizHesabi) h;
            return String.format(Locale.US,
                "Kur: %,.2f ₺  |  TL Karşılığı: %,.0f ₺", d.getDovizKuru(), d.getBakiyeTL());
        }
        if (h instanceof KrediHesabi) {
            KrediHesabi k = (KrediHesabi) h;
            return String.format(Locale.US,
                "Limit: %,.0f ₺  |  Kalan: %,.0f ₺  |  Faiz: %%%.0f/ay",
                k.getKrediLimiti(), k.kalanKredi(), k.getFaizOrani() * 100);
        }
        return "–";
    }

    private static String tl(double m) {
        return String.format(Locale.US, "%,.2f ₺", m);
    }

    /** Risk uyarısı onay dialogu. true = kullanıcı onayladı. */
    private boolean onayDiyalogu(java.util.List<String> riskler, double miktar) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("⚠ Risk Uyarısı");
        alert.setHeaderText("Bu işlemde risk tespit edildi");
        StringBuilder sb = new StringBuilder();
        sb.append("İşlem tutarı: ").append(tl(miktar)).append("\n\nTespit edilen riskler:\n");
        for (String r : riskler) sb.append("  •  ").append(r).append("\n");
        sb.append("\nDevam etmek istiyor musunuz?");
        alert.setContentText(sb.toString());
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.CANCEL);
        ((Button) alert.getDialogPane().lookupButton(ButtonType.YES)).setText("Onayla");
        ((Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("İptal");
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.YES;
    }

    /** Geri alma sayaç butonunu başlatır. Süre dolunca buton gizlenir. */
    private void geriAlBaslat(BankController.BekleyenIslem bekleyen,
                               Label durumLabel, Button geriAlBtn) {
        if (geriAlTimeline != null) geriAlTimeline.stop();
        geriAlBtn.setVisible(true);
        final long[] kalan = {bekleyen.kalanSaniye()};
        geriAlTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
                kalan[0]--;
                if (kalan[0] <= 0) {
                    geriAlBtn.setVisible(false);
                    geriAlTimeline.stop();
                } else {
                    long d = kalan[0] / 60, s = kalan[0] % 60;
                    geriAlBtn.setText(String.format("↩ Geri Al (%d:%02d)", d, s));
                }
            })
        );
        geriAlTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        geriAlTimeline.play();
        long d = kalan[0] / 60, s = kalan[0] % 60;
        geriAlBtn.setText(String.format("↩ Geri Al (%d:%02d)", d, s));
        geriAlBtn.setOnAction(ev -> {
            BankController.BekleyenIslem sonuc = kontrolcu.geriAl(bekleyen.islemId);
            geriAlTimeline.stop();
            geriAlBtn.setVisible(false);
            if (sonuc != null) {
                UITema.durumGoster(durumLabel, "İşlem geri alındı: " + tl(sonuc.miktar), true);
                combolarYenile(); hesaplarimYenile();
            } else {
                UITema.durumGoster(durumLabel, "Geri alma süresi doldu.", false);
            }
        });
    }
}
