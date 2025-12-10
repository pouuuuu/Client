package model;

public class Player {
    private int id;
    private String name;
    private Hand hand;
    private boolean isConnected;


    public Player(int id, String name) {
        this.id = id;
        this.name = name;
        this.hand = new Hand();
        this.isConnected = false;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Hand getHand() {
        return hand;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        this.isConnected = connected;
    }

    public boolean canCreateCard() {
        return hand.getNbCards() < hand.getMaxCards();
    }

    public int getCardCount() {
        return hand.getNbCards();
    }
}