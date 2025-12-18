package Client.view;

import Client.controller.GameController;
import Client.controller.GameObserver;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import Client.model.CombatResult;
import Client.model.GameState;

import java.time.LocalTime;

public class LogView implements GameObserver {

    private ViewManager viewManager;
    private GameController controller;
    private VBox layout;
    private TextArea logArea;

    public LogView(ViewManager viewManager, GameController controller) {
        this.viewManager = viewManager;
        this.controller = controller;
        createLayout();
    }

    private void createLayout() {
        layout = new VBox(20);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setPadding(new Insets(30));

        Label title = new Label("Page 3: Journal du Serveur et des Résultats");
        title.setStyle("-fx-font-size: 24pt; -fx-font-weight: bold;");

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setText("--- Début du Journal ---\n");

        VBox.setVgrow(logArea, Priority.ALWAYS);
        layout.getChildren().addAll(title, logArea);
    }

    public Node getView() {
        return layout;
    }

    private void log(String msg) {
        String timestamp = "[" + LocalTime.now().withNano(0) + "] ";
        Platform.runLater(() -> logArea.appendText(timestamp + msg + "\n"));
    }

    // --- Capturer les événements ---

    @Override public void onGameStateChanged(GameState gameState) { log("État du jeu mis à jour."); }
    @Override public void onError(String msg) { log("ERREUR: " + msg); }
    @Override public void onExchangeRequestReceived(String r, int f, String n, int o, int req) { log("Demande d'échange reçue de " + n); }
    @Override public void onCombatRequestReceived(String r, int f, String n, int a, int t) { log("COMBAT: " + n + " attaque !"); }
    @Override public void onCombatCompleted(CombatResult res) { log("Combat terminé. Dégâts: " + res.getDamageDealt()); }

    @Override public void onExchangeRequestReceived(String r, String f, String n, String o, String req) {}
    @Override public void onCombatRequestReceived(String r, String f, String n, String a, String t) {}
}