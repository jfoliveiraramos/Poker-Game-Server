package poker.game.common;

import java.util.ArrayList;

public class GameState {
    ArrayList<PokerPlayer> players;
    ArrayList<PokerPlayer> winners;
    ArrayList<Card> communityCards;
    ArrayList<HandRank> handRanks;
    GamePhase phase;
    boolean isGameOver;
    boolean isHandOver;
    int player;
    int currPlayer;
    int smallBlind;
    int bigBlind;
    int smallBlindBet;
    int bigBlindBet;
    int handsPlayed;

    public GameState(ArrayList<PokerPlayer> players, ArrayList<PokerPlayer> winners, ArrayList<Card> communityCards, ArrayList<HandRank> handRanks, GamePhase phase, boolean isGameOver, boolean isHandOver, int player, int currPlayer, int smallBlind, int bigBlind, int smallBlindBet, int bigBlindBet, int handsPlayed) {
        this.players = players;
        this.winners = winners;
        this.communityCards = communityCards;
        this.handRanks = handRanks;
        this.phase = phase;
        this.isGameOver = isGameOver;
        this.isHandOver = isHandOver;
        this.player = player;
        this.currPlayer = currPlayer;
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;
        this.smallBlindBet = smallBlindBet;
        this.bigBlindBet = bigBlindBet;
        this.handsPlayed = handsPlayed;
    }

    public ArrayList<PokerPlayer> getPlayers() {
        return this.players;
    }

    public ArrayList<PokerPlayer> getWinners() {
        return this.winners;
    }

    public ArrayList<Card> getCommunityCards() {
        return this.communityCards;
    }

    public ArrayList<HandRank> getHandRanks() {
        return this.handRanks;
    }

    public GamePhase getPhase() {
        return this.phase;
    }

    public boolean isGameOver() {
        return this.isGameOver;
    }

    public boolean isHandOver() {
        return this.isHandOver;
    }

    public int getPlayer() {
        return this.player;
    }

    public int getCurrPlayer() {
        return this.currPlayer;
    }

    public int getSmallBlind() {
        return this.smallBlind;
    }

    public int getBigBlind() {
        return this.bigBlind;
    }

    public int getSmallBlindBet() {
        return this.smallBlindBet;
    }

    public int getBigBlindBet() {
        return this.bigBlindBet;
    }

    public int getHandsPlayed() {
        return this.handsPlayed;
    }

    public int getCurrBet() {
        int maximumBet = 0;
        for (PokerPlayer player : players) {
            if (player.getTurnBet() > maximumBet) {
                maximumBet = player.getTurnBet();
            }
        }
        return maximumBet;
    }
}
