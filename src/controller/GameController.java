package controller;

import model.*;
import network.ServerConnection;
import java.util.ArrayList;
import view.CardViewModel;
import view.PlayerViewModel;

// Controleur MVC - Gere toute la logique metier
public class GameController {
    private GameState gameState;
    private ServerConnection serverConnection;
    private ArrayList<GameObserver> observers;

    public GameController(String serverIp, int serverPort) {
        this.gameState = new GameState();
        this.observers = new ArrayList<>();
        this.serverConnection = new ServerConnection(serverIp, serverPort, this);
    }

    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers() {
        for (GameObserver observer : observers) {
            observer.onGameStateChanged(gameState);
        }
    }

    // ===== ACTIONS PUBLIQUES (appelees par la View) =====

    public boolean connect(String playerName) {
        if (!validatePlayerName(playerName)) {
            onError("Nom de joueur invalide");
            return false;
        }
        return serverConnection.connect(playerName);
    }

    public void disconnect() {
        serverConnection.disconnect();
    }

    // Valide et cree une carte
    public boolean createCard(String name, int attack, int defense, int health) {
        // Validation dans le controller (pas dans la view!)
        if (!validateCardData(name, attack, defense, health)) {
            return false;
        }

        Player player = gameState.getCurrentPlayer();
        if (player == null || !player.canCreateCard()) {
            onError("Impossible de creer une carte (main pleine ou non connecte)");
            return false;
        }

        return serverConnection.sendCreateCardRequest(name, attack, defense, health);
    }

    public boolean exchangeCard(int myCardId, int targetPlayerId, int targetCardId) {
        if (!validateExchange(myCardId, targetPlayerId, targetCardId)) {
            return false;
        }
        return serverConnection.sendExchangeRequest(myCardId, targetPlayerId, targetCardId);
    }

    public boolean acceptExchange(String requestId) {
        return serverConnection.sendExchangeResponse(requestId, true);
    }

    public boolean rejectExchange(String requestId) {
        return serverConnection.sendExchangeResponse(requestId, false);
    }

    public boolean combatCard(int myCardId, int targetPlayerId, int targetCardId) {
        if (!validateCombat(myCardId, targetPlayerId, targetCardId)) {
            return false;
        }
        return serverConnection.sendCombatRequest(myCardId, targetPlayerId, targetCardId);
    }

    public boolean acceptCombat(String requestId) {
        return serverConnection.sendCombatResponse(requestId, true);
    }

    public boolean rejectCombat(String requestId) {
        return serverConnection.sendCombatResponse(requestId, false);
    }

    // ===== GETTERS POUR LA VIEW =====

    public GameState getGameState() {
        return gameState;
    }

    public boolean isConnected() {
        return serverConnection.isConnected();
    }

    // Retourne les infos du joueur actuel pour l'affichage
    public PlayerViewModel getCurrentPlayerViewModel() {
        Player player = gameState.getCurrentPlayer();
        if (player == null) return null;

        return new PlayerViewModel(
                player.getId(),
                player.getName(),
                player.getCardCount(),
                player.getHand().getMaxCards(),
                convertCardsToViewModels(player.getHand().getCards())
        );
    }

    // Retourne la liste des autres joueurs pour l'affichage
    public ArrayList<PlayerViewModel> getOtherPlayersViewModels() {
        ArrayList<PlayerViewModel> result = new ArrayList<>();
        Player currentPlayer = gameState.getCurrentPlayer();

        for (Player p : gameState.getConnectedPlayers()) {
            if (currentPlayer == null || p.getId() != currentPlayer.getId()) {
                result.add(new PlayerViewModel(
                        p.getId(),
                        p.getName(),
                        p.getCardCount(),
                        p.getHand().getMaxCards(),
                        convertCardsToViewModels(p.getHand().getCards())
                ));
            }
        }

        return result;
    }

    // ===== VALIDATIONS (logique metier) =====

    private boolean validatePlayerName(String name) {
        return name != null && !name.trim().isEmpty() && name.length() <= 20;
    }

