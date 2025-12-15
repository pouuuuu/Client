package view;

import controller.GameController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
    private Button btnLog;

    private LoginView loginView;
    private GameBoardView gameBoardView;
    private LogView logView;

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

        // Menu à droite
        VBox rightMenu = createNavigationMenu();
        mainLayout.setRight(rightMenu);

        // Vues
        this.loginView = new LoginView(this, controller);
        this.gameBoardView = new GameBoardView(this, controller);
        this.logView = new LogView(this, controller);

        controller.addObserver(gameBoardView);
        controller.addObserver(logView);

        showLoginView();
        setConnected(false);

        Scene scene = new Scene(mainLayout, 1280, 800);
        stage.setTitle("JavaFX Game Client");
        stage.setScene(scene);
        stage.show();
    }

    public void setConnected(boolean connected) {
        btnGame.setDisable(!connected);
        btnLog.setDisable(!connected);
        if (connected) showGameView();
        else showLoginView();
    }

    public void showLoginView() { mainLayout.setCenter(loginView.getView()); }
    public void showGameView() {
        gameBoardView.refresh();
        mainLayout.setCenter(gameBoardView.getView());
    }
    public void showLogView() { mainLayout.setCenter(logView.getView()); }
    public Stage getStage() { return stage; }

    private VBox createNavigationMenu() {
        btnConnexion = new Button("1. Connexion");
        btnGame = new Button("2. Plateau de jeu");
        btnLog = new Button("3. Logs du jeu");

        btnConnexion.setOnAction(e -> showLoginView());
        btnGame.setOnAction(e -> showGameView());
        btnLog.setOnAction(e -> showLogView());

        VBox menu = new VBox(10);
        menu.setPadding(new Insets(20, 10, 20, 10));
        menu.setPrefWidth(200);
        menu.setStyle("-fx-background-color: rgba(255,255,255,0.3);");

        // Application du style
        btnConnexion.setStyle(MENU_BTN_STYLE);
        btnGame.setStyle(MENU_BTN_STYLE);
        btnLog.setStyle(MENU_BTN_STYLE);

        // Max Width pour remplir la colonne
        btnConnexion.setMaxWidth(Double.MAX_VALUE);
        btnGame.setMaxWidth(Double.MAX_VALUE);
        btnLog.setMaxWidth(Double.MAX_VALUE);

        // Hauteur flexible
        btnConnexion.setMaxHeight(Double.MAX_VALUE);
        btnGame.setMaxHeight(Double.MAX_VALUE);
        btnLog.setMaxHeight(Double.MAX_VALUE);

        VBox.setVgrow(btnConnexion, Priority.ALWAYS);
        VBox.setVgrow(btnGame, Priority.ALWAYS);
        VBox.setVgrow(btnLog, Priority.ALWAYS);

        menu.getChildren().addAll(btnConnexion, btnGame, btnLog);
        return menu;
    }
}