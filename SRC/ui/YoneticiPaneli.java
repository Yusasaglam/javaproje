package ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import model.*;
import service.BankController;
import service.KimlikDogrulama;

import java.util.List;
import java.util.Locale;

public class YoneticiPaneli extends BorderPane {

    private final BankController  kontrolcu;
    private final KimlikDogrulama kimlikDogrulama;

    // Müşteri sekmesi
    private TextField adField, epostaField, musteriKulAdiField, musteriSifreField;
    private TableView<ObservableList<String>> musteriTablo;

    // Hesap sekmesi
    private ComboBox<String> hesapMusteriCombo;
    private ComboBox<String> hesapTuruCombo;
    private TextField baslangicBakiyeField;
    private TableView<ObservableList<String>> hesapTablo;

    // İşlemler sekmesi
    private ComboBox<String> pyHesapCombo, pcHesapCombo, trKaynakCombo;
    private TextField pyMiktarField, pcMiktarField, trHedefField, trMiktarField;
    private Label islemDurumLabel;

    // Kullanıcı sekmesi
    private TextField kulAdiField, kulSifreField;
    private ComboBox<String> kulMusteriCombo;
    private TableView<ObservableList<String>> kullaniciTablo;

    // Raporlar sekmesi
    private PieChart hesapTuruPieChart;
    private BarChart<String, Number> bakiyeBarChart;
    private Label istatMusteriLabel, istatHesapLabel, istatBakiyeLabel, istatSupheliLabel;

    // Risk & Limitler sekmesi
    private ComboBox<String> limitHesapCombo;
    private TextField limitGunlukCekimField, limitGunlukTransferField;
    private TextField limitTekCekimField, limitTekTransferField;
    private TableView<ObservableList<String>> limitTablo;
    private TableView<ObservableList<String>> riskTablo;

    @SuppressWarnings("this-escape")
    public YoneticiPaneli(BankController kontrolcu, KimlikDogrulama kimlikDogrulama) {
        this.kontrolcu       = kontrolcu;
        this.kimlikDogrulama = kimlikDogrulama;
        setStyle("-fx-background-color: #f0f3f9;");
        setPadding(new Insets(8));
        bilesimleriBaslat();
    }

    private void bilesimleriBaslat() {
        TabPane sekmeler = new TabPane();
        sekmeler.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab t1 = new Tab("  Müşteri Yönetimi  ",  musteriSekme());
        Tab t2 = new Tab("  Hesap Yönetimi  ",    hesapSekme());
        Tab t3 = new Tab("  İşlemler  ",           islemlerSekme());
        Tab t4 = new Tab("  Kullanıcı Yönetimi  ", kullaniciSekme());
        Tab t5 = new Tab("  Raporlar  ",            raporlarSekme());
        Tab t6 = new Tab("  Risk & Limitler  ",     riskLimitlerSekme());

        sekmeler.getTabs().addAll(t1, t2, t3, t4, t5, t6);

        sekmeler.getSelectionModel().selectedItemProperty().addListener((obs, eski, yeni) -> {
            if (yeni == t5) raporlariYenile();
            if (yeni == t6) { hesapComboGuncelle(limitHesapCombo); limitlariYenile(); riskTablosunuYenile(); }
        });

        setCenter(sekmeler);
    }

