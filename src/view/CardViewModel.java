package view;

// Cette classe est la représentation des données d'une carte
// optimisée pour l'affichage dans la View.
public class CardViewModel {
    private int id;
    private String name;
    private int attack;
    private int defense;
    private int health;

    public CardViewModel(int id, String name, int attack, int defense, int health) {
        this.id = id;
        this.name = name;
        this.attack = attack;
        this.defense = defense;
        this.health = health;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getAttack() { return attack; }
    public int getDefense() { return defense; }
    public int getHealth() { return health; }
}