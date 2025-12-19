package Client.jsonUtils;

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
    public String jsonFightRequest(int playerId, int cardId, int opponentId, int opponentCardId) {
        return "{\"cmd\":\"FIGHT_REQUEST\",\"id\":" + playerId + ",\"data\":{\"cardId\":" + cardId + ",\"opponentId\":\"" + opponentId + "\",\"opponentCardId\":" + opponentCardId + "}}";
    }

    public String jsonTradeAccept(int playerId, int traderId, int cardId, int traderCardId) {
        return buildTradeResponse("TRADE_ACCEPT", playerId, traderId, cardId, traderCardId);
    }

    public String jsonTradeDeny(int playerId, int traderId, int cardId, int traderCardId) {
        return buildTradeResponse("TRADE_DENY", playerId, traderId, cardId, traderCardId);
    }

    private String buildTradeResponse(String cmd, int playerId, int traderId, int cardId, int traderCardId) {
        return "{" + "\"cmd\":\"" + cmd + "\"," + "\"data\":{" + "\"playerId\":" + playerId + "," + "\"cardId\":" + cardId + "," + "\"traderId\":" + traderId + "," + "\"traderCardId\":" + traderCardId + "}" + "}";
    }


    public String jsonFightAccept(int playerId) {
        return "{\"cmd\":\"FIGHT_ACCEPT\",\"id\":" + playerId + "}";
    }

    public String jsonFightDeny(int playerId) {
        return "{\"cmd\":\"FIGHT_DENY\",\"id\":" + playerId + "}";
    }
}