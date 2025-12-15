package network;

import controller.GameController;
import jsonUtils.jsonBuilder;
import java.io.*;
import java.net.Socket;

public class ServerConnection {
    // Adapter l'IP selon votre configuration
    private static final String SERVER_IP = "127.0.0.1";
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

    // --- CONNECTION ---
    public boolean connect(String playerName) {
        this.currentPlayerName = playerName;
        System.out.println("[NETWORK] Attempting to connect to " + SERVER_IP + ":" + SERVER_PORT + "...");

        try {
            // 1. Open socket
            socket = new Socket(SERVER_IP, SERVER_PORT);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            connected = true;

            System.out.println("[NETWORK] Connection established!");

            // 2. Start listening thread immediately
            startListenerThread();

            // 3. Send Authentication Request
            sendJson(jsonBuilder.jsonAuth(playerName));

            return true;
        } catch (IOException e) {
            System.err.println("[NETWORK] Socket error: " + e.getMessage());
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
            // --- LOG: PACKET SENT ---
            System.out.println("[PACKET OUT] " + json);

            output.println(json);
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