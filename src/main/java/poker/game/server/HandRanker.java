package poker.game.server;

import poker.game.common.*;
import java.util.*;

public class HandRanker {
    public final Poker game;

    public HandRanker(Poker game) {
        this.game = game;
    }

    private ArrayList<Card> getPlayerFullHand(PokerPlayer player) {
        ArrayList<Card> communityCards = this.game.getCommunityCards();
        ArrayList<Card> playerCards = player.getHand();

        ArrayList<Card> playerHand = new ArrayList<>();
        playerHand.addAll(communityCards);
        playerHand.addAll(playerCards);

        return playerHand;
    }

    private boolean isRoyalFlush(ArrayList<Card> hand) {
        return isStraightFlush(hand) && hand.getLast().getRank() == Card.Rank.ACE;
    }

    private boolean isStraightFlush(ArrayList<Card> hand) {
        return isStraight(hand) && isFlush(hand);
    }

    private boolean isFourOfAKind(ArrayList<Card> hand) {
        for (int i = 0; i < hand.size() - 3; i++) {
            if (hand.get(i).isSameRank(hand.get(i + 1)) && hand.get(i).isSameRank(hand.get(i + 2)) && hand.get(i).isSameRank(hand.get(i + 3))) {
                return true;
            }
        }
        return false;
    }

    private boolean isFullHouse(ArrayList<Card> hand) {
        ArrayList<Card> handCopy = new ArrayList<>(hand);
        if (handCopy.get(0).isSameRank(handCopy.get(1)) && handCopy.get(0).isSameRank(handCopy.get(2))) {
            return handCopy.get(3).isSameRank(handCopy.get(4));
        }
        else if (handCopy.get(2).isSameRank(handCopy.get(3)) && handCopy.get(2).isSameRank(handCopy.get(4))) {
            return handCopy.get(0).isSameRank(handCopy.get(1));
        }
        return false;
    }

    private boolean isFlush(ArrayList<Card> hand) {
        int sameSuitCards = 0;

        for (int i = 0; i < hand.size() - 1; i++) {
            if (hand.get(i).isSameSuit(hand.get(i + 1))) {
                sameSuitCards++;
            }
        }

        return sameSuitCards == 4;
    }

    private boolean isStraight(ArrayList<Card> hand) {
        int straightCards = 0;

        for (int i = 0; i < hand.size() - 1; i++) {
            if (hand.get(i).compareTo(hand.get(i + 1)) == -1) {
                straightCards++;
            }
        }

        if (hand.getFirst().getRank() == Card.Rank.TWO && hand.getLast().getRank() == Card.Rank.ACE) {
            straightCards++;
        }

        return straightCards == 4;
    }

    private boolean isThreeOfAKind(ArrayList<Card> hand) {
        for (int i = 0; i < hand.size() - 2; i++) {
            if (hand.get(i).isSameRank(hand.get(i + 1)) && hand.get(i).isSameRank(hand.get(i + 2))) {
                return true;
            }
        }
        return false;
    }

    private boolean isTwoPair(ArrayList<Card> hand) {
        int pairs = 0;
        for (int i = 0; i < hand.size()-1; i++) {
            if (hand.get(i).isSameRank(hand.get(i+1))) pairs++;
            i++;
        }

        return pairs == 2;
    }

    private boolean isPair(ArrayList<Card> hand) {
        for (int i = 0; i < hand.size()-1; i++) {
            if (hand.get(i).isSameRank(hand.get(i+1))) return true;
        }

        return false;
    }

    private ArrayList<ArrayList<Card>> getAllPossibleHands(ArrayList<Card> hand) {
        ArrayList<ArrayList<Card>> possibleHands = new ArrayList<>();
        for (int i = 0; i < hand.size(); i++) {
            for (int j = i + 1; j < hand.size(); j++) {
                ArrayList<Card> possibleHand = new ArrayList<>();
                for (int k = 0; k < hand.size(); k++) {
                    if (k != i && k != j) {
                        possibleHand.add(hand.get(k));
                    }
                }
                possibleHands.add(possibleHand);
            }
        }

        return possibleHands;
    }

    private HandRank getHandRank(ArrayList<Card> hand) {
        if (hand.size() != 5) {
            throw new IllegalArgumentException("Hand must have 5 cards");
        }

        hand.sort(Comparator.comparing(Card::getRank).reversed());

        if (isRoyalFlush(hand)) {
            return HandRank.ROYAL_FLUSH;
        } else if (isStraightFlush(hand)) {
            return HandRank.STRAIGHT_FLUSH;
        } else if (isFourOfAKind(hand)) {
            return HandRank.FOUR_OF_A_KIND;
        } else if (isFullHouse(hand)) {
            return HandRank.FULL_HOUSE;
        } else if (isFlush(hand)) {
            return HandRank.FLUSH;
        } else if (isStraight(hand)) {
            return HandRank.STRAIGHT;
        } else if (isThreeOfAKind(hand)) {
            return HandRank.THREE_OF_A_KIND;
        } else if (isTwoPair(hand)) {
            return HandRank.TWO_PAIR;
        } else if (isPair(hand)) {
            return HandRank.PAIR;
        } else {
            return HandRank.HIGH_CARD;
        }
    }

