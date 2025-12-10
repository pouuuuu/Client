package model;

import java.util.ArrayList;

public class GameState {
    private Player currentPlayer;
    private ArrayList<Player> connectedPlayers;

    public GameState() {
        this.connectedPlayers = new ArrayList<>();
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(Player player) {
        this.currentPlayer = player;
    }

    public ArrayList<Player> getConnectedPlayers() {
        return connectedPlayers;
    }

    public void updateConnectedPlayers(ArrayList<Player> players) {
        this.connectedPlayers = players;
    }

    public Player getPlayerById(int playerId) {
        for (Player p : connectedPlayers) {
            if (p.getId() == playerId) {
                return p;
            }
        }
        return null;
    }
}