    // ── Müşteri Yönetimi ──────────────────────────────────────────────────────
    private ScrollPane musteriSekme() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(14));
        panel.setStyle("-fx-background-color: #f0f3f9;");

        // Form
        VBox form = UITema.kart("Yeni Müşteri Oluştur");
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(8);
        grid.add(UITema.etiket("Ad Soyad:"), 0, 0);
        adField = UITema.alan(); grid.add(adField, 1, 0);
        grid.add(UITema.etiket("E-posta:"), 0, 1);
        epostaField = UITema.alan(); grid.add(epostaField, 1, 1);
        grid.add(UITema.etiket("Kullanıcı Adı:"), 0, 2);
        musteriKulAdiField = UITema.alan(); grid.add(musteriKulAdiField, 1, 2);
        grid.add(UITema.etiket("Şifre:"), 0, 3);
        musteriSifreField = UITema.alan(); grid.add(musteriSifreField, 1, 3);
        GridPane.setHgrow(adField, Priority.ALWAYS);
        GridPane.setHgrow(epostaField, Priority.ALWAYS);
        GridPane.setHgrow(musteriKulAdiField, Priority.ALWAYS);
        GridPane.setHgrow(musteriSifreField, Priority.ALWAYS);
        grid.getColumnConstraints().addAll(sütunKısıt(0), sütunKısıt(1));
        Button olusturBtn = UITema.anaButon("Müşteri Oluştur");
        olusturBtn.setOnAction(e -> musteriOlustur());
        HBox btnBox = new HBox(olusturBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        form.getChildren().addAll(grid, btnBox);

        // Tablo
        musteriTablo = UITema.tablo("Müşteri No", "Ad Soyad", "E-posta", "Hesap Sayısı");
        musteriTablo.setPrefHeight(300);
        Button yenileBtn = UITema.normalButon("Listeyi Yenile");
        yenileBtn.setOnAction(e -> musterileriYenile());
        VBox tabloKart = tabloKartOlustur("Müşteri Listesi", musteriTablo, yenileBtn);

        panel.getChildren().addAll(form, tabloKart);
        musterileriYenile();

        ScrollPane sp = new ScrollPane(panel);
        sp.setFitToWidth(true); sp.setStyle("-fx-background-color: transparent;");
        return sp;
    }

    // ── Hesap Yönetimi ────────────────────────────────────────────────────────
    private ScrollPane hesapSekme() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(14));
        panel.setStyle("-fx-background-color: #f0f3f9;");

        VBox form = UITema.kart("Yeni Hesap Oluştur");
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(8);
        hesapMusteriCombo = new ComboBox<>(); hesapMusteriCombo.setMaxWidth(Double.MAX_VALUE);
        hesapTuruCombo = new ComboBox<>(FXCollections.observableArrayList("VADESİZ", "VADELİ"));
        hesapTuruCombo.setMaxWidth(Double.MAX_VALUE);
        baslangicBakiyeField = UITema.alan();
        grid.add(UITema.etiket("Müşteri:"), 0, 0);      grid.add(hesapMusteriCombo, 1, 0);
        grid.add(UITema.etiket("Hesap Türü:"), 0, 1);   grid.add(hesapTuruCombo, 1, 1);
        grid.add(UITema.etiket("Başlangıç Bakiyesi (₺):"), 0, 2); grid.add(baslangicBakiyeField, 1, 2);
        grid.getColumnConstraints().addAll(sütunKısıt(0), sütunKısıt(1));
        Button olusturBtn = UITema.anaButon("Hesap Oluştur");
        olusturBtn.setOnAction(e -> hesapOlustur());
        HBox btnBox = new HBox(olusturBtn); btnBox.setAlignment(Pos.CENTER_RIGHT);
        form.getChildren().addAll(grid, btnBox);

        hesapTablo = UITema.tablo("Hesap No", "Müşteri", "Tür", "Bakiye", "Durum");
        hesapTablo.setPrefHeight(300);
        Button isaretle = UITema.tehlikeButon("⚠ Şüpheli İşaretle");
        Button kaldir   = UITema.normalButon("✓ Şüpheyi Kaldır");
        Button yenile   = UITema.normalButon("Listeyi Yenile");
        isaretle.setOnAction(e -> {
            int idx = hesapTablo.getSelectionModel().getSelectedIndex();
            if (idx < 0) { UITema.uyari("Uyarı", "Lütfen bir hesap seçin."); return; }
            kontrolcu.hesapIsaretle(hesapTablo.getItems().get(idx).get(0));
            hesaplariYenile();
        });
        kaldir.setOnAction(e -> {
            int idx = hesapTablo.getSelectionModel().getSelectedIndex();
            if (idx < 0) { UITema.uyari("Uyarı", "Lütfen bir hesap seçin."); return; }
            kontrolcu.isaretKaldir(hesapTablo.getItems().get(idx).get(0));
            hesaplariYenile();
        });
        yenile.setOnAction(e -> hesaplariYenile());
        VBox tabloKart = tabloKartOlustur("Hesap Listesi", hesapTablo, isaretle, kaldir, yenile);

        panel.getChildren().addAll(form, tabloKart);
        musteriComboGuncelle(hesapMusteriCombo, false);
        hesaplariYenile();

        ScrollPane sp = new ScrollPane(panel);
        sp.setFitToWidth(true); sp.setStyle("-fx-background-color: transparent;");
        return sp;
    }

    // ── İşlemler ──────────────────────────────────────────────────────────────
    private BorderPane islemlerSekme() {
        BorderPane panel = new BorderPane();
        panel.setPadding(new Insets(14));
        panel.setStyle("-fx-background-color: #f0f3f9;");

        HBox icerik = new HBox(14);
        icerik.setFillHeight(true);

        pyHesapCombo  = new ComboBox<>(); pyHesapCombo.setMaxWidth(Double.MAX_VALUE);
        pyMiktarField = UITema.alan();
        pcHesapCombo  = new ComboBox<>(); pcHesapCombo.setMaxWidth(Double.MAX_VALUE);
        pcMiktarField = UITema.alan();
        trKaynakCombo = new ComboBox<>(); trKaynakCombo.setMaxWidth(Double.MAX_VALUE);
        trHedefField  = UITema.alan();
        trMiktarField = UITema.alan();

        icerik.getChildren().addAll(
            islemBlogu("Para Yatır",  pyHesapCombo, pyMiktarField,  UITema.anaButon("Para Yatır"),  () -> paraYatir()),
            islemBlogu("Para Çek",   pcHesapCombo, pcMiktarField,  UITema.anaButon("Para Çek"),   () -> paraCek()),
            transferBlogu()
        );
        for (javafx.scene.Node n : icerik.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);

        islemDurumLabel = UITema.durumLabel();
        islemDurumLabel.setFont(Font.font("System", FontWeight.BOLD, 13));

        Button yenileBtn = UITema.normalButon("Hesapları Yenile");
        yenileBtn.setOnAction(e -> { hesapComboGuncelle(pyHesapCombo); hesapComboGuncelle(pcHesapCombo); hesapComboGuncelle(trKaynakCombo); });

        HBox alt = new HBox(10, islemDurumLabel, yenileBtn);
        alt.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(islemDurumLabel, Priority.ALWAYS);
        alt.setPadding(new Insets(10, 0, 0, 0));

        hesapComboGuncelle(pyHesapCombo); hesapComboGuncelle(pcHesapCombo); hesapComboGuncelle(trKaynakCombo);
        panel.setCenter(icerik);
        panel.setBottom(alt);
        return panel;
    }

    private VBox islemBlogu(String baslik, ComboBox<String> combo, TextField miktarField,
                             Button btn, Runnable aksyon) {
        VBox kart = UITema.kart(baslik);
        kart.setMaxWidth(Double.MAX_VALUE);
        kart.getChildren().addAll(
            UITema.etiket("Hesap:"), combo,
            UITema.etiket("Miktar (₺):"), miktarField
        );
        btn.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(btn, new Insets(12, 0, 0, 0));
        btn.setOnAction(e -> aksyon.run());
        kart.getChildren().add(btn);
        return kart;
    }

    private VBox transferBlogu() {
        VBox kart = UITema.kart("Transfer");
        kart.setMaxWidth(Double.MAX_VALUE);
        Button btn = UITema.anaButon("Transfer Yap");
        btn.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(btn, new Insets(12, 0, 0, 0));
        btn.setOnAction(e -> transferYap());
        kart.getChildren().addAll(
            UITema.etiket("Kaynak Hesap:"), trKaynakCombo,
            UITema.etiket("Hedef Hesap No:"), trHedefField,
            UITema.etiket("Miktar (₺):"), trMiktarField,
            btn
        );
        return kart;
    }

    // ── Kullanıcı Yönetimi ────────────────────────────────────────────────────
    private ScrollPane kullaniciSekme() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(14));
        panel.setStyle("-fx-background-color: #f0f3f9;");

        VBox form = UITema.kart("Yeni Kullanıcı Hesabı Oluştur");
        GridPane grid = new GridPane(); grid.setHgap(12); grid.setVgap(8);
        kulAdiField    = UITema.alan();
        kulSifreField  = UITema.alan();
        kulMusteriCombo = new ComboBox<>(); kulMusteriCombo.setMaxWidth(Double.MAX_VALUE);
        grid.add(UITema.etiket("Kullanıcı Adı:"), 0, 0); grid.add(kulAdiField, 1, 0);
        grid.add(UITema.etiket("Şifre:"), 0, 1);         grid.add(kulSifreField, 1, 1);
        grid.add(UITema.etiket("Müşteri Bağla:"), 0, 2); grid.add(kulMusteriCombo, 1, 2);
        grid.getColumnConstraints().addAll(sütunKısıt(0), sütunKısıt(1));
        Button olusturBtn = UITema.anaButon("Kullanıcı Oluştur");
        olusturBtn.setOnAction(e -> kullaniciOlustur());
        HBox btnBox = new HBox(olusturBtn); btnBox.setAlignment(Pos.CENTER_RIGHT);
        form.getChildren().addAll(grid, btnBox);

        kullaniciTablo = UITema.tablo("Kullanıcı Adı", "Rol", "Müşteri No", "Durum");
        kullaniciTablo.setPrefHeight(280);
        @SuppressWarnings("unchecked")
        TableColumn<ObservableList<String>, String> durumKol =
            (TableColumn<ObservableList<String>, String>) kullaniciTablo.getColumns().get(3);
        durumKol.setCellFactory(col -> new TableCell<ObservableList<String>, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setFont(Font.font("System", FontWeight.BOLD, 12));
                switch (item) {
                    case "ENGELLİ": setStyle("-fx-text-fill: #af1414;"); break;
                    case "PASİF":   setStyle("-fx-text-fill: #af4b00;"); break;
                    default:        setStyle("-fx-text-fill: #146418;"); break;
                }
            }
        });

        Button engelKaldirBtn = UITema.normalButon("🔓 Engeli Kaldır");
        Button pasifBtn       = UITema.normalButon("⏸ Pasif Yap");
        Button aktifBtn       = UITema.normalButon("▶ Aktif Yap");
        Button yenileBtn      = UITema.normalButon("Listeyi Yenile");

        engelKaldirBtn.setOnAction(e -> {
            Kullanici k = seciliKullanici();
            if (k == null) return;
            k.engelKaldir();
            kontrolcu.durumKaydet("banka_durumu.dat");
            kullanicilariYenile();
            UITema.bilgi("Bilgi", k.getKullaniciAdi() + " kullanıcısının engeli kaldırıldı.");
        });
        pasifBtn.setOnAction(e -> {
            Kullanici k = seciliKullanici();
            if (k == null) return;
            if (k.getRol() == Kullanici.Rol.YONETICI) { UITema.uyari("Uyarı", "Yönetici hesabı pasif yapılamaz."); return; }
            k.pasifYap();
            kontrolcu.durumKaydet("banka_durumu.dat");
            kullanicilariYenile();
            UITema.bilgi("Bilgi", k.getKullaniciAdi() + " kullanıcısı pasif yapıldı.");
        });
        aktifBtn.setOnAction(e -> {
            Kullanici k = seciliKullanici();
            if (k == null) return;
            k.aktifYap(); k.engelKaldir();
            kontrolcu.durumKaydet("banka_durumu.dat");
            kullanicilariYenile();
            UITema.bilgi("Bilgi", k.getKullaniciAdi() + " kullanıcısı aktif yapıldı.");
        });
        yenileBtn.setOnAction(e -> kullanicilariYenile());

        VBox tabloKart = tabloKartOlustur("Kullanıcı Listesi", kullaniciTablo,
                engelKaldirBtn, pasifBtn, aktifBtn, yenileBtn);

        musteriComboGuncelle(kulMusteriCombo, true);
        panel.getChildren().addAll(form, tabloKart);
        kullanicilariYenile();

        ScrollPane sp = new ScrollPane(panel);
        sp.setFitToWidth(true); sp.setStyle("-fx-background-color: transparent;");
        return sp;
    }

    // ── Raporlar ──────────────────────────────────────────────────────────────
    private ScrollPane raporlarSekme() {
        VBox panel = new VBox(14);
        panel.setPadding(new Insets(14));
        panel.setStyle("-fx-background-color: #f0f3f9;");

        // İstatistik kartları
        HBox istatRow = new HBox(12);
        istatMusteriLabel = istatKartOlustur(istatRow, "Toplam Müşteri",   "0", "#163264");
        istatHesapLabel   = istatKartOlustur(istatRow, "Toplam Hesap",     "0", "#235299");
        istatBakiyeLabel  = istatKartOlustur(istatRow, "Toplam Bakiye",    "0 ₺", "#146418");
        istatSupheliLabel = istatKartOlustur(istatRow, "Şüpheli Hesap",    "0", "#af1414");
        for (javafx.scene.Node n : istatRow.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);

        // Grafikler
        hesapTuruPieChart = new PieChart();
        hesapTuruPieChart.setTitle("Hesap Türü Dağılımı");
        hesapTuruPieChart.setPrefHeight(260);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        bakiyeBarChart = new BarChart<>(xAxis, yAxis);
        bakiyeBarChart.setTitle("En Yüksek Bakiyeli Hesaplar");
        bakiyeBarChart.setPrefHeight(260);
        bakiyeBarChart.setLegendVisible(false);

        HBox charts = new HBox(12, hesapTuruPieChart, bakiyeBarChart);
        HBox.setHgrow(hesapTuruPieChart, Priority.ALWAYS);
        HBox.setHgrow(bakiyeBarChart, Priority.ALWAYS);

        Button raporBtn  = UITema.normalButon("Raporu Güncelle");
        Button kaydetBtn = UITema.anaButon("Durumu Kaydet");
        raporBtn.setOnAction(e -> raporlariYenile());
        kaydetBtn.setOnAction(e -> {
            kontrolcu.durumKaydet("banka_durumu.dat");
            UITema.bilgi("Bilgi", "Durum başarıyla kaydedildi.");
        });
        HBox butonRow = new HBox(8, raporBtn, kaydetBtn);
        butonRow.setAlignment(Pos.CENTER_RIGHT);

        panel.getChildren().addAll(istatRow, charts, butonRow);
        raporlariYenile();

        ScrollPane sp = new ScrollPane(panel);
        sp.setFitToWidth(true); sp.setStyle("-fx-background-color: transparent;");
        return sp;
    }

    private Label istatKartOlustur(HBox satir, String baslik, String deger, String renk) {
        VBox kart = new VBox(6);
        kart.getStyleClass().add("istat-kart");
        kart.setAlignment(Pos.CENTER);
        Label degerLabel = new Label(deger);
        degerLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        degerLabel.setStyle("-fx-text-fill: " + renk + ";");
        Label baslikLabel = new Label(baslik);
        baslikLabel.setStyle("-fx-text-fill: #808080; -fx-font-size: 11;");
        kart.getChildren().addAll(degerLabel, baslikLabel);
        satir.getChildren().add(kart);
        return degerLabel;
    }

    // ── Risk & Limitler ───────────────────────────────────────────────────────
    private HBox riskLimitlerSekme() {
        HBox panel = new HBox(14);
        panel.setPadding(new Insets(14));
        panel.setStyle("-fx-background-color: #f0f3f9;");

        panel.getChildren().addAll(limitYonetimiPaneli(), supheSebebleriPaneli());
        HBox.setHgrow(panel.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(panel.getChildren().get(1), Priority.ALWAYS);
        return panel;
    }

    private VBox limitYonetimiPaneli() {
        VBox dis = new VBox(12);
        dis.setMaxWidth(Double.MAX_VALUE);

        VBox form = UITema.kart("Hesap Limit Yönetimi");
        GridPane grid = new GridPane(); grid.setHgap(12); grid.setVgap(8);
        limitHesapCombo          = new ComboBox<>(); limitHesapCombo.setMaxWidth(Double.MAX_VALUE);
        limitGunlukCekimField    = UITema.alan();
        limitGunlukTransferField = UITema.alan();
        limitTekCekimField       = UITema.alan();
        limitTekTransferField    = UITema.alan();
        grid.add(UITema.etiket("Hesap:"), 0, 0);                 grid.add(limitHesapCombo, 1, 0);
        grid.add(UITema.etiket("Günlük Çekim (₺):"), 0, 1);     grid.add(limitGunlukCekimField, 1, 1);
        grid.add(UITema.etiket("Günlük Transfer (₺):"), 0, 2);   grid.add(limitGunlukTransferField, 1, 2);
        grid.add(UITema.etiket("Tek Çekim Maks (₺):"), 0, 3);   grid.add(limitTekCekimField, 1, 3);
        grid.add(UITema.etiket("Tek Transfer Maks (₺):"), 0, 4); grid.add(limitTekTransferField, 1, 4);
        grid.getColumnConstraints().addAll(sütunKısıt(0), sütunKısıt(1));

        Button gosterBtn  = UITema.normalButon("Mevcut Limiti Göster");
        Button sifirlaBtn = UITema.normalButon("Varsayılana Sıfırla");
        Button kaydetBtn  = UITema.anaButon("Limiti Kaydet");
        gosterBtn.setOnAction(e -> limitGoster());
        sifirlaBtn.setOnAction(e -> limitSifirla());
        kaydetBtn.setOnAction(e -> limitKaydet());
        HBox btnBox = new HBox(8, sifirlaBtn, gosterBtn, kaydetBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        form.getChildren().addAll(grid, btnBox);

        limitTablo = UITema.tablo("Hesap No", "Müşteri", "G. Çekim", "G. Transfer", "Tek Çekim", "Tek Transfer");
        limitTablo.setPrefHeight(240);
        VBox tabloKart = tabloKartOlustur("Özel Limitler", limitTablo);

        dis.getChildren().addAll(form, tabloKart);
        hesapComboGuncelle(limitHesapCombo);
        limitlariYenile();
        return dis;
    }

    private VBox supheSebebleriPaneli() {
        VBox dis = new VBox(12);
        dis.setMaxWidth(Double.MAX_VALUE);

        riskTablo = UITema.tablo("Hesap No", "Müşteri", "Şüphe Sebebi", "Miktar", "Tarih/Saat");
        riskTablo.setPrefHeight(400);

        Button kaldirBtn = UITema.normalButon("✓ Şüpheyi Kaldır");
        Button yenileBtn = UITema.normalButon("Yenile");
        kaldirBtn.setOnAction(e -> {
            int idx = riskTablo.getSelectionModel().getSelectedIndex();
            if (idx < 0) { UITema.uyari("Uyarı", "Lütfen bir hesap seçin."); return; }
            kontrolcu.isaretKaldir(riskTablo.getItems().get(idx).get(0));
            riskTablosunuYenile();
            hesaplariYenile();
        });
        yenileBtn.setOnAction(e -> riskTablosunuYenile());

        VBox tabloKart = tabloKartOlustur("Şüpheli Hesaplar ve Sebepler", riskTablo, kaldirBtn, yenileBtn);
        dis.getChildren().add(tabloKart);
        riskTablosunuYenile();
        return dis;
    }

    // ── İşlem mantığı ─────────────────────────────────────────────────────────
    private void musteriOlustur() {
        String ad    = adField.getText().trim();
        String ep    = epostaField.getText().trim();
        String kulAd = musteriKulAdiField.getText().trim();
        String sifre = musteriSifreField.getText().trim();
        if (ad.isEmpty() || ep.isEmpty()) { UITema.uyari("Uyarı", "Ad ve e-posta zorunludur."); return; }
        if (kulAd.isEmpty() || sifre.isEmpty()) { UITema.uyari("Uyarı", "Kullanıcı adı ve şifre zorunludur."); return; }
        if (kimlikDogrulama.getKullanicilar().containsKey(kulAd)) {
            UITema.hata("Hata", "Bu kullanıcı adı zaten alınmış: " + kulAd); return;
        }
        Customer m = kontrolcu.musteriOlustur(ad, ep);
        kimlikDogrulama.kullaniciEkle(new Kullanici(kulAd, sifre, Kullanici.Rol.MUSTERI, m.getMusteriId()));
        kontrolcu.durumKaydet("banka_durumu.dat");
        adField.clear(); epostaField.clear(); musteriKulAdiField.clear(); musteriSifreField.clear();
        musterileriYenile();
        musteriComboGuncelle(hesapMusteriCombo, false);
        musteriComboGuncelle(kulMusteriCombo, true);
        kullanicilariYenile();
        UITema.bilgi("Başarılı", "Müşteri oluşturuldu.\nNo: " + m.getMusteriId() + "\nAd: " + m.getAd() + "\nGiriş: " + kulAd);
    }

    private void hesapOlustur() {
        String secim = hesapMusteriCombo.getValue();
        if (secim == null) { UITema.uyari("Uyarı", "Müşteri seçiniz."); return; }
        String musteriId = secim.split(" – ")[0].trim();
        String tur = hesapTuruCombo.getValue() != null ? hesapTuruCombo.getValue() : "VADESİZ";
        try {
            double bakiye = Double.parseDouble(baslangicBakiyeField.getText().trim());
            if (bakiye < 0) throw new NumberFormatException();
            Account hesap = kontrolcu.hesapOlustur(musteriId, tur, bakiye);
            if (hesap == null) { UITema.hata("Hata", "Müşteri bulunamadı."); return; }
            baslangicBakiyeField.clear();
            hesaplariYenile();
            hesapComboGuncelle(pyHesapCombo); hesapComboGuncelle(pcHesapCombo); hesapComboGuncelle(trKaynakCombo);
            UITema.bilgi("Başarılı", "Hesap oluşturuldu.\nNo: " + hesap.getHesapId() + "\nBakiye: " + tl(hesap.getBakiye()));
        } catch (NumberFormatException ex) { UITema.hata("Hata", "Geçersiz bakiye değeri."); }
    }

    private void kullaniciOlustur() {
        String ad = kulAdiField.getText().trim(), sifre = kulSifreField.getText().trim();
        if (ad.isEmpty() || sifre.isEmpty()) { UITema.uyari("Uyarı", "Ad ve şifre zorunludur."); return; }
        String secim = kulMusteriCombo.getValue();
        String mId   = (secim != null && !secim.startsWith("–")) ? secim.split(" – ")[0].trim() : null;
        kimlikDogrulama.kullaniciEkle(new Kullanici(ad, sifre, Kullanici.Rol.MUSTERI, mId));
        kulAdiField.clear(); kulSifreField.clear();
        kullanicilariYenile();
        kontrolcu.durumKaydet("banka_durumu.dat");
        UITema.bilgi("Başarılı", "Kullanıcı oluşturuldu: " + ad);
    }

    private void paraYatir() {
        String id = seciliHesapId(pyHesapCombo);
        if (id == null) { UITema.durumGoster(islemDurumLabel, "Hesap seçilmedi.", false); return; }
        try {
            double m = Double.parseDouble(pyMiktarField.getText().trim());
            if (kontrolcu.paraYatir(id, m)) {
                UITema.durumGoster(islemDurumLabel, "Para yatırma başarılı: " + tl(m), true);
                pyMiktarField.clear(); hesaplariYenile(); hesapComboGuncelle(pyHesapCombo);
            } else UITema.durumGoster(islemDurumLabel, "Para yatırma başarısız. Limit aşıldı.", false);
        } catch (NumberFormatException e) { UITema.durumGoster(islemDurumLabel, "Geçersiz miktar.", false); }
    }

    private void paraCek() {
        String id = seciliHesapId(pcHesapCombo);
        if (id == null) { UITema.durumGoster(islemDurumLabel, "Hesap seçilmedi.", false); return; }
        try {
            double m = Double.parseDouble(pcMiktarField.getText().trim());
            if (kontrolcu.paraCek(id, m)) {
                UITema.durumGoster(islemDurumLabel, "Para çekme başarılı: " + tl(m), true);
                pcMiktarField.clear(); hesaplariYenile(); hesapComboGuncelle(pcHesapCombo);
            } else UITema.durumGoster(islemDurumLabel, "Başarısız. Yetersiz bakiye/limit/şüpheli hesap.", false);
        } catch (NumberFormatException e) { UITema.durumGoster(islemDurumLabel, "Geçersiz miktar.", false); }
    }

    private void transferYap() {
        String kId = seciliHesapId(trKaynakCombo), hId = trHedefField.getText().trim();
        if (kId == null) { UITema.durumGoster(islemDurumLabel, "Kaynak hesap seçilmedi.", false); return; }
        if (hId.isEmpty()) { UITema.durumGoster(islemDurumLabel, "Hedef hesap no giriniz.", false); return; }
        if (kId.equals(hId)) { UITema.durumGoster(islemDurumLabel, "Kaynak ve hedef aynı olamaz.", false); return; }
        try {
            double m = Double.parseDouble(trMiktarField.getText().trim());
            if (kontrolcu.transferYap(kId, hId, m)) {
                UITema.durumGoster(islemDurumLabel, "Transfer başarılı: " + tl(m), true);
                trHedefField.clear(); trMiktarField.clear();
                hesaplariYenile(); hesapComboGuncelle(trKaynakCombo);
            } else UITema.durumGoster(islemDurumLabel, "Transfer başarısız. Limit/bakiye/hedef bulunamadı.", false);
        } catch (NumberFormatException e) { UITema.durumGoster(islemDurumLabel, "Geçersiz miktar.", false); }
    }

    private void limitGoster() {
        String id = seciliHesapId(limitHesapCombo); if (id == null) return;
        HesapLimiti l = kontrolcu.getHesapLimiti(id);
        if (l == null) l = kontrolcu.varsayilanLimit();
        limitGunlukCekimField.setText(String.valueOf((long) l.getGunlukCekimLimiti()));
        limitGunlukTransferField.setText(String.valueOf((long) l.getGunlukTransferLimiti()));
        limitTekCekimField.setText(String.valueOf((long) l.getTekIslemCekimLimiti()));
        limitTekTransferField.setText(String.valueOf((long) l.getTekIslemTransferLimiti()));
    }

    private void limitKaydet() {
        String id = seciliHesapId(limitHesapCombo);
        if (id == null) { UITema.uyari("Uyarı", "Lütfen bir hesap seçin."); return; }
        try {
            double gC = Double.parseDouble(limitGunlukCekimField.getText().trim());
            double gT = Double.parseDouble(limitGunlukTransferField.getText().trim());
            double tC = Double.parseDouble(limitTekCekimField.getText().trim());
            double tT = Double.parseDouble(limitTekTransferField.getText().trim());
            if (gC <= 0 || gT <= 0 || tC <= 0 || tT <= 0) throw new NumberFormatException();
            kontrolcu.limitGuncelle(id, new HesapLimiti(gC, gT, tC, tT));
            limitlariYenile();
            UITema.bilgi("Bilgi", "Limit başarıyla güncellendi.");
        } catch (NumberFormatException e) { UITema.hata("Hata", "Geçerli pozitif sayı giriniz."); }
    }

    private void limitSifirla() {
        String id = seciliHesapId(limitHesapCombo); if (id == null) return;
        kontrolcu.limitGuncelle(id, kontrolcu.varsayilanLimit());
        limitlariYenile();
        UITema.bilgi("Bilgi", "Limit varsayılana sıfırlandı.");
    }

    // ── Yenileme ──────────────────────────────────────────────────────────────
    private void musterileriYenile() {
        musteriTablo.getItems().clear();
        for (Customer m : kontrolcu.tumMusteriler())
            UITema.satirEkle(musteriTablo, m.getMusteriId(), m.getAd(), m.getEposta(), String.valueOf(m.getHesaplar().size()));
    }

    private void hesaplariYenile() {
        if (hesapTablo == null) return;
        hesapTablo.getItems().clear();
        for (Account h : kontrolcu.tumHesaplar()) {
            Customer m  = kontrolcu.getMusteri(h.getSahibiId());
            String durum = kontrolcu.suphelihMi(h.getHesapId()) ? "ŞÜPHELİ" : "Normal";
            UITema.satirEkle(hesapTablo, h.getHesapId(), m != null ? m.getAd() : h.getSahibiId(), h.getHesapTuru(), tl(h.getBakiye()), durum);
        }
    }

    private void kullanicilariYenile() {
        kullaniciTablo.getItems().clear();
        for (Kullanici k : kimlikDogrulama.getKullanicilar().values()) {
            String durum = k.isEngelliMi() ? "ENGELLİ" : k.isPasifMi() ? "PASİF" : "Aktif";
            UITema.satirEkle(kullaniciTablo, k.getKullaniciAdi(), k.getRol().toString(),
                k.getMusteriId() != null ? k.getMusteriId() : "–", durum);
        }
    }

    private void raporlariYenile() {
        List<Account> hesaplar = kontrolcu.tumHesaplar();
        int vadesiz = 0, vadeli = 0, supheli = 0;
        double toplam = 0;
        for (Account h : hesaplar) {
            toplam += h.getBakiye();
            if ("VADESİZ".equals(h.getHesapTuru())) vadesiz++; else vadeli++;
            if (kontrolcu.suphelihMi(h.getHesapId())) supheli++;
        }
        if (istatMusteriLabel != null) {
            istatMusteriLabel.setText(String.valueOf(kontrolcu.tumMusteriler().size()));
            istatHesapLabel.setText(String.valueOf(hesaplar.size()));
            istatBakiyeLabel.setText(tl(toplam));
            istatSupheliLabel.setText(String.valueOf(supheli));
            istatSupheliLabel.setStyle("-fx-text-fill: " + (supheli > 0 ? "#af1414" : "#146418") + "; -fx-font-weight: bold; -fx-font-size: 22;");
        }
        // PieChart
        if (hesapTuruPieChart != null) {
            hesapTuruPieChart.getData().clear();
            if (vadesiz > 0) hesapTuruPieChart.getData().add(new PieChart.Data("Vadesiz (" + vadesiz + ")", vadesiz));
            if (vadeli > 0)  hesapTuruPieChart.getData().add(new PieChart.Data("Vadeli (" + vadeli + ")", vadeli));
        }
        // BarChart – Top 5
        if (bakiyeBarChart != null) {
            bakiyeBarChart.getData().clear();
            XYChart.Series<String, Number> seri = new XYChart.Series<>();
            hesaplar.stream()
                .sorted((a, b) -> Double.compare(b.getBakiye(), a.getBakiye()))
                .limit(5)
                .forEach(h -> seri.getData().add(new XYChart.Data<>(h.getHesapId(), h.getBakiye())));
            bakiyeBarChart.getData().add(seri);
        }
    }

    private void limitlariYenile() {
        if (limitTablo == null) return;
        limitTablo.getItems().clear();
        for (Account h : kontrolcu.tumHesaplar()) {
            Customer m   = kontrolcu.getMusteri(h.getSahibiId());
            HesapLimiti l = kontrolcu.getHesapLimiti(h.getHesapId());
            if (l == null) l = kontrolcu.varsayilanLimit();
            UITema.satirEkle(limitTablo, h.getHesapId(), m != null ? m.getAd() : h.getSahibiId(),
                tl(l.getGunlukCekimLimiti()), tl(l.getGunlukTransferLimiti()),
                tl(l.getTekIslemCekimLimiti()), tl(l.getTekIslemTransferLimiti()));
        }
    }

    private void riskTablosunuYenile() {
        if (riskTablo == null) return;
        riskTablo.getItems().clear();
        for (SupheSebebi s : kontrolcu.tumSupheSebebleri()) {
            Customer m  = s.getMusteriId() != null ? kontrolcu.getMusteri(s.getMusteriId()) : null;
            String zaman = s.getZaman().toString().replace("T", " ");
            if (zaman.length() > 19) zaman = zaman.substring(0, 19);
            UITema.satirEkle(riskTablo, s.getHesapId(),
                m != null ? m.getAd() : (s.getMusteriId() != null ? s.getMusteriId() : "–"),
                s.getSebep(),
                s.getIlgiliMiktar() > 0 ? tl(s.getIlgiliMiktar()) : "–",
                zaman);
        }
    }

    // ── Yardımcılar ───────────────────────────────────────────────────────────
    private void musteriComboGuncelle(ComboBox<String> combo, boolean bosEkle) {
        String secili = combo.getValue();
        combo.getItems().clear();
        if (bosEkle) combo.getItems().add("– Bağlamadan oluştur –");
        for (Customer m : kontrolcu.tumMusteriler())
            combo.getItems().add(m.getMusteriId() + " – " + m.getAd());
        if (secili != null) combo.setValue(secili);
    }

    private void hesapComboGuncelle(ComboBox<String> combo) {
        if (combo == null) return;
        String secili = combo.getValue();
        combo.getItems().clear();
        for (Account h : kontrolcu.tumHesaplar()) {
            Customer m = kontrolcu.getMusteri(h.getSahibiId());
            combo.getItems().add(h.getHesapId() + " – " + h.getHesapTuru() + " – " + (m != null ? m.getAd() : "") + " – " + tl(h.getBakiye()));
        }
        if (secili != null) combo.setValue(secili);
    }

    private static String seciliHesapId(ComboBox<String> combo) {
        String s = combo.getValue();
        return (s == null || s.isEmpty()) ? null : s.split(" – ")[0].trim();
    }

    private Kullanici seciliKullanici() {
        int idx = kullaniciTablo.getSelectionModel().getSelectedIndex();
        if (idx < 0) { UITema.uyari("Uyarı", "Lütfen bir kullanıcı seçin."); return null; }
        return kimlikDogrulama.getKullanicilar().get(kullaniciTablo.getItems().get(idx).get(0));
    }

    private static String tl(double m) {
        return String.format(Locale.US, "%,.2f ₺", m);
    }

    private static ColumnConstraints sütunKısıt(int idx) {
        ColumnConstraints cc = new ColumnConstraints();
        if (idx == 0) { cc.setMinWidth(160); cc.setHgrow(Priority.NEVER); }
        else           { cc.setHgrow(Priority.ALWAYS); }
        return cc;
    }

    private static VBox tabloKartOlustur(String baslik,
            TableView<ObservableList<String>> tablo, Button... butonlar) {
        VBox kart = UITema.kart(baslik);
        kart.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(tablo, Priority.ALWAYS);
        kart.getChildren().add(tablo);
        if (butonlar.length > 0) {
            HBox btnRow = new HBox(8);
            btnRow.setAlignment(Pos.CENTER_RIGHT);
            btnRow.setPadding(new Insets(8, 0, 0, 0));
            btnRow.getChildren().addAll(butonlar);
            kart.getChildren().add(btnRow);
        }
        return kart;
    }
}
