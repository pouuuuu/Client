package view;

import controller.GameController;
import controller.GameObserver;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.CombatResult;
import model.GameState;

public class GameBoardView implements GameObserver {

    private ViewManager viewManager;
    private GameController controller;
    private VBox layout;

    private HBox handDisplay;
    private HBox opponentCardsDisplay; // Pour afficher les cartes adverses (simulé ou réel)
    private VBox playersListDisplay; // Remplacement du TextArea par une liste de boutons

    public GameBoardView(ViewManager viewManager, GameController controller) {
        this.viewManager = viewManager;
        this.controller = controller;
        createLayout();
    }

    private void createLayout() {
        layout = new VBox(20);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setPadding(new Insets(20));

        // Titre
        Label title = new Label("Jeu de cartes");
        title.setStyle("-fx-font-size: 24pt; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, white, 2, 1.0, 0, 0);");

        // --- MA MAIN ---
        Label myHandTitle = new Label("Votre main");
        myHandTitle.setStyle("-fx-font-size: 16pt; -fx-font-weight: bold;");

        handDisplay = new HBox(15);
        styleCardContainer(handDisplay);

        // --- ACTIONS ---
        Label actionsTitle = new Label("Actions disponibles");
        actionsTitle.setStyle("-fx-font-size: 16pt; -fx-font-weight: bold;");

        HBox actionsBox = new HBox(20);
        actionsBox.setAlignment(Pos.CENTER);

        Button btnCreate = createActionButton("Créer une carte", "#4682B4");
        btnCreate.setOnAction(e -> openCreateCardDialog());

        // Boutons d'action génériques (logique à connecter selon besoin)
        Button btnTrade = createActionButton("Echanger", "#00897B");
        btnTrade.setOnAction(e -> viewManager.showTradeView());

        Button btnFight = createActionButton("Combattre", "#DC143C");
        btnFight.setOnAction(e -> viewManager.showCombatView());

        actionsBox.getChildren().addAll(btnCreate, btnTrade, btnFight);

        // --- LISTE JOUEURS ---
        Label playersTitle = new Label("Joueur(s) connecté(s)");
        playersTitle.setStyle("-fx-font-size: 16pt; -fx-font-weight: bold;");

        playersListDisplay = new VBox(10);
        playersListDisplay.setAlignment(Pos.CENTER_LEFT);

        layout.getChildren().addAll(title, myHandTitle, handDisplay, actionsTitle, actionsBox, playersTitle, playersListDisplay);
    }

    // --- STYLING METHODS (Adaptées de PlateauJeuPage) ---