    public HandRank analyzeHand(PokerPlayer player) {
        ArrayList<Card> fullHand = getPlayerFullHand(player);
        ArrayList<ArrayList<Card>> possibleHands = getAllPossibleHands(fullHand);

        HandRank highestRank = HandRank.NONE;
        for (ArrayList<Card> hand : possibleHands) {
            HandRank currentRank = getHandRank(hand);
            if (currentRank.compareTo(highestRank) > 0) {
                highestRank = currentRank;
            }
        }

        return highestRank;
    }

    private ArrayList<Card> sortHand(ArrayList<Card> hand) {
        // Sort by rank in descending order
        hand.sort(Comparator.comparing(Card::getRank).reversed());
        if (isFourOfAKind(hand)){
            if (!hand.get(0).isSameRank(hand.get(1))) {
                Collections.swap(hand, 0, 4);
            }
        }
        else if (isFullHouse(hand)) {
            if (!hand.get(0).isSameRank(hand.get(2))) {
                Collections.swap(hand, 0, 3);
                Collections.swap(hand, 1, 4);
            }
        }
        else if (isThreeOfAKind(hand)) {
            if (!hand.get(0).isSameRank(hand.get(2)) && !hand.get(4).isSameRank(hand.get(2))) {
                Collections.swap(hand, 0, 3);
            }
            else if (!hand.get(0).isSameRank(hand.get(2)) && !hand.get(1).isSameRank(hand.get(2))) {
                hand.sort(Comparator.comparing(Card::getRank));
                Collections.swap(hand, 4, 3);
            }
        }
        else if (isTwoPair(hand)) {
            if (!hand.get(0).isSameRank(hand.get(1))) {
                for (int i = 0; i < hand.size() - 1; i++) {
                    Collections.swap(hand, i, i + 1);
                }
            }
            else if (hand.get(0).isSameRank(hand.get(1)) && hand.get(3).isSameRank(hand.get(4))) {
                Collections.swap(hand, 2, 4);
            }
        }
        else if (isPair(hand)) {
            if (hand.get(1).isSameRank(hand.get(2))) {
                Collections.swap(hand, 0, 2);
            }
            else if (hand.get(2).isSameRank(hand.get(3))) {
                Collections.swap(hand, 0, 2);
                Collections.swap(hand, 1, 3);
            }
            else if (hand.get(3).isSameRank(hand.get(4))) {
                hand.sort(Comparator.comparing(Card::getRank));
                Collections.swap(hand, 2, 4);
            }
        }

        return hand;
    }

    private ArrayList<Card> getBestHand(ArrayList<Card> allCards) {
        ArrayList<ArrayList<Card>> possibleHands = getAllPossibleHands(allCards);
        ArrayList<ArrayList<Card>> orderedHands = new ArrayList<>();

        for (ArrayList<Card> hand : possibleHands) {
            orderedHands.add(sortHand(hand));
        }

        ArrayList<Card> bestHand = orderedHands.getFirst();
        for (int i = 1; i < orderedHands.size(); i++) {
            ArrayList<Card> currentHand = orderedHands.get(i);
            for (int j = 0; j < bestHand.size(); j++) {
                if (bestHand.get(j).compareTo(currentHand.get(j)) < 0) {
                    bestHand = currentHand;
                    break;
                }
                else if (bestHand.get(j).compareTo(currentHand.get(j)) > 0) {
                    break;
                }
            }
        }

        return bestHand;
    }

    public ArrayList<PokerPlayer> getWinners(){
        ArrayList<PokerPlayer> winners = new ArrayList<>();
        HandRank highestRank = HandRank.NONE;

        for (PokerPlayer player : this.game.getActivePlayers()) {
            HandRank playerRank = analyzeHand(player);
            if (playerRank.compareTo(highestRank) > 0) {
                highestRank = playerRank;
                winners.clear();
                winners.add(player);
            }
            else if (playerRank.compareTo(highestRank) == 0) {
                ArrayList<Card> playerFullHand = getPlayerFullHand(player);
                ArrayList<Card> bestHand = getBestHand(playerFullHand);
                ArrayList<Card> bestHandWinner = getBestHand(getPlayerFullHand(winners.getFirst()));
                for (int i = 0; i < bestHand.size(); i++) {
                    if (bestHand.get(i).compareTo(bestHandWinner.get(i)) < 0) {
                        break;
                    }
                    else if (bestHand.get(i).compareTo(bestHandWinner.get(i)) > 0) {
                        winners.clear();
                        winners.add(player);
                        break;
                    }
                    else if (i == bestHand.size() - 1 && bestHand.get(i).compareTo(bestHandWinner.get(i)) == 0) {
                        winners.add(player);
                    }
                }
            }
        }

        return winners;
    }
}
