package view;

import controller.GameController;
import controller.GameObserver;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Popup;
import javafx.stage.Stage;
import model.CombatResult;
import model.GameState;

import java.time.LocalTime;
import java.util.ArrayList;

// CHANGED: Implémente GameObserver pour écouter le Controller
public class GameView extends Application implements GameObserver {

    private final String SERVER_IP = "127.0.0.1";
    private final int SERVER_PORT = 44444;
    private final String BUTTON_STYLE = "-fx-font-size: 12pt; -fx-background-color: #4CAF50; -fx-text-fill: white;";

    private BorderPane mainLayout;
    private GameController controller; // NOUVEAU: Référence au Controller
    private TextArea logArea;

    @Override
    public void start(Stage primaryStage) {

        // 1. Initialisation du Controller et de l'Observer
        controller = new GameController(SERVER_IP, SERVER_PORT);
        controller.addObserver(this);

        mainLayout = new BorderPane();
        mainLayout.setTop(createNavigationMenu());
        mainLayout.setBottom(createLogView()); // Nouvelle vue de journal en bas

        // Lance la vue de connexion au serveur
        mainLayout.setCenter(createServerConnexionView());

        Scene scene = new Scene(mainLayout, 1366, 768);
        primaryStage.setTitle("Interface Client de Jeu");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private MenuBar createNavigationMenu() {
        MenuBar menuBar = new MenuBar();
        Menu menuGame = new Menu("Jeu");
        MenuItem connectItem = new MenuItem("Connexion au Serveur");
        connectItem.setOnAction(e -> mainLayout.setCenter(createServerConnexionView()));

        MenuItem gameItem = new MenuItem("Interface de Jeu");
        gameItem.setOnAction(e -> {
            if (controller.isConnected()) {
                mainLayout.setCenter(createGameView());
            } else {
                showError("Veuillez vous connecter au serveur pour accéder à l'interface de jeu.");
            }
        });

        menuGame.getItems().addAll(connectItem, gameItem, new SeparatorMenuItem(), new MenuItem("Quitter"));
        menuBar.getMenus().add(menuGame);
        return menuBar;
    }

    private Node createServerConnexionView() {
        VBox view = new VBox(20);
        view.setAlignment(Pos.CENTER);
        view.setPadding(new Insets(50));

        Label title = new Label("Page 1: Connexion au Serveur");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setAlignment(Pos.CENTER);

        TextField playerNameField = new TextField("Player1");
        form.addRow(0, new Label("Nom de Joueur:"), playerNameField);

        Button btnConnect = new Button("Se connecter");
        btnConnect.setStyle("-fx-font-size: 14pt; -fx-background-color: #00897B; -fx-text-fill: white;");

        // CHANGED: Liaison de l'action de connexion au Controller
        btnConnect.setOnAction(e -> {
            String playerName = playerNameField.getText();
            if (controller.connect(playerName)) {
                logMessage("Connexion réussie au serveur en tant que " + playerName + ".");
                // Passage automatique à la vue de jeu
                mainLayout.setCenter(createGameView());
            } else {
                // Le controller appelle onError() en cas d'échec
                showError("Échec de la connexion au serveur.");
            }
        });

        view.getChildren().addAll(title, form, btnConnect);
        return view;
    }

    /**
     * 2e page: Interface de Jeu. Utilise les ViewModels du Controller.
     */
    private Node createGameView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(20));

