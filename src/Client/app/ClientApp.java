package Client.app;

import Client.controller.GameController;
import javafx.application.Application;
import javafx.stage.Stage;

public class ClientApp extends Application {

    @Override
    public void start(Stage stage) {
        new GameController(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}