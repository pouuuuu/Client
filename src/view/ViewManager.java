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

    // Références aux boutons pour pouvoir les activer/désactiver
    private Button btnConnexion;
    private Button btnGame;
    private Button btnLog;

    // Les sous-vues
    private LoginView loginView;
    private GameBoardView gameBoardView;
    private LogView logView;

    // Style des boutons (Ton style)
    private final String BUTTON_STYLE =
            "-fx-background-color: rgba(255, 255, 255, 0.3);" +
                    "-fx-border-color: rgba(255, 255, 255, 0.2);" +
                    "-fx-border-width: 2;" +
                    "-fx-faint-focus-color: transparent;" +
                    "-fx-focus-color: transparent;";

    public ViewManager(Stage stage, GameController controller) {
        this.stage = stage;
        this.controller = controller;
        initUI();
    }

    private void initUI() {
        mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(10));
        mainLayout.setStyle("-fx-background-color: #E0F7FA;");

        // Création du menu à droite
        VBox rightMenu = createNavigationMenu();
        mainLayout.setRight(rightMenu);

        // Création des vues
        this.loginView = new LoginView(this, controller);
        this.gameBoardView = new GameBoardView(this, controller);
        this.logView = new LogView(this, controller);

        // Abonnement des vues
        controller.addObserver(gameBoardView);
        controller.addObserver(logView);

        // Affichage initial
        showLoginView();

        // ÉTAT INITIAL : Verrouillé
        setConnected(false);

        Scene scene = new Scene(mainLayout, 1366, 768);
        stage.setTitle("Interface Client de Jeu - MVC");
        stage.setScene(scene);
        stage.show();
    }

    // --- GESTION DE L'ACCÈS (C'est ici que ça se joue) ---

    public void setConnected(boolean connected) {
        // Si connecté : on active les boutons Jeu et Log
        // Si pas connecté : on les désactive (grisés)
        btnGame.setDisable(!connected);
        btnLog.setDisable(!connected);

        if (connected) {
            // Si on vient de se connecter, on va directement au jeu
            showGameView();
        } else {
            // Si on est déconnecté, on retourne au login
            showLoginView();
        }
    }

    // --- NAVIGATION ---

    public void showLoginView() {
        mainLayout.setCenter(loginView.getView());
    }

    public void showGameView() {
        gameBoardView.refresh();
        mainLayout.setCenter(gameBoardView.getView());
    }

    public void showLogView() {
        mainLayout.setCenter(logView.getView());
    }

    public Stage getStage() {
        return stage;
    }

    // --- MENU ---

    private VBox createNavigationMenu() {
        btnConnexion = new Button("1. Connexion Serveur");
        btnGame = new Button("2. Plateau de Jeu");
        btnLog = new Button("3. Journal d'Actions");

        btnConnexion.setOnAction(e -> showLoginView());
        btnGame.setOnAction(e -> showGameView());
        btnLog.setOnAction(e -> showLogView());

        VBox menu = new VBox(10);
        menu.setAlignment(Pos.TOP_CENTER);
        menu.setPadding(new Insets(0, 0, 0, 10));

        // Style
        btnConnexion.setStyle(BUTTON_STYLE);
        btnGame.setStyle(BUTTON_STYLE);
        btnLog.setStyle(BUTTON_STYLE);

        btnConnexion.setAlignment(Pos.CENTER);
        btnGame.setAlignment(Pos.CENTER);
        btnLog.setAlignment(Pos.CENTER);

        // Taille
        btnConnexion.setMaxWidth(Double.MAX_VALUE);
        btnGame.setMaxWidth(Double.MAX_VALUE);
        btnLog.setMaxWidth(Double.MAX_VALUE);
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