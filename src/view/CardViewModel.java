package view;

public class CardViewModel {
    private int id;
    private String name;
    private int attack;
    private int defense;
    private int health;
    private int maxHealth; // AJOUTÉ

    // Mise à jour du constructeur
    public CardViewModel(int id, String name, int attack, int defense, int health, int maxHealth) {
        this.id = id;
        this.name = name;
        this.attack = attack;
        this.defense = defense;
        this.health = health;
        this.maxHealth = maxHealth; // AJOUTÉ
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getAttack() { return attack; }
    public int getDefense() { return defense; }
    public int getHealth() { return health; }

    // AJOUTÉ
    public int getMaxHealth() { return maxHealth; }
}