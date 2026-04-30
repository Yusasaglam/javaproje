package ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Locale;

class UITema {

    static final String CSS_YOLU;
    static {
        java.net.URL url = UITema.class.getResource("banka.css");
        CSS_YOLU = url != null ? url.toExternalForm() : "";
    }

    // ── Renk sabitleri ────────────────────────────────────────────────────────
    static final String HEX_BIRINCIL    = "#163264";
    static final String HEX_IKINCIL     = "#235299";
    static final String HEX_BASARILI    = "#146418";
    static final String HEX_HATA        = "#af1414";
    static final String HEX_UYARI       = "#af4b00";
    static final String HEX_ARKA_PLAN   = "#f0f3f9";

    // ── Buton fabrika ─────────────────────────────────────────────────────────
    static Button anaButon(String metin) {
        Button b = new Button(metin);
        b.getStyleClass().add("buton-ana");
        return b;
    }

    static Button normalButon(String metin) {
        Button b = new Button(metin);
        b.getStyleClass().add("buton-normal");
        return b;
    }

    static Button tehlikeButon(String metin) {
        Button b = new Button(metin);
        b.getStyleClass().add("buton-tehlike");
        return b;
    }

    // ── Form bileşen fabrikası ────────────────────────────────────────────────
    static TextField alan() {
        TextField tf = new TextField();
        tf.getStyleClass().add("alan");
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    static PasswordField sifreAlani() {
        PasswordField pf = new PasswordField();
        pf.getStyleClass().add("alan");
        pf.setMaxWidth(Double.MAX_VALUE);
        return pf;
    }

    static Label etiket(String metin) {
        Label l = new Label(metin);
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: #37415a;");
        return l;
    }

    static Label baslikLabel(String metin) {
        Label l = new Label(metin);
        l.setFont(Font.font("System", FontWeight.BOLD, 13));
        l.setStyle("-fx-text-fill: " + HEX_BIRINCIL + ";");
        return l;
    }

    static Label durumLabel() {
        Label l = new Label();
        l.setMaxWidth(Double.MAX_VALUE);
        l.setAlignment(Pos.CENTER);
        l.setPadding(new Insets(4, 10, 4, 10));
        l.setFont(Font.font("System", FontWeight.BOLD, 12));
        return l;
    }

    static Label bilgiLabel(String metin) {
        Label l = new Label(metin);
        l.setStyle("-fx-text-fill: #646e82; -fx-font-size: 11;");
        l.setPadding(new Insets(2, 8, 2, 8));
        return l;
    }

    // ── Kart paneli ───────────────────────────────────────────────────────────
    static VBox kart(String baslik) {
        VBox kart = new VBox(10);
        kart.getStyleClass().add("kart");
        kart.setPadding(new Insets(14, 16, 14, 16));
        if (baslik != null && !baslik.isEmpty()) {
            Label bl = new Label(baslik);
            bl.setFont(Font.font("System", FontWeight.BOLD, 12));
            bl.setStyle("-fx-text-fill: " + HEX_BIRINCIL + ";");
            Separator sep = new Separator();
            sep.setStyle("-fx-background-color: #c6d2e4;");
            kart.getChildren().addAll(bl, sep);
        }
        return kart;
    }

    // ── TableView fabrikası ───────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    static TableView<ObservableList<String>> tablo(String... sutunlar) {
        TableView<ObservableList<String>> tv = new TableView<>();
        for (int i = 0; i < sutunlar.length; i++) {
            final int idx = i;
            TableColumn<ObservableList<String>, String> col = new TableColumn<>(sutunlar[i]);
            col.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                    idx < data.getValue().size() ? data.getValue().get(idx) : ""));
            col.setResizable(true);
            tv.getColumns().add(col);
        }
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tv.setPlaceholder(new Label("Veri bulunamadı"));
        tv.setPrefHeight(220);
        return tv;
    }

    static void satirEkle(TableView<ObservableList<String>> tv, String... degerler) {
        tv.getItems().add(FXCollections.observableArrayList(degerler));
    }

    // ── TL biçimlendirici ─────────────────────────────────────────────────────
    static String tl(double m) {
        return String.format(Locale.US, "%,.2f ₺", m);
    }

    // ── Bildirim dialogları ───────────────────────────────────────────────────
    static void bilgi(String baslik, String mesaj) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(baslik);
        a.setHeaderText(null);
        a.setContentText(mesaj);
        a.showAndWait();
    }

    static void hata(String baslik, String mesaj) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(baslik);
        a.setHeaderText(null);
        a.setContentText(mesaj);
        a.showAndWait();
    }

    static void uyari(String baslik, String mesaj) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(baslik);
        a.setHeaderText(null);
        a.setContentText(mesaj);
        a.showAndWait();
    }

    static boolean onay(String baslik, String mesaj) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(baslik);
        a.setHeaderText(null);
        a.setContentText(mesaj);
        return a.showAndWait().filter(r -> r == ButtonType.OK).isPresent();
    }

    // ── Renk ile durum metni ──────────────────────────────────────────────────
    static void durumGoster(Label label, String mesaj, boolean basarili) {
        label.setText(mesaj);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: " +
                (basarili ? HEX_BASARILI : HEX_HATA) + ";");
    }
}
