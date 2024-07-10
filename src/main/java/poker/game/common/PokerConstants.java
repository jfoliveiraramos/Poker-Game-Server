package poker.game.common;

public final class PokerConstants {
    public static final int HAND_SIZE = 2;
    public static final int NUM_PLAYERS = 6;
    public static final int NUM_FLOP_CARDS = 3;
    public static final int NUM_TURN_CARDS = 1;
    public static final int NUM_RIVER_CARDS = 1;
    public static final int NUM_COMMUNITY_CARDS = NUM_FLOP_CARDS + NUM_TURN_CARDS + NUM_RIVER_CARDS;
    public static final int HANDS_PER_BLIND = 5;
    public static final int BLIND_INCREASE = 2;
    public static final int STARTING_MONEY = 10000;
    public static final int MAX_NUM_HANDS = 20;

    private PokerConstants() {
        throw new AssertionError("This class should not be instantiated.");
    }
}
