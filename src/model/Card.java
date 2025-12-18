package model;

import java.util.UUID;

public class Card {
    private int id;
    private String name;
    private int attack;
    private int defense;
    private int health;
    private int maxHealth;
    private int ownerId;

    public Card(int id, String name, int attack, int defense, int health, int ownerId) {
        this.id = id;
        this.name = name;
        this.attack = attack;
        this.defense = defense;
        this.health = health;
        this.maxHealth = health;
        this.ownerId = ownerId;

        System.out.println("[MODEL] (+) Card Instance Created (Owner: " + ownerId + "): " + this.toString());
    }

    // Getters and setters
    public int getId() { return id; }
    public String getName() { return name; }
    public int getAttack() { return attack; }
    public int getDefense() { return defense; }
    public int getHealth() { return health; }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getOwnerId() { return ownerId; }

    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }
    public void setHealth(int health) { this.health = health; }

    public boolean isUsable() { return health > 0; }

    @Override
    public String toString() {
        return String.format("Card[id=%d, name='%s', AP=%d, DF=%d, HP=%d]",
                id, name, attack, defense, health);
    }
}