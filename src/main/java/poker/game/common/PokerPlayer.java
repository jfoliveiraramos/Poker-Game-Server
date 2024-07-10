package poker.game.common;

import java.util.ArrayList;

public class PokerPlayer {
    private final String username;
    private int money;
    private int bet;
    private int turnBet;
    private ArrayList<Card> hand;
    private PLAYER_STATE state;

    public enum PLAYER_STATE {
        FOLDED, BETTING, ALL_IN, WAITING, OUT_OF_MONEY
    }

    public enum PLAYER_ACTION {
        FOLD("fold"),
        CHECK("check"),
        BET("bet"),
        CALL("call"),
        ALL_IN("all_in");
        final String value;

        PLAYER_ACTION(String value) {
            this.value = value;
        }

        public Boolean equals(PLAYER_ACTION action) {
            return this.value.equals(action.value);
        }

        public String toString() {
            return this.value;
        }

        public static PLAYER_ACTION fromString(String action) {
            return PLAYER_ACTION.valueOf(action.toUpperCase());
        }
    }

    public PokerPlayer(String name, int money) {
        this.username = name;
        this.money = money;
        this.hand = new ArrayList<Card>(2);
        this.state = PLAYER_STATE.WAITING;
        this.bet = 0;
        this.turnBet = 0;
    }

    public PokerPlayer(String name, int money, PLAYER_STATE state, int bet, int turnBet) {
        this.username = name;
        this.money = money;
        this.hand = new ArrayList<Card>(2);
        this.state = state;
        this.bet = bet;
        this.turnBet = turnBet;
    }

    public PokerPlayer privateCopy() {
        return new PokerPlayer(username, money, state, bet, turnBet);
    }

    public String getUsername() {
        return username;
    }

    public int getMoney() {
        return money;
    }

    public ArrayList<Card> getHand() {
        return hand;
    }

    public PLAYER_STATE getState() {
        return state;
    }

    public int getBet() {
        return bet;
    }

    public int getTurnBet() {
        return turnBet;
    }

    public void setHand(ArrayList<Card> hand) {
        this.hand = hand;
    }

    public void addMoney(int amount) {
        this.money += amount;
    }

    public void placeBet(int amount) {
        if (this.money <= amount) {
            this.bet += this.money;
            this.turnBet += this.money;
            this.money = 0;
            this.state = PLAYER_STATE.ALL_IN;
        } else {
            this.bet += amount;
            this.turnBet += amount;
            this.money -= amount;
            this.state = PLAYER_STATE.BETTING;
        }
    }

    public void resetBet() {
        this.bet = 0;
    }

    public void resetTurnBet() {
        this.turnBet = 0;
    }

    public void fold() {
        this.state = PLAYER_STATE.FOLDED;
    }

    public void resetState() {
        this.state = PLAYER_STATE.WAITING;
        if (this.money == 0) {
            this.state = PLAYER_STATE.OUT_OF_MONEY;
        }
    }

    public String toString() {
        return username + ": " + money + " | " + state + " | " + turnBet;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof PokerPlayer)) {
            return false;
        }
        PokerPlayer player = (PokerPlayer) obj;
        return this.username.equals(player.username);
    }
}
