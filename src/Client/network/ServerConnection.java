package Client.network;

import Client.controller.GameController;
import Client.jsonUtils.jsonBuilder;
import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;

public class ServerConnection {
    // Adapter l'IP selon votre configuration
    private static final String SERVER_IP = "134.59.27.143";
    private static final int SERVER_PORT = 8080;

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private boolean connected;
    private jsonBuilder jsonBuilder;
    private String currentPlayerName;
    private int currentPlayerId;
    private GameController controller;

    public ServerConnection(GameController controller) {
        this.controller = controller;
        this.connected = false;
        this.jsonBuilder = new jsonBuilder();
    }

    public void setPlayerId(int id) {
        this.currentPlayerId = id;
    }

    // --- CONNECTION ---
    public boolean connect(String playerName) {
        this.currentPlayerName = playerName;
        System.out.println("[NETWORK] Tentative de connexion à " + SERVER_IP + ":" + SERVER_PORT + "...");

        try {
            // 1. Créer un socket vide (ne bloque pas)
            socket = new Socket();

            // 2. Se connecter avec un TIMEOUT de 2000ms (2 secondes)
            // Si le serveur est éteint, cela échoue au bout de 2s au lieu de figer 1 minute.
            socket.connect(new InetSocketAddress(SERVER_IP, SERVER_PORT), 2000);

            // 3. Si on arrive ici, c'est réussi : on ouvre les flux
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            connected = true;

            System.out.println("[NETWORK] Connection established");

            // 4. Lancer le thread d'écoute
            startListenerThread();

            // 5. Envoyer la requête d'authentification
            sendJson(jsonBuilder.jsonAuth(playerName));

            return true;

        } catch (IOException e) {
            System.err.println("[NETWORK] connection error : " + e.getMessage());

            // C'est ici que l'on déclenche l'affichage du message sur le client
            if (controller != null) {
                controller.onError("Connexion impossible : Serveur inaccessible.");
            }
            return false;
        }
    }

    // --- LISTENER (RECEIVE) ---
    private void startListenerThread() {
        Thread listener = new Thread(() -> {
            try {
                String message;
                // Read loop
                while (connected && (message = input.readLine()) != null) {

                    // --- LOG: PACKET RECEIVED ---
                    System.out.println("[PACKET IN]  " + message);

                    // Send raw JSON to controller
                    controller.processServerMessage(message);
                }
            } catch (IOException e) {
                if (connected) {
                    System.err.println("[NETWORK] Connection lost.");
                    controller.onError("Connection lost with server.");
                }
            } finally {
                disconnect();
            }
        });
        listener.setDaemon(true); // Stop thread if app closes
        listener.start();
    }

    // --- SENDER (SEND) ---

    public void sendCreateCardRequest(String n, int a, int d, int h) {
        sendJson(jsonBuilder.jsonCreateCard(currentPlayerId, n, h, a, d));
    }

    public void sendTradeRequest(int m, int tp, int tc) {
        sendJson(jsonBuilder.jsonTradeRequest(currentPlayerId, m, String.valueOf(tp), tc));
    }

    public void sendTradeResponse(boolean accepted, int traderId, int myCardId, int theirCardId) {
        String json;
        if (accepted) {
            json = jsonBuilder.jsonTradeAccept(currentPlayerId, traderId, myCardId, theirCardId);
        } else {
            json = jsonBuilder.jsonTradeDeny(currentPlayerId, traderId, myCardId, theirCardId);
        }

        // On envoie
        sendJson(json);
    }

    public void sendFightRequest(int m, int tp, int tc) {
        sendJson(jsonBuilder.jsonFightRequest(currentPlayerId, m, String.valueOf(tp), tc));
    }

    public void sendFightResponse(boolean accepted) {

    }

    private void sendJson(String json) {
        if (output != null) {
            System.out.println("[PACKET OUT] " + json);

            output.println(json);

            // AJOUT : Vérifier si l'envoi a réellement fonctionné
            if (output.checkError()) {
                System.err.println("[NETWORK CRITICAL] Erreur d'envoi : Le flux est cassé ou le serveur est déconnecté.");
                controller.onError("Erreur réseau : Message non envoyé.");
            }
        } else {
            System.err.println("[NETWORK] Tentative d'envoi sans connexion (output is null)");
        }
    }

    public void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (Exception e) {}
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
}