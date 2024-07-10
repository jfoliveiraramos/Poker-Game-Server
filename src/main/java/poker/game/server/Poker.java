package poker.game.server;

import poker.game.common.*;

import java.util.ArrayList;
import java.util.Comparator;

import static poker.game.common.PokerConstants.*;

public class Poker {
    private int handsPlayed;
    private int smallBlindBet;
    private int bigBlindBet;
    private int smallBlind;
    private int bigBlind;
    private int lastRaiser;
    private int currPlayer;
    private int pot;
    private int currBet;
    private boolean isHandOver = false;
    private boolean isGameOver = false;
    private GamePhase state;

    private final ArrayList<PokerPlayer> players = new ArrayList<>(NUM_PLAYERS);
    private final Deck deck = new Deck();
    private final ArrayList<Card> communityCards = new ArrayList<>(NUM_COMMUNITY_CARDS);
    private final HandRanker handRanker = new HandRanker(this);

    public Poker(ArrayList<String> players) {
        this.handsPlayed = 0;
        this.smallBlindBet = 50;
        this.bigBlindBet = 100;
        this.smallBlind = 0;
        this.bigBlind = 1;
        this.currPlayer = 0;
        this.state = GamePhase.PREFLOP;

        for (String player : players) {
            this.players.add(new PokerPlayer(player, STARTING_MONEY));
        }

        this.startHand();
    }

    public ArrayList<Card> getCommunityCards() {
        return this.communityCards;
    }

    public ArrayList<PokerPlayer> getPlayers() {
        return this.players;
    }

    public ArrayList<PokerPlayer> getActivePlayers() {
        ArrayList<PokerPlayer> activePlayers = new ArrayList<>();
        for (PokerPlayer player : players) {
            if (player.getState() != PokerPlayer.PLAYER_STATE.FOLDED && player.getState() != PokerPlayer.PLAYER_STATE.OUT_OF_MONEY) {
                activePlayers.add(player);
            }
        }

        return activePlayers;
    }

    public int getCurrPlayer() {
        return this.currPlayer;
    }

    public int getCurrBet() {
        return this.currBet;
    }

    public boolean getIsHandOver() {
        return this.isHandOver;
    }

    public boolean getIsGameOver() {
        return this.isGameOver;
    }

    public int getPot() {
        return this.pot;
    }

    public int getHandsPlayed() {
        return this.handsPlayed;
    }

    private boolean isPlayerInactive(int playerIndex) {
        return this.players.get(playerIndex).getState() == PokerPlayer.PLAYER_STATE.FOLDED || this.players.get(playerIndex).getState() == PokerPlayer.PLAYER_STATE.ALL_IN || this.players.get(playerIndex).getState() == PokerPlayer.PLAYER_STATE.OUT_OF_MONEY;
    }

    private int getNextActivePlayer(int playerIndex) {
        int nextPlayer = (playerIndex + 1) % NUM_PLAYERS;
        while (isPlayerInactive(nextPlayer)) {
            nextPlayer = (nextPlayer + 1) % NUM_PLAYERS;
        }
        return nextPlayer;
    }

    private void updateBlinds() {
        this.smallBlind = getNextActivePlayer(this.smallBlind);
        this.bigBlind = getNextActivePlayer(this.bigBlind);

        if ((this.handsPlayed % HANDS_PER_BLIND) == 0) {
            this.smallBlindBet *= BLIND_INCREASE;
            this.bigBlindBet *= BLIND_INCREASE;
        }
    }

    private boolean isHandOver() {
        if ((this.state == GamePhase.RIVER) && (this.currPlayer == this.lastRaiser)) return true;
        if (this.getActivePlayers().size() == 1) return true;

        int numPlayersNotAllIn = 0;
        for (PokerPlayer player : getActivePlayers()) {
            if (player.getState() != PokerPlayer.PLAYER_STATE.ALL_IN) {
                numPlayersNotAllIn++;
            }
        }

        return numPlayersNotAllIn == 0;
    }

    private boolean isGameOver() {
        if (this.handsPlayed == MAX_NUM_HANDS) return true;

        int numPlayersWithMoney = 0;
        for (PokerPlayer player : players) {
            if (player.getMoney() > 0) {
                numPlayersWithMoney++;
            }
        }

        return numPlayersWithMoney == 1;
    }

    public ArrayList<PokerPlayer> getGameWinners() {
        ArrayList<PokerPlayer> winners = new ArrayList<PokerPlayer>(players);
        winners.sort(Comparator.comparingInt(PokerPlayer::getMoney));
        return winners;
    }

    public ArrayList<PokerPlayer> getHandWinners() {
        return handRanker.getWinners();
    }

    private void startHand() {
        this.pot = 0;
        this.lastRaiser = -1;
        this.currBet = 0;
        this.currPlayer = this.smallBlind;
        this.state = GamePhase.PREFLOP;
        this.isHandOver = false;
        this.isGameOver = false;

        deck.reset();
        deck.shuffle();

        for (PokerPlayer player : players) {
            if (player.getState() != PokerPlayer.PLAYER_STATE.OUT_OF_MONEY)
                player.setHand(deck.dealCards(HAND_SIZE));
        }

        this.communityCards.clear();
        this.communityCards.addAll(deck.dealCards(NUM_COMMUNITY_CARDS));
        this.takeAction(PokerPlayer.PLAYER_ACTION.BET, this.smallBlindBet);
        this.takeAction(PokerPlayer.PLAYER_ACTION.BET, this.bigBlindBet);
    }

