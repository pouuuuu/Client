package Client.view;

import Client.controller.GameController;
import Client.controller.GameObserver;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import Client.model.CombatResult;
import Client.model.GameState;

public class GameBoardView implements GameObserver {

    private ViewManager viewManager;
    private GameController controller;
    private BorderPane mainLayout;

    // Zones d'affichage
    private GridPane myHandContainer;
    private GridPane opponentHandContainer;
    private VBox centerContainer;
    private VBox notificationBox;

    // État de la sélection locale (pour l'UI uniquement)
    private CardViewModel selectedMyCard = null;
    private CardViewModel selectedOpponentCard = null;
    private PlayerViewModel selectedOpponent = null;

    // Labels d'info
    private Label lblMySelection;
    private Label lblOppSelection;
    private Label lblOppCardSelection;

    // Boutons d'action
    private Button btnCombat;
    private Button btnTrade;

    public GameBoardView(ViewManager viewManager, GameController controller) {
        this.viewManager = viewManager;
        this.controller = controller;
    }

    public Node getView() {
        mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(10));

        // --- EN-TÊTE ---
        Label title = new Label("PLATEAU DE JEU");
        title.setStyle("-fx-font-size: 28pt; -fx-font-weight: bold; -fx-text-fill: #006064; -fx-effect: dropshadow(gaussian, rgba(255,255,255,0.5), 0, 0, 0, 1);");
        BorderPane.setAlignment(title, Pos.CENTER);
        mainLayout.setTop(title);

        // --- CORPS (3 Colonnes) ---
        HBox body = new HBox(20);
        body.setAlignment(Pos.CENTER);
        body.setPadding(new Insets(20));
        HBox.setHgrow(body, Priority.ALWAYS);

        // 1. GAUCHE : Ma Main
        VBox leftSection = createMyHandSection();
        HBox.setHgrow(leftSection, Priority.ALWAYS);
        leftSection.setPrefWidth(450);

        // 2. CENTRE : Joueurs & Actions
        VBox centerSection = createCenterSection();
        centerSection.setPrefWidth(350);
        centerSection.setMinWidth(300);

        // 3. DROITE : Main Adversaire
        VBox rightSection = createOpponentHandSection();
        HBox.setHgrow(rightSection, Priority.ALWAYS);
        rightSection.setPrefWidth(450);

        body.getChildren().addAll(leftSection, centerSection, rightSection);
        mainLayout.setCenter(body);

        // Initialisation de l'affichage
        refresh();

