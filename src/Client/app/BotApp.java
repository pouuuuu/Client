package Client.app;

import Client.controller.GameController;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.List;

public class BotApp extends Application {

    @Override
    public void start(Stage stage) {
        Parameters params = getParameters();
        List<String> list = params.getRaw();

        String botName = list.isEmpty() ? "Bot" : list.get(0);

        GameController botController = new GameController(stage, true);

        System.out.println("Starting bot : " + botName);
        boolean success = botController.connect(botName);

        if (!success) {
            System.err.println("The bot can't access the server");
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}