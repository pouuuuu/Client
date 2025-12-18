package Client.view;

import Client.controller.GameController;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ViewManager {

    private final Stage stage;
    private final GameController controller;
    private BorderPane mainLayout;

    private Button btnConnexion;
    private Button btnGame;

    private LoginView loginView;
    private GameBoardView gameBoardView;

    // Style de bouton adapté de MenuNavigation
    private final String MENU_BTN_STYLE =
            "-fx-background-color: rgba(255, 255, 255, 0.4);" +
                    "-fx-border-color: rgba(255, 255, 255, 0.3);" +
                    "-fx-border-width: 1;" +
                    "-fx-font-size: 12pt;" +
                    "-fx-text-fill: #37474F;" +
                    "-fx-font-weight: bold;";

    public ViewManager(Stage stage, GameController controller) {
        this.stage = stage;
        this.controller = controller;
        initUI();
    }

    private void initUI() {
        mainLayout = new BorderPane();
        // Fond global
        mainLayout.setStyle("-fx-background-color: linear-gradient(to bottom right, #E0F7FA, #80DEEA);");

        // Vues
        this.loginView = new LoginView(this, controller);
        this.gameBoardView = new GameBoardView(this, controller);

        controller.addObserver(gameBoardView);

        showLoginView();
        setConnected(false);

        Scene scene = new Scene(mainLayout, 1280, 800);
        stage.setTitle("Jeu de cartes");
        stage.setScene(scene);
        stage.show();
    }

    public void setConnected(boolean connected) {
        if (connected) showGameView();
        else showLoginView();
    }
    public GameBoardView getGameBoardView() {
        return this.gameBoardView;
    }

    public void showLoginView() {
        mainLayout.setCenter(loginView.getView());
    }
    public void showGameView() {
        gameBoardView.refresh();
        mainLayout.setCenter(gameBoardView.getView());
    }

    public Stage getStage() { return stage; }
}