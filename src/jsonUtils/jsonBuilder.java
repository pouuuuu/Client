package jsonUtils;

// Constructeur de messages JSON pour le protocole client-serveur
public class jsonBuilder {

    // Authentification au serveur
    public String jsonAuth(String playerName) {
        return "{\"cmd\":\"AUTH\",\"data\":{\"user\":\"" + playerName + "\"}}";
    }

    // Creation d'une carte
    public String jsonCreateCard(String playerName, String cardName, int hp, int ap, int dp) {
        return "{\"cmd\":\"CREATE_CARD\",\"user\":\"" + playerName + "\",\"data\":{\"cardName\":\"" + cardName + "\",\"HP\":" + hp + ",\"AP\":" + ap + ",\"DP\":" + dp + "}}";
    }

    // Demande d'echange de carte
    public String jsonTradeRequest(String playerName, int cardId, String traderId, int traderCardId) {
        return "{\"cmd\":\"TRADE_REQUEST\",\"user\":\"" + playerName + "\",\"data\":{\"cardId\":" + cardId + ",\"traderId\":\"" + traderId + "\",\"traderCardId\":" + traderCardId + "}}";
    }

    // Acceptation d'un echange
    public String jsonTradeAccept(String playerName) {
        return "{\"cmd\":\"TRADE_ACCEPT\",\"user\":\"" + playerName + "\"}";
    }

    // Refus d'un echange
    public String jsonTradeDeny(String playerName) {
        return "{\"cmd\":\"TRADE_DENY\",\"user\":\"" + playerName + "\"}";
    }

    // Demande de combat
    public String jsonFightRequest(String playerName, int cardId, String opponentId, int opponentCardId) {
        return "{\"cmd\":\"FIGHT_REQUEST\",\"user\":\"" + playerName + "\",\"data\":{\"cardId\":" + cardId + ",\"opponentId\":\"" + opponentId + "\",\"opponentCardId\":" + opponentCardId + "}}";
    }

    // Acceptation d'un combat
    public String jsonFightAccept(String playerName) {
        return "{\"cmd\":\"FIGHT_ACCEPT\",\"user\":\"" + playerName + "\"}";
    }

    // Refus d'un combat
    public String jsonFightDeny(String playerName) {
        return "{\"cmd\":\"FIGHT_DENY\",\"user\":\"" + playerName + "\"}";
    }

    // Message d'erreur generique
    public String jsonError() {
        return "{\"cmd\":\"ERROR\",\"data\":{\"error\":\"error\"}}";
    }
}