        return mainLayout;
    }

    // =========================================================================
    // SECTION 1 : MA MAIN (GAUCHE)
    // =========================================================================

    private VBox createMyHandSection() {
        VBox section = new VBox(15);
        section.setAlignment(Pos.TOP_CENTER);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: rgba(255,255,255,0.6); -fx-background-radius: 15; -fx-border-color: #4CAF50; -fx-border-width: 3; -fx-border-radius: 12;");

        Label title = new Label("VOTRE MAIN");
        title.setStyle("-fx-font-size: 18pt; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        Button btnCreate = new Button("＋ Créer Carte");
        btnCreate.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnCreate.setOnAction(e -> openCreateCardDialog());

        // Grille pour les cartes (3 colonnes)
        myHandContainer = new GridPane();
        myHandContainer.setHgap(10);
        myHandContainer.setVgap(10);
        myHandContainer.setAlignment(Pos.TOP_CENTER);

        ScrollPane scroll = new ScrollPane(myHandContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        section.getChildren().addAll(title, btnCreate, scroll);
        return section;
    }

    // =========================================================================
    // SECTION 2 : CENTRE (JOUEURS ET ACTIONS)
    // =========================================================================

    private VBox createCenterSection() {
        centerContainer = new VBox(20);
        centerContainer.setAlignment(Pos.TOP_CENTER);
        centerContainer.setPadding(new Insets(15));
        centerContainer.setStyle("-fx-background-color: rgba(0,0,0,0.1); -fx-background-radius: 15; -fx-border-color: #FFC107; -fx-border-width: 3; -fx-border-radius: 12;");

        Label title = new Label("JOUEURS CONNECTÉS");
        title.setStyle("-fx-font-size: 16pt; -fx-font-weight: bold; -fx-text-fill: #FF6F00;");

        // Liste des joueurs (sera remplie dynamiquement)
        VBox playersList = new VBox(10);
        playersList.setId("playersList"); // Pour le retrouver lors du refresh
        playersList.setAlignment(Pos.CENTER);

        Separator sep = new Separator();

        // Zone de sélection
        VBox selectionBox = new VBox(10);
        selectionBox.setAlignment(Pos.CENTER);
        selectionBox.setStyle("-fx-background-color: rgba(255,255,255,0.3); -fx-padding: 10; -fx-background-radius: 10;");

        Label selTitle = new Label("SÉLECTION");
        selTitle.setStyle("-fx-font-weight: bold; -fx-underline: true;");

        lblMySelection = new Label("Moi: Aucune");
        lblMySelection.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");

        lblOppSelection = new Label("Adv: Aucun");
        lblOppSelection.setStyle("-fx-text-fill: #1565C0; -fx-font-weight: bold;");

        lblOppCardSelection = new Label("Carte Adv: Aucune");
        lblOppCardSelection.setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold;");

        selectionBox.getChildren().addAll(selTitle, lblMySelection, lblOppSelection, lblOppCardSelection);

        notificationBox = new VBox(10);
        notificationBox.setAlignment(Pos.CENTER);
        notificationBox.setMinHeight(0);

        // Boutons d'action
        btnCombat = new Button("COMBAT");
        btnCombat.setStyle("-fx-background-color: #D32F2F; -fx-text-fill: white; -fx-font-size: 14pt; -fx-font-weight: bold;");
        btnCombat.setMaxWidth(Double.MAX_VALUE);
        btnCombat.setOnAction(e -> openCombatConfirmation());

        btnTrade = new Button("ÉCHANGE");
        btnTrade.setStyle("-fx-background-color: #FBC02D; -fx-text-fill: black; -fx-font-size: 14pt; -fx-font-weight: bold;");
        btnTrade.setMaxWidth(Double.MAX_VALUE);
        btnTrade.setOnAction(e -> openTradeConfirmation());

        // Désactivés par défaut tant que pas de sélection complète
        btnCombat.setDisable(true);
        btnTrade.setDisable(true);

        centerContainer.getChildren().addAll(title, playersList, sep, selectionBox, btnCombat, btnTrade, notificationBox);
        return centerContainer;
    }

    // =========================================================================
    // SECTION 3 : MAIN ADVERSAIRE (DROITE)
    // =========================================================================

    private VBox createOpponentHandSection() {
        VBox section = new VBox(15);
        section.setAlignment(Pos.TOP_CENTER);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: rgba(255,255,255,0.6); -fx-background-radius: 15; -fx-border-color: #D32F2F; -fx-border-width: 3; -fx-border-radius: 12;");

        Label title = new Label("MAIN ADVERSE");
        title.setStyle("-fx-font-size: 18pt; -fx-font-weight: bold; -fx-text-fill: #C62828;");

        Label info = new Label("Cliquez sur un joueur pour voir ses cartes");
        info.setStyle("-fx-font-style: italic; -fx-text-fill: grey;");

        opponentHandContainer = new GridPane();
        opponentHandContainer.setHgap(10);
        opponentHandContainer.setVgap(10);
        opponentHandContainer.setAlignment(Pos.TOP_CENTER);

        ScrollPane scroll = new ScrollPane(opponentHandContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        section.getChildren().addAll(title, info, scroll);
        return section;
    }

    // =========================================================================
    // LOGIQUE DE MISE A JOUR (REFRESH)
    // =========================================================================

    // Dans src/view/GameBoardView.java

    public void refresh() {
        // --- CORRECTIF : Vérification de sécurité ---
        // Si l'interface n'est pas encore initialisée (getView() n'a pas encore été appelé),
        // on arrête ici pour éviter le NullPointerException.
        if (myHandContainer == null || opponentHandContainer == null) {
            return;
        }

        // --- Le reste de votre code refresh() reste identique ---

        // 1. Rafraichir MA MAIN
        myHandContainer.getChildren().clear();
        PlayerViewModel me = controller.getCurrentPlayerViewModel();

        if (me != null && me.getCards() != null) {
            int col = 0, row = 0;
            for (CardViewModel c : me.getCards()) {
                // Vérifie si cette carte est celle sélectionnée
                boolean isSelected = (selectedMyCard != null && selectedMyCard.getId() == c.getId());

                // Création de la Vue avec le Callback
                // NOTE : Assurez-vous d'avoir bien le constructeur compatible dans CardView ou d'utiliser celui à 4 paramètres
                CardView cv = new CardView(c, 1.2, isSelected, () -> {
                    if (isSelected) {
                        this.selectedMyCard = null;
                    } else {
                        this.selectedMyCard = c;
                    }
                    updateSelectionUI();
                });

                myHandContainer.add(cv, col, row);
                col++;
                if (col > 2) { col = 0; row++; }
            }
        }

        // 2. Rafraichir LISTE JOUEURS (Centre)
        VBox playersList = (VBox) centerContainer.lookup("#playersList");
        if (playersList != null) {
            playersList.getChildren().clear();
            for (PlayerViewModel p : controller.getOtherPlayersViewModels()) {
                Button pBtn = new Button(p.getName() + " (" + p.getCardCount() + ")");
                pBtn.setMaxWidth(Double.MAX_VALUE);

                if (selectedOpponent != null && selectedOpponent.getId() == p.getId()) {
                    pBtn.setStyle("-fx-background-color: #1565C0; -fx-text-fill: white; -fx-font-weight: bold;");
                } else {
                    pBtn.setStyle("-fx-background-color: #90CAF9; -fx-text-fill: black;");
                }

                pBtn.setOnAction(e -> {
                    this.selectedOpponent = p;
                    this.selectedOpponentCard = null;
                    updateSelectionUI();
                });
                playersList.getChildren().add(pBtn);
            }
        }

        // 3. Rafraichir MAIN ADVERSAIRE (Droite)
        opponentHandContainer.getChildren().clear();
        if (selectedOpponent != null) {
            int col = 0, row = 0;
            for (CardViewModel c : selectedOpponent.getCards()) {
                boolean isSelected = (selectedOpponentCard != null && selectedOpponentCard.getId() == c.getId());

                CardView cv = new CardView(c, 1.2, isSelected, () -> {
                    if (isSelected) {
                        this.selectedOpponentCard = null;
                    } else {
                        this.selectedOpponentCard = c;
                    }
                    updateSelectionUI();
                });

                opponentHandContainer.add(cv, col, row);
                col++;
                if (col > 2) { col = 0; row++; }
            }
        }
    }

    private void updateSelectionUI() {
        // Mettre à jour les textes
        lblMySelection.setText("Moi: " + (selectedMyCard != null ? selectedMyCard.getName() : "Aucune"));
        lblOppSelection.setText("Adv: " + (selectedOpponent != null ? selectedOpponent.getName() : "Aucun"));
        lblOppCardSelection.setText("Carte Adv: " + (selectedOpponentCard != null ? selectedOpponentCard.getName() : "Aucune"));

        // Activer les boutons seulement si TOUT est sélectionné
        boolean ready = (selectedMyCard != null && selectedOpponent != null && selectedOpponentCard != null);
        btnCombat.setDisable(!ready);
        btnTrade.setDisable(!ready);

        // Rafraichir les bordures des cartes
        refresh();
    }

    // =========================================================================
    // POPUPS & DIALOGUES
    // =========================================================================

    private void openCreateCardDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(viewManager.getStage());
        dialog.setTitle("Créer une carte");

        VBox form = new VBox(10);
        form.setPadding(new Insets(20));
        form.setAlignment(Pos.CENTER);

        TextField tfName = new TextField(); tfName.setPromptText("Nom de la carte");
        TextField tfHP = new TextField("100"); tfHP.setPromptText("HP");
        TextField tfAP = new TextField("50"); tfAP.setPromptText("ATK");
        TextField tfDP = new TextField("50"); tfDP.setPromptText("DEF");

        Button btnOk = new Button("Créer");
        btnOk.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        btnOk.setOnAction(e -> {
            try {
                String nom = tfName.getText();
                int hp = Integer.parseInt(tfHP.getText());
                int ap = Integer.parseInt(tfAP.getText());
                int dp = Integer.parseInt(tfDP.getText());
                controller.sendCreateCard(nom, ap, dp, hp);
                dialog.close();
            } catch (Exception ex) {
                System.err.println("Erreur saisie: " + ex.getMessage());
            }
        });

        form.getChildren().addAll(new Label("Nouvelle Carte"), tfName, tfHP, tfAP, tfDP, btnOk);
        dialog.setScene(new Scene(form, 250, 300));
        dialog.show();
    }

    private void openCombatConfirmation() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Confirmation Combat");

        VBox box = new VBox(15);
        box.setPadding(new Insets(20));
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: #FFEBEE;");

        Label title = new Label("COMBAT");
        title.setStyle("-fx-font-size: 20px; -fx-text-fill: #C62828; -fx-font-weight: bold;");

        Label desc = new Label("Voulez-vous attaquer " + selectedOpponent.getName() + " ?\n" +
                "Votre carte : " + selectedMyCard.getName() + " (ATK: " + selectedMyCard.getAttack() + ")\n" +
                "Contre : " + selectedOpponentCard.getName());
        desc.setStyle("-fx-text-alignment: center;");

        HBox btns = new HBox(10);
        btns.setAlignment(Pos.CENTER);
        Button btnYes = new Button("ATTAQUER");
        btnYes.setStyle("-fx-background-color: #D32F2F; -fx-text-fill: white; -fx-font-weight: bold;");
        btnYes.setOnAction(e -> {
            controller.sendFightCard(selectedMyCard.getId(), selectedOpponent.getId(), selectedOpponentCard.getId());
            dialog.close();
        });

        Button btnNo = new Button("Annuler");
        btnNo.setOnAction(e -> dialog.close());

        btns.getChildren().addAll(btnYes, btnNo);
        box.getChildren().addAll(title, desc, btns);

        dialog.setScene(new Scene(box, 400, 250));
        dialog.show();
    }

    private void openTradeConfirmation() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Confirmation Échange");

        VBox box = new VBox(15);
        box.setPadding(new Insets(20));
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: #FFF8E1;");

        Label title = new Label("🔄 ÉCHANGE 🔄");
        title.setStyle("-fx-font-size: 20px; -fx-text-fill: #F57F17; -fx-font-weight: bold;");

        Label desc = new Label("Proposer un échange à " + selectedOpponent.getName() + " ?\n" +
                "Vous donnez : " + selectedMyCard.getName() + "\n" +
                "Vous demandez : " + selectedOpponentCard.getName());
        desc.setStyle("-fx-text-alignment: center;");

        HBox btns = new HBox(10);
        btns.setAlignment(Pos.CENTER);
        Button btnYes = new Button("PROPOSER");
        btnYes.setStyle("-fx-background-color: #FBC02D; -fx-text-fill: black; -fx-font-weight: bold;");
        btnYes.setOnAction(e -> {
            controller.sendTradeCard(selectedMyCard.getId(), selectedOpponent.getId(), selectedOpponentCard.getId());
            dialog.close();
        });

        Button btnNo = new Button("Annuler");
        btnNo.setOnAction(e -> dialog.close());

        btns.getChildren().addAll(btnYes, btnNo);
        box.getChildren().addAll(title, desc, btns);

        dialog.setScene(new Scene(box, 400, 250));
        dialog.show();
    }

    public void showTradePopup(String message, Runnable onAccept, Runnable onDeny) {
        Platform.runLater(() -> {
            System.out.println("[VIEW] Affichage Popup Trade");
            if (notificationBox == null) {
                System.err.println("[VIEW] ERREUR: notificationBox est null !");
                return;
            }
            notificationBox.getChildren().clear();

            // Création du panneau visuel
            VBox panel = new VBox(15);
            panel.setAlignment(Pos.CENTER);
            panel.setPadding(new Insets(15));
            panel.setMaxWidth(300);

            // Style : Fond blanc, Bordure orange, Ombre
            panel.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-border-color: #FF9800; -fx-border-width: 3;" +
                            "-fx-background-radius: 10; -fx-border-radius: 10;" +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 0);"
            );

            // Le Message
            Label lblMsg = new Label(message);
            lblMsg.setWrapText(true);
            lblMsg.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-alignment: center;");

            // Les Boutons
            Button btnYes = new Button("✔ OUI");
            btnYes.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
            btnYes.setOnAction(e -> {
                clearNotification();
                onAccept.run();
            });

            Button btnNo = new Button("✖ NON");
            btnNo.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
            btnNo.setOnAction(e -> {
                clearNotification();
                onDeny.run();
            });

            HBox buttons = new HBox(15, btnYes, btnNo);
            buttons.setAlignment(Pos.CENTER);

            panel.getChildren().addAll(lblMsg, buttons);
            notificationBox.getChildren().add(panel);
        });
    }

    public void clearNotification() {
        Platform.runLater(() -> notificationBox.getChildren().clear());
    }

    // =========================================================================
    // OBSERVER METHODS
    // =========================================================================

    @Override public void onGameStateChanged(GameState gameState) { Platform.runLater(this::refresh); }
    @Override public void onError(String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, msg);
            alert.show();
        });
    }
    // Notifications simples pour l'instant (on pourrait ajouter des popups de réception ici aussi)
    @Override public void onExchangeRequestReceived(String r, int f, String n, int o, int req) {
        Platform.runLater(() -> {
            Alert alrt = new Alert(Alert.AlertType.INFORMATION, "Demande d'échange reçue de " + n);
            alrt.show();
        });
    }
    @Override public void onCombatRequestReceived(String r, int f, String n, int a, int t) {
        Platform.runLater(() -> {
            Alert alrt = new Alert(Alert.AlertType.WARNING, "Vous êtes attaqué par " + n + " !");
            alrt.show();
        });
    }
    @Override public void onCombatCompleted(CombatResult result) {
        Platform.runLater(() -> {
            Alert alrt = new Alert(Alert.AlertType.INFORMATION, "Combat terminé ! Dégâts : " + result.getDamageDealt());
            alrt.show();
            refresh(); // Rafraichir les PV
        });
    }
}