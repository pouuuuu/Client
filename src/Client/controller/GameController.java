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
    private boolean isBot = false;

    public GameController(Stage stage) {
        this(stage, false);
    }

    public GameController(Stage stage, boolean isBot) {
        this.isBot = isBot;
        System.out.println("[CTRL] Starting " + (isBot ? "BOT MODE" : "CLIENT MODE") + "...");

        this.gameState = new GameState();
        this.observers = new ArrayList<>();
        this.jsonReader = new jsonReader();

        // Si c'est un bot, on ne lance pas l'interface graphique (ViewManager)
        if (!isBot && stage != null) {
            this.viewManager = new ViewManager(stage, this);
            this.viewManager.showLoginView();
            stage.setMaximized(true);
        }
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
                handleConnection(id);
                if (isBot) {
                    generateStarterDeck();
                }
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
                    handleCreateCard(c);
                }
                break;
            }

            case "TRADE_REQUEST": {
                handleTradeRequest(json);
                break;
            }

            case "TRADE_ACCEPTED": {
                handleTrade(json);
                break;
            }

            case "TRADE_DENIED" : {
                handleTradeDenied(json);
                break;
            }

            case "TRADE_RESPONSE_SENT", "TRADE_COMPLETE": {
                break;
            }

            case "FIGHT_REQUEST": {
                //handleFightRequest(json);
                break;
            }

            case "FIGHT_ACCEPTED": {
                handleFightResult(json);
                break;
            }

            case "FIGHT_DENIED": {
                //handleFightDenied(json);
                break;
            }

            case "ERROR":
                String err = jsonReader.getErrorMessage(json);
                onError(err);
                break;

            default:
                System.out.println("[CTRL] Unknown command: " + cmd);
        }
    }

    // --- 3. ACTION HANDLERS ---

    private void handleConnection(int playerId) {
        javafx.application.Platform.runLater(() -> {
            System.out.println("[CTRL] Auth success. My ID: " + playerId);

            if (serverConnection != null) {
                serverConnection.setPlayerId(playerId);
            }

            Player me = new Player(playerId, pendingPlayerName);
            me.setConnected(true);
            gameState.setCurrentPlayer(me);

            if (viewManager != null) {
                viewManager.setConnected(true);
            }

            notifyObservers();
        });
    }

    private void handleCreateCard(Card card) {
        System.out.println("[CTRL] Card created: " + card.getName());
        if (gameState.getCurrentPlayer() != null) {
            gameState.getCurrentPlayer().getHand().addCard(card);
            notifyObservers();
        }
    }

    private void handleTradeRequest(String json) {
        try {
            System.out.println("[CTRL] Trade Request...");

            // 1. Lecture des données
            int traderId = jsonReader.getTradeSenderId(json);
            String traderName = jsonReader.getTradeSenderName(json);
            int myCardId = jsonReader.getTradeRequestedCardId(json);
            int traderCardId = jsonReader.getTradeOfferedCardId(json);

            if (isBot) {
                System.out.println("[BOT] Auto-accepting trade from ID " + traderId);
                serverConnection.sendTradeResponse(true, traderId, myCardId, traderCardId);
            }

            //text
            String msg = "ECHANGE PROPOSÉ par " + traderName + "\n" +
                    "Il te donne " + gameState.getPlayerById(traderId).getHand().getCardById(traderCardId).getName() + " (#" + traderCardId + ")\n" +
                    "Contre " + gameState.getCurrentPlayer().getHand().getCardById(myCardId).getName() + " (#" + myCardId + ")";

            //ui call
            if (viewManager.getGameBoardView() != null) {
                viewManager.getGameBoardView().showTradePopup(
                        msg,
                        () -> serverConnection.sendTradeResponse(true, traderId, myCardId, traderCardId),
                        () -> serverConnection.sendTradeResponse(false, traderId, myCardId, traderCardId)
                );
            }
        } catch (Exception e) {

        }
    }

    private void handleTrade(String json) {
        int id1 = jsonReader.getTradePlayerId1(json);
        int id2 = jsonReader.getTradePlayerId2(json);
        int cardId1 = jsonReader.getTradeCardId1(json);
        int cardId2 = jsonReader.getTradeCardId2(json);

        System.out.println("[CTRL] Trade between:" + id1 + " et ID:" + id2 +" done");

        //player affectation
        Player p1 = resolvePlayer(id1);
        Player p2 = resolvePlayer(id2);

        if (p1 == null || p2 == null) {
            System.err.println("[TRADE ERROR] Impossible de trouver les joueurs (ID " + id1 + " ou " + id2 + ")");
            return;
        }

        //get cards
        Card c1 = p1.getHand().getCardById(cardId1);
        Card c2 = p2.getHand().getCardById(cardId2);

        if (c1 == null || c2 == null) {
            System.err.println("[TRADE ERROR] Can't find cards for trade");
            return;
        }

        //trade logic
        p1.getHand().removeCard(c1);
        p2.getHand().removeCard(c2);
        c1.setOwnerId(p2.getId());
        c2.setOwnerId(p1.getId());
        p1.getHand().addCard(c2);
        p2.getHand().addCard(c1);

        System.out.println("[TRADE] Swap : " + c1.getName() + " <-> " + c2.getName());

        notifyObservers();

        //popup, go to view
        int myId = gameState.getCurrentPlayer().getId();
        if (myId == id1 || myId == id2) {
            String otherName = (myId == id1) ? p2.getName() : p1.getName();
            if (viewManager.getGameBoardView() != null) {
                viewManager.getGameBoardView().showSuccessPopup("Échange effectué avec " + otherName + " !");
            }
        }
    }

    private void handleTradeDenied(String json) {
        String refuaserName = jsonReader.getTradeSenderName(json);
        if(refuaserName == null || refuaserName.isEmpty()) refuaserName = "L'adversaire";
        String finalName = refuaserName;
        if (viewManager.getGameBoardView() != null) {
            viewManager.getGameBoardView().showErrorPopup(finalName + " a refusé votre échange.");
        }
    }

    private void handleFightResult(String json) {
        int winnerId = jsonReader.getFightWinnerId(json);
        int loserId = jsonReader.getFightLoserId(json);
        int winnerCardId = jsonReader.getFightWinnerCardId(json);
        int loserCardId = jsonReader.getFightLoserCardId(json);
        int damage = jsonReader.getFightDamage(json);

        System.out.println("[CTRL] Fight result : winner ID " + winnerId + ", looser ID " + loserId);
        
        Player winner = resolvePlayer(winnerId);
        Player loser = resolvePlayer(loserId);

        if (winner == null || loser == null) {
            System.err.println("[FIGHT ERROR] Can't find players W:" + winnerId + ", L:" + loserId);
            return;
        }
        
        Card winnerCard = winner.getHand().getCardById(winnerCardId);
        Card loserCard = loser.getHand().getCardById(loserCardId);
        
        //destroy card
        if (loserCard != null) {
            System.out.println("[FIGHT] Destroying card : " + loserCard.getName());
            loser.getHand().removeCard(loserCard);
        } else {
            System.err.println("[FIGHT WARN] Can't find looser card (ID " + loserCardId + ")");
        }

        //taking damages
        if (winnerCard != null) {
            int oldHp = winnerCard.getHealth();
            int newHp = Math.max(0, oldHp - damage);
            winnerCard.setHealth(newHp);
            System.out.println("[FIGHT] Winner " + winnerCard.getName() + " : " + oldHp + " -> " + newHp + " PV");
        } else {
            System.err.println("[FIGHT WARN] Can't find winner card (ID " + winnerCardId + ")");
        }

        notifyObservers();

        if (viewManager != null && viewManager.getGameBoardView() != null) {
            int myId = gameState.getCurrentPlayer().getId();

            if (myId == winnerId) {
                String msg = "VICTOIRE !\n" +
                        "Votre " + winnerCard.getName() + " a écrasé l'adversaire.\nElle subit " + damage + " dégâts.";
                viewManager.getGameBoardView().showSuccessPopup(msg);

            } else if (myId == loserId) {
                String msg = "DÉFAITE\n" +
                        "Votre " + loserCard.getName() + " a été détruite par " + winner.getName() + ".\nElle est retirée de votre main.";
                viewManager.getGameBoardView().showErrorPopup(msg);
            }
        }
    }

    private Player resolvePlayer(int id) {
        if (gameState.getCurrentPlayer() != null && gameState.getCurrentPlayer().getId() == id) {
            return gameState.getCurrentPlayer();
        }
        return gameState.getPlayerById(id);
    }

    // --- 4. SEND TO SERVER ---

    public void sendCreateCard(String name, int atk, int def, int hp) {
        if (checkConn()) serverConnection.sendCreateCardRequest(name, atk, def, hp);
    }
    public void sendTradeCard(int m, int tp, int tc) {
        if(checkConn()) serverConnection.sendTradeRequest(m, tp, tc);
    }
    public void sendFightCard(int m, int tp, int tc) {
        if(checkConn()) serverConnection.sendFightRequest(m, tp, tc);
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

    private void generateStarterDeck() {
        System.out.println("[BOT] Generate deck...");
        //create 4 cards for the bot
        sendCreateCard("carteBot1", 40, 40, 100);
        sendCreateCard("carteBot2", 10, 80, 150);
        sendCreateCard("carteBot3", 80, 10, 60);
        sendCreateCard("carteBot4", 20, 20, 80);
    }

    // Debug
    public void startDebugMode(String name) {
        Player p = new Player(999, name + " (Test)");
        gameState.setCurrentPlayer(p);
        viewManager.setConnected(true);
        notifyObservers();
    }
}