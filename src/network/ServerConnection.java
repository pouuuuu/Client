package network;

import controller.GameController;
import jsonUtils.jsonBuilder;
import java.io.*;
import java.net.Socket;

public class ServerConnection {
    private static final String SERVER_IP = "127.0.0.1"; // Remets ton IP si différente
    private static final int SERVER_PORT = 8080;

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private boolean connected;
    private jsonBuilder jsonBuilder;
    private String currentPlayerName;
    private GameController controller;

    public ServerConnection(GameController controller) {
        this.controller = controller;
        this.connected = false;
        this.jsonBuilder = new jsonBuilder();
    }

    // --- 1. CONNEXION ASYNCHRONE ---
    public boolean connect(String playerName) {
        this.currentPlayerName = playerName;
        System.out.println("[RESEAU] Tentative de connexion...");

        try {
            // 1. On ouvre le tuyau
            socket = new Socket(SERVER_IP, SERVER_PORT);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            connected = true;

            // 2. IMPORTANT : On lance l'écoute TOUT DE SUITE
            startListenerThread();

            // 3. On envoie le paquet d'auth, mais ON N'ATTEND PAS LA REPONSE ICI
            sendJson(jsonBuilder.jsonAuth(playerName));

            return true; // Le socket est ouvert, c'est tout ce qu'on sait pour l'instant
        } catch (IOException e) {
            System.err.println("[RESEAU] Erreur socket : " + e.getMessage());
            return false;
        }
    }

    // --- 2. ECOUTE PERMANENTE ---
    private void startListenerThread() {
        Thread listener = new Thread(() -> {
            try {
                String message;
                // Tant qu'on est connecté, on lit ce qui arrive
                while (connected && (message = input.readLine()) != null) {
                    System.out.println("[RECU] " + message);

                    // ON DELEGUE IMMEDIATEMENT AU CONTROLEUR
                    controller.processServerMessage(message);
                }
            } catch (IOException e) {
                if (connected) {
                    System.err.println("[RESEAU] Connexion coupée.");
                    controller.onError("Connexion perdue.");
                }
            }
        });
        listener.setDaemon(true); // Le thread s'arrête si l'appli ferme
        listener.start();
    }

    // --- 3. ACTIONS (Juste de l'envoi) ---

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

    public void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (Exception e) {}
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
}