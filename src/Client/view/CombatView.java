package Client.view;

import Client.controller.GameController;
import Client.controller.GameObserver;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import Client.model.CombatResult;
import Client.model.GameState;

public class CombatView implements GameObserver {
    private ViewManager viewManager;
    private GameController controller;
    private BorderPane mainLayout;

    public CombatView(ViewManager viewManager, GameController controller) {
        this.viewManager = viewManager;
        this.controller = controller;
    }

    public Node getView() {
        mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(20));
        // Fond semi-transparent ou style global
        mainLayout.setStyle("-fx-background-color: linear-gradient(to bottom, #263238, #37474F);");

        // HEADER
        Label title = new Label("ARÈNE DE COMBAT");
        title.setStyle("-fx-font-size: 28pt; -fx-font-weight: bold; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, black, 5, 0, 0, 0);");

        Button backBtn = new Button("⬅ Retour au Plateau");
        backBtn.setOnAction(e -> viewManager.showGameView());
        backBtn.setStyle("-fx-background-color: #546E7A; -fx-text-fill: white; -fx-font-weight: bold;");

        BorderPane topBar = new BorderPane();
        topBar.setCenter(title);
        topBar.setLeft(backBtn);
        mainLayout.setTop(topBar);

        // CENTER (Arena)
        HBox arena = new HBox(40);
        arena.setAlignment(Pos.CENTER);

        // 1. Joueur (Gauche) - Données réelles
        PlayerViewModel me = controller.getCurrentPlayerViewModel();
        VBox mySide = createPlayerSection("VOUS", me != null ? me.getName() : "Moi", true);

        // 2. Zone VS (Centre)
        VBox vsZone = createVsZone();

        // 3. Adversaire (Droite) - Données simulées ou à récupérer
        // Ici on met un placeholder, à connecter avec le vrai adversaire sélectionné
        VBox oppSide = createPlayerSection("ADVERSAIRE", "Adversaire", false);

        arena.getChildren().addAll(mySide, vsZone, oppSide);
        mainLayout.setCenter(arena);

        return mainLayout;
    }

    private VBox createPlayerSection(String label, String playerName, boolean isMe) {
        VBox box = new VBox(15);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPadding(new Insets(20));
        box.setPrefWidth(300);
        box.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 15; -fx-border-color: " + (isMe ? "#4CAF50" : "#F44336") + "; -fx-border-width: 2;");

        Label nameLbl = new Label(playerName);
        nameLbl.setStyle("-fx-font-size: 18pt; -fx-font-weight: bold; -fx-text-fill: white;");

        Label subLbl = new Label(label);
        subLbl.setStyle("-fx-text-fill: " + (isMe ? "#4CAF50" : "#F44336") + "; -fx-font-weight: bold;");

        // Placeholder pour une carte au combat
        // Dans une version finale, on passerait un CardViewModel ici
        CardViewModel dummy = new CardViewModel(99, "Carte Cachée", 0, 0, 100, 100);
        CardView cardView = new CardView(dummy, 1.2);

        box.getChildren().addAll(subLbl, nameLbl, cardView);
        return box;
    }

    private VBox createVsZone() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        Label vs = new Label("VS");
        vs.setStyle("-fx-font-size: 40pt; -fx-font-weight: bold; -fx-text-fill: gold; -fx-effect: dropshadow(gaussian, black, 10, 0, 0, 0);");
        box.getChildren().add(vs);
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