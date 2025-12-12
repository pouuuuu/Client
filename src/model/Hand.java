package model;

import java.util.ArrayList;

public class Hand {
    private ArrayList<Card> cards;
    private static final int MAX_CARDS = 10;

    public Hand() {
        this.cards = new ArrayList<Card>();
    }

    public ArrayList<Card> getCards() {
        return cards;
    }

    public int getNbCards() {
        return cards.size();
    }

    public int getMaxCards() {
        return MAX_CARDS;
    }

    public void addCard(Card card) {
        this.cards.add(card);
    }

    public void removeCard(Card card) {
        this.cards.remove(card);
    }

    public Card getSpecificCardById(int id) {
        for (Card card : this.cards) {
            if(card.getId() == id) {
                return card;
            }
        }
        return null;
    }
}
