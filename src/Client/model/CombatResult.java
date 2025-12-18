package Client.model;

public class CombatResult {
    private int winnerId;
    private int loserId;
    private int winnerCardId;
    private int loserCardId;
    private int damageDealt;

    public CombatResult(int winnerId, int loserId,
                        int winnerCardId, int loserCardId, int damageDealt) {
        this.winnerId = winnerId;
        this.loserId = loserId;
        this.winnerCardId = winnerCardId;
        this.loserCardId = loserCardId;
        this.damageDealt = damageDealt;
    }

    public int getWinnerId() {
        return winnerId;
    }
    public int getLoserId() {
        return loserId;
    }
    public int getWinnerCardId() {
        return winnerCardId;
    }
    public int getLoserCardId() {
        return loserCardId;
    }
    public int getDamageDealt() {
        return damageDealt;
    }
}