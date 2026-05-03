package ui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.util.Duration;
import model.*;
import service.BankController;
import service.KimlikDogrulama;
import service.RiskDinleyici;
import service.RiskOlayi;

import service.ActivityLog;
import service.AktiviteLogServisi;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
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
    private Label     baslangicBakiyeEtiketi;
    private TableView<ObservableList<String>> hesapTablo;

    // İşlemler sekmesi
    private ComboBox<String> pyHesapCombo, pcHesapCombo, trKaynakCombo;
    private TextField pyMiktarField, pcMiktarField, trHedefField, trMiktarField;
    private Label islemDurumLabel;

    // Kullanıcı sekmesi
    private TextField kulAdiField, kulSifreField;
    private ComboBox<String> kulMusteriCombo;
    private TableView<ObservableList<String>> kullaniciTablo;
    private TextField adminSifreKulAdiField, adminYeniSifreField;
    private Label    adminSifreDurumLabel;

    // Raporlar sekmesi
    private PieChart hesapTuruPieChart;
    private BarChart<String, Number> bakiyeBarChart;
    private Label istatMusteriLabel, istatHesapLabel, istatBakiyeLabel, istatSupheliLabel;
    private Button sonuclarBtn;
    private java.util.List<String> botSimLoglar;

    // Müşteri İzleme sekmesi
    private TableView<ObservableList<String>> izlemeMusteriTablo;
    private TableView<ObservableList<String>> izlemeLogTablo;
    private TextField    izlemeAramaField;
    private ComboBox<String> izlemeRiskFiltre;
    private ComboBox<String> izlemeIslemFiltre;
    private CheckBox     izlemeSadeceDonuk;
    private VBox         izlemeDetayKutusu;
    private String       izlemeSeciliMusteriId;
    private DatePicker   izlemeBasTarih, izlemeBitisTarih;

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
        riskDinleyiciKaydet();
    }

    private void riskDinleyiciKaydet() {
        RiskDinleyici dinleyici = (RiskOlayi olay) -> Platform.runLater(() -> {
            String mesaj = "[" + olay.getTur().name() + "] " + olay.getHesapId()
                    + " — " + olay.getMesaj();
            UITema.toast(getScene(), mesaj, false);
        });
        kontrolcu.dinleyiciEkle(dinleyici);
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
        Tab t7 = new Tab("  Müşteri İzleme  ",      musteriIzlemeSekme());

        sekmeler.getTabs().addAll(t1, t2, t3, t4, t5, t6, t7);

        sekmeler.getSelectionModel().selectedItemProperty().addListener((obs, eski, yeni) -> {
            if (yeni == t5) raporlariYenile();
            if (yeni == t6) { hesapComboGuncelle(limitHesapCombo); limitlariYenile(); riskTablosunuYenile(); }
            if (yeni == t7) izlemeMusteriListesiniYenile();
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
        Button yenileBtn  = UITema.normalButon("Listeyi Yenile");
        Button musteriSilBtn = UITema.tehlikeButon("🗑 Müşteri Sil");
        yenileBtn.setOnAction(e -> musterileriYenile());
        musteriSilBtn.setOnAction(e -> {
            int idx = musteriTablo.getSelectionModel().getSelectedIndex();
            if (idx < 0) { UITema.uyari("Uyarı", "Lütfen bir müşteri seçin."); return; }
            String mId = musteriTablo.getItems().get(idx).get(0);
            if (!UITema.onay("Müşteri Sil", mId + " numaralı müşteri ve tüm hesapları silinecek.\nOnaylıyor musunuz?")) return;
            kontrolcu.musteriSil(mId);
            musterileriYenile();
            musteriComboGuncelle(hesapMusteriCombo, false);
            musteriComboGuncelle(kulMusteriCombo, true);
            hesaplariYenile();
        });
        VBox tabloKart = tabloKartOlustur("Müşteri Listesi", musteriTablo, musteriSilBtn, yenileBtn);

        panel.getChildren().addAll(form, tabloKart);
        musterileriYenile();

        ScrollPane sp = new ScrollPane(panel);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color: transparent;");
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
        hesapTuruCombo = new ComboBox<>(FXCollections.observableArrayList(
                "VADESİZ", "VADELİ", "DÖVİZ-USD", "DÖVİZ-EUR", "DÖVİZ-GBP", "KREDİ"));
        hesapTuruCombo.setMaxWidth(Double.MAX_VALUE);
        baslangicBakiyeField = UITema.alan();
        baslangicBakiyeEtiketi = UITema.etiket("Başlangıç Bakiyesi (₺):");
        hesapTuruCombo.setOnAction(e -> {
            String tur = hesapTuruCombo.getValue();
            if ("KREDİ".equals(tur))
                baslangicBakiyeEtiketi.setText("Kredi Limiti (₺):");
            else if (tur != null && tur.startsWith("DÖVİZ"))
                baslangicBakiyeEtiketi.setText("Başlangıç Miktar (yabancı para):");
            else
                baslangicBakiyeEtiketi.setText("Başlangıç Bakiyesi (₺):");
        });
        grid.add(UITema.etiket("Müşteri:"), 0, 0);      grid.add(hesapMusteriCombo, 1, 0);
        grid.add(UITema.etiket("Hesap Türü:"), 0, 1);   grid.add(hesapTuruCombo, 1, 1);
        grid.add(baslangicBakiyeEtiketi, 0, 2);         grid.add(baslangicBakiyeField, 1, 2);
        grid.getColumnConstraints().addAll(sütunKısıt(0), sütunKısıt(1));
        Button olusturBtn = UITema.anaButon("Hesap Oluştur");
        olusturBtn.setOnAction(e -> hesapOlustur());
        HBox btnBox = new HBox(olusturBtn); btnBox.setAlignment(Pos.CENTER_RIGHT);
        form.getChildren().addAll(grid, btnBox);

        hesapTablo = UITema.tablo("Hesap No", "Müşteri", "Tür", "Bakiye", "Ek Bilgi", "Durum");
        hesapTablo.setPrefHeight(300);
        Button isaretle = UITema.tehlikeButon("⚠ Şüpheli İşaretle");
        Button kaldir   = UITema.normalButon("✓ Şüpheyi Kaldır");
        Button detayBtn = UITema.normalButon("✎ Faiz / Kur Güncelle");
        Button yenile   = UITema.normalButon("Listeyi Yenile");
        detayBtn.setOnAction(e -> hesapDetayGuncelle());
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
        Button hesapSilBtn = UITema.tehlikeButon("🗑 Hesap Sil");
        yenile.setOnAction(e -> hesaplariYenile());
        hesapSilBtn.setOnAction(e -> {
            int idx = hesapTablo.getSelectionModel().getSelectedIndex();
            if (idx < 0) { UITema.uyari("Uyarı", "Lütfen bir hesap seçin."); return; }
            String hId = hesapTablo.getItems().get(idx).get(0);
            if (!UITema.onay("Hesap Sil", hId + " numaralı hesap silinecek.\nOnaylıyor musunuz?")) return;
            kontrolcu.hesapSil(hId);
            hesaplariYenile();
            hesapComboGuncelle(pyHesapCombo); hesapComboGuncelle(pcHesapCombo); hesapComboGuncelle(trKaynakCombo);
        });
        VBox tabloKart = tabloKartOlustur("Hesap Listesi", hesapTablo, isaretle, kaldir, detayBtn, hesapSilBtn, yenile);

        panel.getChildren().addAll(form, tabloKart);
        musteriComboGuncelle(hesapMusteriCombo, false);
        hesaplariYenile();

        ScrollPane sp = new ScrollPane(panel);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color: transparent;");
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

        // Admin şifre sıfırlama kartı
        VBox sifreKart = UITema.kart("Admin: Kullanıcı Şifresi Sıfırla");
        sifreKart.setMaxWidth(Double.MAX_VALUE);
        adminSifreKulAdiField = UITema.alan();
        adminSifreKulAdiField.setPromptText("Kullanıcı adı girin...");
        adminYeniSifreField   = UITema.alan();
        adminYeniSifreField.setPromptText("Yeni şifre (en az 6 karakter)...");
        adminSifreDurumLabel  = UITema.durumLabel();
        Button sifirlaBtn = UITema.tehlikeButon("Şifreyi Sıfırla");
        sifirlaBtn.setOnAction(e -> adminSifreSifirla());
        GridPane sifreGrid = new GridPane(); sifreGrid.setHgap(12); sifreGrid.setVgap(8);
        sifreGrid.add(UITema.etiket("Kullanıcı Adı:"), 0, 0); sifreGrid.add(adminSifreKulAdiField, 1, 0);
        sifreGrid.add(UITema.etiket("Yeni Şifre:"),    0, 1); sifreGrid.add(adminYeniSifreField, 1, 1);
        sifreGrid.getColumnConstraints().addAll(sütunKısıt(0), sütunKısıt(1));
        HBox sifreBtnBox = new HBox(8, adminSifreDurumLabel, sifirlaBtn);
        sifreBtnBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(adminSifreDurumLabel, Priority.ALWAYS);
        sifreBtnBox.setPadding(new Insets(8, 0, 0, 0));
        sifreKart.getChildren().addAll(sifreGrid, sifreBtnBox);

        // Müşteri profil düzenleme kartı
        VBox profilKart = UITema.kart("Müşteri Profili Düzenle");
        profilKart.setMaxWidth(Double.MAX_VALUE);
        kulMusteriCombo = new ComboBox<>(); kulMusteriCombo.setMaxWidth(Double.MAX_VALUE);
        kulAdiField    = UITema.alan(); kulAdiField.setPromptText("Yeni ad soyad...");
        kulSifreField  = UITema.alan(); kulSifreField.setPromptText("Yeni e-posta...");
        GridPane profilGrid = new GridPane(); profilGrid.setHgap(12); profilGrid.setVgap(8);
        profilGrid.add(UITema.etiket("Müşteri:"),  0, 0); profilGrid.add(kulMusteriCombo, 1, 0);
        profilGrid.add(UITema.etiket("Ad Soyad:"), 0, 1); profilGrid.add(kulAdiField, 1, 1);
        profilGrid.add(UITema.etiket("E-posta:"),  0, 2); profilGrid.add(kulSifreField, 1, 2);
        profilGrid.getColumnConstraints().addAll(sütunKısıt(0), sütunKısıt(1));
        Button profilGuncelleBtn = UITema.anaButon("Profili Güncelle");
        profilGuncelleBtn.setOnAction(e -> musteriProfilGuncelle());
        HBox profilBtnBox = new HBox(profilGuncelleBtn); profilBtnBox.setAlignment(Pos.CENTER_RIGHT);
        profilBtnBox.setPadding(new Insets(8, 0, 0, 0));
        kulMusteriCombo.setOnAction(ev -> {
            String secim = kulMusteriCombo.getValue();
            if (secim == null || secim.startsWith("–")) return;
            String mId = secim.split(" – ")[0].trim();
            Customer m = kontrolcu.getMusteri(mId);
            if (m != null) { kulAdiField.setText(m.getAd()); kulSifreField.setText(m.getEposta()); }
        });
        profilKart.getChildren().addAll(profilGrid, profilBtnBox);

        HBox formRow = new HBox(12, sifreKart, profilKart);
        formRow.setFillHeight(false);
        HBox.setHgrow(sifreKart, Priority.ALWAYS);
        HBox.setHgrow(profilKart, Priority.ALWAYS);

        // Kullanıcı listesi tablosu
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
        panel.getChildren().addAll(formRow, tabloKart);
        kullanicilariYenile();

        ScrollPane sp = new ScrollPane(panel);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color: transparent;");
        return sp;
    }

    private void adminSifreSifirla() {
        String kulAdi  = adminSifreKulAdiField.getText().trim();
        String yeniSif = adminYeniSifreField.getText().trim();
        if (kulAdi.isEmpty() || yeniSif.isEmpty()) {
            UITema.durumGoster(adminSifreDurumLabel, "Kullanıcı adı ve yeni şifre boş olamaz.", false); return;
        }
        if (yeniSif.length() < 6) {
            UITema.durumGoster(adminSifreDurumLabel, "Şifre en az 6 karakter olmalıdır.", false); return;
        }
        if (kontrolcu.sifreSifirla(kulAdi, yeniSif)) {
            kontrolcu.durumKaydet("banka_durumu.dat");
            UITema.durumGoster(adminSifreDurumLabel, kulAdi + " şifresi sıfırlandı.", true);
            adminSifreKulAdiField.clear(); adminYeniSifreField.clear();
        } else {
            UITema.durumGoster(adminSifreDurumLabel, "Kullanıcı bulunamadı: " + kulAdi, false);
        }
    }

    private void musteriProfilGuncelle() {
        String secim = kulMusteriCombo.getValue();
        if (secim == null || secim.startsWith("–")) { UITema.uyari("Uyarı", "Müşteri seçiniz."); return; }
        String mId = secim.split(" – ")[0].trim();
        Customer m = kontrolcu.getMusteri(mId);
        if (m == null) return;
        String yeniAd = kulAdiField.getText().trim();
        String yeniEp = kulSifreField.getText().trim();
        if (yeniAd.isEmpty() || yeniEp.isEmpty()) { UITema.uyari("Uyarı", "Ad ve e-posta boş olamaz."); return; }
        m.setAd(yeniAd); m.setEposta(yeniEp);
        musterileriYenile();
        UITema.bilgi("Başarılı", mId + " profili güncellendi.\nAd: " + yeniAd + "\nE-posta: " + yeniEp);
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

        Button raporBtn   = UITema.normalButon("Raporu Güncelle");
        Button faizBtn    = UITema.normalButon("Vadeli Hesaplara Faiz Uygula");
        Button krediBtn   = UITema.normalButon("Kredi Hesaplarına Faiz Uygula");
        Button botBtn     = UITema.normalButon("🤖 Bot Simülasyonu (30 İşlem)");
        Button demoBtn    = UITema.normalButon("📦 Demo Veri Yükle");
        Button kaydetBtn  = UITema.anaButon("Durumu Kaydet");
        sonuclarBtn = UITema.normalButon("📋 30 Adet Deneme Sonuçları");
        sonuclarBtn.setDisable(true);
        raporBtn.setOnAction(e -> raporlariYenile());
        faizBtn.setOnAction(e -> {
            int n = kontrolcu.faizUygula();
            raporlariYenile();
            UITema.bilgi("Faiz Uygulandı", n + " vadeli hesaba faiz uygulandı.");
        });
        krediBtn.setOnAction(e -> {
            int n = kontrolcu.krediAylikFaizUygula();
            raporlariYenile();
            UITema.bilgi("Kredi Faizi", n + " kredi hesabına aylık faiz uygulandı.");
        });
        botBtn.setOnAction(e -> botSimulasyonuCalistir(botBtn));
        sonuclarBtn.setOnAction(e -> botSonuclarGoster());
        demoBtn.setOnAction(e -> demoVeriYukle());
        kaydetBtn.setOnAction(e -> {
            kontrolcu.durumKaydet("banka_durumu.dat");
            UITema.bilgi("Bilgi", "Durum başarıyla kaydedildi.");
        });
        HBox butonRow = new HBox(8, raporBtn, faizBtn, krediBtn, botBtn, sonuclarBtn, demoBtn, kaydetBtn);
        butonRow.setAlignment(Pos.CENTER_RIGHT);

        panel.getChildren().addAll(istatRow, charts, butonRow);
        raporlariYenile();

        ScrollPane sp = new ScrollPane(panel);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color: transparent;");
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
        riskTablo.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        if (riskTablo.getColumns().size() >= 5) {
            riskTablo.getColumns().get(0).setPrefWidth(90);
            riskTablo.getColumns().get(1).setPrefWidth(120);
            riskTablo.getColumns().get(2).setPrefWidth(340);
            riskTablo.getColumns().get(3).setPrefWidth(110);
            riskTablo.getColumns().get(4).setPrefWidth(160);
        }
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
            kontrolcu.durumKaydet("banka_durumu.dat");
            baslangicBakiyeField.clear();
            hesaplariYenile();
            hesapComboGuncelle(pyHesapCombo); hesapComboGuncelle(pcHesapCombo); hesapComboGuncelle(trKaynakCombo);
            UITema.bilgi("Başarılı", "Hesap oluşturuldu.\nNo: " + hesap.getHesapId() + "\nBakiye: " + tl(hesap.getBakiye()));
        } catch (NumberFormatException ex) { UITema.hata("Hata", "Geçersiz bakiye değeri."); }
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
            } else UITema.durumGoster(islemDurumLabel, "Başarısız: Hesap şüpheli olarak işaretlenmiş.", false);
        } catch (YetersizBakiyeException e) {
            UITema.durumGoster(islemDurumLabel, "Yetersiz bakiye! Mevcut: " + tl(e.getMevcutBakiye()), false);
        } catch (RiskLimitiAsildiException e) {
            UITema.durumGoster(islemDurumLabel, "Risk limiti aşıldı: " + e.getMessage(), false);
        } catch (NumberFormatException e) {
            UITema.durumGoster(islemDurumLabel, "Geçersiz miktar.", false);
        }
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
            } else UITema.durumGoster(islemDurumLabel, "Transfer başarısız: Şüpheli hesap veya hedef bulunamadı.", false);
        } catch (YetersizBakiyeException e) {
            UITema.durumGoster(islemDurumLabel, "Yetersiz bakiye! Mevcut: " + tl(e.getMevcutBakiye()), false);
        } catch (RiskLimitiAsildiException e) {
            UITema.durumGoster(islemDurumLabel, "Risk limiti aşıldı: " + e.getMessage(), false);
        } catch (NumberFormatException e) {
            UITema.durumGoster(islemDurumLabel, "Geçersiz miktar.", false);
        }
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
        for (Customer m : kontrolcu.tumMusteriler()) {
            String ad = kontrolcu.isDemoMusteri(m.getMusteriId()) ? "[DEMO] " + m.getAd() : m.getAd();
            UITema.satirEkle(musteriTablo, m.getMusteriId(), ad, m.getEposta(), String.valueOf(m.getHesaplar().size()));
        }
    }

    private void hesaplariYenile() {
        if (hesapTablo == null) return;
        hesapTablo.getItems().clear();
        for (Account h : kontrolcu.tumHesaplar()) {
            Customer m  = kontrolcu.getMusteri(h.getSahibiId());
            String durum = kontrolcu.suphelihMi(h.getHesapId()) ? "ŞÜPHELİ" : "Normal";
            UITema.satirEkle(hesapTablo, h.getHesapId(),
                m != null ? m.getAd() : h.getSahibiId(),
                h.getHesapTuru(), bakiyeStr(h), MusteriPaneli.hesapEkBilgi(h), durum);
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
        int vadesiz = 0, vadeli = 0, doviz = 0, kredi = 0, supheli = 0;
        double toplam = 0;
        for (Account h : hesaplar) {
            // TL cinsinden toplam (döviz TL karşılığı dahil, kredi borcu hariç)
            if (h instanceof DovizHesabi) toplam += ((DovizHesabi) h).getBakiyeTL();
            else if (h.getBakiye() > 0)   toplam += h.getBakiye();
            // Tür sayımı
            if      ("VADESİZ".equals(h.getHesapTuru()))      vadesiz++;
            else if ("VADELİ".equals(h.getHesapTuru()))       vadeli++;
            else if (h.getHesapTuru().startsWith("DÖVİZ"))    doviz++;
            else if ("KREDİ".equals(h.getHesapTuru()))        kredi++;
            if (kontrolcu.suphelihMi(h.getHesapId())) supheli++;
        }
        if (istatMusteriLabel != null) {
            istatMusteriLabel.setText(String.valueOf(kontrolcu.tumMusteriler().size()));
            istatHesapLabel.setText(String.valueOf(hesaplar.size()));
            istatBakiyeLabel.setText(tl(toplam));
            istatSupheliLabel.setText(String.valueOf(supheli));
            istatSupheliLabel.setStyle("-fx-text-fill: " + (supheli > 0 ? "#af1414" : "#146418") + "; -fx-font-weight: bold; -fx-font-size: 22;");
        }
        // PieChart — tüm hesap türleri
        if (hesapTuruPieChart != null) {
            hesapTuruPieChart.getData().clear();
            if (vadesiz > 0) hesapTuruPieChart.getData().add(new PieChart.Data("Vadesiz (" + vadesiz + ")", vadesiz));
            if (vadeli  > 0) hesapTuruPieChart.getData().add(new PieChart.Data("Vadeli ("  + vadeli  + ")", vadeli));
            if (doviz   > 0) hesapTuruPieChart.getData().add(new PieChart.Data("Döviz ("   + doviz   + ")", doviz));
            if (kredi   > 0) hesapTuruPieChart.getData().add(new PieChart.Data("Kredi ("   + kredi   + ")", kredi));
        }
        // BarChart – Top 5 (sıfırdan büyüyen animasyonla)
        if (bakiyeBarChart != null) {
            bakiyeBarChart.getData().clear();
            XYChart.Series<String, Number> seri = new XYChart.Series<>();
            List<Account> top5 = hesaplar.stream()
                .sorted((a, b) -> Double.compare(b.getBakiye(), a.getBakiye()))
                .limit(5)
                .collect(java.util.stream.Collectors.toList());
            for (Account h : top5)
                seri.getData().add(new XYChart.Data<>(h.getHesapId(), 0));
            bakiyeBarChart.getData().add(seri);

            // Her bar sıfırdan gerçek değere büyür
            Timeline tl = new Timeline();
            for (int i = 0; i < seri.getData().size(); i++) {
                XYChart.Data<String, Number> nokta = seri.getData().get(i);
                double hedefBakiye = top5.get(i).getBakiye();
                tl.getKeyFrames().add(new KeyFrame(
                    Duration.millis(600 + i * 120),
                    new KeyValue(nokta.YValueProperty(), hedefBakiye)
                ));
            }
            tl.play();
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
            combo.getItems().add(h.getHesapId() + " – " + h.getHesapTuru() + " – " + (m != null ? m.getAd() : "") + " – " + bakiyeStr(h));
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

    private static String bakiyeStr(Account h) {
        if (h instanceof DovizHesabi) {
            DovizHesabi d = (DovizHesabi) h;
            return String.format(Locale.US, "%,.2f %s", h.getBakiye(), d.getParaBirimiSimgesi());
        }
        return tl(h.getBakiye());
    }

    /** Seçili hesabın faiz oranı (VADELİ) veya döviz kurunu (DÖVİZ) günceller. */
    private void hesapDetayGuncelle() {
        int idx = hesapTablo.getSelectionModel().getSelectedIndex();
        if (idx < 0) { UITema.uyari("Uyarı", "Lütfen bir hesap seçin."); return; }
        String hesapId = hesapTablo.getItems().get(idx).get(0);
        Account h = kontrolcu.getHesap(hesapId);
        if (h == null) return;

        if (h instanceof SavingsAccount) {
            double mevcutFaiz = ((SavingsAccount) h).getFaizOrani() * 100;
            TextInputDialog dialog = new TextInputDialog(String.format(Locale.US, "%.1f", mevcutFaiz));
            dialog.setTitle("Faiz Oranı Güncelle");
            dialog.setHeaderText(hesapId + "  –  Vadeli Hesap");
            dialog.setContentText("Yeni yıllık faiz oranı (%):");
            Optional<String> sonuc = dialog.showAndWait();
            sonuc.ifPresent(val -> {
                try {
                    double yeniOran = Double.parseDouble(val.trim().replace(",", "."));
                    if (yeniOran < 0 || yeniOran > 100) throw new NumberFormatException();
                    kontrolcu.faizOraniGuncelle(hesapId, yeniOran / 100.0);
                    hesaplariYenile();
                    UITema.bilgi("Başarılı",
                        hesapId + " vadeli hesabının faiz oranı güncellendi: %" +
                        String.format(Locale.US, "%.1f", yeniOran));
                } catch (NumberFormatException ex) {
                    UITema.hata("Hata", "Geçerli bir oran giriniz (0–100 arası).");
                }
            });
        } else if (h instanceof DovizHesabi) {
            DovizHesabi d = (DovizHesabi) h;
            TextInputDialog dialog = new TextInputDialog(String.format(Locale.US, "%.2f", d.getDovizKuru()));
            dialog.setTitle("Döviz Kuru Güncelle");
            dialog.setHeaderText(hesapId + "  –  " + h.getHesapTuru());
            dialog.setContentText("Yeni kur (1 " + d.getParaBirimi().name() + " = ? ₺):");
            Optional<String> sonuc = dialog.showAndWait();
            sonuc.ifPresent(val -> {
                try {
                    double yeniKur = Double.parseDouble(val.trim().replace(",", "."));
                    if (yeniKur <= 0) throw new NumberFormatException();
                    kontrolcu.dovizKuruGuncelle(hesapId, yeniKur);
                    hesaplariYenile();
                    UITema.bilgi("Başarılı",
                        "Kur güncellendi: 1 " + d.getParaBirimi().name() + " = " +
                        String.format(Locale.US, "%.2f", yeniKur) + " ₺");
                } catch (NumberFormatException ex) {
                    UITema.hata("Hata", "Geçerli bir kur değeri giriniz (sıfırdan büyük).");
                }
            });
        } else if (h instanceof KrediHesabi) {
            KrediHesabi k = (KrediHesabi) h;
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Kredi Hesabı Güncelle");
            dialog.setHeaderText(hesapId + "  –  Kredi Hesabı");
            GridPane g = new GridPane(); g.setHgap(10); g.setVgap(10);
            g.setPadding(new Insets(10));
            TextField limitField = new TextField(String.format(Locale.US, "%.0f", k.getKrediLimiti()));
            TextField faizField  = new TextField(String.format(Locale.US, "%.1f", k.getFaizOrani() * 100));
            g.add(new Label("Kredi Limiti (₺):"), 0, 0); g.add(limitField, 1, 0);
            g.add(new Label("Aylık Faiz (%):"),   0, 1); g.add(faizField,  1, 1);
            dialog.getDialogPane().setContent(g);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            Optional<ButtonType> sonuc = dialog.showAndWait();
            if (sonuc.isPresent() && sonuc.get() == ButtonType.OK) {
                try {
                    double yeniLimit = Double.parseDouble(limitField.getText().trim().replace(",", "."));
                    double yeniFaiz  = Double.parseDouble(faizField.getText().trim().replace(",", "."));
                    if (yeniLimit <= 0 || yeniFaiz < 0 || yeniFaiz > 100) throw new NumberFormatException();
                    kontrolcu.krediLimitiGuncelle(hesapId, yeniLimit);
                    kontrolcu.krediFaizOraniGuncelle(hesapId, yeniFaiz / 100.0);
                    hesaplariYenile();
                    UITema.bilgi("Başarılı", "Kredi hesabı güncellendi.\nYeni limit: " +
                        tl(yeniLimit) + "  |  Aylık faiz: %" + String.format(Locale.US, "%.1f", yeniFaiz));
                } catch (NumberFormatException ex) {
                    UITema.hata("Hata", "Geçerli değer giriniz (limit > 0, faiz 0-100).");
                }
            }
        } else {
            UITema.bilgi("Bilgi",
                "Bu hesap türü için güncellenebilir parametre bulunmuyor.\nHesap Türü: " + h.getHesapTuru());
        }
    }

    private void botSimulasyonuCalistir(Button botBtn) {
        java.util.List<Account> hesaplar = kontrolcu.tumHesaplar();
        if (hesaplar.isEmpty()) {
            UITema.uyari("Uyarı", "Bot simülasyonu için hesap gerekli. Demo Veri Yükle'yi deneyin.");
            return;
        }
        botBtn.setDisable(true);
        botBtn.setText("⏳ Simülasyon Çalışıyor...");
        sonuclarBtn.setDisable(true);
        botSimLoglar = null;

        kontrolcu.durumKaydet("banka_bot_oncesi.dat");
        kontrolcu.getKaydedici().setSessiz(true);
        kontrolcu.botModuBaslat();
        new Thread(() -> {
            java.util.Random rand = new java.util.Random(42L);
            java.util.List<Account> liste = new java.util.ArrayList<>(hesaplar);
            java.util.List<String> L = new java.util.ArrayList<>();
            int basarili = 0, basarisiz = 0;

            // Test hesabı: en düşük riskli vadesiz hesap seçilir (donmamış olmalı)
            Account testH = liste.stream()
                .filter(h -> !(h instanceof KrediHesabi) && !(h instanceof DovizHesabi)
                             && !kontrolcu.suphelihMi(h.getHesapId()))
                .min((a, b) -> Integer.compare(
                         kontrolcu.getRiskSkoru(a.getHesapId()),
                         kontrolcu.getRiskSkoru(b.getHesapId())))
                .orElse(liste.get(0));
            kontrolcu.limitGuncelle(testH.getHesapId(), new HesapLimiti(500_000, 500_000, 150_000, 150_000));
            kontrolcu.paraYatir(testH.getHesapId(), 300_000);
            Customer testM = kontrolcu.getMusteri(testH.getSahibiId());
            String testAd = testM != null ? testM.getAd() : testH.getHesapId();
            java.util.List<Account> diger = new java.util.ArrayList<>();
            for (Account h : liste) if (!h.getHesapId().equals(testH.getHesapId())) diger.add(h);

            L.add("HEADER:BOT SİMÜLASYONU — RİSK SEVİYELERİ YOLCULUĞU (30 İŞLEM)");
            L.add("INFO:Test Hesabı  :  " + testH.getHesapId() + " — " + testAd + "  │  Limit: 500,000 ₺/gün  │  Başlangıç bakiyesi: 300,000+ ₺");
            double riskEsigi = kontrolcu.getYuksekRiskEsigi();
            String riskEsigiStr = String.format(Locale.US, "%,.0f", riskEsigi);
            L.add("INFO:Risk Puanları:  Büyük işlem (≥" + riskEsigiStr + " ₺) → +20   Gece modu (≥10K) → +15   Ani düşüş (≥%95,min 5K) → +15   Velocity (≥10/5dk, tüm hesaplar) → +30");
            L.add("INFO:Risk Eşikleri:  🟡 İZLENİYOR ≥31   🟠 RİSKLİ ≥61   🔴 ŞÜPHELİ/DONDURULDU ≥86");
            L.add("INFO:");

            // ── AŞAMA 1: GÜVENLİ (işlem 1-6) ─────────────────────────────────
            L.add("PHASE:▌ AŞAMA 1 — 🟢 GÜVENLİ  (İşlem 1-6)  Küçük ve normal işlemler — hiçbir risk tetiklenmiyor");
            double[] kM = {1200, 2500, 3000, 800, 1500, 2000};
            String[] kT = {"YATIRMA","YATIRMA","ÇEKİM","YATIRMA","ÇEKİM","TRANSFER"};
            for (int i = 0; i < 6; i++) {
                int no = i + 1;
                double m = kM[i]; String tip = kT[i];
                double bOnce = testH.getBakiye();
                int sBefore = kontrolcu.getRiskSkoru(testH.getHesapId());
                try {
                    boolean ok = false;
                    if ("YATIRMA".equals(tip)) ok = kontrolcu.paraYatir(testH.getHesapId(), m);
                    else if ("ÇEKİM".equals(tip)) ok = kontrolcu.paraCek(testH.getHesapId(), m);
                    else if (!diger.isEmpty()) {
                        Account hd = diger.stream().filter(h -> !kontrolcu.suphelihMi(h.getHesapId())).findFirst().orElse(null);
                        if (hd != null) ok = kontrolcu.transferYap(testH.getHesapId(), hd.getHesapId(), m);
                    }
                    if (ok) basarili++; else basarisiz++;
                    int sAfter = kontrolcu.getRiskSkoru(testH.getHesapId());
                    L.add("TXNOK:#" + String.format("%02d", no) + "  " + tip + "  │  " + testAd + "  [" + testH.getHesapId() + "]");
                    L.add("TXNAMT:   Tutar: " + String.format(Locale.US, "%,.0f ₺", m)
                        + "    │    Bakiye: " + String.format(Locale.US, "%,.0f ₺", bOnce)
                        + " → " + String.format(Locale.US, "%,.0f ₺", testH.getBakiye()));
                    L.add("TXNRISK:  ✅  Risk faktörü tetiklenmedi — tutar " + riskEsigiStr + " ₺ eşiğinin çok altında");
                    L.add("TXNSCORE: Skor: " + sBefore + " → " + sAfter + "   " + riskSeviyeEmoji(sAfter));
                } catch (Exception e) {
                    basarisiz++;
                    L.add("TXNFAIL:#" + String.format("%02d", no) + "  " + tip + "  │  " + testAd + "  [" + testH.getHesapId() + "]");
                    L.add("TXNAMT:   Tutar: " + String.format(Locale.US, "%,.0f ₺", m));
                    L.add("TXNRISK:  ❌  Hata: " + e.getMessage());
                    L.add("TXNSCORE: Skor değişmedi: " + kontrolcu.getRiskSkoru(testH.getHesapId()) + "/100");
                }
                L.add("TXNSEP:");
                try { Thread.sleep(20); } catch (InterruptedException ig) {}
            }
            L.add("SCORE:▸ Aşama 1 tamamlandı.  Skor: " + kontrolcu.getRiskSkoru(testH.getHesapId()) + "/100 — " + riskSeviyeEmoji(kontrolcu.getRiskSkoru(testH.getHesapId())));
            L.add("INFO:");

            // ── AŞAMA 2: Büyük İşlemler — Risk Tırmanması (işlem 7-14) ─────────
            L.add("PHASE:▌ AŞAMA 2 — İZLENİYOR→RİSKLİ→ŞÜPHELİ  (İşlem 7-14)  Büyük tutarlar → her biri +20 puan ekliyor");
            double[] bM = {55000, 62000, 57000, 58000, 65000, 70000, 53000, 68000};
            String[] bT = {"YATIRMA","YATIRMA","ÇEKİM","YATIRMA","YATIRMA","ÇEKİM","YATIRMA","YATIRMA"};
            for (int i = 0; i < 8; i++) {
                int no = i + 7;
                if (kontrolcu.suphelihMi(testH.getHesapId())) {
                    L.add("TXNFROZEN:#" + String.format("%02d", no) + "  " + bT[i] + "  │  " + testAd + "  [" + testH.getHesapId() + "]");
                    L.add("TXNAMT:   Planlanmış tutar: " + String.format(Locale.US, "%,.0f ₺", bM[i]));
                    L.add("TXNRISK:  🔴  Hesap ŞÜPHELİ/DONDURULMUŞ — tüm işlemler otomatik engellendi");
                    L.add("TXNSCORE: " + riskSeviyeEmoji(kontrolcu.getRiskSkoru(testH.getHesapId())) + "  │  Skor: " + kontrolcu.getRiskSkoru(testH.getHesapId()) + "/100");
                    L.add("TXNSEP:");
                    basarisiz++;
                    try { Thread.sleep(15); } catch (InterruptedException ig) {}
                    continue;
                }
                double m = bM[i]; String tip = bT[i];
                double bOnce = testH.getBakiye();
                int sBefore = kontrolcu.getRiskSkoru(testH.getHesapId());
                try {
                    boolean ok = false;
                    if ("YATIRMA".equals(tip)) ok = kontrolcu.paraYatir(testH.getHesapId(), m);
                    else ok = kontrolcu.paraCek(testH.getHesapId(), m);
                    if (ok) basarili++; else basarisiz++;
                    int sAfter = kontrolcu.getRiskSkoru(testH.getHesapId());
                    boolean dondu = kontrolcu.suphelihMi(testH.getHesapId());
                    String hTag = dondu ? "TXNFROZEN:" : (sAfter >= 61 ? "TXNWARN:" : "TXNOK:");
                    String sfx = dondu ? "  ← 🔴 HESAP OTOMATİK DONDURULDU!" :
                        (sAfter >= 61 && sBefore < 61 ? "  ← 🟠 RİSKLİ seviyesine girdi!" :
                        (sAfter >= 31 && sBefore < 31 ? "  ← 🟡 İZLENİYOR seviyesine girdi!" : ""));
                    L.add(hTag + "#" + String.format("%02d", no) + "  " + tip + "  │  " + testAd + "  [" + testH.getHesapId() + "]" + sfx);
                    L.add("TXNAMT:   Tutar: " + String.format(Locale.US, "%,.0f ₺", m)
                        + "    │    Bakiye: " + String.format(Locale.US, "%,.0f ₺", bOnce)
                        + " → " + String.format(Locale.US, "%,.0f ₺", testH.getBakiye()));
                    boolean anyRisk = false;
                    if (m >= riskEsigi) {
                        L.add("TXNRISK:  ⚡  Büyük tutarlı işlem  (" + String.format(Locale.US, "%,.0f", m) + " ₺  ≥  " + riskEsigiStr + " ₺ eşiği)  →  +20 puan");
                        anyRisk = true;
                    }
                    if ("ÇEKİM".equals(tip) && bOnce > 0 && m >= 5_000 && m / bOnce >= 0.95) {
                        L.add("TXNRISK:  ⚡  Ani bakiye düşüşü — bakiyenin %95'inden fazlası tek seferde çekildi  →  +15 puan");
                        anyRisk = true;
                    }
                    if (kontrolcu.kisaVadeliCokIslemMiMusteri(testH.getSahibiId())) {
                        long kSayi = kontrolcu.kisaVadeliMusteriIslemSayisi(testH.getSahibiId());
                        L.add("TXNRISK:  ⚡  Velocity uyarısı!  Son 5 dakikada tüm hesaplarda " + kSayi + " işlem yapıldı  (eşik: ≥10)  →  +30 puan");
                        anyRisk = true;
                    }
                    if (dondu) {
                        L.add("TXNRISK:  🔴  TOPLAM SKOR " + sAfter + "/100  — ŞÜPHELİ eşiğini (≥86) aştı  →  HESAP OTOMATİK DONDURULDU!");
                    }
                    if (!anyRisk && !dondu) L.add("TXNRISK:  ✅  Risk faktörü tetiklenmedi");
                    int df = sAfter - sBefore;
                    L.add("TXNSCORE: Skor: " + sBefore + " → " + sAfter + (df > 0 ? "  (+" + df + " puan)" : "") + "   " + riskSeviyeEmoji(sAfter));
                } catch (model.YetersizBakiyeException e) {
                    basarisiz++;
                    L.add("TXNFAIL:#" + String.format("%02d", no) + "  " + tip + "  │  " + testAd + "  [" + testH.getHesapId() + "]");
                    L.add("TXNAMT:   Tutar: " + String.format(Locale.US, "%,.0f ₺", m) + "    │    Mevcut bakiye: " + String.format(Locale.US, "%,.0f ₺", e.getMevcutBakiye()));
                    L.add("TXNRISK:  ❌  Yetersiz bakiye — işlem reddedildi, skor değişmedi");
                    L.add("TXNSCORE: Skor değişmedi: " + kontrolcu.getRiskSkoru(testH.getHesapId()) + "/100   " + riskSeviyeEmoji(kontrolcu.getRiskSkoru(testH.getHesapId())));
                } catch (model.RiskLimitiAsildiException e) {
                    basarisiz++;
                    L.add("TXNFAIL:#" + String.format("%02d", no) + "  " + tip + "  │  " + testAd + "  [" + testH.getHesapId() + "]");
                    L.add("TXNAMT:   Tutar: " + String.format(Locale.US, "%,.0f ₺", m));
                    L.add("TXNRISK:  ❌  Risk limiti aşıldı: " + e.getMessage());
                    L.add("TXNSCORE: Skor değişmedi: " + kontrolcu.getRiskSkoru(testH.getHesapId()) + "/100");
                } catch (Exception e) {
                    basarisiz++;
                    L.add("TXNFAIL:#" + String.format("%02d", no) + "  " + tip + "  │  " + testAd);
                    L.add("TXNRISK:  ❌  " + e.getMessage());
                    L.add("TXNSCORE: Skor: " + kontrolcu.getRiskSkoru(testH.getHesapId()) + "/100");
                }
                L.add("TXNSEP:");
                try { Thread.sleep(20); } catch (InterruptedException ig) {}
            }
            L.add("SCORE:▸ Aşama 2 tamamlandı.  Test hesabı: Skor " + kontrolcu.getRiskSkoru(testH.getHesapId()) + "/100 — " + riskSeviyeEmoji(kontrolcu.getRiskSkoru(testH.getHesapId())));
            L.add("INFO:");

            // ── AŞAMA 3: Gece Modu (işlem 15-22) ────────────────────────────────
            kontrolcu.simuleGeceModuAktifEt(true);
            L.add("PHASE:▌ AŞAMA 3 — 🌙 GECE MODU  (İşlem 15-22)  01:00-06:00 simülasyonu — ≥10,000 ₺ işlemlere +15 puan ek");
            java.util.List<Account> geceH = new java.util.ArrayList<>();
            for (Account h : liste) {
                if (!kontrolcu.suphelihMi(h.getHesapId()) && !(h instanceof KrediHesabi) && !(h instanceof DovizHesabi))
                    geceH.add(h);
            }
            if (geceH.isEmpty()) geceH.addAll(liste);
            double[] gM = {15000, 18000, 12000, 22000, 16000, 25000, 14000, 20000};
            for (int i = 0; i < 8; i++) {
                int no = i + 15;
                Account gh = geceH.get(i % geceH.size());
                if (gh instanceof KrediHesabi || gh instanceof DovizHesabi) gh = geceH.get(0);
                Customer gc = kontrolcu.getMusteri(gh.getSahibiId());
                String gAd = gc != null ? gc.getAd() : gh.getSahibiId();
                double m = gM[i];
                double bOnce = gh.getBakiye();
                int sBefore = kontrolcu.getRiskSkoru(gh.getHesapId());
                boolean supheliydi = kontrolcu.suphelihMi(gh.getHesapId());
                if (supheliydi) {
                    L.add("TXNFROZEN:#" + String.format("%02d", no) + "  🌙 YATIRMA  │  " + gAd + "  [" + gh.getHesapId() + "]");
                    L.add("TXNAMT:   Tutar: " + String.format(Locale.US, "%,.0f ₺", m));
                    L.add("TXNRISK:  🔴  Hesap ŞÜPHELİ/DONDURULMUŞ — gece de olsa işlem engellendi");
                    L.add("TXNSCORE: Skor: " + sBefore + "/100  " + riskSeviyeEmoji(sBefore));
                    L.add("TXNSEP:");
                    basarisiz++;
                    try { Thread.sleep(15); } catch (InterruptedException ig) {}
                    continue;
                }
                try {
                    boolean ok = kontrolcu.paraYatir(gh.getHesapId(), m);
                    if (ok) basarili++; else basarisiz++;
                    int sAfter = kontrolcu.getRiskSkoru(gh.getHesapId());
                    boolean dondu = kontrolcu.suphelihMi(gh.getHesapId());
                    String hTag = dondu ? "TXNFROZEN:" : (sAfter >= 31 ? "TXNWARN:" : "TXNOK:");
                    String sfx = dondu ? "  ← 🔴 DONDURULDU!" :
                        (sAfter >= 61 && sBefore < 61 ? "  ← 🟠 RİSKLİ!" :
                        (sAfter >= 31 && sBefore < 31 ? "  ← 🟡 İZLENİYOR!" : ""));
                    L.add(hTag + "#" + String.format("%02d", no) + "  🌙 YATIRMA  │  " + gAd + "  [" + gh.getHesapId() + "]" + sfx);
                    L.add("TXNAMT:   Tutar: " + String.format(Locale.US, "%,.0f ₺", m)
                        + "    │    Bakiye: " + String.format(Locale.US, "%,.0f ₺", bOnce)
                        + " → " + String.format(Locale.US, "%,.0f ₺", gh.getBakiye()));
                    boolean anyRisk = false;
                    if (m >= 10_000) {
                        L.add("TXNRISK:  🌙  Gece saati yüksek tutarlı işlem  (01:00-06:00, tutar " + String.format(Locale.US, "%,.0f", m) + " ₺  ≥  10,000 ₺)  →  +15 puan");
                        anyRisk = true;
                    }
                    if (m >= riskEsigi) {
                        L.add("TXNRISK:  ⚡  Aynı zamanda büyük tutarlı işlem (≥" + riskEsigiStr + " ₺)  →  +20 puan ek");
                        anyRisk = true;
                    }
                    if (kontrolcu.kisaVadeliCokIslemMiMusteri(gh.getSahibiId())) {
                        L.add("TXNRISK:  ⚡  Velocity: Son 5 dk'da " + kontrolcu.kisaVadeliMusteriIslemSayisi(gh.getSahibiId()) + " işlem (tüm hesaplar, eşik ≥10)  →  +30 puan");
                        anyRisk = true;
                    }
                    if (dondu) L.add("TXNRISK:  🔴  Skor eşiği aşıldı (≥86)  →  Hesap otomatik donduruldu!");
                    if (!anyRisk && !dondu) L.add("TXNRISK:  ✅  Risk faktörü tetiklenmedi");
                    int df = sAfter - sBefore;
                    L.add("TXNSCORE: Skor: " + sBefore + " → " + sAfter + (df > 0 ? "  (+" + df + " puan)" : "") + "   " + riskSeviyeEmoji(sAfter));
                } catch (Exception e) {
                    basarisiz++;
                    L.add("TXNFAIL:#" + String.format("%02d", no) + "  🌙 YATIRMA  │  " + gAd + "  [" + gh.getHesapId() + "]");
                    L.add("TXNRISK:  ❌  " + e.getMessage());
                    L.add("TXNSCORE: Skor: " + kontrolcu.getRiskSkoru(gh.getHesapId()) + "/100");
                }
                L.add("TXNSEP:");
                try { Thread.sleep(20); } catch (InterruptedException ig) {}
            }
            kontrolcu.simuleGeceModuAktifEt(false);
            L.add("SCORE:▸ Aşama 3 tamamlandı.  🌙 Gece modu kapatıldı.");
            L.add("INFO:");

            // ── AŞAMA 4: Çeşitli İşlemler (işlem 23-30) ─────────────────────────
            L.add("PHASE:▌ AŞAMA 4 — ÇEŞİTLİ  (İşlem 23-30)  Tüm hesaplar — transfer, çekim, yatırma karışımı");
            for (int i = 0; i < 8; i++) {
                int no = i + 23;
                Account h = liste.get(rand.nextInt(liste.size()));
                if (h instanceof KrediHesabi) h = liste.get(0);
                Customer mc = kontrolcu.getMusteri(h.getSahibiId());
                String mAd = mc != null ? mc.getAd() : h.getSahibiId();
                double m = 1000 + rand.nextInt(12000);
                double bOnce = h.getBakiye();
                int sBefore = kontrolcu.getRiskSkoru(h.getHesapId());
                if (kontrolcu.suphelihMi(h.getHesapId())) {
                    L.add("TXNFROZEN:#" + String.format("%02d", no) + "  YATIRMA  │  " + mAd + "  [" + h.getHesapId() + "]");
                    L.add("TXNAMT:   Planlanmış tutar: " + String.format(Locale.US, "%,.0f ₺", m));
                    L.add("TXNRISK:  🔴  Hesap ŞÜPHELİ/DONDURULMUŞ — tüm işlemler engellendi");
                    L.add("TXNSCORE: Skor: " + sBefore + "/100  " + riskSeviyeEmoji(sBefore));
                    L.add("TXNSEP:");
                    basarisiz++;
                    try { Thread.sleep(15); } catch (InterruptedException ig) {}
                    continue;
                }
                String tipStr = "YATIRMA";
                try {
                    boolean ok = false;
                    int tipIdx = rand.nextInt(3);
                    if (tipIdx == 0) {
                        tipStr = "YATIRMA"; ok = kontrolcu.paraYatir(h.getHesapId(), m);
                    } else if (tipIdx == 1 && h.getBakiye() >= m) {
                        tipStr = "ÇEKİM"; ok = kontrolcu.paraCek(h.getHesapId(), m);
                    } else {
                        Account hd = null;
                        for (Account x : liste) {
                            if (!x.getHesapId().equals(h.getHesapId()) && !kontrolcu.suphelihMi(x.getHesapId()) && !(x instanceof KrediHesabi)) { hd = x; break; }
                        }
                        if (hd != null && h.getBakiye() >= m) {
                            tipStr = "TRANSFER→" + hd.getHesapId(); ok = kontrolcu.transferYap(h.getHesapId(), hd.getHesapId(), m);
                        } else { tipStr = "YATIRMA"; ok = kontrolcu.paraYatir(h.getHesapId(), m); }
                    }
                    if (ok) basarili++; else basarisiz++;
                    int sAfter = kontrolcu.getRiskSkoru(h.getHesapId());
                    boolean dondu = kontrolcu.suphelihMi(h.getHesapId());
                    L.add((dondu ? "TXNFROZEN:" : "TXNOK:") + "#" + String.format("%02d", no) + "  " + tipStr + "  │  " + mAd + "  [" + h.getHesapId() + "]" + (dondu ? "  ← 🔴 DONDURULDU!" : ""));
                    L.add("TXNAMT:   Tutar: " + String.format(Locale.US, "%,.0f ₺", m)
                        + "    │    Bakiye: " + String.format(Locale.US, "%,.0f ₺", bOnce)
                        + " → " + String.format(Locale.US, "%,.0f ₺", h.getBakiye()));
                    boolean anyRisk = false;
                    if (m >= riskEsigi) { L.add("TXNRISK:  ⚡  Büyük tutarlı işlem (≥" + riskEsigiStr + " ₺)  →  +20 puan"); anyRisk = true; }
                    if (kontrolcu.kisaVadeliCokIslemMiMusteri(h.getSahibiId())) {
                        L.add("TXNRISK:  ⚡  Velocity: " + kontrolcu.kisaVadeliMusteriIslemSayisi(h.getSahibiId()) + " işlem/5 dk (tüm hesaplar)  →  +30 puan");
                        anyRisk = true;
                    }
                    if (!anyRisk && !dondu) L.add("TXNRISK:  ✅  Risk faktörü tetiklenmedi");
                    if (dondu) L.add("TXNRISK:  🔴  Skor eşiği aşıldı  →  Hesap otomatik donduruldu!");
                    int df = sAfter - sBefore;
                    L.add("TXNSCORE: Skor: " + sBefore + " → " + sAfter + (df > 0 ? "  (+" + df + " puan)" : "") + "   " + riskSeviyeEmoji(sAfter));
                } catch (model.YetersizBakiyeException e) {
                    basarisiz++;
                    L.add("TXNFAIL:#" + String.format("%02d", no) + "  " + tipStr + "  │  " + mAd + "  [" + h.getHesapId() + "]");
                    L.add("TXNAMT:   Tutar: " + String.format(Locale.US, "%,.0f ₺", m) + "    │    Mevcut bakiye: " + String.format(Locale.US, "%,.0f ₺", e.getMevcutBakiye()));
                    L.add("TXNRISK:  ❌  Yetersiz bakiye — işlem reddedildi, skor değişmedi");
                    L.add("TXNSCORE: Skor değişmedi: " + kontrolcu.getRiskSkoru(h.getHesapId()) + "/100");
                } catch (Exception e) {
                    basarisiz++;
                    L.add("TXNFAIL:#" + String.format("%02d", no) + "  " + tipStr + "  │  " + mAd + "  [" + h.getHesapId() + "]");
                    L.add("TXNRISK:  ❌  " + e.getMessage());
                    L.add("TXNSCORE: Skor: " + kontrolcu.getRiskSkoru(h.getHesapId()) + "/100");
                }
                L.add("TXNSEP:");
                try { Thread.sleep(15); } catch (InterruptedException ig) {}
            }

            // Özet
            L.add("HEADER:ÖZET — Simülasyon Tamamlandı  (30 İşlem)");
            L.add("OK:✓ Başarılı işlem sayısı    :  " + basarili);
            L.add("FAIL:✗ Başarısız / Engellenen  :  " + basarisiz);
            int fSkor = kontrolcu.getRiskSkoru(testH.getHesapId());
            L.add("SCORE:Test hesabı final skor   :  " + fSkor + "/100  —  " + riskSeviyeEmoji(fSkor));
            L.add("INFO:→ Admin Paneli — Risk & Limitler sekmesinde şüpheli hesapları ve sebeplerini görüntüleyin");

            final java.util.List<String> sonLoglar = L;
            final int fb = basarili, fsz = basarisiz;
            Platform.runLater(() -> {
                kontrolcu.botModuBitir();
                kontrolcu.getKaydedici().setSessiz(false);
                kontrolcu.durumYukle("banka_bot_oncesi.dat");
                new java.io.File("banka_bot_oncesi.dat").delete();
                raporlariYenile(); hesaplariYenile(); riskTablosunuYenile();
                botSimLoglar = sonLoglar;
                sonuclarBtn.setDisable(false);
                botBtn.setDisable(false);
                botBtn.setText("🤖 Bot Simülasyonu (30 İşlem)");
                UITema.bilgi("Simülasyon Tamamlandı",
                    "✓ Başarılı: " + fb + "   ✗ Başarısız: " + fsz + "\n\n" +
                    "Risk Yolculuğu:\n  🟢 GÜVENLİ → 🟡 İZLENİYOR → 🟠 RİSKLİ → 🔴 ŞÜPHELİ\n\n" +
                    "📋 Detaylar için \"Deneme Sonuçları\" butonuna tıklayın.");
            });
        }).start();
    }

    private static String riskSeviyeEmoji(int skor) {
        if (skor >= 86) return "🔴 ŞÜPHELİ/DONDURULDU";
        if (skor >= 61) return "🟠 RİSKLİ";
        if (skor >= 31) return "🟡 İZLENİYOR";
        return "🟢 GÜVENLİ";
    }

    private void botSonuclarGoster() {
        if (botSimLoglar == null || botSimLoglar.isEmpty()) return;
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Bot Simülasyonu — Risk Seviyeleri Yolculuğu");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(980);
        dialog.getDialogPane().setPrefHeight(740);
        dialog.getDialogPane().setStyle("-fx-background-color: #0d1117; -fx-padding: 0;");

        VBox container = new VBox(0);
        container.setPadding(new Insets(10));
        container.setStyle("-fx-background-color: #0d1117;");

        // Kart durumu — her işlem başlığı renk temasını belirler
        String[] cardBg   = {"#0d1117"};
        String[] cardType = {"NORMAL"};

        for (String satir : botSimLoglar) {
            if (satir == null) continue;
            Label lbl = new Label();
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setWrapText(true);

            // ── İşlem başlık satırları — kart rengini belirler ──────────────
            if (satir.startsWith("TXNOK:")) {
                cardType[0] = "OK"; cardBg[0] = "#0a1a0a";
                lbl.setText("  ✓  " + satir.substring(6));
                lbl.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
                lbl.setStyle("-fx-background-color: #122212; -fx-text-fill: #56d364; "
                    + "-fx-padding: 7 12 4 10; -fx-border-color: #2a6e2a; -fx-border-width: 0 0 0 4;");
                VBox.setMargin(lbl, new Insets(8, 0, 0, 0));

            } else if (satir.startsWith("TXNWARN:")) {
                cardType[0] = "WARN"; cardBg[0] = "#1f1800";
                lbl.setText("  ⚠  " + satir.substring(8));
                lbl.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
                lbl.setStyle("-fx-background-color: #2a2200; -fx-text-fill: #e3b341; "
                    + "-fx-padding: 7 12 4 10; -fx-border-color: #8a6300; -fx-border-width: 0 0 0 4;");
                VBox.setMargin(lbl, new Insets(8, 0, 0, 0));

            } else if (satir.startsWith("TXNFAIL:")) {
                cardType[0] = "FAIL"; cardBg[0] = "#1a0800";
                lbl.setText("  ✗  " + satir.substring(8));
                lbl.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
                lbl.setStyle("-fx-background-color: #241000; -fx-text-fill: #f0883e; "
                    + "-fx-padding: 7 12 4 10; -fx-border-color: #8a4000; -fx-border-width: 0 0 0 4;");
                VBox.setMargin(lbl, new Insets(8, 0, 0, 0));

            } else if (satir.startsWith("TXNFROZEN:")) {
                cardType[0] = "FROZEN"; cardBg[0] = "#200000";
                lbl.setText("  🔴  " + satir.substring(10));
                lbl.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
                lbl.setStyle("-fx-background-color: #2d0000; -fx-text-fill: #ff7070; "
                    + "-fx-padding: 7 12 4 10; -fx-border-color: #8a0000; -fx-border-width: 0 0 0 4;");
                VBox.setMargin(lbl, new Insets(8, 0, 0, 0));

            // ── Kart detay satırları — başlığın renk temasını kullanır ─────
            } else if (satir.startsWith("TXNAMT:")) {
                String tc = "FROZEN".equals(cardType[0]) ? "#cc5555"
                          : "FAIL".equals(cardType[0])   ? "#cc7733"
                          : "WARN".equals(cardType[0])   ? "#ccaa33" : "#88bb88";
                lbl.setText(satir.substring(7));
                lbl.setFont(Font.font("Consolas", 11));
                lbl.setStyle("-fx-background-color: " + cardBg[0] + "; -fx-text-fill: " + tc + "; -fx-padding: 3 12 1 26;");

            } else if (satir.startsWith("TXNRISK:")) {
                lbl.setText(satir.substring(8));
                lbl.setFont(Font.font("Segoe UI", 12));
                lbl.setStyle("-fx-background-color: " + cardBg[0] + "; -fx-text-fill: #c9d1d9; -fx-padding: 2 12 1 26;");

            } else if (satir.startsWith("TXNSCORE:")) {
                String sc = "FROZEN".equals(cardType[0]) ? "#ff9090"
                          : "WARN".equals(cardType[0])   ? "#ffcc44"
                          : "FAIL".equals(cardType[0])   ? "#ff9966" : "#58a6ff";
                lbl.setText(satir.substring(9));
                lbl.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
                lbl.setStyle("-fx-background-color: " + cardBg[0] + "; -fx-text-fill: " + sc + "; -fx-padding: 3 12 7 26;");

            } else if (satir.startsWith("TXNSEP:")) {
                cardType[0] = "NORMAL"; cardBg[0] = "#0d1117";
                lbl.setPrefHeight(2); lbl.setText("");
                lbl.setStyle("-fx-background-color: #161b22;");

            // ── Bölüm başlıkları ve özet satırları ──────────────────────────
            } else if (satir.startsWith("HEADER:")) {
                lbl.setText(satir.substring(7));
                lbl.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
                lbl.setStyle("-fx-background-color: #1c2d5a; -fx-text-fill: #e6edf3; "
                    + "-fx-background-radius: 6; -fx-padding: 10 16;");
                VBox.setMargin(lbl, new Insets(8, 0, 4, 0));

            } else if (satir.startsWith("PHASE:")) {
                lbl.setText(satir.substring(6));
                lbl.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
                lbl.setStyle("-fx-background-color: #1a3566; -fx-text-fill: #ffd532; "
                    + "-fx-background-radius: 5; -fx-padding: 9 14;");
                VBox.setMargin(lbl, new Insets(16, 0, 3, 0));

            } else if (satir.startsWith("SCORE:")) {
                lbl.setText("📊  " + satir.substring(6));
                lbl.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
                lbl.setStyle("-fx-background-color: #161b26; -fx-text-fill: #58a6ff; "
                    + "-fx-background-radius: 5; -fx-padding: 7 14; "
                    + "-fx-border-color: #1c3a6e; -fx-border-radius: 5; -fx-border-width: 1;");
                VBox.setMargin(lbl, new Insets(5, 0, 10, 0));

            } else if (satir.startsWith("OK:")) {
                lbl.setText("  ✓  " + satir.substring(3));
                lbl.setFont(Font.font("Consolas", 12));
                lbl.setStyle("-fx-background-color: #0a1f0a; -fx-text-fill: #56d364; -fx-background-radius: 4; -fx-padding: 5 12;");
            } else if (satir.startsWith("FAIL:")) {
                lbl.setText("  ✗  " + satir.substring(5));
                lbl.setFont(Font.font("Consolas", 12));
                lbl.setStyle("-fx-background-color: #240d00; -fx-text-fill: #f0883e; -fx-background-radius: 4; -fx-padding: 5 12;");
            } else if (satir.startsWith("INFO:")) {
                String t = satir.substring(5);
                if (t.trim().isEmpty()) { lbl.setText(" "); lbl.setPrefHeight(3); lbl.setStyle("-fx-background-color: transparent;"); }
                else { lbl.setText("  " + t); lbl.setFont(Font.font("Consolas", 11)); lbl.setStyle("-fx-background-color: transparent; -fx-text-fill: #8b949e; -fx-padding: 1 12;"); }
            } else {
                lbl.setText(satir); lbl.setFont(Font.font("Consolas", 11));
                lbl.setStyle("-fx-background-color: transparent; -fx-text-fill: #6e7681;");
            }
            container.getChildren().add(lbl);
        }

        ScrollPane sp = new ScrollPane(container);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: #0d1117; -fx-background: #0d1117;");
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        dialog.getDialogPane().setContent(sp);
        dialog.showAndWait();
    }

    private void demoVeriYukle() {
        if (!kontrolcu.tumMusteriler().isEmpty()) {
            UITema.uyari("Uyarı", "Zaten müşteri verisi mevcut.\nDemo veri yalnızca boş sistemde yüklenir.");
            return;
        }

        // ── Müşteri 1: Ahmet Yılmaz — GÜVENLİ (skor 0) ───────────────────────
        Customer ahmet = kontrolcu.musteriOlustur("Ahmet Yılmaz", "ahmet@ornek.com");
        kontrolcu.demoMusteriIsaretle(ahmet.getMusteriId());
        kimlikDogrulama.kullaniciEkle(new Kullanici("ahmet", "Ahmet123", Kullanici.Rol.MUSTERI, ahmet.getMusteriId()));
        Account ahmetV  = kontrolcu.hesapOlustur(ahmet.getMusteriId(), "VADESİZ", 12000);
        Account ahmetVd = kontrolcu.hesapOlustur(ahmet.getMusteriId(), "VADELİ",  35000);
        if (ahmetVd instanceof SavingsAccount) kontrolcu.faizOraniGuncelle(ahmetVd.getHesapId(), 0.08);
        kontrolcu.paraYatir(ahmetV.getHesapId(), 2500);
        kontrolcu.paraCek(ahmetV.getHesapId(), 800);
        // Skor: 0 → 🟢 GÜVENLİ

        // ── Müşteri 2: Fatma Şahin — İZLENİYOR (skor 45) ─────────────────────
        Customer fatma = kontrolcu.musteriOlustur("Fatma Şahin", "fatma@ornek.com");
        kontrolcu.demoMusteriIsaretle(fatma.getMusteriId());
        kimlikDogrulama.kullaniciEkle(new Kullanici("fatma", "Fatma123", Kullanici.Rol.MUSTERI, fatma.getMusteriId()));
        Account fatmaV   = kontrolcu.hesapOlustur(fatma.getMusteriId(), "VADESİZ",   28000);
        Account fatmaUSD = kontrolcu.hesapOlustur(fatma.getMusteriId(), "DÖVİZ-USD", 600);
        Account fatmaK   = kontrolcu.hesapOlustur(fatma.getMusteriId(), "KREDİ",     15000);
        kontrolcu.dovizKuruGuncelle(fatmaUSD.getHesapId(), 33.5);
        kontrolcu.krediFaizOraniGuncelle(fatmaK.getHesapId(), 0.015);
        kontrolcu.paraYatir(fatmaV.getHesapId(), 1000);
        kontrolcu.skorEkleDemo(fatmaV.getHesapId(), 45);
        // Skor: 45 → 🟡 İZLENİYOR (31-60)

        // ── Müşteri 3: Mehmet Demir — RİSKLİ (skor 75) ───────────────────────
        Customer mehmet = kontrolcu.musteriOlustur("Mehmet Demir", "mehmet@ornek.com");
        kontrolcu.demoMusteriIsaretle(mehmet.getMusteriId());
        kimlikDogrulama.kullaniciEkle(new Kullanici("mehmet", "Mehmet123", Kullanici.Rol.MUSTERI, mehmet.getMusteriId()));
        Account mehmetV   = kontrolcu.hesapOlustur(mehmet.getMusteriId(), "VADESİZ",   80000);
        Account mehmetEUR = kontrolcu.hesapOlustur(mehmet.getMusteriId(), "DÖVİZ-EUR", 1500);
        Account mehmetVd  = kontrolcu.hesapOlustur(mehmet.getMusteriId(), "VADELİ",    60000);
        kontrolcu.dovizKuruGuncelle(mehmetEUR.getHesapId(), 36.0);
        if (mehmetVd instanceof SavingsAccount) kontrolcu.faizOraniGuncelle(mehmetVd.getHesapId(), 0.10);
        kontrolcu.paraYatir(mehmetV.getHesapId(), 2000);
        kontrolcu.skorEkleDemo(mehmetV.getHesapId(), 75);
        // Skor: 75 → 🟠 RİSKLİ (61-85)

        // ── Müşteri 4: Zeynep Kaya — ŞÜPHELİ (otomatik dondurulmuş) ──────────
        Customer zeynep = kontrolcu.musteriOlustur("Zeynep Kaya", "zeynep@ornek.com");
        kontrolcu.demoMusteriIsaretle(zeynep.getMusteriId());
        kimlikDogrulama.kullaniciEkle(new Kullanici("zeynep", "Zeynep123", Kullanici.Rol.MUSTERI, zeynep.getMusteriId()));
        Account zeynepV   = kontrolcu.hesapOlustur(zeynep.getMusteriId(), "VADESİZ",   55000);
        Account zeynepGBP = kontrolcu.hesapOlustur(zeynep.getMusteriId(), "DÖVİZ-GBP", 200);
        kontrolcu.dovizKuruGuncelle(zeynepGBP.getHesapId(), 42.0);
        // Skoru 90'a getir, sonraki küçük işlem otomatik dondurma tetikler
        kontrolcu.skorEkleDemo(zeynepV.getHesapId(), 90);
        kontrolcu.paraYatir(zeynepV.getHesapId(), 100); // → islemSonrasiRiskKontrol → auto-freeze
        // Skor: 90 → 🔴 ŞÜPHELİ (DONDURULDU)

        musterileriYenile(); hesaplariYenile();
        musteriComboGuncelle(hesapMusteriCombo, false);
        musteriComboGuncelle(kulMusteriCombo, true);
        kullanicilariYenile(); raporlariYenile(); riskTablosunuYenile();

        UITema.bilgi("Demo Veri Yüklendi",
            "4 müşteri oluşturuldu — 4 farklı risk seviyesi:\n\n" +
            "  🟢 GÜVENLİ   → ahmet  / Ahmet123   (skor: 0)\n" +
            "  🟡 İZLENİYOR → fatma  / Fatma123   (skor: 45)\n" +
            "  🟠 RİSKLİ    → mehmet / Mehmet123  (skor: 75)\n" +
            "  🔴 ŞÜPHELİ   → zeynep / Zeynep123  (otomatik donduruldu)\n\n" +
            "Admin: admin / admin123");
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

    // ── Müşteri İzleme Sekmesi ────────────────────────────────────────────────

    private javafx.scene.Node musteriIzlemeSekme() {
        VBox solPanel = new VBox(8);
        solPanel.setPadding(new Insets(10));
        solPanel.setStyle("-fx-background-color: #f0f3f9;");
        solPanel.setMinWidth(270);

        izlemeAramaField = UITema.alan();
        izlemeAramaField.setPromptText("İsim, ID veya e-posta ara...");

        izlemeRiskFiltre = new ComboBox<>();
        izlemeRiskFiltre.getItems().addAll("Tüm Seviyeler", "GÜVENLİ", "İZLENİYOR", "RİSKLİ", "DONDURULDU");
        izlemeRiskFiltre.setValue("Tüm Seviyeler");
        izlemeRiskFiltre.setMaxWidth(Double.MAX_VALUE);

        izlemeSadeceDonuk = new CheckBox("Sadece donuk hesaplı müşteriler");

        izlemeMusteriTablo = new TableView<>();
        izlemeMusteriTablo.setPlaceholder(new Label("Müşteri bulunamadı"));
        izlemeMusteriTablo.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(izlemeMusteriTablo, Priority.ALWAYS);

        TableColumn<ObservableList<String>, String> colAd     = new TableColumn<>("Ad Soyad");
        TableColumn<ObservableList<String>, String> colId     = new TableColumn<>("ID");
        TableColumn<ObservableList<String>, String> colSkor   = new TableColumn<>("Risk");
        TableColumn<ObservableList<String>, String> colSeviye = new TableColumn<>("Seviye");
        colAd    .setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().size() > 0 ? d.getValue().get(0) : ""));
        colId    .setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().size() > 1 ? d.getValue().get(1) : ""));
        colSkor  .setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().size() > 2 ? d.getValue().get(2) : ""));
        colSeviye.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().size() > 3 ? d.getValue().get(3) : ""));
        colSeviye.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("-fx-font-weight:bold;-fx-text-fill:" + riskSeviyesiRengi(item) + ";");
            }
        });
        colSkor.setMaxWidth(52); colSkor.setMinWidth(45);
        colId  .setMaxWidth(82); colId  .setMinWidth(72);
        izlemeMusteriTablo.getColumns().addAll(colAd, colId, colSkor, colSeviye);

        solPanel_statsLabel = UITema.bilgiLabel("Yükleniyor...");

        solPanel.getChildren().addAll(
                UITema.baslikLabel("Gerçek Müşteri Listesi"),
                izlemeAramaField, izlemeRiskFiltre, izlemeSadeceDonuk,
                izlemeMusteriTablo, solPanel_statsLabel);

        // ── Sağ panel ────────────────────────────────────────────────────
        izlemeDetayKutusu = new VBox(10);
        izlemeDetayKutusu.setPadding(new Insets(12));
        izlemeDetayKutusu.setStyle("-fx-background-color: #f0f3f9;");
        Label beklemeLabel = new Label("← Soldan bir müşteri seçin");
        beklemeLabel.setStyle("-fx-text-fill: #8895aa; -fx-font-size: 13;");
        beklemeLabel.setPadding(new Insets(30));
        izlemeDetayKutusu.getChildren().add(beklemeLabel);

        ScrollPane sagScroll = new ScrollPane(izlemeDetayKutusu);
        sagScroll.setFitToWidth(true);
        sagScroll.setStyle("-fx-background-color: #f0f3f9;");

        // Dinleyiciler
        izlemeAramaField.textProperty().addListener((o, e, n) -> izlemeMusteriListesiniYenile());
        izlemeRiskFiltre.valueProperty().addListener((o, e, n) -> izlemeMusteriListesiniYenile());
        izlemeSadeceDonuk.selectedProperty().addListener((o, e, n) -> izlemeMusteriListesiniYenile());
        izlemeMusteriTablo.getSelectionModel().selectedItemProperty().addListener((obs, eski, yeni) -> {
            if (yeni != null && yeni.size() > 1) {
                izlemeSeciliMusteriId = yeni.get(1);
                izlemeDetayGoster(izlemeSeciliMusteriId);
            }
        });

        SplitPane split = new SplitPane(solPanel, sagScroll);
        split.setDividerPositions(0.30);
        return split;
    }

    private void izlemeMusteriListesiniYenile() {
        if (izlemeMusteriTablo == null) return;
        String ara  = izlemeAramaField.getText() == null ? "" : izlemeAramaField.getText().trim().toLowerCase();
        String risk = izlemeRiskFiltre.getValue();
        boolean donukFiltre = izlemeSadeceDonuk.isSelected();

        izlemeMusteriTablo.getItems().clear();
        int toplam = 0, donukSayac = 0;

        for (model.Customer m : kontrolcu.tumMusteriler()) {
            if (kontrolcu.isDemoMusteri(m.getMusteriId())) continue;
            if (!ara.isEmpty() && !m.getAd().toLowerCase().contains(ara)
                    && !m.getMusteriId().toLowerCase().contains(ara)
                    && !m.getEposta().toLowerCase().contains(ara)) continue;

            List<model.Account> hs = kontrolcu.musteriHesaplari(m.getMusteriId());
            int maxSkor = hs.stream().mapToInt(h -> kontrolcu.getRiskSkoru(h.getHesapId())).max().orElse(0);
            String seviye = riskSeviyesiMetni(maxSkor);
            boolean hesapDonuk = hs.stream().anyMatch(h -> kontrolcu.suphelihMi(h.getHesapId()));

            if (donukFiltre && !hesapDonuk) continue;
            if (!"Tüm Seviyeler".equals(risk) && !seviye.equals(risk)) continue;

            izlemeMusteriTablo.getItems().add(FXCollections.observableArrayList(
                    m.getAd(), m.getMusteriId(), String.valueOf(maxSkor), seviye));
            toplam++;
            if (hesapDonuk) donukSayac++;
        }
        // stats
        if (solPanel_statsLabel != null)
            solPanel_statsLabel.setText("Toplam: " + toplam + "  |  Donuk hesaplı: " + donukSayac);
    }

    // stats label referansı için küçük bir çözüm
    private Label solPanel_statsLabel;

    private void izlemeDetayGoster(String musteriId) {
        model.Customer m = kontrolcu.getMusteri(musteriId);
        if (m == null) return;
        List<model.Account> hesaplar = kontrolcu.musteriHesaplari(musteriId);
        List<ActivityLog> hamLoglar = kontrolcu.getLogServisi().musteriyeGore(musteriId, true);

        izlemeDetayKutusu.getChildren().clear();

        // ── Profil kartı ─────────────────────────────────────────────────
        int maxSkor = hesaplar.stream().mapToInt(h -> kontrolcu.getRiskSkoru(h.getHesapId())).max().orElse(0);
        String seviye = riskSeviyesiMetni(maxSkor);
        boolean donukMu = hesaplar.stream().anyMatch(h -> kontrolcu.suphelihMi(h.getHesapId()));

        VBox profilKarti = UITema.kart("Müşteri Profili");
        GridPane grid = new GridPane(); grid.setHgap(12); grid.setVgap(6);
        grid.add(UITema.etiket("Ad Soyad:"),   0, 0); grid.add(new Label(m.getAd()), 1, 0);
        grid.add(UITema.etiket("Müşteri ID:"), 0, 1); grid.add(new Label(m.getMusteriId()), 1, 1);
        grid.add(UITema.etiket("E-posta:"),    0, 2); grid.add(new Label(m.getEposta()), 1, 2);
        Label skorLabel = new Label(maxSkor + " / 100");
        skorLabel.setStyle("-fx-font-weight:bold;-fx-font-size:14;-fx-text-fill:" + riskSeviyesiRengi(seviye) + ";");
        Label seviyeBadge = new Label("  " + seviye + "  ");
        seviyeBadge.setStyle("-fx-background-color:" + riskSeviyesiRengi(seviye)
                + ";-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:4;-fx-font-size:10;");
        HBox skorSatir = new HBox(8, skorLabel, seviyeBadge);
        skorSatir.setAlignment(Pos.CENTER_LEFT);
        grid.add(UITema.etiket("Risk Skoru:"), 0, 3); grid.add(skorSatir, 1, 3);
        if (donukMu) {
            Label donukBadge = new Label("  DONDURULMUŞ HESAP VAR  ");
            donukBadge.setStyle("-fx-background-color:#af1414;-fx-text-fill:white;"
                    + "-fx-font-weight:bold;-fx-background-radius:4;-fx-font-size:10;");
            grid.add(donukBadge, 1, 4);
        }
        profilKarti.getChildren().add(grid);

        // ── Hesap kartı ───────────────────────────────────────────────────
        VBox hesapKarti = UITema.kart("Hesaplar (" + hesaplar.size() + ")");
        for (model.Account h : hesaplar) {
            int hSkor  = kontrolcu.getRiskSkoru(h.getHesapId());
            boolean hD = kontrolcu.suphelihMi(h.getHesapId());
            String renk = hD ? "#af1414" : riskSeviyesiRengi(riskSeviyesiMetni(hSkor));
            Label hl = new Label(h.getHesapId() + "  [" + h.getHesapTuru() + "]"
                    + "  Bakiye: " + tl(h.getBakiye())
                    + "  Risk: " + hSkor + (hD ? "  🔒 DONUK" : ""));
            hl.setStyle("-fx-text-fill:" + renk + ";-fx-font-size:12;");
            hesapKarti.getChildren().add(hl);
        }

        // ── Filtre çubuğu ─────────────────────────────────────────────────
        VBox filtrePaneli = UITema.kart("Filtrele");
        izlemeIslemFiltre = new ComboBox<>();
        izlemeIslemFiltre.getItems().add("Tüm İşlemler");
        for (ActivityLog.IslemTipi tip : ActivityLog.IslemTipi.values())
            izlemeIslemFiltre.getItems().add(islemTipiAdi(tip));
        izlemeIslemFiltre.setValue("Tüm İşlemler");
        izlemeBasTarih   = new DatePicker(LocalDate.now().minusMonths(1));
        izlemeBitisTarih = new DatePicker(LocalDate.now());
        Button filtreBtn = UITema.normalButon("Uygula");
        filtreBtn.setOnAction(e -> izlemeLogTablosunuDoldur(hamLoglar));
        HBox filtreRow = new HBox(8, new Label("İşlem:"), izlemeIslemFiltre,
                new Label("Tarih:"), izlemeBasTarih, new Label("–"), izlemeBitisTarih, filtreBtn);
        filtreRow.setAlignment(Pos.CENTER_LEFT);
        filtrePaneli.getChildren().add(filtreRow);

        // ── Log tablosu ───────────────────────────────────────────────────
        VBox logKarti = UITema.kart("Aktivite Geçmişi — sadece gerçek işlemler (" + hamLoglar.size() + " kayıt)");
        izlemeLogTablo = new TableView<>();
        izlemeLogTablo.setPlaceholder(new Label("Kayıt bulunamadı"));
        izlemeLogTablo.setPrefHeight(340);

        TableColumn<ObservableList<String>, String> cTarih     = izSutun("Tarih/Saat",       0, 132);
        TableColumn<ObservableList<String>, String> cIslem     = izSutun("İşlem",             1, 120);
        TableColumn<ObservableList<String>, String> cHesap     = izSutun("Hesap",             2,  88);
        TableColumn<ObservableList<String>, String> cTutar     = izSutun("Tutar",             3, 105);
        TableColumn<ObservableList<String>, String> cOnce      = izSutun("Risk Önce→Sonra",   4, 115);
        TableColumn<ObservableList<String>, String> cDelta     = izSutun("Δ",                 5,  48);
        TableColumn<ObservableList<String>, String> cKural     = izSutun("Tetiklenen Kural",  6, 155);
        TableColumn<ObservableList<String>, String> cKaynak    = izSutun("Kaynak",            7,  65);

        cDelta.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty); if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                setStyle(v.startsWith("+") && !"+0".equals(v)
                    ? "-fx-font-weight:bold;-fx-text-fill:#af1414;"
                    : v.startsWith("-") ? "-fx-font-weight:bold;-fx-text-fill:#146418;"
                    : "-fx-text-fill:#646e82;");
            }
        });
        cKaynak.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty); if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                setStyle("GERÇEK".equals(v) ? "-fx-font-weight:bold;-fx-text-fill:#146418;" : "-fx-text-fill:#8895aa;");
            }
        });
        izlemeLogTablo.getColumns().addAll(cTarih, cIslem, cHesap, cTutar, cOnce, cDelta, cKural, cKaynak);
        izlemeLogTablo.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        logKarti.getChildren().add(izlemeLogTablo);

        izlemeDetayKutusu.getChildren().addAll(profilKarti, hesapKarti, filtrePaneli, logKarti);
        izlemeLogTablosunuDoldur(hamLoglar);
    }

    private void izlemeLogTablosunuDoldur(List<ActivityLog> hamLoglar) {
        if (izlemeLogTablo == null) return;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM HH:mm:ss",
                new java.util.Locale("tr", "TR"));

        String secilenIslem = izlemeIslemFiltre != null ? izlemeIslemFiltre.getValue() : "Tüm İşlemler";
        LocalDate bas   = izlemeBasTarih   != null ? izlemeBasTarih.getValue()   : LocalDate.now().minusMonths(1);
        LocalDate bitis = izlemeBitisTarih != null ? izlemeBitisTarih.getValue() : LocalDate.now();

        AktiviteLogServisi srv = kontrolcu.getLogServisi();
        List<ActivityLog> filtreli = srv.tarihFiltrele(hamLoglar, bas, bitis);
        if (!"Tüm İşlemler".equals(secilenIslem))
            filtreli = srv.islemTipiFiltrele(filtreli, islemTipiEnum(secilenIslem));

        izlemeLogTablo.getItems().clear();
        for (ActivityLog log : filtreli) {
            String delta  = (log.riskDelta >= 0 ? "+" : "") + log.riskDelta;
            String tutar  = log.miktar > 0 ? tl(log.miktar) : "—";
            String hesap  = log.hesapId != null ? log.hesapId : "—";
            String kaynak = log.kaynak == ActivityLog.Kaynak.GERCEK ? "GERÇEK" : "DEMO";
            izlemeLogTablo.getItems().add(FXCollections.observableArrayList(
                    log.olusturmaTarihi.format(fmt),
                    islemTipiAdi(log.islemTipi),
                    hesap, tutar,
                    log.riskOncesi + " → " + log.riskSonrasi,
                    delta,
                    log.tetiklenenKural.isEmpty() ? "—" : log.tetiklenenKural,
                    kaynak));
        }
    }

    private static TableColumn<ObservableList<String>, String> izSutun(String baslik, int idx, double gen) {
        TableColumn<ObservableList<String>, String> col = new TableColumn<>(baslik);
        col.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                idx < d.getValue().size() ? d.getValue().get(idx) : ""));
        col.setPrefWidth(gen); col.setMinWidth(gen - 10);
        return col;
    }

    private static String riskSeviyesiMetni(int skor) {
        if (skor >= 86) return "DONDURULDU";
        if (skor >= 61) return "RİSKLİ";
        if (skor >= 31) return "İZLENİYOR";
        return "GÜVENLİ";
    }

    private static String riskSeviyesiRengi(String seviye) {
        switch (seviye) {
            case "DONDURULDU": return "#af1414";
            case "RİSKLİ":    return "#af4b00";
            case "İZLENİYOR":  return "#c8a000";
            default:           return "#146418";
        }
    }

    private static String islemTipiAdi(ActivityLog.IslemTipi tip) {
        switch (tip) {
            case GIRIS:             return "Giriş";
            case BASARISIZ_GIRIS:   return "Başarısız Giriş";
            case PARA_YATIRMA:      return "Para Yatırma";
            case PARA_CEKME:        return "Para Çekme";
            case TRANSFER:          return "Transfer";
            case GERI_AL:           return "Geri Al";
            case KREDI_ODEME:       return "Kredi Ödemesi";
            case HESAP_OLUSTURMA:   return "Hesap Oluşturma";
            case MUSTERI_OLUSTURMA: return "Müşteri Oluşturma";
            case HESAP_DONDURMA:    return "Hesap Dondurma";
            case HESAP_COZ:         return "Dondurma Kaldırma";
            case LIMIT_DEGISIMI:    return "Limit Değişimi";
            default:                return tip.name();
        }
    }

    private static ActivityLog.IslemTipi islemTipiEnum(String ad) {
        for (ActivityLog.IslemTipi tip : ActivityLog.IslemTipi.values())
            if (islemTipiAdi(tip).equals(ad)) return tip;
        return null;
    }
}
