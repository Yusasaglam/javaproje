package ui;

import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import model.Kullanici;
import persistence.FileLogger;
import service.BankController;
import service.KimlikDogrulama;

import java.io.File;

public class GirisEkrani {

    private static final String DURUM_DOSYASI = "banka_durumu.dat";

    private final Stage           stage;
    private final BankController  kontrolcu;
    private final KimlikDogrulama kimlikDogrulama;

    private TextField     kullaniciAdiField;
    private PasswordField sifreField;
    private Label         mesajLabel;
    private VBox          girisKart;

    public GirisEkrani(Stage stage) {
        this.stage = stage;
        FileLogger kaydedici = new FileLogger();
        this.kimlikDogrulama  = new KimlikDogrulama(kaydedici);
        this.kontrolcu        = new BankController(kimlikDogrulama);
        if (new File(DURUM_DOSYASI).exists()) {
            kontrolcu.durumYukle(DURUM_DOSYASI);
        }
        goster();
    }

    private void goster() {
        HBox kok = new HBox();
        kok.setPrefSize(1000, 640);

        Pane sol = solPanel();
        StackPane sag = sagPanel();
        HBox.setHgrow(sol, Priority.ALWAYS);
        HBox.setHgrow(sag, Priority.ALWAYS);
        sol.setMinWidth(420);
        sag.setMinWidth(400);

        kok.getChildren().addAll(sol, sag);

        Scene scene = new Scene(kok, 1000, 640);
        scene.getStylesheets().add(UITema.CSS_YOLU);

        stage.setTitle("Türk Bankası – Giriş");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(800);
        stage.setMinHeight(560);
        stage.show();
        // İlk layout bittikten sonra kart yüksekliğini kilitle —
        // StackPane, sabit yükseklik sayesinde kartı her zaman aynı Y'de konumlandırır.
        javafx.application.Platform.runLater(() -> {
            double h = girisKart.getHeight();
            if (h > 0) {
                girisKart.setMinHeight(h);
                girisKart.setMaxHeight(h);
            }
        });
    }

    // ── Sol panel: marka ──────────────────────────────────────────────────────
    private Pane solPanel() {
        Pane sol = new Pane();

        // Gradient arkaplan
        LinearGradient gradient = new LinearGradient(
            0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.web("#080f2e")),
            new Stop(0.5, Color.web("#0d1f4a")),
            new Stop(1.0, Color.web("#163264"))
        );
        Rectangle bg = new Rectangle();
        bg.widthProperty().bind(sol.widthProperty());
        bg.heightProperty().bind(sol.heightProperty());
        bg.setFill(gradient);

        // Dekoratif halkalar (yarı saydam)
        Circle c1 = daire(220); c1.setLayoutX(-80); c1.setLayoutY(-80);
        Circle c2 = daire(180);
        c2.layoutXProperty().bind(sol.widthProperty().subtract(60));
        c2.layoutYProperty().bind(sol.heightProperty().subtract(60));
        Circle c3 = daire(100);
        c3.layoutXProperty().bind(sol.widthProperty().multiply(0.55));
        c3.layoutYProperty().bind(sol.heightProperty().multiply(0.42));
        Circle c4 = daire(60);
        c4.layoutXProperty().bind(sol.widthProperty().multiply(0.15));
        c4.layoutYProperty().bind(sol.heightProperty().multiply(0.75));

        // Ana içerik bloğu
        VBox icerik = new VBox(0);
        icerik.setAlignment(Pos.CENTER_LEFT);
        icerik.layoutXProperty().bind(sol.widthProperty().multiply(0.10));
        icerik.layoutYProperty().bind(sol.heightProperty().multiply(0.20));
        icerik.prefWidthProperty().bind(sol.widthProperty().multiply(0.80));

        // Banka ikonu + isim
        Label ikon = new Label("🏦");
        ikon.setFont(Font.font("Segoe UI Emoji", 58));

        VBox.setMargin(ikon, new Insets(0, 0, 12, 0));

