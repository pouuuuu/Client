package view;

import controller.GameController;
import controller.GameObserver;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import model.CombatResult;
import model.GameState;

public class TradeView implements GameObserver {
    private ViewManager viewManager;
    private GameController controller;

    public TradeView(ViewManager viewManager, GameController controller) {
        this.viewManager = viewManager;
        this.controller = controller;
    }

    public Node getView() {
        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: linear-gradient(to bottom right, #1A237E, #3949AB);");

        // Top
        Label title = new Label("ZONE D'ÉCHANGE");
        title.setStyle("-fx-font-size: 30pt; -fx-text-fill: white; -fx-font-weight: bold;");

        Button backBtn = new Button("⬅ Retour");
        backBtn.setOnAction(e -> viewManager.showGameView());
        backBtn.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white;");

        VBox topBox = new VBox(10, backBtn, title);
        topBox.setAlignment(Pos.CENTER_LEFT);
        layout.setTop(topBox);

        // Center: 2 colonnes (Moi vs Eux)
        HBox content = new HBox(30);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20));

        // Ma Collection
        VBox myCol = createCollectionBox("MA COLLECTION", true);
        HBox.setHgrow(myCol, Priority.ALWAYS);

        // Zone centrale (Boutons action)
        VBox actions = new VBox(20);
        actions.setAlignment(Pos.CENTER);
        Button btnTrade = new Button("Proposer Échange ➡");
        btnTrade.setStyle("-fx-background-color: gold; -fx-font-size: 14pt; -fx-font-weight: bold;");
        actions.getChildren().add(btnTrade);

        // Collection Adverse (Vide pour l'instant ou liste globale)
        VBox oppCol = createCollectionBox("OFFRE ADVERSE", false);
        HBox.setHgrow(oppCol, Priority.ALWAYS);

        content.getChildren().addAll(myCol, actions, oppCol);
        layout.setCenter(content);

        return layout;
    }

    private VBox createCollectionBox(String title, boolean isMine) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 10;");

        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: white; -fx-font-size: 16pt; -fx-font-weight: bold;");

        TilePane grid = new TilePane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPrefColumns(3);

        // Remplir avec mes cartes si c'est ma collection
        if (isMine) {
            PlayerViewModel me = controller.getCurrentPlayerViewModel();
            if (me != null && me.getCards() != null) {
                for (CardViewModel c : me.getCards()) {
                    // Utilisation de la nouvelle CardView !
                    CardView cv = new CardView(c, 0.8); // 0.8 scale pour que ce soit plus petit
                    grid.getChildren().add(cv);
                }
            }
        }

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        box.getChildren().addAll(lbl, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return box;
    }

    @Override
    public void onGameStateChanged(GameState gameState) {

    }

    @Override
    public void onError(String errorMessage) {

    }

    @Override
    public void onExchangeRequestReceived(String requestId, int fromPlayerId, String fromPlayerName, int offeredCardId, int requestedCardId) {

    }

    @Override
    public void onCombatRequestReceived(String requestId, int fromPlayerId, String fromPlayerName, int attackingCardId, int targetCardId) {

    }

    @Override
    public void onCombatCompleted(CombatResult result) {

    }
}