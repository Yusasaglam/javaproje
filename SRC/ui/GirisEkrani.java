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
        kok.setPrefSize(860, 540);

        kok.getChildren().addAll(solPanel(), sagPanel());
        HBox.setHgrow(kok.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(kok.getChildren().get(1), Priority.ALWAYS);

        Scene scene = new Scene(kok, 860, 540);
        scene.getStylesheets().add(UITema.CSS_YOLU);

        stage.setTitle("Türk Bankası – Giriş");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    // ── Sol panel: marka ──────────────────────────────────────────────────────
    private Pane solPanel() {
        Pane sol = new Pane();
        sol.setMinWidth(350);

        // Gradient arkaplan
        LinearGradient gradient = new LinearGradient(
            0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#0a1e46")),
            new Stop(1, Color.web("#23509e"))
        );
        Rectangle bg = new Rectangle();
        bg.widthProperty().bind(sol.widthProperty());
        bg.heightProperty().bind(sol.heightProperty());
        bg.setFill(gradient);

        // Dekoratif daireler
        Circle c1 = daire(40);
        c1.setLayoutX(-30); c1.setLayoutY(-30);
        Circle c2 = daire(130);
        c2.layoutXProperty().bind(sol.widthProperty().subtract(70));
        c2.layoutYProperty().bind(sol.heightProperty().subtract(70));
        Circle c3 = daire(80);
        c3.layoutXProperty().bind(sol.widthProperty().divide(2).subtract(40));
        c3.layoutYProperty().bind(sol.heightProperty().divide(2).subtract(40));

        // İçerik
        VBox icerik = new VBox(16);
        icerik.setAlignment(Pos.CENTER);
        icerik.layoutXProperty().bind(sol.widthProperty().multiply(0.08));
        icerik.layoutYProperty().bind(sol.heightProperty().multiply(0.25));
        icerik.prefWidthProperty().bind(sol.widthProperty().multiply(0.84));

        Label ikon = new Label("🏦");
        ikon.setFont(Font.font("System", 52));
        ikon.setMaxWidth(Double.MAX_VALUE);
        ikon.setAlignment(Pos.CENTER);

        Label baslik = new Label("TÜRK BANKASI");
        baslik.setFont(Font.font("System", FontWeight.BOLD, 22));
        baslik.setTextFill(Color.WHITE);
        baslik.setMaxWidth(Double.MAX_VALUE);
        baslik.setAlignment(Pos.CENTER);

        Rectangle cizgi = new Rectangle(140, 2);
        cizgi.setFill(Color.web("#ffd532"));
        StackPane cizgiPane = new StackPane(cizgi);
        cizgiPane.setMaxWidth(Double.MAX_VALUE);

        Label slogan = new Label("Güvenli · Hızlı · Güvenilir");
        slogan.setFont(Font.font("System", 13));
        slogan.setTextFill(Color.web("#b4d2ff"));
        slogan.setMaxWidth(Double.MAX_VALUE);
        slogan.setAlignment(Pos.CENTER);

        VBox ozellikler = new VBox(8);
        ozellikler.setPadding(new Insets(10, 20, 0, 20));
        for (String s : new String[]{"✓  256-bit SSL Şifreleme", "✓  7/24 Güvenli Erişim", "✓  Anlık Bildirimler"}) {
            Label oz = new Label(s);
            oz.setFont(Font.font("System", 12));
            oz.setTextFill(Color.web("#a0c8ff"));
            ozellikler.getChildren().add(oz);
        }

        icerik.getChildren().addAll(ikon, baslik, cizgiPane, slogan, ozellikler);
        sol.getChildren().addAll(bg, c1, c2, c3, icerik);
        return sol;
    }

    private static Circle daire(double r) {
        Circle c = new Circle(r);
        c.setFill(Color.web("#ffffff", 0.06));
        return c;
    }

    // ── Sağ panel: form ───────────────────────────────────────────────────────
    private StackPane sagPanel() {
        StackPane sag = new StackPane();
        sag.setStyle("-fx-background-color: #ebf0fa;");

        VBox kart = new VBox(0);
        kart.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 10;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 16, 0, 0, 4);"
        );
        kart.setPadding(new Insets(36, 40, 36, 40));
        kart.setMaxWidth(340);
        kart.setMaxHeight(Double.MAX_VALUE);

        // Başlık
        Label baslik = new Label("Hesabınıza Giriş Yapın");
        baslik.setFont(Font.font("System", FontWeight.BOLD, 17));
        baslik.setStyle("-fx-text-fill: #1e2841;");

        Label altBaslik = new Label("Devam etmek için bilgilerinizi girin");
        altBaslik.setFont(Font.font("System", 11));
        altBaslik.setStyle("-fx-text-fill: #828faa;");
        altBaslik.setPadding(new Insets(4, 0, 24, 0));

        // Kullanıcı adı
        Label kulLabel = new Label("Kullanıcı Adı");
        kulLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        kulLabel.setStyle("-fx-text-fill: #374564;");
        kulLabel.setPadding(new Insets(0, 0, 5, 0));

        kullaniciAdiField = new TextField();
        kullaniciAdiField.setPromptText("kullanıcı adınızı girin");
        kullaniciAdiField.getStyleClass().add("alan");
        kullaniciAdiField.setPrefHeight(40);
        kullaniciAdiField.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(kullaniciAdiField, new Insets(0, 0, 16, 0));

        // Şifre
        Label sifreLabel = new Label("Şifre");
        sifreLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        sifreLabel.setStyle("-fx-text-fill: #374564;");
        sifreLabel.setPadding(new Insets(0, 0, 5, 0));

        sifreField = new PasswordField();
        sifreField.setPromptText("şifrenizi girin");
        sifreField.getStyleClass().add("alan");
        sifreField.setPrefHeight(40);
        sifreField.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(sifreField, new Insets(0, 0, 24, 0));

        // Giriş butonu
        Button girisBtn = new Button("Giriş Yap  →");
        girisBtn.getStyleClass().add("buton-ana");
        girisBtn.setMaxWidth(Double.MAX_VALUE);
        girisBtn.setPrefHeight(44);
        girisBtn.setFont(Font.font("System", FontWeight.BOLD, 13));
        VBox.setMargin(girisBtn, new Insets(0, 0, 14, 0));

        // Mesaj
        mesajLabel = new Label(" ");
        mesajLabel.setFont(Font.font("System", 11));
        mesajLabel.setStyle("-fx-text-fill: #b41e1e;");
        mesajLabel.setMaxWidth(Double.MAX_VALUE);
        mesajLabel.setAlignment(Pos.CENTER);
        mesajLabel.setWrapText(true);

        // Alt bilgi
        Label altBilgi = new Label("Varsayılan: admin / admin123");
        altBilgi.setFont(Font.font("System", 10));
        altBilgi.setStyle("-fx-text-fill: #b4beef;");
        altBilgi.setMaxWidth(Double.MAX_VALUE);
        altBilgi.setAlignment(Pos.CENTER);
        VBox.setMargin(altBilgi, new Insets(16, 0, 0, 0));

        kart.getChildren().addAll(
            baslik, altBaslik,
            kulLabel, kullaniciAdiField,
            sifreLabel, sifreField,
            girisBtn, mesajLabel, altBilgi
        );

        sag.getChildren().add(kart);

        // Olaylar
        girisBtn.setOnAction(e -> girisYap());
        sifreField.setOnAction(e -> girisYap());
        kullaniciAdiField.setOnAction(e -> sifreField.requestFocus());

        return sag;
    }

    // ── Giriş işlemi ──────────────────────────────────────────────────────────
    private void girisYap() {
        String ad    = kullaniciAdiField.getText().trim();
        String sifre = sifreField.getText().trim();

        if (ad.isEmpty() || sifre.isEmpty()) {
            mesajLabel.setText("⚠  Kullanıcı adı ve şifre boş bırakılamaz.");
            return;
        }

        Kullanici kullanici = kimlikDogrulama.girisYap(ad, sifre);
        if (kullanici == null) {
            String mesaj = kimlikDogrulama.engelliMi(ad)
                    ? "⛔  Hesap kilitlendi. Yönetici ile iletişime geçin."
                    : kimlikDogrulama.pasifMi(ad)
                    ? "⛔  Hesap pasif durumda. Yönetici ile iletişime geçin."
                    : "✗  Hatalı kullanıcı adı veya şifre.";
            mesajLabel.setText(mesaj);
            sifreField.clear();
        } else {
            new MainFrame(stage, kullanici, kontrolcu, kimlikDogrulama);
        }
    }
}