        Label baslik = new Label("TÜRK BANKASI");
        baslik.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 28));
        baslik.setTextFill(Color.WHITE);
        baslik.setStyle("-fx-letter-spacing: 2;");

        VBox.setMargin(baslik, new Insets(0, 0, 6, 0));

        // Sarı ayraç çizgisi
        Rectangle cizgi = new Rectangle(64, 3);
        cizgi.setFill(Color.web("#ffd532"));
        cizgi.setArcWidth(3); cizgi.setArcHeight(3);
        VBox.setMargin(cizgi, new Insets(0, 0, 16, 0));

        Label slogan = new Label("Güvenli · Hızlı · Güvenilir");
        slogan.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 15));
        slogan.setTextFill(Color.web("#8eb4e8"));
        VBox.setMargin(slogan, new Insets(0, 0, 44, 0));

        // Özellik listesi
        VBox ozellikler = new VBox(14);
        String[][] ozList = {
            {"🔒", "256-bit SSL Şifreleme"},
            {"⚡", "Anlık İşlem & Bildirim"},
            {"🛡️", "Akıllı Risk Motoru"},
            {"🌙", "7/24 Güvenli Erişim"}
        };
        for (String[] oz : ozList) {
            HBox satir = new HBox(12);
            satir.setAlignment(Pos.CENTER_LEFT);

            Label emj = new Label(oz[0]);
            emj.setFont(Font.font("Segoe UI Emoji", 16));
            emj.setMinWidth(26);

            Label txt = new Label(oz[1]);
            txt.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
            txt.setTextFill(Color.web("#b8d4f5"));

            satir.getChildren().addAll(emj, txt);
            ozellikler.getChildren().add(satir);
        }

        icerik.getChildren().addAll(ikon, baslik, cizgi, slogan, ozellikler);

        // Alt versiyon etiketi
        Label versiyon = new Label("v2.0  –  OOP Bankacılık Sistemi");
        versiyon.setFont(Font.font("Segoe UI", 11));
        versiyon.setTextFill(Color.web("#4a6494"));
        versiyon.layoutXProperty().bind(sol.widthProperty().multiply(0.10));
        versiyon.layoutYProperty().bind(sol.heightProperty().subtract(36));

        // c2: merkezi (solW-60, Y), yarıçap 180 → sağ kenar = solW+120 (taşıyor).
        // Pane, managed çocuklardan tercih genişliği hesaplayınca solW+120 bulur →
        // HBox daha fazla yer verir → döngü → yatay sallantı. Sadece c2 unmanaged.
        c2.setManaged(false);
        sol.getChildren().addAll(bg, c1, c2, c3, c4, icerik, versiyon);
        return sol;
    }

    private static Circle daire(double r) {
        Circle c = new Circle(r);
        c.setFill(Color.web("#ffffff", 0.04));
        c.setStroke(Color.web("#ffffff", 0.07));
        c.setStrokeWidth(1);
        return c;
    }

    // ── Sağ panel: giriş formu ────────────────────────────────────────────────
    private StackPane sagPanel() {
        StackPane sag = new StackPane();
        sag.setStyle("-fx-background-color: #edf2fa;");

        girisKart = new VBox(0);
        VBox kart = girisKart;
        kart.getStyleClass().add("giris-kart");
        kart.setPadding(new Insets(44, 48, 44, 48));
        kart.setMaxWidth(400);
        kart.setMinWidth(340);
        StackPane.setMargin(kart, new Insets(24));

        // Üst ikon
        Label kartIkon = new Label("👤");
        kartIkon.setFont(Font.font("Segoe UI Emoji", 32));
        kartIkon.setMaxWidth(Double.MAX_VALUE);
        kartIkon.setAlignment(Pos.CENTER);
        VBox.setMargin(kartIkon, new Insets(0, 0, 12, 0));

        // Başlık
        Label baslik = new Label("Hesabınıza Giriş Yapın");
        baslik.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        baslik.setStyle("-fx-text-fill: #1e2841;");
        baslik.setMaxWidth(Double.MAX_VALUE);
        baslik.setAlignment(Pos.CENTER);

        Label altBaslik = new Label("Devam etmek için bilgilerinizi girin");
        altBaslik.setFont(Font.font("Segoe UI", 13));
        altBaslik.setStyle("-fx-text-fill: #7080a0;");
        altBaslik.setMaxWidth(Double.MAX_VALUE);
        altBaslik.setAlignment(Pos.CENTER);
        VBox.setMargin(altBaslik, new Insets(6, 0, 32, 0));

        // Kullanıcı adı
        Label kulLabel = formEtiketi("Kullanıcı Adı");
        kullaniciAdiField = new TextField();
        kullaniciAdiField.setPromptText("kullanıcı adınızı girin");
        kullaniciAdiField.getStyleClass().add("giris-alan");
        kullaniciAdiField.setMaxWidth(Double.MAX_VALUE);
        kullaniciAdiField.setPrefHeight(48);
        VBox.setMargin(kulLabel, new Insets(0, 0, 6, 0));
        VBox.setMargin(kullaniciAdiField, new Insets(0, 0, 20, 0));

        // Şifre
        Label sifreLabel = formEtiketi("Şifre");
        sifreField = new PasswordField();
        sifreField.setPromptText("şifrenizi girin");
        sifreField.getStyleClass().add("giris-alan");
        sifreField.setMaxWidth(Double.MAX_VALUE);
        sifreField.setPrefHeight(48);
        VBox.setMargin(sifreLabel, new Insets(0, 0, 6, 0));
        VBox.setMargin(sifreField, new Insets(0, 0, 28, 0));

        // Giriş butonu
        Button girisBtn = new Button("Giriş Yap  →");
        girisBtn.getStyleClass().add("giris-buton");
        girisBtn.setMaxWidth(Double.MAX_VALUE);
        girisBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        VBox.setMargin(girisBtn, new Insets(0, 0, 16, 0));

        // Hata mesajı — sabit yükseklik (36 px), layout hiç değişmez
        mesajLabel = new Label(" ");
        mesajLabel.setFont(Font.font("Segoe UI", 12));
        mesajLabel.setStyle("-fx-text-fill: transparent; -fx-background-color: transparent; "
                + "-fx-background-radius: 6; -fx-padding: 6 12;");
        mesajLabel.setMaxWidth(Double.MAX_VALUE);
        mesajLabel.setAlignment(Pos.CENTER);
        mesajLabel.setWrapText(false);
        mesajLabel.setPrefHeight(36);
        mesajLabel.setMinHeight(36);
        mesajLabel.setMaxHeight(36);
        VBox.setMargin(mesajLabel, new Insets(0, 0, 8, 0));

        // Ayraç
        Separator sep = new Separator();
        VBox.setMargin(sep, new Insets(16, 0, 16, 0));

        // Alt bilgi
        Label altBilgi = new Label("🔑  Varsayılan giriş: admin / admin123");
        altBilgi.setFont(Font.font("Segoe UI", 11));
        altBilgi.setStyle("-fx-text-fill: #99a8c4;");
        altBilgi.setMaxWidth(Double.MAX_VALUE);
        altBilgi.setAlignment(Pos.CENTER);

        kart.getChildren().addAll(
            kartIkon, baslik, altBaslik,
            kulLabel, kullaniciAdiField,
            sifreLabel, sifreField,
            girisBtn, mesajLabel,
            sep, altBilgi
        );

        sag.getChildren().add(kart);

        // Olaylar
        girisBtn.setOnAction(e -> girisYap());
        sifreField.setOnAction(e -> girisYap());
        kullaniciAdiField.setOnAction(e -> sifreField.requestFocus());

        return sag;
    }

    private static Label formEtiketi(String metin) {
        Label l = new Label(metin);
        l.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        l.setStyle("-fx-text-fill: #374564;");
        return l;
    }

    // ── Giriş işlemi ──────────────────────────────────────────────────────────
    private void girisYap() {
        String ad    = kullaniciAdiField.getText().trim();
        String sifre = sifreField.getText().trim();

        if (ad.isEmpty() || sifre.isEmpty()) {
            hataGoster("⚠  Kullanıcı adı ve şifre boş bırakılamaz.");
            return;
        }

        Kullanici kullanici = kimlikDogrulama.girisYap(ad, sifre);
        if (kullanici == null) {
            String mesaj = kimlikDogrulama.engelliMi(ad)
                    ? "⛔  Hesap kilitlendi. Yönetici ile iletişime geçin."
                    : kimlikDogrulama.pasifMi(ad)
                    ? "⛔  Hesap pasif durumda."
                    : "✗  Hatalı kullanıcı adı veya şifre.";
            hataGoster(mesaj);
            sifreField.clear();
        } else {
            hataGizle();
            new MainFrame(stage, kullanici, kontrolcu, kimlikDogrulama);
        }
    }

    private void hataGoster(String mesaj) {
        mesajLabel.setText(mesaj);
        mesajLabel.setStyle("-fx-text-fill: #b41e1e; -fx-background-color: #fff0f0; "
                + "-fx-background-radius: 6; -fx-padding: 6 12;");
    }

    private void hataGizle() {
        mesajLabel.setText(" ");
        mesajLabel.setStyle("-fx-text-fill: transparent; -fx-background-color: transparent; "
                + "-fx-background-radius: 6; -fx-padding: 6 12;");
    }
}
