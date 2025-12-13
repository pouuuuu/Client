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

    // On garde le nom en mémoire en attendant la validation du serveur
    private String pendingPlayerName;

    public GameController(Stage stage) {
        System.out.println("[CTRL] Démarrage.");
        this.gameState = new GameState();
        this.observers = new ArrayList<>();

        this.viewManager = new ViewManager(stage, this);
        this.viewManager.showLoginView();
    }

    // --- 1. CONNEXION ---
    public boolean connect(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) return false;

        this.pendingPlayerName = playerName;
        this.serverConnection = new ServerConnection(this);

        // Lance la connexion socket et l'écoute
        return this.serverConnection.connect(playerName);
    }

    public void disconnect() {
        if (serverConnection != null) serverConnection.disconnect();
        viewManager.setConnected(false);
    }

    // --- 2. TRAITEMENT DES MESSAGES (Le Cerveau) ---

    // Appelé par ServerConnection à chaque message reçu
    public void processServerMessage(String json) {
        // Extraction de la commande
        String cmd = extractValue(json, "cmd");

        switch (cmd) {
            // A. VALIDATION DE CONNEXION
            case "AUTH_OK": // Adaptez selon votre serveur (ex: "AUTH" ou "OK")
                handleAuthSuccess(json);
                break;

            // B. CARTE CRÉÉE
            case "CARD_CREATED":
                Card c = parseSingleCard(json);
                if (c != null) {
                    onCardCreated(c);
                }
                break;

            // C. MISE A JOUR GLOBALE DES JOUEURS
            case "PLAYERS_UPDATE":
                ArrayList<Player> players = parsePlayersList(json);
                onPlayersUpdated(players);
                break;

            case "ERROR":
                String err = extractValue(json, "error");
                onError(err);
                break;

            default:
                System.out.println("[CTRL] Commande ignorée ou inconnue : " + cmd);
        }
    }

    // --- 3. GESTIONNAIRES D'ACTIONS ---

    private void handleAuthSuccess(String json) {
        System.out.println("[CTRL] Authentification réussie !");

        // 1. On récupère l'ID attribué par le serveur
        int myId = 0;
        try { myId = Integer.parseInt(extractValue(json, "id")); } catch(Exception e){}

        // 2. On CRÉE notre joueur local
        Player me = new Player(myId, pendingPlayerName);
        me.setConnected(true);
        gameState.setCurrentPlayer(me);

        // 3. On déverrouille l'interface graphique
        // (Note: on utilise javafx.application.Platform.runLater si on touche à l'UI depuis un thread réseau,
        // mais ViewManager gère généralement ça ou les observers le feront)
        viewManager.setConnected(true);
        notifyObservers();
    }

    private void onCardCreated(Card card) {
        System.out.println("[CTRL] Nouvelle carte créée : " + card.getName());

        // On ajoute la carte à la main de NOTRE joueur
        if (gameState.getCurrentPlayer() != null) {
            gameState.getCurrentPlayer().getHand().addCard(card);
            notifyObservers();
        }
    }

    private void onPlayersUpdated(ArrayList<Player> players) {
        System.out.println("[CTRL] Mise à jour de la liste des joueurs (" + players.size() + " joueurs).");

        // On met à jour tout le GameState avec la nouvelle liste
        gameState.updateConnectedPlayers(players);

        // Petite astuce : si notre joueur local est dans la liste, on peut mettre à jour ses infos
        // pour être sûr d'avoir la version serveur (ex: synchro des ID)
        if (gameState.getCurrentPlayer() != null) {
            for(Player p : players) {
                if (p.getName().equals(gameState.getCurrentPlayer().getName())) {
                    // On pourrait mettre à jour l'ID ici si besoin
                }
            }
        }

        notifyObservers();
    }

    // --- 4. ENVOIS VERS LE SERVEUR ---

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

    // --- 5. PARSING MANUEL (Sans librairie externe) ---

    // Extrait une valeur simple "cle":"valeur"
    private String extractValue(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"?([^,\"}]+)\"?");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : "";
    }

    // Parse une carte unique
    private Card parseSingleCard(String json) {
        try {
            int id = Integer.parseInt(extractValue(json, "id"));
            String name = extractValue(json, "name");
            if(name.isEmpty()) name = extractValue(json, "cardName"); // Supporte les deux formats
            int hp = Integer.parseInt(extractValue(json, "HP"));
            int ap = Integer.parseInt(extractValue(json, "AP"));
            int dp = Integer.parseInt(extractValue(json, "DP"));
            return new Card(id, name, ap, dp, hp);
        } catch (Exception e) {
            System.err.println("Erreur parsing carte: " + e.getMessage());
            return null;
        }
    }

    // Parse la liste complète des joueurs et de leurs cartes
    private ArrayList<Player> parsePlayersList(String json) {
        ArrayList<Player> list = new ArrayList<>();
        try {
            // On cherche le contenu du tableau principal [ ... ]
            int start = json.indexOf("[");
            int end = json.lastIndexOf("]");
            if (start == -1 || end == -1) {
                return list;
            }

            String content = json.substring(start + 1, end);

            // On découpe par objet joueur
            ArrayList<String> pJsons = splitJsonObjects(content);

            for (String pj : pJsons) {
                // Création du Joueur
                int id = Integer.parseInt(extractValue(pj, "id"));
                String name = extractValue(pj, "user");
                Player p = new Player(id, name);
                p.setConnected(true);

                // Parsing des cartes de ce joueur (tableau imbriqué)
                int cS = pj.indexOf("[");
                int cE = pj.lastIndexOf("]");
                if (cS != -1 && cE != -1) {
                    String cContent = pj.substring(cS + 1, cE);
                    for (String cj : splitJsonObjects(cContent)) {
                        if(!cj.trim().isEmpty()) {
                            Card c = parseSingleCard(cj);
                            if(c!=null) p.getHand().addCard(c);
                        }
                    }
                }
                list.add(p);
            }
        } catch (Exception e) {
            System.err.println("[PARSING] Erreur liste joueurs : " + e.getMessage());
        }
        return list;
    }

    // Utilitaire pour découper "objet1},{objet2" proprement
    private ArrayList<String> splitJsonObjects(String t) {
        ArrayList<String> l = new ArrayList<>();
        int lvl = 0;
        StringBuilder sb = new StringBuilder();
        for (char c : t.toCharArray()) {
            if (c == '{') {
                lvl++;
            }
            if (c == '}') {
                lvl--;
            }
            if (c == ',' && lvl == 0) {
                l.add(sb.toString()); sb = new StringBuilder();
            } else sb.append(c);
        }
        if (sb.length() > 0) l.add(sb.toString());
        return l;
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
        Player p = new Player(0, name + " (Test)");
        gameState.setCurrentPlayer(p);
        viewManager.setConnected(true);
        notifyObservers();
    }
}