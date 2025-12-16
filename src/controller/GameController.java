package controller;

import jsonUtils.jsonReader;
import javafx.stage.Stage;
import model.*;
import network.ServerConnection;
import view.CardViewModel;
import view.PlayerViewModel;
import view.ViewManager;

import java.util.ArrayList;
import javafx.application.Platform;

public class GameController {

    private GameState gameState;
    private ViewManager viewManager;
    private ServerConnection serverConnection;
    private ArrayList<GameObserver> observers;

    // Ajout du parser
    private jsonReader jsonReader;

    private String pendingPlayerName;

    public GameController(Stage stage) {
        System.out.println("[CTRL] Starting...");
        this.gameState = new GameState();
        this.observers = new ArrayList<>();
        this.jsonReader = new jsonReader(); // Instantiation

        this.viewManager = new ViewManager(stage, this);
        this.viewManager.showLoginView();
    }

    // --- 1. CONNECTION ---
    public boolean connect(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) return false;
        this.pendingPlayerName = playerName;
        this.serverConnection = new ServerConnection(this);
        return this.serverConnection.connect(playerName);
    }

    public void disconnect() {
        if (serverConnection != null) serverConnection.disconnect();
        viewManager.setConnected(false);
    }

    // --- 2. PROCESS MESSAGES (Delegation to JSON_Reader) ---

    public void processServerMessage(String json) {
        // Use JSON_Reader to get the command
        String cmd = jsonReader.getCommand(json);

        switch (cmd) {
            case "AUTH_OK":
                int id = jsonReader.getAuthId(json);
                handleAuthSuccess(id);
                break;

            case "CARD_CREATED":
                Card c = jsonReader.parseCardCreated(json);
                if (c != null) {
                    onCardCreated(c);
                }
                break;

            case "PLAYERS_UPDATE":
                ArrayList<Player> players = jsonReader.parsePlayersList(json);
                onPlayersUpdated(players);
                break;

            case "ERROR":
                String err = jsonReader.getErrorMessage(json);
                onError(err);
                break;

            default:
                System.out.println("[CTRL] Unknown command: " + cmd);
        }
    }

    // --- 3. ACTION HANDLERS ---

    private void handleAuthSuccess(int playerId) {
        Platform.runLater(() -> {
            System.out.println("[CTRL] Auth success. My ID: " + playerId);

            Player me = new Player(playerId, pendingPlayerName);
            me.setConnected(true);
            gameState.setCurrentPlayer(me);

            viewManager.setConnected(true);
            notifyObservers();
        });
    }

    private void onCardCreated(Card card) {
        System.out.println("[CTRL] Card created: " + card.getName());
        if (gameState.getCurrentPlayer() != null) {
            gameState.getCurrentPlayer().getHand().addCard(card);
            notifyObservers();
        }
    }

    private void onPlayersUpdated(ArrayList<Player> players) {
        System.out.println("[CTRL] Players list updated (" + players.size() + ").");
        gameState.updateConnectedPlayers(players);
        notifyObservers();
    }

    // --- 4. SEND TO SERVER ---

    public void createCard(String name, int atk, int def, int hp) {
        if (checkConn()) serverConnection.sendCreateCardRequest(name, atk, def, hp);
    }
    public void exchangeCard(int m, int tp, int tc) {
        if(checkConn()) serverConnection.sendExchangeRequest(m, tp, tc);
    }
    public void combatCard(int m, int tp, int tc) {
        if(checkConn()) serverConnection.sendCombatRequest(m, tp, tc);
    }

    private boolean checkConn() {
        if (serverConnection != null && serverConnection.isConnected()) return true;
        onError("Not connected."); return false;
    }

    // --- VIEW MODELS & OBSERVERS ---

    public void onError(String msg) { for(GameObserver o : observers) o.onError(msg); }
    public void addObserver(GameObserver o) { observers.add(o); }
    private void notifyObservers() { for(GameObserver o : observers) o.onGameStateChanged(gameState); }

    public PlayerViewModel getCurrentPlayerViewModel() {
        Player p = gameState.getCurrentPlayer();
        if(p==null) return null;
        return new PlayerViewModel(p.getId(), p.getName(), p.getCardCount(), 10, convertCards(p.getHand().getCards()));
    }
    public ArrayList<PlayerViewModel> getOtherPlayersViewModels() {
        ArrayList<PlayerViewModel> res = new ArrayList<>();
        if (gameState.getCurrentPlayer() == null) return res;
        for(Player p : gameState.getConnectedPlayers()) {
            if(p.getId() != gameState.getCurrentPlayer().getId()) {
                res.add(new PlayerViewModel(p.getId(), p.getName(), p.getCardCount(), 10, convertCards(p.getHand().getCards())));
            }
        }
        return res;
    }
    private ArrayList<CardViewModel> convertCards(ArrayList<Card> cards) {
        ArrayList<CardViewModel> r = new ArrayList<>();
        for(Card c : cards) r.add(new CardViewModel(c.getId(), c.getName(), c.getAttack(), c.getDefense(), c.getHealth()));
        return r;
    }

    // Debug
    public void startDebugMode(String name) {
        Player p = new Player(999, name + " (Test)");
        gameState.setCurrentPlayer(p);
        viewManager.setConnected(true);
        notifyObservers();
    }
}