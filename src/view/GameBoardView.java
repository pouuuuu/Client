package view;

import controller.GameController;
import controller.GameObserver;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.CombatResult;
import model.GameState;

public class GameBoardView implements GameObserver {

    private ViewManager viewManager;
    private GameController controller;
    private VBox layout;

    private HBox handDisplay;
    private TextArea playersDisplay;

    public GameBoardView(ViewManager viewManager, GameController controller) {
        this.viewManager = viewManager;
        this.controller = controller;
        createLayout();
    }

    private void createLayout() {
        layout = new VBox(20);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setPadding(new Insets(20));

        Label title = new Label("Page 2: Plateau d'Échange et de Combat");
        title.setStyle("-fx-font-size: 24pt; -fx-font-weight: bold;");

        // --- MA MAIN ---
        Label myHandTitle = new Label("Votre Main (Cartes Possédées)");
        myHandTitle.setStyle("-fx-font-size: 16pt; -fx-font-weight: bold;");

        handDisplay = new HBox(15);
        handDisplay.setStyle("-fx-border-color: lightgray; -fx-padding: 10;");
        handDisplay.setPrefHeight(200);
        handDisplay.setAlignment(Pos.CENTER_LEFT);

        // --- ACTIONS ---
        Label actionsTitle = new Label("Actions Disponibles");
        actionsTitle.setStyle("-fx-font-size: 16pt; -fx-font-weight: bold;");

        HBox actionsBox = new HBox(20);
        actionsBox.setAlignment(Pos.CENTER);

        Button btnCreate = new Button("Créer Nouvelle Carte");
        Button btnTrade = new Button("Échanger Carte");
        Button btnFight = new Button("Combattre Carte");

        // MODIFICATION ICI : Ouverture de la fenêtre de création
        btnCreate.setOnAction(e -> openCreateCardDialog());

        // Actions temporaires pour les autres boutons
        btnTrade.setOnAction(e -> controller.exchangeCard(1, 2, 1));
        btnFight.setOnAction(e -> controller.combatCard(1, 2, 1));

        actionsBox.getChildren().addAll(btnCreate, btnTrade, btnFight);

        // --- LISTE JOUEURS ---
        Label playersTitle = new Label("Liste des Joueurs Connectés");
        playersTitle.setStyle("-fx-font-size: 16pt; -fx-font-weight: bold;");

        playersDisplay = new TextArea();
        playersDisplay.setEditable(false);
        playersDisplay.setPrefHeight(150);

        layout.getChildren().addAll(title, myHandTitle, handDisplay, actionsTitle, actionsBox, playersTitle, playersDisplay);
        VBox.setVgrow(playersDisplay, Priority.ALWAYS);
    }

    // --- NOUVELLE FENÊTRE DE CRÉATION ---
    private void openCreateCardDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL); // Bloque la fenêtre principale
        dialog.initOwner(viewManager.getStage());
        dialog.setTitle("Création de Carte");

        VBox form = new VBox(15);
        form.setPadding(new Insets(20));
        form.setAlignment(Pos.CENTER);

        Label lblTitle = new Label("Configurer la Carte");
        lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        TextField nameField = new TextField();
        nameField.setPromptText("Nom de la carte");

        TextField hpField = new TextField();
        hpField.setPromptText("Points de Vie (HP)");

        TextField apField = new TextField();
        apField.setPromptText("Attaque (AP)");

        TextField dpField = new TextField();
        dpField.setPromptText("Défense (DP)");

        Button btnConfirm = new Button("Envoyer au Serveur");
        btnConfirm.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

        Label errorLbl = new Label();
        errorLbl.setStyle("-fx-text-fill: red;");

        btnConfirm.setOnAction(e -> {
            try {
                String name = nameField.getText();
                if (name.isEmpty()) throw new IllegalArgumentException("Nom vide");

                int hp = Integer.parseInt(hpField.getText());
                int ap = Integer.parseInt(apField.getText());
                int dp = Integer.parseInt(dpField.getText());

                // Appel au contrôleur
                controller.createCard(name, ap, dp, hp);

                dialog.close(); // Ferme la fenêtre si tout va bien
            } catch (NumberFormatException ex) {
                errorLbl.setText("Les stats doivent être des nombres entiers.");
            } catch (IllegalArgumentException ex) {
                errorLbl.setText("Le nom est obligatoire.");
            }
        });

        form.getChildren().addAll(lblTitle, nameField, hpField, apField, dpField, btnConfirm, errorLbl);
        Scene scene = new Scene(form, 300, 400);
        dialog.setScene(scene);
        dialog.show();
    }

    public Node getView() { return layout; }

    // --- MISE A JOUR UI (Observer) ---
    public void refresh() {
        handDisplay.getChildren().clear();
        PlayerViewModel p = controller.getCurrentPlayerViewModel();
        if (p != null && p.getCards() != null) {
            for (CardViewModel c : p.getCards()) {
                handDisplay.getChildren().add(createCardPlaceholder(c));
            }
        }
        StringBuilder sb = new StringBuilder();
        for (PlayerViewModel op : controller.getOtherPlayersViewModels()) {
            sb.append(op.getName()).append(" (ID: ").append(op.getId()).append(")\n");
        }
        playersDisplay.setText(sb.toString());
    }

    private VBox createCardPlaceholder(CardViewModel carte) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.BOTTOM_CENTER);
        card.setPrefSize(120, 180);
        card.setStyle("-fx-border-color: black; -fx-border-width: 2; -fx-background-color: #CFD8DC; -fx-padding: 5;");
        VBox img = new VBox();
        img.setPrefHeight(120);
        img.setAlignment(Pos.CENTER);
        img.getChildren().add(new Label("IMG"));
        VBox.setVgrow(img, Priority.ALWAYS);
        Label title = new Label(carte.getName());
        title.setStyle("-fx-font-weight: bold;");
        card.getChildren().addAll(img, title);

        // Popup Détails (inchangé)
        card.setOnMouseClicked(e -> showCardDetails(carte));
        return card;
    }

    private void showCardDetails(CardViewModel c) { /* Ton code de popup détails existant */ }

    // --- OBSERVER ---
    @Override public void onGameStateChanged(GameState gameState) { Platform.runLater(this::refresh); }
    @Override public void onError(String msg) {} // Géré par LogView
    @Override public void onExchangeRequestReceived(String r, int f, String n, int o, int req) {}
    @Override public void onCombatRequestReceived(String r, int f, String n, int a, int t) {}
    @Override public void onCombatCompleted(CombatResult result) {}
    @Override public void onExchangeRequestReceived(String r, String f, String n, String o, String req) {}
    @Override public void onCombatRequestReceived(String r, String f, String n, String a, String t) {}
}