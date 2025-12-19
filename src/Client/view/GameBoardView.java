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

    //containers
    private GridPane myHandContainer;
    private GridPane opponentHandContainer;
    private VBox centerContainer;
    private VBox notificationBox;

    //viewmodels for cards and players
    //TODO : make it pass through controller
    private CardViewModel selectedMyCard = null;
    private CardViewModel selectedOpponentCard = null;
    private PlayerViewModel selectedOpponent = null;

    private Label lblMySelection;
    private Label lblOppSelection;
    private Label lblOppCardSelection;

    private Button btnCombat;
    private Button btnTrade;

    public GameBoardView(ViewManager viewManager, GameController controller) {
        this.viewManager = viewManager;
        this.controller = controller;
    }

    public Node getView() {
        mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(10));

        Label title = new Label("PLATEAU DE JEU");
        title.setStyle("-fx-font-size: 28pt; -fx-font-weight: bold; -fx-text-fill: #006064; -fx-effect: dropshadow(gaussian, rgba(255,255,255,0.5), 0, 0, 0, 1);");
        BorderPane.setAlignment(title, Pos.CENTER);
        mainLayout.setTop(title);

        HBox body = new HBox(20);
        body.setAlignment(Pos.CENTER);
        body.setPadding(new Insets(20));
        HBox.setHgrow(body, Priority.ALWAYS);

        //my hand
        VBox leftSection = createMyHandSection();
        HBox.setHgrow(leftSection, Priority.ALWAYS);
        leftSection.setPrefWidth(450);

        //actions
        VBox centerSection = createCenterSection();
        centerSection.setPrefWidth(350);
        centerSection.setMinWidth(300);

        //other players hand
        VBox rightSection = createOpponentHandSection();
        HBox.setHgrow(rightSection, Priority.ALWAYS);
        rightSection.setPrefWidth(450);

        body.getChildren().addAll(leftSection, centerSection, rightSection);
        mainLayout.setCenter(body);

        //init view
        refresh();

        return mainLayout;
    }

    //my hand
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

        //for card view
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

    //actions
    private VBox createCenterSection() {
        centerContainer = new VBox(20);
        centerContainer.setAlignment(Pos.TOP_CENTER);
        centerContainer.setPadding(new Insets(15));
        centerContainer.setStyle("-fx-background-color: rgba(0,0,0,0.1); -fx-background-radius: 15; -fx-border-color: #FFC107; -fx-border-width: 3; -fx-border-radius: 12;");

        Label title = new Label("JOUEURS CONNECTÉS");
        title.setStyle("-fx-font-size: 16pt; -fx-font-weight: bold; -fx-text-fill: #FF6F00;");

        //playerlist
        VBox playersList = new VBox(10);
        playersList.setId("playersList");
        playersList.setAlignment(Pos.CENTER);

        Separator sep = new Separator();

        //selection zone
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

    //other player hand
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

    //update datas
    public void refresh() {
        if (myHandContainer == null || opponentHandContainer == null) {
            return;
        }

        myHandContainer.getChildren().clear();
        PlayerViewModel me = controller.getCurrentPlayerViewModel();

        if (me != null && me.getCards() != null) {
            int col = 0, row = 0;
            for (CardViewModel c : me.getCards()) {
                boolean isSelected = (selectedMyCard != null && selectedMyCard.getId() == c.getId());
                if (isSelected) selectedMyCard = c;

                CardView cv = new CardView(c, 1.2, isSelected, () -> {
                    if (isSelected) {
                        this.selectedMyCard = null;
                    } else {
                        this.selectedMyCard = c;
                    }
                    refresh();
                });

                myHandContainer.add(cv, col, row);
                col++;
                if (col > 2) { col = 0; row++; }
            }
        }

        VBox playersList = (VBox) centerContainer.lookup("#playersList");
        if (playersList != null) {
            playersList.getChildren().clear();

            java.util.ArrayList<PlayerViewModel> freshPlayers = controller.getOtherPlayersViewModels();
            PlayerViewModel refreshedOpponent = null;

            for (PlayerViewModel p : freshPlayers) {
                if (selectedOpponent != null && p.getId() == selectedOpponent.getId()) {
                    refreshedOpponent = p;
                }

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
                    refresh(); // Appel direct
                });
                playersList.getChildren().add(pBtn);
            }

            if (selectedOpponent != null) {
                selectedOpponent = refreshedOpponent;
                if (selectedOpponent == null) selectedOpponentCard = null;
            }
        }

        opponentHandContainer.getChildren().clear();
        if (selectedOpponent != null) {
            int col = 0, row = 0;
            for (CardViewModel c : selectedOpponent.getCards()) {
                boolean isSelected = (selectedOpponentCard != null && selectedOpponentCard.getId() == c.getId());
                if (isSelected) selectedOpponentCard = c;

                CardView cv = new CardView(c, 1.2, isSelected, () -> {
                    if (isSelected) {
                        this.selectedOpponentCard = null;
                    } else {
                        this.selectedOpponentCard = c;
                    }
                    refresh(); // Appel direct
                });

                opponentHandContainer.add(cv, col, row);
                col++;
                if (col > 2) { col = 0; row++; }
            }
        }
        updateControlBar();
    }

    private void updateControlBar() {
        // Mettre à jour les textes
        lblMySelection.setText("Moi: " + (selectedMyCard != null ? selectedMyCard.getName() : "Aucune"));
        lblOppSelection.setText("Adv: " + (selectedOpponent != null ? selectedOpponent.getName() : "Aucun"));
        lblOppCardSelection.setText("Carte Adv: " + (selectedOpponentCard != null ? selectedOpponentCard.getName() : "Aucune"));

        // Activer les boutons seulement si TOUT est sélectionné
        boolean ready = (selectedMyCard != null && selectedOpponent != null && selectedOpponentCard != null);
        btnCombat.setDisable(!ready);
        btnTrade.setDisable(!ready);

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
                String nom = tfName.getText().trim();

                if (nom.isEmpty()) {
                    showAlert("Erreur", "Le nom de la carte ne peut pas être vide.");
                    return;
                }
                if (nom.length() > 50) {
                    showAlert("Erreur", "Le nom est trop long (Max 50 caractères).");
                    return;
                }
                if (!nom.matches("[a-zA-Z_ ]+")) {
                    showAlert("Erreur", "Le nom contient des caractères interdits.\nAutorisé : Lettres, espaces, underscore (_).");
                    return;
                }

                int hp = Integer.parseInt(tfHP.getText());
                int ap = Integer.parseInt(tfAP.getText());
                int dp = Integer.parseInt(tfDP.getText());

                if ( hp > 100 || ap > 100 || dp > 100) {
                    showAlert("Erreur", "Les statistiques des cartes ne peuvent pas dépasser 100.");
                }

                controller.sendCreateCard(nom, ap, dp, hp);
                dialog.close();

            } catch (NumberFormatException ex) {
                showAlert("Erreur", "Les statistiques (HP, ATK, DEF) doivent être des nombres entiers.");
            } catch (Exception ex) {
                System.err.println("Error: " + ex.getMessage());
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

        Label title = new Label("ÉCHANGE");
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void showInfoPopup(String msg, String colorHex) {
        Platform.runLater(() -> {
            Label lbl = new Label(msg);
            lbl.setWrapText(true);
            lbl.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-padding: 10; -fx-background-radius: 5;");
            lbl.setMaxWidth(300);

            // Auto-destruction au clic
            lbl.setOnMouseClicked(e -> notificationBox.getChildren().remove(lbl));

            notificationBox.getChildren().add(0, lbl);
        });
    }

    public void showTradePopup(String message, Runnable onAccept, Runnable onDeny) {
        Platform.runLater(() -> {
            if (notificationBox == null) return;

            VBox panel = new VBox(15);
            panel.setAlignment(Pos.CENTER);
            panel.setPadding(new Insets(15));
            panel.setMaxWidth(300);
            panel.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-border-color: #FF9800; -fx-border-width: 3;" +
                            "-fx-background-radius: 10; -fx-border-radius: 10;" +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 0);"
            );

            Label lblMsg = new Label(message);
            lblMsg.setWrapText(true);
            lblMsg.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-alignment: center;");

            Button btnYes = new Button("OUI");
            btnYes.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

            btnYes.setOnAction(e -> {
                notificationBox.getChildren().remove(panel); // Ferme ce popup
                onAccept.run();
            });

            Button btnNo = new Button("NON");
            btnNo.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

            btnNo.setOnAction(e -> {
                notificationBox.getChildren().remove(panel); // Ferme ce popup
                onDeny.run();
            });

            HBox buttons = new HBox(15, btnYes, btnNo);
            buttons.setAlignment(Pos.CENTER);

            panel.getChildren().addAll(lblMsg, buttons);

            notificationBox.getChildren().add(0, panel);
        });
    }
    public void showFightPopup(String message, Runnable onAccept, Runnable onDeny) {
        Platform.runLater(() -> {
            if (notificationBox == null) return;

            VBox panel = new VBox(15);
            panel.setAlignment(Pos.CENTER);
            panel.setPadding(new Insets(15));
            panel.setMaxWidth(320);

            panel.setStyle(
                    "-fx-background-color: #2c0b0e;" +
                            "-fx-border-color: #FF0000; -fx-border-width: 2;" +
                            "-fx-background-radius: 10; -fx-border-radius: 10;" +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 10, 0, 0, 0);"
            );

            Label lblMsg = new Label(message);
            lblMsg.setWrapText(true);
            lblMsg.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");

            Button btnFight = new Button("SE BATTRE");
            btnFight.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;");

            btnFight.setOnAction(e -> {
                notificationBox.getChildren().remove(panel);
                onAccept.run();
            });

            // --- BOUTON FUIR ---
            Button btnFlee = new Button("FUIR");
            btnFlee.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;");

            btnFlee.setOnAction(e -> {
                notificationBox.getChildren().remove(panel);
                onDeny.run();
            });

            HBox buttons = new HBox(15, btnFight, btnFlee);
            buttons.setAlignment(Pos.CENTER);

            panel.getChildren().addAll(lblMsg, buttons);

            notificationBox.getChildren().add(0, panel);
        });
    }

    public void showSuccessPopup(String message) {
        Platform.runLater(() -> {
            if (notificationBox == null) return;


            VBox panel = new VBox(15);
            panel.setAlignment(Pos.CENTER);
            panel.setPadding(new Insets(15));
            panel.setMaxWidth(300);
            panel.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-border-color: #4CAF50; -fx-border-width: 3;" +
                            "-fx-background-radius: 10; -fx-border-radius: 10;" +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 0);"
            );

            Label lblMsg = new Label(message);
            lblMsg.setWrapText(true);
            lblMsg.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-alignment: center; -fx-text-fill: #2E7D32;");

            Button btnOk = new Button("OK");
            btnOk.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

            btnOk.setOnAction(e -> {
                notificationBox.getChildren().remove(panel);
            });

            panel.getChildren().addAll(lblMsg, btnOk);

            notificationBox.getChildren().add(0, panel);
        });
    }

    public void showErrorPopup(String message) {
        Platform.runLater(() -> {
            if (notificationBox == null) return;


            VBox panel = new VBox(15);
            panel.setAlignment(Pos.CENTER);
            panel.setPadding(new Insets(15));
            panel.setMaxWidth(300);
            panel.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-border-color: #F44336; -fx-border-width: 3;" +
                            "-fx-background-radius: 10; -fx-border-radius: 10;" +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 0);"
            );

            Label lblMsg = new Label(message);
            lblMsg.setWrapText(true);
            lblMsg.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-alignment: center; -fx-text-fill: #D32F2F;");

            Button btnOk = new Button("OK");
            btnOk.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

            btnOk.setOnAction(e -> {
                notificationBox.getChildren().remove(panel);
            });

            panel.getChildren().addAll(lblMsg, btnOk);

            notificationBox.getChildren().add(0, panel);
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