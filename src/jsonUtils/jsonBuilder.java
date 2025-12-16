package jsonUtils;

// Constructeur de messages JSON pour le protocole client-serveur
public class jsonBuilder {

    // Authentification au serveur
    public String jsonAuth(String playerName) {
        return "{\"cmd\":\"AUTH\",\"data\":{\"user\":\"" + playerName + "\"}}";
    }

    // Creation d'une carte
    public String jsonCreateCard(int playerId, String cardName, int hp, int ap, int dp) {
        return "{\"cmd\":\"CREATE_CARD\",\"id\":" + playerId + ",\"data\":{\"cardName\":\"" + cardName + "\",\"HP\":" + hp + ",\"AP\":" + ap + ",\"DP\":" + dp + "}}";
    }

    // Demande d'echange
    public String jsonTradeRequest(int playerId, int cardId, String traderId, int traderCardId) {
        return "{\"cmd\":\"TRADE_REQUEST\",\"id\":" + playerId + ",\"data\":{\"cardId\":" + cardId + ",\"traderId\":\"" + traderId + "\",\"traderCardId\":" + traderCardId + "}}";
    }

    // Demande de combat
    public String jsonFightRequest(int playerId, int cardId, String opponentId, int opponentCardId) {
        return "{\"cmd\":\"FIGHT_REQUEST\",\"id\":" + playerId + ",\"data\":{\"cardId\":" + cardId + ",\"opponentId\":\"" + opponentId + "\",\"opponentCardId\":" + opponentCardId + "}}";
    }

    // Pour les Accept/Deny, on passe aussi l'ID
    public String jsonTradeAccept(int playerId) {
        return "{\"cmd\":\"TRADE_ACCEPT\",\"id\":" + playerId + "}";
    }

    public String jsonTradeDeny(int playerId) {
        return "{\"cmd\":\"TRADE_DENY\",\"id\":" + playerId + "}";
    }

    public String jsonFightAccept(int playerId) {
        return "{\"cmd\":\"FIGHT_ACCEPT\",\"id\":" + playerId + "}";
    }

    public String jsonFightDeny(int playerId) {
        return "{\"cmd\":\"FIGHT_DENY\",\"id\":" + playerId + "}";
    }

    public String jsonError() {
        return "{\"cmd\":\"ERROR\",\"data\":{\"error\":\"error\"}}";
    }
}