        Label title = new Label("Page 2: Interface de Jeu");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));

        PlayerViewModel currentPlayer = controller.getCurrentPlayerViewModel();

        // Section Main du Joueur
        String playerName = currentPlayer != null ? currentPlayer.getName() : "Non Connecté";
        Label myHandTitle = new Label("Votre Main (" + playerName + " - " +
                (currentPlayer != null ? currentPlayer.getCardCount() + "/" + currentPlayer.getMaxCards() : "0/0") + " Cartes)");
        myHandTitle.setStyle("-fx-font-size: 16pt; -fx-font-weight: bold;");

        HBox handDisplay = new HBox(15);
        handDisplay.setStyle("-fx-border-color: lightgray; -fx-padding: 10; -fx-background-color: #f4f4f4;");

        // CHANGED: Affichage dynamique des cartes
        if (currentPlayer != null && currentPlayer.getCards() != null) {
            for (CardViewModel cardVm : currentPlayer.getCards()) {
                handDisplay.getChildren().add(createCardPlaceholder(cardVm));
            }
        }

        // Section Actions
        Label actionsTitle = new Label("Actions Disponibles");
        actionsTitle.setStyle("-fx-font-size: 16pt; -fx-font-weight: bold;");

        VBox actionsBox = new VBox(10);
        actionsBox.setPadding(new Insets(10));

        // CHANGED: Lier le bouton d'action de création de carte au Controller
        Button btnCreateCard = new Button("Créer Nouvelle Carte");
        btnCreateCard.setStyle(BUTTON_STYLE);
        btnCreateCard.setOnAction(e -> {
            // Exemple simple d'appel au Controller. En réalité, une dialogue serait nécessaire.
            controller.createCard("Nouvelle Carte", 50, 50, 100);
            logMessage("Tentative de création d'une nouvelle carte.");
        });

        actionsBox.getChildren().addAll(
                btnCreateCard,
                new Button("Échanger Carte"), // TODO: Lier au Controller
                new Button("Combattre Carte") // TODO: Lier au Controller
        );

        // Section Liste des Joueurs
        Label playersTitle = new Label("Liste des Joueurs Connectés");
        playersTitle.setStyle("-fx-font-size: 16pt; -fx-font-weight: bold;");

        TextArea playersDisplay = new TextArea();
        playersDisplay.setEditable(false);
        playersDisplay.setPrefHeight(150);

        // CHANGED: Affichage dynamique des autres joueurs
        StringBuilder playersSb = new StringBuilder();
        if (controller.getOtherPlayersViewModels() != null) {
            for (PlayerViewModel p : controller.getOtherPlayersViewModels()) {
                playersSb.append(String.format("%s (ID: %d, Cartes: %d)\n", p.getName(), p.getId(), p.getCardCount()));
            }
        }
        playersDisplay.setText(playersSb.length() > 0 ? playersSb.toString() : "Aucun autre joueur connecté.");

        view.getChildren().addAll(title, myHandTitle, handDisplay, actionsTitle, actionsBox, playersTitle, playersDisplay);
        return view;
    }

    private VBox createCardPlaceholder(CardViewModel cardVm) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-border-color: black; -fx-border-radius: 5; -fx-background-color: white;");
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(100, 150);

        Label name = new Label(cardVm.getName());
        name.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        Label stats = new Label("ATK:" + cardVm.getAttack() + " / DEF:" + cardVm.getDefense() + " / HP:" + cardVm.getHealth());

        card.getChildren().addAll(name, stats);
        card.setOnMouseClicked(e -> showCardDetailsPopup(cardVm));
        return card;
    }

    private void showCardDetailsPopup(CardViewModel cardVm) {
        Popup popup = new Popup();
        Label details = new Label(String.format("ID: %d\nNom: %s\nAttaque: %d\nDéfense: %d\nSanté: %d",
                cardVm.getId(), cardVm.getName(), cardVm.getAttack(), cardVm.getDefense(), cardVm.getHealth()));
        details.setStyle("-fx-background-color: lightyellow; -fx-padding: 10; -fx-border-color: orange;");

        popup.getContent().add(details);
        popup.setAutoHide(true);
        popup.show(mainLayout.getScene().getWindow());
    }

    private Node createLogView() {
        VBox view = new VBox(5);
        view.setPadding(new Insets(5));
        view.setStyle("-fx-border-color: lightgray;");

        Label title = new Label("Journal des Événements");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(100);
        logArea.setWrapText(true);

        view.getChildren().addAll(title, logArea);
        return view;
    }

    // NOUVEAU: Méthode utilitaire pour journaliser
    private void logMessage(String message) {
        if (logArea != null) {
            Platform.runLater(() -> {
                logArea.appendText("\n[" + LocalTime.now().withNano(0) + "] " + message);
            });
        }
    }

    // NOUVEAU: Méthode utilitaire pour afficher une erreur (journalise l'erreur)
    private void showError(String message) {
        logMessage("ERREUR: " + message);
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur du Client");
            alert.setHeaderText("Une erreur est survenue");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // ===== IMPLEMENTATION DE GAME OBSERVER (Résout l'erreur de compilation) =====

    @Override
    public void onGameStateChanged(GameState gameState) {
        // S'assurer que les mises à jour UI se font sur le thread JavaFX
        Platform.runLater(() -> {
            logMessage("État du jeu mis à jour par le serveur. Rafraîchissement de l'UI.");
            // Recharger la vue de jeu pour refléter les changements d'état
            // On vérifie qu'on est bien sur l'interface de jeu pour éviter les erreurs
            Node currentCenter = mainLayout.getCenter();
            if (controller.isConnected() && currentCenter instanceof VBox) {
                // Une vérification simple pour s'assurer que l'on rafraîchit la bonne vue
                if (((VBox) currentCenter).getChildren().stream()
                        .anyMatch(node -> node instanceof Label && ((Label) node).getText().startsWith("Page 2:"))) {
                    mainLayout.setCenter(createGameView());
                }
            }
        });
    }

    @Override
    public void onError(String errorMessage) {
        showError(errorMessage);
    }

    @Override
    public void onExchangeRequestReceived(String requestId, int fromPlayerId, String fromPlayerName, int offeredCardId, int requestedCardId) {
        Platform.runLater(() -> {
            logMessage(String.format("Requete d'echange de %s (ID: %d) reçue. Carte offerte: %d, Carte demandée: %d.",
                    fromPlayerName, fromPlayerId, offeredCardId, requestedCardId));
            // TODO: Afficher une notification ou une boîte de dialogue pour accepter/refuser.
        });
    }

    @Override
    public void onCombatRequestReceived(String requestId, int fromPlayerId, String fromPlayerName, int attackingCardId, int targetCardId) {
        Platform.runLater(() -> {
            logMessage(String.format("Requete de combat de %s (ID: %d) reçue. Carte attaquante: %d, Carte cible: %d.",
                    fromPlayerName, fromPlayerId, attackingCardId, targetCardId));
            // TODO: Afficher une notification ou une boîte de dialogue pour réagir au combat.
        });
    }

    @Override
    public void onCombatCompleted(CombatResult result) {
        Platform.runLater(() -> {
            logMessage(String.format("Combat terminé! Vainqueur: %d, Perdant: %d. Dégâts: %d.",
                    result.getWinnerId(), result.getLoserId(), result.getDamageDealt()));
        });
    }

    // Implémentations pour les surcharges de GameObserver utilisant des IDs en String (laisser vides si non utilisées)
    @Override public void onExchangeRequestReceived(String requestId, String fromPlayerId, String fromPlayerName, String offeredCardId, String requestedCardId) { }
    @Override public void onCombatRequestReceived(String requestId, String fromPlayerId, String fromPlayerName, String attackingCardId, String targetCardId) { }

    public static void main(String[] args) {
        launch(args);
    }
}