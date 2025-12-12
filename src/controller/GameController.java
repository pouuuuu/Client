package controller;

import javafx.stage.Stage;
import model.*;
import network.ServerConnection;
import view.CardViewModel;
import view.PlayerViewModel;
import view.ViewManager;
import jsonUtils.jsonBuilder;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameController {

    private GameState gameState;
    private ViewManager viewManager;
    private ServerConnection serverConnection;
    private ArrayList<GameObserver> observers;

    // On garde le nom en mémoire pour créer le joueur lors de la validation
    private String pendingPlayerName;

    public GameController(Stage stage) {
        System.out.println("[CTRL] Démarrage.");
        this.gameState = new GameState();
        this.observers = new ArrayList<>();

        this.viewManager = new ViewManager(stage, this);
        this.viewManager.showLoginView();
    }

    // --- CONNEXION ---
    public boolean connect(String playerName) {
        if (playerName == null || playerName.isEmpty()) return false;

        this.pendingPlayerName = playerName;
        this.serverConnection = new ServerConnection(this);

        // Connect ne fait qu'ouvrir le socket. La validation viendra plus tard via processServerMessage
        return this.serverConnection.connect(playerName);
    }

    public void disconnect() {
        if (serverConnection != null) serverConnection.disconnect();
        viewManager.setConnected(false);
    }

    // --- COEUR DE LA REACTION : Traitement des messages ---

    // Cette méthode est appelée par ServerConnection dès qu'un message arrive
    public void processServerMessage(String json) {
        String cmd = extractValue(json, "cmd");

        switch (cmd) {
            // 1. VALIDATION CONNEXION
            case "AUTH_OK": // Ou peu importe ce que ton serveur envoie pour dire "C'est bon"
                handleAuthSuccess(json);
                break;

            // 2. CARTE CRÉÉE
            case "CARD_CREATED":
                Card c = parseSingleCard(json);
                if (c != null) onCardCreated(c);
                break;

            // 3. MISE A JOUR DES JOUEURS
            case "PLAYERS_UPDATE":
                ArrayList<Player> players = parsePlayersList(json);
                onPlayersUpdated(players);
                break;

            case "ERROR":
                String err = extractValue(json, "error");
                onError(err);
                break;
        }
    }

    // --- GESTIONNAIRES D'EVENEMENTS ---

    private void handleAuthSuccess(String json) {
        System.out.println("[CTRL] Connexion validée par le serveur !");

        // On récupère l'ID attribué par le serveur (si dispo dans le JSON)
        int myId = 1;
        try { myId = Integer.parseInt(extractValue(json, "id")); } catch(Exception e){}

        // CREATION DU JOUEUR LOCAL
        Player me = new Player(myId, pendingPlayerName);
        me.setConnected(true);
        gameState.setCurrentPlayer(me);

        // On déverrouille l'interface
        viewManager.setConnected(true);
        notifyObservers();
    }

    private void onCardCreated(Card card) {
        System.out.println("[CTRL] Carte reçue : " + card.getName());
        // On ajoute la carte à notre joueur local
        if (gameState.getCurrentPlayer() != null) {
            gameState.getCurrentPlayer().getHand().addCard(card);
            notifyObservers();
        }
    }

    private void onPlayersUpdated(ArrayList<Player> players) {
        System.out.println("[CTRL] Liste joueurs reçue.");
        gameState.updateConnectedPlayers(players);
        notifyObservers();
    }

    // --- ENVOIS ---

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
        onError("Non connecté."); return false;
    }

    // --- OUTILS PARSING ---

    private String extractValue(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"?([^,\"}]+)\"?");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : "";
    }

    private Card parseSingleCard(String json) {
        try {
            int id = Integer.parseInt(extractValue(json, "id"));
            String name = extractValue(json, "name");
            if(name.isEmpty()) name = extractValue(json, "cardName"); // Fallback
            int hp = Integer.parseInt(extractValue(json, "HP"));
            int ap = Integer.parseInt(extractValue(json, "AP"));
            int dp = Integer.parseInt(extractValue(json, "DP"));
            return new Card(id, name, ap, dp, hp);
        } catch (Exception e) { return null; }
    }

    private ArrayList<Player> parsePlayersList(String json) {
        // (Copie ici ta méthode parsePlayersList que je t'ai donnée précédemment)
        // Elle est un peu longue, je ne la remets pas pour ne pas saturer la réponse
        // mais elle est indispensable ici.
        return new ArrayList<>(); // Placeholder
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

    // Méthode de debug hors ligne
    public void startDebugMode(String name) {
        Player p = new Player(0, name + " (Test)");
        gameState.setCurrentPlayer(p);
        viewManager.setConnected(true);
        notifyObservers();
    }
}