    private void nextHand() {
        this.handsPlayed++;

        for (PokerPlayer player : players) {
            player.resetBet();
            player.resetTurnBet();
            player.resetState();
        }

        this.updateBlinds();
        this.startHand();
    }

    public void endHand() {
        ArrayList<PokerPlayer> winners = this.getHandWinners();
        winners.sort(Comparator.comparingInt(PokerPlayer::getBet));

        int numWinners = winners.size();

        for (int i = 0; i < numWinners - 1; i++) {
            int sidePot = winners.get(i).getBet() - winners.get(i + 1).getBet();
            this.pot -= sidePot;

            for (int j = i; j >= 0; j--) {
                winners.get(j).addMoney(sidePot / (i + 1));
            }
        }

        int winnings = this.pot / numWinners;

        for (PokerPlayer winner : winners) {
            winner.addMoney(winnings);
        }

        if (this.isGameOver()) {
            this.isGameOver = true;
        } else {
            this.nextHand();
        }
    }

    public void nextTurn() {
        switch (this.state) {
            case PREFLOP:
                this.state = GamePhase.FLOP;
                break;
            case FLOP:
                this.state = GamePhase.TURN;
                break;
            case TURN:
                this.state = GamePhase.RIVER;
                break;
        }

        this.currBet = 0;
        for (PokerPlayer player : players) {
            player.resetTurnBet();
        }

        this.currPlayer = this.smallBlind;
        if (isPlayerInactive(this.currPlayer)) {
            this.currPlayer = getNextActivePlayer(this.currPlayer);
        }

        this.lastRaiser = this.currPlayer;
    }

    public void nextPlayer() {
        this.currPlayer = (this.currPlayer + 1) % NUM_PLAYERS;
    }

    private void afterPlayerAction() {
        this.nextPlayer();
        if (this.isHandOver()) {
            this.isHandOver = true;
            this.state = GamePhase.RIVER;
        } else if (this.currPlayer == this.lastRaiser) {
            this.nextTurn();
        } else if (this.players.get(this.currPlayer).getState() == PokerPlayer.PLAYER_STATE.FOLDED || this.players.get(this.currPlayer).getState() == PokerPlayer.PLAYER_STATE.ALL_IN || this.players.get(this.currPlayer).getState() == PokerPlayer.PLAYER_STATE.OUT_OF_MONEY) {
            this.afterPlayerAction();
        }
    }

    public void takeAction(PokerPlayer.PLAYER_ACTION action, int amount) {
        if (this.isHandOver || this.isGameOver) return;
        PokerPlayer player = this.players.get(this.currPlayer);
        int playerBet = player.getBet();
        switch (action) {
            case FOLD:
                player.fold();
                break;
            case BET:
                player.placeBet(amount);
                if (player.getBet() > this.currBet) {
                    this.currBet = player.getBet();
                    this.lastRaiser = this.currPlayer;
                }
                break;
            case CALL:
                player.placeBet(this.currBet - player.getBet());
                break;
            case CHECK:
                break;
            case ALL_IN:
                player.placeBet(player.getMoney());
                if (player.getBet() > this.currBet) {
                    this.currBet = player.getBet();
                    this.lastRaiser = this.currPlayer;
                }
                break;
        }

        this.pot += player.getBet() - playerBet;

        // After player action logic
        this.afterPlayerAction();
    }

    public GameState getGameStateToSend(int playerAsking) {
        ArrayList<PokerPlayer> playersToSend = new ArrayList<>();
        ArrayList<PokerPlayer> winnersToSend = new ArrayList<>();
        ArrayList<Card> communityCardsToSend = new ArrayList<>();
        ArrayList<HandRank> handRanksToSend = new ArrayList<>();

        if (this.isGameOver) {
            winnersToSend = this.getGameWinners();
        } else if (this.isHandOver) {
            for (PokerPlayer player : players) {
                handRanksToSend.add(handRanker.analyzeHand(player));
            }
            playersToSend.addAll(players);
            winnersToSend = this.getHandWinners();
        } else {
            for (PokerPlayer player : players) {
                if (player.getUsername().equals(players.get(playerAsking).getUsername())) {
                    playersToSend.add(player);
                } else {
                    playersToSend.add(player.privateCopy());
                }
            }
        }

        switch (this.state) {
            case PREFLOP:
                break;
            case FLOP:
                communityCardsToSend.add(this.communityCards.get(0));
                communityCardsToSend.add(this.communityCards.get(1));
                communityCardsToSend.add(this.communityCards.get(2));
                break;
            case TURN:
                communityCardsToSend.add(this.communityCards.get(0));
                communityCardsToSend.add(this.communityCards.get(1));
                communityCardsToSend.add(this.communityCards.get(2));
                communityCardsToSend.add(this.communityCards.get(3));
                break;
            case RIVER:
                communityCardsToSend.add(this.communityCards.get(0));
                communityCardsToSend.add(this.communityCards.get(1));
                communityCardsToSend.add(this.communityCards.get(2));
                communityCardsToSend.add(this.communityCards.get(3));
                communityCardsToSend.add(this.communityCards.get(4));
                break;
        }

        return new GameState(playersToSend, winnersToSend, communityCardsToSend, handRanksToSend, state, isGameOver, isHandOver, playerAsking, currPlayer, smallBlind, bigBlind, smallBlindBet, bigBlindBet, handsPlayed);
    }
}
