package poker.game.common;

import java.util.*;

public class Deck {
    private final int DECK_SIZE = 52;
    private final ArrayList<Card> deck = new ArrayList<>(DECK_SIZE);

    public Deck() {
        reset();
    }

    public void reset() {
        deck.clear();
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                deck.add(new Card(suit, rank));
            }
        }
    }

    public void shuffle() {
        Collections.shuffle(deck);
    }

    public Card deal() {
        return deck.removeFirst();
    }

    public ArrayList<Card> dealCards(int numCards) {
        ArrayList<Card> cards = new ArrayList<>(numCards);
        for (int i = 0; i < numCards; i++) {
            cards.add(deal());
        }
        return cards;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Card card : deck) {
            sb.append(card);
            sb.append("\n");
        }
        return sb.toString();
    }
}
