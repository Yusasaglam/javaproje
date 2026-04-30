import javafx.application.Application;
import javafx.stage.Stage;
import ui.GirisEkrani;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        new GirisEkrani(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
