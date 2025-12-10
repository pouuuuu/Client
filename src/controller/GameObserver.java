package controller;

import model.CombatResult;
import model.GameState;

public interface GameObserver {
    void onGameStateChanged(GameState gameState);
    void onExchangeRequestReceived(String requestId, String fromPlayerId,
                                   String fromPlayerName, String offeredCardId,
                                   String requestedCardId);
    void onCombatRequestReceived(String requestId, String fromPlayerId,
                                 String fromPlayerName, String attackingCardId,
                                 String targetCardId);

    void onExchangeRequestReceived(String requestId, int fromPlayerId,
                                   String fromPlayerName, int offeredCardId,
                                   int requestedCardId);

    void onCombatRequestReceived(String requestId, int fromPlayerId,
                                 String fromPlayerName, int attackingCardId,
                                 int targetCardId);

    void onCombatCompleted(CombatResult result);
    void onError(String errorMessage);
}
