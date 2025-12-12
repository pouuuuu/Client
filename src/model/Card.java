package model;

import java.util.UUID;

public class Card {
    private int id;
    private String name;
    private int attack;
    private int defense;
    private int health;
    private String ownerId;

    public Card(int id, String name, int attack, int defense, int health) {
        this.id = id;
        this.name = name;
        this.attack = attack;
        this.defense = defense;
        this.health = health;
    }

    public Card(int id, String name, int attack, int defense, int health, String ownerId) {
        this.id = id;
        this.name = name;
        this.attack = attack;
        this.defense = defense;
        this.health = health;
        this.ownerId = ownerId;
    }

    // Getters and setters
    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public int getAttack() {
        return attack;
    }
    public int getDefense() {
        return defense;
    }
    public int getHealth() {
        return health;
    }
    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
    public void setHealth(int health) {
        this.health = health;
    }

    public boolean isUsable() {
        return health > 0;
    }

    @Override
    public String toString() {
        return String.format("Card[id=%s, name=%s, atk=%d, def=%d, hp=%d]",
                id, name, attack, defense, health);
    }
}
