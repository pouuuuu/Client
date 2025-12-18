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
        String cmd = jsonReader.getCommand(json);

        switch (cmd) {
            case "AUTH_OK":
                int id = jsonReader.getAuthId(json);
                handleAuthSuccess(id);
                break;

            case "CONNECTED_PLAYERS": {
                ArrayList<Player> players = jsonReader.parseConnectedPlayers(json);
                for (Player p : players) {
                    addOrUpdatePlayer(p);
                }
                break;
            }

            // --- 2. RECEPTION D'UNE CARTE ---
            case "CONNECTED_PLAYERS_CARDS": {
                ArrayList<Card> cards = jsonReader.parseConnectedCards(json);
                for (Card c : cards) {
                    distributeCard(c);
                }
                break;
            }

            case "CREATE_CARD_OK": {
                Card c = jsonReader.parseCardCreated(json);
                if (c != null) {
                    onCardCreated(c);
                }
                break;
            }

            case "PLAYERS_UPDATE":
                ArrayList<Player> players = jsonReader.parsePlayersList(json);
                onPlayersUpdated(players);
                break;

            case "FIGHT_REQUESTED":


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
        javafx.application.Platform.runLater(() -> {
            System.out.println("[CTRL] Auth success. My ID: " + playerId);

            if (serverConnection != null) {
                serverConnection.setPlayerId(playerId);
            }

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

    private void addOrUpdatePlayer(Player newP) {
        // On vérifie si c'est "Moi" (basé sur l'ID qu'on a reçu à l'AUTH)
        // Si vous stockez votre ID dans une variable 'myPlayerId' :
        if (gameState.getCurrentPlayer() != null && newP.getId() == gameState.getCurrentPlayer().getId()) {
            // C'est moi, on ne fait rien ou on met à jour le nom
            return;
        }

        boolean found = false;
        for (Player existing : gameState.getConnectedPlayers()) {
            if (existing.getId() == newP.getId()) {
                found = true;
                break;
            }
        }
        if (!found) {
            gameState.getConnectedPlayers().add(newP);
            notifyObservers();
        }
    }

    private void distributeCard(Card card) {
        int ownerId = card.getOwnerId();

        // 1. Est-ce à moi ?
        if (gameState.getCurrentPlayer() != null && gameState.getCurrentPlayer().getId() == ownerId) {
            gameState.getCurrentPlayer().getHand().addCard(card);
            notifyObservers();
            return;
        }

        // 2. Est-ce à un adversaire ?
        for (Player p : gameState.getConnectedPlayers()) {
            if (p.getId() == ownerId) {
                p.getHand().addCard(card);
                notifyObservers();
                return;
            }
        }
        // Si on arrive ici, le joueur n'est pas encore connu, on pourrait stocker la carte en attente
        System.out.println("[CTRL] Carte reçue pour joueur inconnu ID: " + ownerId);
    }

    private ArrayList<CardViewModel> convertCards(ArrayList<Card> cards) {
        ArrayList<CardViewModel> res = new ArrayList<>();
        if (cards != null) {
            for (Card c : cards) {
                // On passe c.getMaxHealth() en dernier argument
                res.add(new CardViewModel(c.getId(), c.getName(), c.getAttack(), c.getDefense(), c.getHealth(), c.getMaxHealth()
                ));
            }
        }
        return res;
    }

    // Debug
    public void startDebugMode(String name) {
        Player p = new Player(999, name + " (Test)");
        gameState.setCurrentPlayer(p);
        viewManager.setConnected(true);
        notifyObservers();
    }
}