    private void styleCardContainer(HBox box) {
        box.setStyle(
                "-fx-border-color: lightgray;" +
                        "-fx-border-radius: 15;" +
                        "-fx-background-radius: 15;" +
                        "-fx-padding: 10;" +
                        "-fx-background-color: rgba(255, 255, 255, 0.6);" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0, 0, 2);"
        );
        box.setMinHeight(260); // Hauteur min pour l'esthétique
        box.setAlignment(Pos.CENTER_LEFT);
    }

    private Button createActionButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-font-size: 14pt; -fx-font-weight: bold; -fx-background-color: " + color + "; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 8;");
        btn.setCursor(Cursor.HAND);
        return btn;
    }

    // --- LOGIQUE VISUELLE DES CARTES ---

    private VBox createCardPlaceholder(CardViewModel cardData) {
        // Facteur d'échelle pour adapter la taille
        double scale = 1.0;
        double width = 160 * scale;

        // Simulation d'une catégorie basée sur l'ID pour la couleur (1-5)
        int category = (cardData.getId() % 5) + 1;

        VBox card = new VBox(5);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(width);
        card.setStyle(getCategoryStyle(category));
        card.setCursor(Cursor.HAND);

        // Header
        Label title = new Label(cardData.getName());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 10pt; -fx-text-fill: #333333;");

        // Image Placeholder
        VBox imgPlaceholder = new VBox();
        imgPlaceholder.setPrefHeight(100);
        imgPlaceholder.setAlignment(Pos.CENTER);
        imgPlaceholder.setStyle("-fx-background-color: #F5F5F5; -fx-border-color: #333333; -fx-border-width: 1;");
        imgPlaceholder.getChildren().add(new Label("IMG"));

        // Health Bar (On suppose MaxHP = 200 pour l'exemple visuel, ou cardData.getHealth() si c'est le max)
        StackPane healthBar = createHealthBar(cardData.getHealth(), 200, width - 10);

        // Stats Row
        HBox statRow = new HBox(5);
        statRow.setAlignment(Pos.CENTER);
        statRow.getChildren().addAll(
                createStatBox("ATK", cardData.getAttack(), "#FF9800"),
                createStatBox("DEF", cardData.getDefense(), "#2196F3")
        );

        card.getChildren().addAll(title, imgPlaceholder, healthBar, statRow);

        // Interaction
        card.setOnMouseClicked(e -> showCardDetails(cardData));

        return card;
    }

    private String getCategoryStyle(int category) {
        String borderColor;
        String bgColor;
        switch (category) {
            case 1: bgColor = "#8BC34A"; borderColor = "#4CAF50"; break; // Nature
            case 2: bgColor = "#B0BEC5"; borderColor = "#78909C"; break; // Metal
            case 3: bgColor = "#FF8A65"; borderColor = "#F44336"; break; // Fire
            case 4: bgColor = "#9C27B0"; borderColor = "#673AB7"; break; // Dark
            default: bgColor = "#4FC3F7"; borderColor = "#03A9F4"; break; // Ice
        }
        return "-fx-border-color: " + borderColor + "; -fx-border-width: 3; -fx-background-color: " + bgColor + "; -fx-padding: 5; -fx-background-radius: 10; -fx-border-radius: 8;";
    }

    private StackPane createHealthBar(int current, int max, double width) {
        double percentage = Math.max(0, Math.min(1.0, (double) current / max));

        Rectangle bg = new Rectangle(width, 10);
        bg.setFill(javafx.scene.paint.Color.web("#BDBDBD"));
        bg.setArcWidth(5); bg.setArcHeight(5);

        Rectangle fg = new Rectangle(width * percentage, 10);
        fg.setFill(percentage > 0.5 ? javafx.scene.paint.Color.web("#4CAF50") : javafx.scene.paint.Color.web("#F44336"));
        fg.setArcWidth(5); fg.setArcHeight(5);

        StackPane pane = new StackPane(bg, fg);
        pane.setAlignment(Pos.CENTER_LEFT);
        return pane;
    }

    private HBox createStatBox(String label, int value, String color) {
        Label l = new Label(label + ": " + value);
        l.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-padding: 2 4; -fx-font-size: 9pt; -fx-font-weight: bold; -fx-background-radius: 3;");
        return new HBox(l);
    }

    // --- POPUPS & DIALOGUES ---

    private void openCreateCardDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(viewManager.getStage());
        dialog.setTitle("Créer une nouvelle carte");

        VBox form = new VBox(15);
        form.setPadding(new Insets(20));
        form.setAlignment(Pos.CENTER_LEFT);
        form.setStyle("-fx-background-color: #ECEFF1;");

        TextField nameField = new TextField(); nameField.setPromptText("Nom (ex. Dragon)");
        TextField hpField = new TextField("100");
        TextField apField = new TextField("50");
        TextField dpField = new TextField("50");

        Button btnConfirm = createActionButton("Create", "#4CAF50");
        Label errorLbl = new Label(); errorLbl.setStyle("-fx-text-fill: red;");

        btnConfirm.setOnAction(e -> {
            try {
                if (nameField.getText().isEmpty()) throw new IllegalArgumentException("Nom requis");
                int hp = Integer.parseInt(hpField.getText());
                int ap = Integer.parseInt(apField.getText());
                int dp = Integer.parseInt(dpField.getText());
                controller.createCard(nameField.getText(), ap, dp, hp);
                dialog.close();
            } catch (Exception ex) {
                errorLbl.setText("Entrée invalide: " + ex.getMessage());
            }
        });

        form.getChildren().addAll(new Label("Détails des cartes:"), nameField, new Label("HP:"), hpField, new Label("AP:"), apField, new Label("DP:"), dpField, btnConfirm, errorLbl);
        dialog.setScene(new Scene(form, 300, 450));
        dialog.show();
    }

    private void showCardDetails(CardViewModel c) {
        // Implémentation simplifiée du popup de détails
        System.out.println("Clique sur " + c.getName());
    }

    public Node getView() { return layout; }

    // --- MISE A JOUR UI (Observer) ---
    public void refresh() {
        handDisplay.getChildren().clear();
        playersListDisplay.getChildren().clear();

        // Afficher ma main
        PlayerViewModel me = controller.getCurrentPlayerViewModel();
        if (me != null && me.getCards() != null) {
            for (CardViewModel c : me.getCards()) {
                handDisplay.getChildren().add(createCardPlaceholder(c));
            }
        }

        // Afficher les autres joueurs (Boutons stylisés)
        for (PlayerViewModel op : controller.getOtherPlayersViewModels()) {
            Button playerBtn = new Button(op.getName() + " (" + op.getCardCount() + " cartes)");
            playerBtn.setStyle("-fx-background-color: #F08080; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 5 15; -fx-background-radius: 20;");
            playerBtn.setCursor(Cursor.HAND);
            playerBtn.setOnAction(e -> System.out.println("Joueur sélectionné: " + op.getName()));
            playersListDisplay.getChildren().add(playerBtn);
        }
    }

    @Override public void onGameStateChanged(GameState gameState) { Platform.runLater(this::refresh); }
    @Override public void onError(String msg) {}
    @Override public void onExchangeRequestReceived(String r, int f, String n, int o, int req) {}
    @Override public void onCombatRequestReceived(String r, int f, String n, int a, int t) {}
    @Override public void onCombatCompleted(CombatResult result) {}
    @Override public void onExchangeRequestReceived(String r, String f, String n, String o, String req) {}
    @Override public void onCombatRequestReceived(String r, String f, String n, String a, String t) {}
}