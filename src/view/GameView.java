package view;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class GameView extends Application {

    private BorderPane mainLayout;
    // Les paramètres de connexion sont toujours là, mais ne sont plus affichés
    private final String SERVER_IP = "127.0.0.1";
    private final int SERVER_PORT = 8080;

    private final String BUTTON_STYLE =
            "-fx-background-color: rgba(255, 255, 255, 0.3);" +
                    "-fx-border-color: rgba(255, 255, 255, 0.2);" +
                    "-fx-border-width: 2;" +
                    "-fx-faint-focus-color: transparent;" +
                    "-fx-focus-color: transparent;";

    /**
     * Classe interne simple pour modéliser les données d'une carte.
     */
    private class Carte {
        String nom;
        int id;
        int attaque;
        int defense;

        public Carte(String nom, int id, int attaque, int defense) {
            this.nom = nom;
            this.id = id;
            this.attaque = attaque;
            this.defense = defense;
        }
    }

    @Override
    public void start(Stage primaryStage) {

        mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(10));

        mainLayout.setStyle("-fx-background-color: #E0F7FA;");

        VBox rightMenu = createNavigationMenu();
        mainLayout.setRight(rightMenu);

        // Commence par la vue de connexion au serveur
        mainLayout.setCenter(createServerConnexionView());

        Scene scene = new Scene(mainLayout, 1366, 768);
        primaryStage.setTitle("Interface Client de Jeu");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // --- Méthodes de Navigation et Vues (Inchagées) ---

    private VBox createNavigationMenu() {
        Button btnConnexion = new Button("1. Connexion Serveur");
        Button btnGame = new Button("2. Plateau de Jeu");
        Button btnLog = new Button("3. Journal d'Actions");

        btnConnexion.setOnAction(e -> mainLayout.setCenter(createServerConnexionView()));
        btnGame.setOnAction(e -> mainLayout.setCenter(createGameView()));
        btnLog.setOnAction(e -> mainLayout.setCenter(createLogView()));

        VBox menu = new VBox(10);

        btnConnexion.setStyle(BUTTON_STYLE);
        btnGame.setStyle(BUTTON_STYLE);
        btnLog.setStyle(BUTTON_STYLE);

        btnConnexion.setAlignment(Pos.CENTER);
        btnGame.setAlignment(Pos.CENTER);
        btnLog.setAlignment(Pos.CENTER);


        btnConnexion.setMaxHeight(Double.MAX_VALUE);
        btnGame.setMaxHeight(Double.MAX_VALUE);
        btnLog.setMaxHeight(Double.MAX_VALUE);
        btnConnexion.setMaxWidth(Double.MAX_VALUE);
        btnGame.setMaxWidth(Double.MAX_VALUE);
        btnLog.setMaxWidth(Double.MAX_VALUE);

        VBox.setVgrow(btnConnexion, Priority.ALWAYS);
        VBox.setVgrow(btnGame, Priority.ALWAYS);
        VBox.setVgrow(btnLog, Priority.ALWAYS);


        menu.getChildren().addAll(btnConnexion, btnGame, btnLog);

        menu.setMaxHeight(Double.MAX_VALUE);

        menu.setAlignment(Pos.TOP_CENTER);
        menu.setPadding(new Insets(0, 0, 0, 10));

        return menu;
    }

    private Node createServerConnexionView() {
        VBox view = new VBox(20);
        view.setAlignment(Pos.TOP_CENTER);
        view.setPadding(new Insets(50));

        Label title = new Label("Page 1: Connexion au Serveur de Jeu");
        title.setStyle("-fx-font-size: 24pt; -fx-font-weight: bold;");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setAlignment(Pos.CENTER);

        form.addRow(0, new Label("Nom de Joueur:"), new TextField("Player1"));

        Button btnConnect = new Button("Se connecter");
        btnConnect.setStyle("-fx-font-size: 14pt; -fx-background-color: #00897B; -fx-text-fill: white;");

        view.getChildren().addAll(title, form, btnConnect);
        return view;
    }

    // ----------------------------------------------------------------------

    /**
     * 2e page: Interface de Jeu. Mise à jour pour utiliser la classe Carte
     * et l'affichage cliquable.
     */
    private Node createGameView() {
        VBox view = new VBox(20);
        view.setAlignment(Pos.TOP_CENTER);
        view.setPadding(new Insets(20));

        Label title = new Label("Page 2: Plateau d'Échange et de Combat");
        title.setStyle("-fx-font-size: 24pt; -fx-font-weight: bold;");

        // --- 1. Zone Ma Main ---
        Label myHandTitle = new Label("Votre Main (Cartes Possédées)");
        myHandTitle.setStyle("-fx-font-size: 16pt; -fx-font-weight: bold;");

        HBox handDisplay = new HBox(15);
        handDisplay.setStyle("-fx-border-color: lightgray; -fx-padding: 10;");

        Carte c1 = new Carte("Carte A", 101, 1000, 500);
        Carte c2 = new Carte("Carte B", 102, 500, 1000);
        Carte c3 = new Carte("Carte C", 103, 750, 750);

        handDisplay.getChildren().addAll(
                createCardPlaceholder(c1),
                createCardPlaceholder(c2),
                createCardPlaceholder(c3)
        );

        Label actionsTitle = new Label("Actions Disponibles");
        actionsTitle.setStyle("-fx-font-size: 16pt; -fx-font-weight: bold;");
        HBox actionsBox = new HBox(20);
        actionsBox.setAlignment(Pos.CENTER);

        actionsBox.getChildren().addAll(
                new Button("Créer Nouvelle Carte"),
                new Button("Échanger Carte"),
                new Button("Combattre Carte")
        );

        Label playersTitle = new Label("Liste des Joueurs Connectés et Leurs Cartes");
        playersTitle.setStyle("-fx-font-size: 16pt; -fx-font-weight: bold;");

        TextArea playersDisplay = new TextArea("Joueur_A (ID cartes: 101, 102, 103)\nJoueur_B (ID cartes: 109,105)");
        playersDisplay.setEditable(false);
        playersDisplay.setPrefHeight(150);

        view.getChildren().addAll(title, myHandTitle, handDisplay, actionsTitle, actionsBox, playersTitle, playersDisplay);

        VBox.setVgrow(playersDisplay, Priority.ALWAYS);

        return view;
    }

    private VBox createCardPlaceholder(Carte carte) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.BOTTOM_CENTER);
        card.setPrefSize(120, 180);
        card.setStyle("-fx-border-color: black; -fx-border-width: 2; -fx-background-color: #CFD8DC; -fx-padding: 5;");

        VBox imgPlaceholder = new VBox();
        imgPlaceholder.setPrefHeight(120);
        imgPlaceholder.setAlignment(Pos.CENTER);
        Label placeholderLabel = new Label("img_placeholder");
        placeholderLabel.setStyle("-fx-text-fill: gray;");
        imgPlaceholder.getChildren().add(placeholderLabel);
        VBox.setVgrow(imgPlaceholder, Priority.ALWAYS);

        Label title = new Label(carte.nom);
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 12pt;");

        card.getChildren().addAll(imgPlaceholder, title);

        card.setOnMouseClicked(e -> showCardDetailsPopup(carte));

        return card;
    }


    private void showCardDetailsPopup(Carte carte) {
        final Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(mainLayout.getScene().getWindow());
        dialog.setTitle("Détails de la Carte: " + carte.nom);

        VBox dialogVbox = new VBox(10);
        dialogVbox.setAlignment(Pos.TOP_CENTER);
        dialogVbox.setPadding(new Insets(20));
        dialogVbox.setPrefSize(300, 400);

        VBox imgPlaceholder = new VBox();
        imgPlaceholder.setPrefSize(200, 200);
        imgPlaceholder.setAlignment(Pos.CENTER);
        imgPlaceholder.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-background-color: #E0E0E0;");
        Label placeholderLabel = new Label("Image Placeholder\n" + carte.nom);
        placeholderLabel.setAlignment(Pos.CENTER);
        imgPlaceholder.getChildren().add(placeholderLabel);

        Label statsLabel = new Label(
                "ID: " + carte.id + "\n" +
                        "Attaque (ATK): " + carte.attaque + "\n" +
                        "Défense (DEF): " + carte.defense
        );
        statsLabel.setStyle("-fx-font-size: 14pt; -fx-font-weight: bold;");

        Button closeButton = new Button("Fermer");
        closeButton.setOnAction(e -> dialog.close());

        dialogVbox.getChildren().addAll(imgPlaceholder, statsLabel, closeButton);

        Scene dialogScene = new Scene(dialogVbox);
        dialog.setScene(dialogScene);
        dialog.show();
    }


    // ----------------------------------------------------------------------

    private Node createLogView() {
        VBox view = new VBox(20);
        view.setAlignment(Pos.TOP_CENTER);
        view.setPadding(new Insets(30));

        Label title = new Label("Page 3: Journal du Serveur et des Résultats");
        title.setStyle("-fx-font-size: 24pt; -fx-font-weight: bold;");

        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setText("--- Début du Journal ---\n" +
                "[11:45:01] Tentative de connexion au serveur...\n" +
                "[11:45:02] Connexion réussie au serveur (" + SERVER_IP + ":" + SERVER_PORT + ").\n" +
                "[11:45:30] Demande 'Créer Carte' envoyée au serveur.\n" +
                "[11:45:31] SERVEUR: Carte ID 10 créée et ajoutée à votre main.");

        VBox.setVgrow(logArea, Priority.ALWAYS);

        view.getChildren().addAll(title, logArea);
        return view;
    }

    public static void main(String[] args) {
        launch(args);
    }
}