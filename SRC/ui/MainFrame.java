package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import model.Kullanici;
import service.BankController;
import service.KimlikDogrulama;

public class MainFrame {

    public MainFrame(Stage stage, Kullanici kullanici,
                     BankController kontrolcu, KimlikDogrulama kimlikDogrulama) {

        final KimlikDogrulama kd = kimlikDogrulama;
        final BankController  kc = kontrolcu;
        boolean yonetici = kullanici.getRol() == Kullanici.Rol.YONETICI;

        // ── Üst başlık şeridi ─────────────────────────────────────────────────
        HBox header = new HBox(0);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPrefHeight(56);
        header.setMinHeight(56);
        header.getStyleClass().add("header-serit");
        header.setStyle("-fx-background-color: linear-gradient(to right, #080f2e, #163264);");

        // Sol: logo + banka adı
        HBox sol = new HBox(10);
        sol.setAlignment(Pos.CENTER_LEFT);
        sol.setPadding(new Insets(0, 0, 0, 20));

        Label bankaIkon = new Label("🏦");
        bankaIkon.setFont(Font.font("Segoe UI Emoji", 22));

        Label bankaLabel = new Label("TÜRK BANKASI");
        bankaLabel.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 15));
        bankaLabel.setTextFill(Color.WHITE);
        bankaLabel.setStyle("-fx-letter-spacing: 1.5;");

        // İnce sarı dikey ayraç
        Rectangle ayrac = new Rectangle(2, 22);
        ayrac.setFill(Color.web("#ffd532", 0.6));
        HBox.setMargin(ayrac, new Insets(0, 6, 0, 14));

        Label subBaslik = new Label("Bankacılık Yönetim Sistemi");
        subBaslik.setFont(Font.font("Segoe UI", 11));
        subBaslik.setTextFill(Color.web("#8eaed8"));

        sol.getChildren().addAll(bankaIkon, bankaLabel, ayrac, subBaslik);

        // Orta: spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Sağ: kullanıcı bilgisi + çıkış
        HBox sag = new HBox(12);
        sag.setAlignment(Pos.CENTER_RIGHT);
        sag.setPadding(new Insets(0, 20, 0, 0));

        // Kullanıcı avatar dairesi
        Circle avatar = new Circle(16);
        avatar.setFill(Color.web(yonetici ? "#ffd532" : "#4caf50"));

        Label avatarHarf = new Label(kullanici.getKullaniciAdi().substring(0, 1).toUpperCase());
        avatarHarf.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        avatarHarf.setTextFill(Color.web(yonetici ? "#163264" : "#0a3c0a"));

        StackPane avatarPane = new StackPane(avatar, avatarHarf);

        // Kullanıcı adı + rol
        VBox kullaniciBilgi = new VBox(2);
        kullaniciBilgi.setAlignment(Pos.CENTER_LEFT);

        Label kullaniciLabel = new Label(kullanici.getKullaniciAdi());
        kullaniciLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        kullaniciLabel.setTextFill(Color.WHITE);

        Label rolLabel = new Label(yonetici ? "Yönetici" : "Müşteri");
        rolLabel.setFont(Font.font("Segoe UI", 10));
        rolLabel.setTextFill(Color.web(yonetici ? "#ffd532" : "#8ce68c"));

        kullaniciBilgi.getChildren().addAll(kullaniciLabel, rolLabel);

        // İnce ayraç
        Rectangle ayrac2 = new Rectangle(1, 28);
        ayrac2.setFill(Color.web("#ffffff", 0.15));

        // Çıkış butonu
        Button cikisBtn = new Button("⏻  Çıkış");
        cikisBtn.setStyle(
            "-fx-background-color: rgba(255,255,255,0.10);" +
            "-fx-text-fill: #c8d8f0;" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 6;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 7 14;" +
            "-fx-border-color: rgba(255,255,255,0.18);" +
            "-fx-border-radius: 6;" +
            "-fx-border-width: 1;"
        );
        cikisBtn.setOnMouseEntered(e -> cikisBtn.setStyle(
            "-fx-background-color: rgba(255,255,255,0.18);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 6;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 7 14;" +
            "-fx-border-color: rgba(255,255,255,0.35);" +
            "-fx-border-radius: 6;" +
            "-fx-border-width: 1;"
        ));
        cikisBtn.setOnMouseExited(e -> cikisBtn.setStyle(
            "-fx-background-color: rgba(255,255,255,0.10);" +
            "-fx-text-fill: #c8d8f0;" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 6;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 7 14;" +
            "-fx-border-color: rgba(255,255,255,0.18);" +
            "-fx-border-radius: 6;" +
            "-fx-border-width: 1;"
        ));
        cikisBtn.setOnAction(e -> {
            stage.close();
            new GirisEkrani(stage, kc, kd);
        });

        sag.getChildren().addAll(avatarPane, kullaniciBilgi, ayrac2, cikisBtn);
        header.getChildren().addAll(sol, spacer, sag);

        // ── İnce alt çizgi ────────────────────────────────────────────────────
        Rectangle headerCizgi = new Rectangle();
        headerCizgi.setHeight(2);
        headerCizgi.setFill(Color.web("#ffd532", 0.35));

        VBox topBar = new VBox(0, header, headerCizgi);
        // Genişliği topBar'a bağla — bağlamadan width=0 olurdu
        headerCizgi.widthProperty().bind(topBar.widthProperty());

        // ── İçerik ────────────────────────────────────────────────────────────
        Region icerik = yonetici
                ? new YoneticiPaneli(kontrolcu, kimlikDogrulama)
                : new MusteriPaneli(kullanici, kontrolcu);

        BorderPane kok = new BorderPane();
        kok.setTop(topBar);
        kok.setCenter(icerik);
        kok.setStyle("-fx-background-color: #f0f3f9;");

        Scene scene = new Scene(kok, 1150, 780);
        scene.getStylesheets().add(UITema.CSS_YOLU);

        stage.setTitle("Türk Bankası  –  " + kullanici.getKullaniciAdi()
                + "  [" + (yonetici ? "YÖNETİCİ" : "MÜŞTERİ") + "]");
        stage.setScene(scene);
        stage.setMinWidth(920);
        stage.setMinHeight(660);
        stage.setResizable(true);
        stage.show();
    }
}
