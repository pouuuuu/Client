package view;

import java.util.ArrayList;

// Cette classe est la représentation des données d'un joueur
// optimisée pour l'affichage dans la View.
public class PlayerViewModel {
    private int id;
    private String name;
    private int cardCount;
    private int maxCards;
    private ArrayList<CardViewModel> cards;

    public PlayerViewModel(int id, String name, int cardCount, int maxCards, ArrayList<CardViewModel> cards) {
        this.id = id;
        this.name = name;
        this.cardCount = cardCount;
        this.maxCards = maxCards;
        this.cards = cards;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getCardCount() { return cardCount; }
    public int getMaxCards() { return maxCards; }
    public ArrayList<CardViewModel> getCards() { return cards; }
}