    private boolean validateCardData(String name, int attack, int defense, int health) {
        if (name == null || name.trim().isEmpty()) {
            onError("Le nom de la carte est vide");
            return false;
        }
        if (attack < 1 || attack > 100) {
            onError("L'attaque doit etre entre 1 et 100");
            return false;
        }
        if (defense < 1 || defense > 100) {
            onError("La defense doit etre entre 1 et 100");
            return false;
        }
        if (health < 1 || health > 200) {
            onError("Les HP doivent etre entre 1 et 200");
            return false;
        }
        return true;
    }

    private boolean validateExchange(int myCardId, int targetPlayerId, int targetCardId) {
        Player player = gameState.getCurrentPlayer();
        if (player == null) {
            onError("Non connecte");
            return false;
        }
        if (player.getHand().getSpecificCardById(myCardId) == null) {
            onError("Vous ne possedez pas cette carte");
            return false;
        }
        if (gameState.getPlayerById(targetPlayerId) == null) {
            onError("Joueur cible introuvable");
            return false;
        }
        return true;
    }

    private boolean validateCombat(int myCardId, int targetPlayerId, int targetCardId) {
        return validateExchange(myCardId, targetPlayerId, targetCardId);
    }

    // ===== CONVERSION MODEL -> VIEWMODEL =====

    private ArrayList<CardViewModel> convertCardsToViewModels(ArrayList<Card> cards) {
        ArrayList<CardViewModel> result = new ArrayList<>();
        for (Card card : cards) {
            result.add(new CardViewModel(
                    card.getId(),
                    card.getName(),
                    card.getAttack(),
                    card.getDefense(),
                    card.getHealth()
            ));
        }
        return result;
    }

    // ===== CALLBACKS RESEAU =====

    public void onConnectionEstablished(int playerId, String playerName, ArrayList<Card> hand) {
        Player player = new Player(playerId, playerName);
        player.setConnected(true);

        for (Card card : hand) {
            player.getHand().addCard(card);
        }

        gameState.setCurrentPlayer(player);
        notifyObservers();
    }

    public void onCardCreated(Card card) {
        Player player = gameState.getCurrentPlayer();
        if (player != null) {
            player.getHand().addCard(card);
            notifyObservers();
        }
    }

    public void onPlayersUpdated(ArrayList<Player> players) {
        gameState.updateConnectedPlayers(players);
        notifyObservers();
    }

    public void onExchangeRequest(String requestId, int fromPlayerId, String fromPlayerName,
                                  int offeredCardId, int requestedCardId) {
        for (GameObserver observer : observers) {
            observer.onExchangeRequestReceived(requestId, fromPlayerId, fromPlayerName,
                    offeredCardId, requestedCardId);
        }
    }

    public void onExchangeCompleted(Card newCard, int lostCardId) {
        Player player = gameState.getCurrentPlayer();
        if (player != null) {
            Card lostCard = player.getHand().getSpecificCardById(lostCardId);
            if (lostCard != null) {
                player.getHand().removeCard(lostCard);
            }
            player.getHand().addCard(newCard);
            notifyObservers();
        }
    }

    public void onCombatRequest(String requestId, int fromPlayerId, String fromPlayerName,
                                int attackingCardId, int targetCardId) {
        for (GameObserver observer : observers) {
            observer.onCombatRequestReceived(requestId, fromPlayerId, fromPlayerName,
                    attackingCardId, targetCardId);
        }
    }

    public void onCombatCompleted(CombatResult result) {
        Player player = gameState.getCurrentPlayer();

        if (player != null) {
            Card winnerCard = player.getHand().getSpecificCardById(result.getWinnerCardId());
            Card loserCard = player.getHand().getSpecificCardById(result.getLoserCardId());

            if (loserCard != null && !loserCard.isUsable()) {
                player.getHand().removeCard(loserCard);
            }

            notifyObservers();
        }

        for (GameObserver observer : observers) {
            observer.onCombatCompleted(result);
        }
    }

    public void onCardRemoved(int cardId) {
        Player player = gameState.getCurrentPlayer();
        if (player != null) {
            Card card = player.getHand().getSpecificCardById(cardId);
            if (card != null) {
                player.getHand().removeCard(card);
                notifyObservers();
            }
        }
    }

    public void onError(String errorMessage) {
        for (GameObserver observer : observers) {
            observer.onError(errorMessage);
        }
    }
}