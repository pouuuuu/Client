package network;

import jsonUtils.jsonBuilder;
import java.io.*;
import java.net.Socket;

public class ServerConnection {
    // Configuration statique
    private static final String SERVER_IP = "134.59.27.143";
    private static final int SERVER_PORT = 8080;

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private boolean connected;
    private jsonBuilder jsonBuilder;
    private String currentPlayerName;

    public ServerConnection() {
        this.connected = false;
        this.jsonBuilder = new jsonBuilder();
    }

    // --- 1. CONNEXION BASIQUE ---
    public boolean connect(String playerName) {
        this.currentPlayerName = playerName;
        System.out.println("[RESEAU] Ouverture socket vers " + SERVER_IP + ":" + SERVER_PORT);

        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            // Envoi de la requête d'auth
            sendJson(jsonBuilder.jsonAuth(playerName));

            // Lecture de la réponse immédiate (Bloquant juste pour l'auth)
            String response = input.readLine();

            if (response != null && (response.contains("AUTH") || response.contains("OK"))) {
                System.out.println("[RESEAU] Auth OK.");
                connected = true;
                return true;
            } else {
                System.err.println("[RESEAU] Auth refusée : " + response);
                return false;
            }
        } catch (IOException e) {
            System.err.println("[RESEAU] Erreur socket : " + e.getMessage());
            return false;
        }
    }

    public void disconnect() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) { /* Ignorer */ }
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    // --- 2. PRIMITIVES D'ECHANGE (Appelées par le Contrôleur) ---

    public String waitResponse() throws IOException {
        if (!connected || input == null) throw new IOException("Non connecté");
        return input.readLine();
    }

    // Envoi simple
    public void sendCreateCardRequest(String n, int a, int d, int h) {
        sendJson(jsonBuilder.jsonCreateCard(currentPlayerName, n, h, a, d));
    }

    public void sendExchangeRequest(int m, int tp, int tc) {
        sendJson(jsonBuilder.jsonTradeRequest(currentPlayerName, m, String.valueOf(tp), tc));
    }

    public void sendCombatRequest(int m, int tp, int tc) {
        sendJson(jsonBuilder.jsonFightRequest(currentPlayerName, m, String.valueOf(tp), tc));
    }

    private void sendJson(String json) {
        if (output != null) {
            output.println(json);
            System.out.println("[ENVOI] " + json);
        }
    }
}