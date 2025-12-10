package network;

import controller.GameController;
import model.*;
import jsonUtils.jsonBuilder;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

// Gere la connexion reseau avec le serveur C
public class ServerConnection {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private String serverIp;
    private int serverPort;
    private GameController controller;
    private Thread listenerThread;
    private boolean connected;
    private jsonBuilder jsonBuilder;
    private String currentPlayerName;

    public ServerConnection(String serverIp, int serverPort, GameController controller) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.controller = controller;
        this.connected = false;
        this.jsonBuilder = new jsonBuilder();
    }

    // Connexion au serveur
    public boolean connect(String playerName) {
        try {
            this.currentPlayerName = playerName;
            socket = new Socket(serverIp, serverPort);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            // Envoie AUTH
            String authJson = jsonBuilder.jsonAuth(playerName);
            sendMessage(authJson);

            // Attend la reponse
            String response = input.readLine();
            if (response != null && parseAuthResponse(response)) {
                connected = true;
                startListenerThread();
                return true;
            }

            return false;
        } catch (IOException e) {
            System.err.println("Erreur connexion: " + e.getMessage());
            return false;
        }
    }

    // Deconnexion
    public void disconnect() {
        try {
            connected = false;
            if (socket != null) {
                socket.close();
            }
            if (listenerThread != null) {
                listenerThread.interrupt();
            }
        } catch (IOException e) {
            System.err.println("Erreur deconnexion: " + e.getMessage());
        }
    }

    // Envoie demande de creation de carte
    public boolean sendCreateCardRequest(String name, int attack, int defense, int health) {
        if (!connected) return false;
        String json = jsonBuilder.jsonCreateCard(currentPlayerName, name, health, attack, defense);
        sendMessage(json);
        return true;
    }

    // Envoie demande d'echange
    public boolean sendExchangeRequest(int myCardId, int targetPlayerId, int targetCardId) {
        if (!connected) return false;
        String json = jsonBuilder.jsonTradeRequest(currentPlayerName, myCardId, String.valueOf(targetPlayerId), targetCardId);
        sendMessage(json);
        return true;
    }

    // Envoie reponse a un echange
    public boolean sendExchangeResponse(String requestId, boolean accept) {
        if (!connected) return false;
        String json = accept ? jsonBuilder.jsonTradeAccept(currentPlayerName) : jsonBuilder.jsonTradeDeny(currentPlayerName);
        sendMessage(json);
        return true;
    }

    // Envoie demande de combat
    public boolean sendCombatRequest(int myCardId, int targetPlayerId, int targetCardId) {
        if (!connected) return false;
        String json = jsonBuilder.jsonFightRequest(currentPlayerName, myCardId, String.valueOf(targetPlayerId), targetCardId);
        sendMessage(json);
        return true;
    }

    // Envoie reponse a un combat
    public boolean sendCombatResponse(String requestId, boolean accept) {
        if (!connected) return false;
        String json = accept ? jsonBuilder.jsonFightAccept(currentPlayerName) : jsonBuilder.jsonFightDeny(currentPlayerName);
        sendMessage(json);
        return true;
    }

    // Envoie un message JSON au serveur
    private void sendMessage(String json) {
        if (output != null) {
            output.println(json);
            output.flush();
            System.out.println("Envoye: " + json);
        }
    }

    // Demarre le thread d'ecoute
    private void startListenerThread() {
        listenerThread = new Thread(() -> {
            try {
                String message;
                while (connected && (message = input.readLine()) != null) {
                    System.out.println("Recu: " + message);
                    handleServerMessage(message);
                }
            } catch (IOException e) {
                if (connected) {
                    System.err.println("Connexion perdue: " + e.getMessage());
                    controller.onError("Connexion perdue");
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    // Parse la reponse AUTH du serveur
    private boolean parseAuthResponse(String json) {
        try {
            // Parsing JSON simple (tu peux utiliser une lib JSON si necessaire)
            if (json.contains("\"cmd\":\"AUTH_OK\"")) {
                // Extrait les infos du joueur
                int playerId = extractInt(json, "\"playerId\":");
                String playerName = extractString(json, "\"user\":\"");

                // Extrait les cartes si presentes
                ArrayList<Card> hand = new ArrayList<>();
                // TODO: parser les cartes depuis le JSON si le serveur les envoie

                controller.onConnectionEstablished(playerId, playerName, hand);
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Erreur parsing AUTH: " + e.getMessage());
            return false;
        }
    }

    // Gere les messages du serveur
    private void handleServerMessage(String json) {
        try {
            // Extrait la commande
            String cmd = extractString(json, "\"cmd\":\"");

            switch (cmd) {
                case "CARD_CREATED":
                    handleCardCreated(json);
                    break;
                case "PLAYERS_UPDATE":
                    handlePlayersUpdate(json);
                    break;
                case "TRADE_REQUEST":
                    handleTradeRequest(json);
                    break;
                case "TRADE_COMPLETED":
                    handleTradeCompleted(json);
                    break;
                case "FIGHT_REQUEST":
                    handleFightRequest(json);
                    break;
                case "FIGHT_RESULT":
                    handleFightResult(json);
                    break;
                case "CARD_REMOVED":
                    handleCardRemoved(json);
                    break;
                case "ERROR":
                    handleError(json);
                    break;
                default:
                    System.err.println("Commande inconnue: " + cmd);
            }
        } catch (Exception e) {
            System.err.println("Erreur traitement message: " + e.getMessage());
        }
    }

    // Parse CARD_CREATED
    private void handleCardCreated(String json) {
        int cardId = extractInt(json, "\"cardId\":");
        String name = extractString(json, "\"cardName\":\"");
        int hp = extractInt(json, "\"HP\":");
        int ap = extractInt(json, "\"AP\":");
        int dp = extractInt(json, "\"DP\":");

        Card card = new Card(cardId, name, ap, dp, hp);
        controller.onCardCreated(card);
    }

    // Parse PLAYERS_UPDATE
    private void handlePlayersUpdate(String json) {
        // TODO: parser la liste des joueurs depuis le JSON
        ArrayList<Player> players = new ArrayList<>();
        controller.onPlayersUpdated(players);
    }

    // Parse TRADE_REQUEST
    private void handleTradeRequest(String json) {
        String requestId = extractString(json, "\"requestId\":\"");
        int fromPlayerId = extractInt(json, "\"fromPlayerId\":");
        String fromPlayerName = extractString(json, "\"fromPlayerName\":\"");
        int offeredCardId = extractInt(json, "\"offeredCardId\":");
        int requestedCardId = extractInt(json, "\"requestedCardId\":");

        controller.onExchangeRequest(requestId, fromPlayerId, fromPlayerName, offeredCardId, requestedCardId);
    }

    // Parse TRADE_COMPLETED
    private void handleTradeCompleted(String json) {
        int newCardId = extractInt(json, "\"cardId\":");
        String name = extractString(json, "\"cardName\":\"");
        int hp = extractInt(json, "\"HP\":");
        int ap = extractInt(json, "\"AP\":");
        int dp = extractInt(json, "\"DP\":");
        int lostCardId = extractInt(json, "\"lostCardId\":");

        Card newCard = new Card(newCardId, name, ap, dp, hp);
        controller.onExchangeCompleted(newCard, lostCardId);
    }

    // Parse FIGHT_REQUEST
    private void handleFightRequest(String json) {
        String requestId = extractString(json, "\"requestId\":\"");
        int fromPlayerId = extractInt(json, "\"fromPlayerId\":");
        String fromPlayerName = extractString(json, "\"fromPlayerName\":\"");
        int attackingCardId = extractInt(json, "\"attackingCardId\":");
        int targetCardId = extractInt(json, "\"targetCardId\":");

        controller.onCombatRequest(requestId, fromPlayerId, fromPlayerName, attackingCardId, targetCardId);
    }

    // Parse FIGHT_RESULT
    private void handleFightResult(String json) {
        int winnerId = extractInt(json, "\"winnerId\":");
        int loserId = extractInt(json, "\"loserId\":");
        int winnerCardId = extractInt(json, "\"winnerCardId\":");
        int loserCardId = extractInt(json, "\"loserCardId\":");
        int damage = extractInt(json, "\"damage\":");

        CombatResult result = new CombatResult(winnerId, loserId, winnerCardId, loserCardId, damage);
        controller.onCombatCompleted(result);
    }

    // Parse CARD_REMOVED
    private void handleCardRemoved(String json) {
        int cardId = extractInt(json, "\"cardId\":");
        controller.onCardRemoved(cardId);
    }

    // Parse ERROR
    private void handleError(String json) {
        String errorMsg = extractString(json, "\"error\":\"");
        controller.onError(errorMsg);
    }

    // Utilitaires pour parser le JSON manuellement
    private String extractString(String json, String key) {
        int start = json.indexOf(key);
        if (start == -1) return "";
        start += key.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end);
    }

    private int extractInt(String json, String key) {
        try {
            int start = json.indexOf(key);
            if (start == -1) return 0;
            start += key.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            if (end == -1) return 0;
            String numStr = json.substring(start, end).trim();
            return Integer.parseInt(numStr);
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
}