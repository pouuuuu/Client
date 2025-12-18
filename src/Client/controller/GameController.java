package Client.controller;

import Client.jsonUtils.jsonReader;
import javafx.stage.Stage;
import Client.model.Player;
import Client.model.Card;
import Client.model.GameState;
import Client.network.ServerConnection;
import Client.view.CardViewModel;
import Client.view.PlayerViewModel;
import Client.view.ViewManager;

import java.util.ArrayList;

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
        stage.setMaximized(true);
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
                connection(id);
                break;

            case "CONNECTED_PLAYERS": {
                ArrayList<Player> serverList = jsonReader.parseConnectedPlayers(json);
                synchronizePlayers(serverList);
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
                Card c = jsonReader.parseCard(json);
                if (c != null) {
                    createCard(c);
                }
                break;
            }

            case "TRADE_REQUEST": {
                handleTradeRequest(json);
                break;
            }

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

    private void connection(int playerId) {
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

    private void createCard(Card card) {
        System.out.println("[CTRL] Card created: " + card.getName());
        if (gameState.getCurrentPlayer() != null) {
            gameState.getCurrentPlayer().getHand().addCard(card);
            notifyObservers();
        }
    }

    private void handleTradeRequest(String json) {
        // 1. On récupère les infos via le jsonReader
        int traderId = jsonReader.getTradeSenderId(json);
        String traderName = jsonReader.getTradeSenderName(json);
        int myCardId = jsonReader.getTradeRequestedCardId(json);
        int theirCardId = jsonReader.getTradeOfferedCardId(json);

        // 2. On construit le message pour l'utilisateur
        String msg = "ECHANGE PROPOSÉ par " + traderName + "\n" +
                "Il te donne la carte #" + theirCardId + "\n" +
                "Contre ta carte #" + myCardId;

        // 3. On appelle la vue (GameBoardView) pour afficher la popup
        // On définit ici ce qui se passe quand on clique sur OUI ou NON
        if (viewManager.getGameBoardView() != null) {
            viewManager.getGameBoardView().showTradePopup(
                    msg,
                    // Action si OUI : On envoie l'acceptation avec tous les IDs
                    () -> serverConnection.sendTradeResponse(true, traderId, myCardId, theirCardId),

                    // Action si NON : On envoie le refus avec tous les IDs
                    () -> serverConnection.sendTradeResponse(false, traderId, myCardId, theirCardId)
            );
        }
    }

    // --- 4. SEND TO SERVER ---

    public void sendCreateCard(String name, int atk, int def, int hp) {
        if (checkConn()) serverConnection.sendCreateCardRequest(name, atk, def, hp);
    }
    public void sendTradeCard(int m, int tp, int tc) {
        if(checkConn()) serverConnection.sendTradeRequest(m, tp, tc);
    }
    public void sendFightCard(int m, int tp, int tc) {
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

    private void distributeCard(Card card) {
        int ownerId = card.getOwnerId();

        //if its you
        if (gameState.getCurrentPlayer() != null && gameState.getCurrentPlayer().getId() == ownerId) {
            gameState.getCurrentPlayer().getHand().addCard(card);
            notifyObservers();
            return;
        }

        //if its not you
        for (Player p : gameState.getConnectedPlayers()) {
            if (p.getId() == ownerId) {
                p.getHand().addCard(card);
                notifyObservers();
                return;
            }
        }
    }

    private ArrayList<CardViewModel> convertCards(ArrayList<Card> cards) {
        ArrayList<CardViewModel> res = new ArrayList<>();
        if (cards != null) {
            for (Card c : cards) {
                res.add(new CardViewModel(c.getId(), c.getName(), c.getAttack(), c.getDefense(), c.getHealth(), c.getMaxHealth()
                ));
            }
        }
        return res;
    }

    private void synchronizePlayers(ArrayList<Player> serverPlayers) {
        ArrayList<Player> localPlayers = gameState.getConnectedPlayers();
        Player me = gameState.getCurrentPlayer();

        // 2. SUPPRESSION : On retire ceux qui ne sont plus dans la liste serveur
        boolean removed = localPlayers.removeIf(localPlayer -> {
            // Ne jamais se supprimer soi-même
            if (me != null && localPlayer.getId() == me.getId()) return false;

            // Vérifier présence sur le serveur
            boolean existsOnServer = false;
            for (Player serverPlayer : serverPlayers) {
                if (serverPlayer.getId() == localPlayer.getId()) {
                    existsOnServer = true;
                    break;
                }
            }
            if (!existsOnServer) {
                System.out.println("[CTRL] Joueur déconnecté (Suppression): " + localPlayer.getName());
            }
            return !existsOnServer;
        });

        // 3. AJOUT : On ajoute les nouveaux
        boolean added = false;
        for (Player serverPlayer : serverPlayers) {
            if (me != null && serverPlayer.getId() == me.getId()) continue;

            boolean existsLocally = false;
            for (Player localPlayer : localPlayers) {
                if (localPlayer.getId() == serverPlayer.getId()) {
                    existsLocally = true;
                    break;
                }
            }

            if (!existsLocally) {
                System.out.println("[CTRL] Nouveau joueur (Ajout): " + serverPlayer.getName());
                localPlayers.add(serverPlayer);
                added = true;
            }
        }
        notifyObservers();
    }

    // Debug
    public void startDebugMode(String name) {
        Player p = new Player(999, name + " (Test)");
        gameState.setCurrentPlayer(p);
        viewManager.setConnected(true);
        notifyObservers();
    }
}