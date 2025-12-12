package controller;

import javafx.stage.Stage;
import model.*;
import network.ServerConnection;
import view.CardViewModel;
import view.PlayerViewModel;
import view.ViewManager;
import jsonUtils.jsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameController {

    private GameState gameState;
    private ViewManager viewManager;
    private ServerConnection serverConnection;
    private ArrayList<GameObserver> observers;
    private jsonBuilder jsonTool;
    private Thread gameLoopThread;
    private boolean isListening;

    public GameController(Stage stage) {
        System.out.println("[CTRL] Démarrage.");
        this.gameState = new GameState();
        this.observers = new ArrayList<>();
        this.jsonTool = new jsonBuilder();

        this.viewManager = new ViewManager(stage, this);
        this.viewManager.showLoginView();
    }

    // --- CONNEXION & BOUCLE D'ÉCOUTE ---

    public boolean connect(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            onError("Pseudo vide."); return false;
        }

        System.out.println("[DEBUG JSON] " + jsonTool.jsonAuth(playerName));

        // On crée la connexion
        this.serverConnection = new ServerConnection();
        boolean success = this.serverConnection.connect(playerName);

        if (success) {
            // SI CONNECTÉ : Le contrôleur lance SA propre boucle d'écoute
            startListening();
        } else {
            onError("Echec connexion serveur.");
        }
        return success;
    }

    private void startListening() {
        isListening = true;
        gameLoopThread = new Thread(() -> {
            System.out.println("[CTRL] Démarrage du Thread d'écoute...");
            try {
                while (isListening && serverConnection.isConnected()) {
                    // 1. ATTENTE (Bloquante)
                    String message = serverConnection.waitResponse();

                    if (message == null) break; // Fin du flux

                    // 2. TRAITEMENT
                    System.out.println("[RECU] " + message);
                    processServerMessage(message);
                }
            } catch (IOException e) {
                if (isListening) {
                    System.err.println("[CTRL] Erreur flux : " + e.getMessage());
                    onError("Connexion perdue.");
                }
            }
        });
        gameLoopThread.setDaemon(true);
        gameLoopThread.start();
    }

    public void disconnect() {
        isListening = false; // Arrête la boucle
        if (serverConnection != null) serverConnection.disconnect();
        viewManager.setConnected(false);
    }

    // --- TRAITEMENT DES MESSAGES (Logique Métier) ---

    private void processServerMessage(String json) {
        String cmd = extractValue(json, "cmd");

        switch (cmd) {
            case "PLAYERS_UPDATE":
                ArrayList<Player> players = parsePlayersList(json);
                onPlayersUpdated(players);
                break;

            case "CARD_CREATED":
                Card c = parseSingleCard(json);
                onCardCreated(c);
                break;

            case "ERROR":
                String err = extractValue(json, "error");
                onError(err);
                break;

            // Ajouter ici TRADE, FIGHT...
        }
    }

    public void createCard(String name, int attack, int defense, int health) {
        // Mode Debug affichage
        System.out.println("[DEBUG JSON] " + jsonTool.jsonCreateCard("Moi", name, health, attack, defense));

        if (serverConnection == null || !serverConnection.isConnected()) {
            System.err.println("[ERREUR] Pas de connexion.");
            onError("Non connecté.");
            return;
        }
        serverConnection.sendCreateCardRequest(name, attack, defense, health);
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

    // --- MODE DEBUG ---
    public void startDebugMode(String name) {
        Player p = new Player(0, name + " (Test)");
        gameState.setCurrentPlayer(p);
        viewManager.setConnected(true);
        notifyObservers();
    }

    // --- CALLBACKS INTERNES ---

    // Appelé quand le parsing PLAYERS_UPDATE est fini
    private void onPlayersUpdated(ArrayList<Player> players) {
        System.out.println("[CTRL] Mise à jour des joueurs reçue.");
        gameState.updateConnectedPlayers(players);

        // Si on n'avait pas encore notre propre joueur, on le cherche
        if (gameState.getCurrentPlayer() == null && serverConnection.isConnected()) {
            // Logique simple: on prend le dernier ou on attend un ID spécifique
            // Pour l'instant, on laisse la logique de connexion gérer l'init
        }

        // Rafraîchir l'interface
        // Astuce : si on vient de se connecter, on active la vue ici aussi
        if (!viewManager.getStage().getTitle().contains("Jeu en cours")) {
            viewManager.setConnected(true);
        }

        notifyObservers();
    }

    private void onCardCreated(Card c) {
        System.out.println("[CTRL] Carte créée.");
        if (gameState.getCurrentPlayer() != null) {
            gameState.getCurrentPlayer().getHand().addCard(c);
            notifyObservers();
        }
    }

    public void onError(String msg) {
        System.err.println("[CTRL] Erreur: " + msg);
        for(GameObserver o : observers) o.onError(msg);
    }

    // --- PARSING MANUEL (Déplacé ici) ---

    private ArrayList<Player> parsePlayersList(String json) {
        ArrayList<Player> list = new ArrayList<>();
        // Découpage manuel du JSON
        try {
            int start = json.indexOf("[");
            int end = json.lastIndexOf("]");
            if (start == -1 || end == -1) return list;

            String content = json.substring(start + 1, end);
            ArrayList<String> pJsons = splitJsonObjects(content);

            for (String pj : pJsons) {
                int id = Integer.parseInt(extractValue(pj, "id"));
                String name = extractValue(pj, "user");
                Player p = new Player(id, name);

                // Parsing des cartes imbriquées
                int cS = pj.indexOf("[");
                int cE = pj.lastIndexOf("]");
                if (cS != -1 && cE != -1) {
                    String cC = pj.substring(cS + 1, cE);
                    for (String cj : splitJsonObjects(cC)) {
                        if(!cj.trim().isEmpty()) {
                            Card c = parseSingleCard(cj);
                            if(c!=null) p.getHand().addCard(c);
                        }
                    }
                }
                list.add(p);
            }
        } catch (Exception e) {
            System.err.println("[PARSING] Erreur: " + e.getMessage());
        }
        return list;
    }

    private Card parseSingleCard(String json) {
        try {
            int id = Integer.parseInt(extractValue(json, "id"));
            String name = extractValue(json, "name");
            int hp = Integer.parseInt(extractValue(json, "HP"));
            int ap = Integer.parseInt(extractValue(json, "AP"));
            int dp = Integer.parseInt(extractValue(json, "DP"));
            return new Card(id, name, ap, dp, hp);
        } catch (Exception e) { return null; }
    }

    private String extractValue(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"?([^,\"}]+)\"?");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : "";
    }

    private ArrayList<String> splitJsonObjects(String t) {
        ArrayList<String> l = new ArrayList<>();
        int lvl = 0;
        StringBuilder sb = new StringBuilder();
        for (char c : t.toCharArray()) {
            if (c == '{') lvl++;
            if (c == '}') lvl--;
            if (c == ',' && lvl == 0) {
                l.add(sb.toString()); sb = new StringBuilder();
            } else sb.append(c);
        }
        if (sb.length() > 0) l.add(sb.toString());
        return l;
    }

    // --- VIEW MODELS & OBSERVERS (Inchangé) ---
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
            // On exclut le joueur courant de la liste des adversaires
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
}