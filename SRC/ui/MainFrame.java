package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import model.Kullanici;
import service.BankController;
import service.KimlikDogrulama;

public class MainFrame {

    public MainFrame(Stage stage, Kullanici kullanici,
                     BankController kontrolcu, KimlikDogrulama kimlikDogrulama) {

        boolean yonetici = kullanici.getRol() == Kullanici.Rol.YONETICI;

        // ── Üst başlık şeridi ─────────────────────────────────────────────────
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 20, 10, 20));
        header.setStyle("-fx-background-color: #163264;");

        Label bankaLabel = new Label("🏦  TÜRK BANKASI");
        bankaLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
        bankaLabel.setTextFill(Color.WHITE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label kullaniciLabel = new Label(kullanici.getKullaniciAdi());
        kullaniciLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        kullaniciLabel.setTextFill(Color.WHITE);

        Label rolLabel = new Label(yonetici ? " YÖNETİCİ " : " MÜŞTERİ ");
        rolLabel.setFont(Font.font("System", FontWeight.BOLD, 10));
        rolLabel.setStyle(yonetici
            ? "-fx-background-color: #ffd500; -fx-text-fill: #163264; -fx-background-radius: 3; -fx-padding: 3 8;"
            : "-fx-background-color: #8ce68c; -fx-text-fill: #0a3c0a; -fx-background-radius: 3; -fx-padding: 3 8;");

        javafx.scene.control.Button cikisBtn = UITema.normalButon("Çıkış Yap");
        cikisBtn.setOnAction(e -> {
            stage.close();
            new GirisEkrani(stage);
        });

        header.getChildren().addAll(bankaLabel, spacer, kullaniciLabel, rolLabel, cikisBtn);

        // ── İçerik ────────────────────────────────────────────────────────────
        Region icerik = yonetici
                ? new YoneticiPaneli(kontrolcu, kimlikDogrulama)
                : new MusteriPaneli(kullanici, kontrolcu);

        BorderPane kok = new BorderPane();
        kok.setTop(header);
        kok.setCenter(icerik);
        kok.setStyle("-fx-background-color: #f0f3f9;");

        Scene scene = new Scene(kok, 1100, 760);
        scene.getStylesheets().add(UITema.CSS_YOLU);

        stage.setTitle("Türk Bankası – " + kullanici.getKullaniciAdi());
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(640);
        stage.setResizable(true);
        stage.show();
    }
}
