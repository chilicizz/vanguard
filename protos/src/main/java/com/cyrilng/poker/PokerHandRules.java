package com.cyrilng.poker;

import com.cyrilng.cards.Card;
import com.cyrilng.cards.Hand;
import com.cyrilng.cards.Rank;
import com.cyrilng.cards.Suit;
import com.cyrilng.game.PokerHandType;

import java.util.*;

public class PokerHandRules {
    /**
     * 1 straight flush
     * 2 four of a kind
     * 3 full house
     * 4 flush
     * 5 straight
     * 6 three of a kind
     * 7 two pair
     * 8 one pair
     * 9 high card
     */

    public static RankedHand getRankedHand(Hand hand) {
        return getRankedHand(hand.getCardsList());
    }

    public static RankedHand getRankedHand(List<Card> cards) {
        return getRankedHand(cards.toArray(Card[]::new));
    }

    /**
     * @param cards
     * @return
     */
    public static RankedHand getRankedHand(Card... cards) {
        // SORT ACCORDING TO REVERSE DIRECTION IE HIGHEST FIRST
        Card[] sortedCards = Arrays.stream(cards).sorted((c1, c2) -> Integer.compare(c2.getRankValue(), c1.getRankValue())).toArray(Card[]::new);
        Map<Suit, List<Card>> suitMap = new EnumMap<>(Suit.class);
        Map<Rank, List<Card>> rankMap = new EnumMap<>(Rank.class);
        for (Card card : sortedCards) {
            suitMap.compute(card.getSuit(), (suit, existingList) -> {
                if (existingList == null) {
                    return new ArrayList<>(Collections.singleton(card));
                } else {
                    existingList.add(card);
                    return existingList;
                }
            });
            rankMap.compute(card.getRank(), (rank, existingList) -> {
                if (existingList == null) {
                    return new ArrayList<>(Collections.singleton(card));
                } else {
                    existingList.add(card);
                    return existingList;
                }
            });
        }
        // check for hands
        Map<PokerHandType, RankedHand> hands = new EnumMap<>(PokerHandType.class);
        // flush
        for (Map.Entry<Suit, List<Card>> suitListEntry : suitMap.entrySet()) {
            List<Card> cardsInSuit = suitListEntry.getValue();
            if (suitListEntry.getValue().size() >= 5) {
                Card[] flush = cardsInSuit.stream().sorted(Comparator.comparingInt(Card::getRankValue)).toArray(Card[]::new);
                getStraight(flush).ifPresentOrElse(straight -> hands.put(PokerHandType.STRAIGHT_FLUSH, new RankedHand(PokerHandType.STRAIGHT_FLUSH, straight)), () -> hands.put(PokerHandType.FLUSH, new RankedHand(PokerHandType.FLUSH, flush)));

            }
        }
        // check for straights
        getStraight(sortedCards).ifPresent(straight -> hands.put(PokerHandType.STRAIGHT, new RankedHand(PokerHandType.STRAIGHT, straight)));
        // get the rest
        getMatchedSet(rankMap).ifPresent(rankedHand -> hands.put(rankedHand.type(), rankedHand));
        var res = hands.entrySet().stream().findFirst();
        if (res.isPresent()) {
            return res.get().getValue();
        } else {
            throw new IllegalStateException("Should always return a hand: " + Arrays.toString(cards));
        }
    }

    public static Optional<RankedHand> getMatchedSet(Map<Rank, List<Card>> rankMap) {
        List<Card> cardList = new ArrayList<>(5);
        List<Integer> dupes = new ArrayList<>();
        for (Map.Entry<Rank, List<Card>> entry : rankMap.entrySet()) {
            List<Card> set = entry.getValue();
            int count = set.size();
            cardList.addAll(set);
            dupes.add(count);
            if (cardList.size() >= 5) {
                if (dupes.contains(4)) {
                    return Optional.of(new RankedHand(PokerHandType.FOUR_OF_A_KIND, cardList.subList(0, 5).toArray(Card[]::new)));
                } else if (dupes.contains(3) && dupes.contains(2)) {
                    return Optional.of(new RankedHand(PokerHandType.FULL_HOUSE, cardList.subList(0, 5).toArray(Card[]::new)));
                } else if (dupes.contains(3)) {
                    return Optional.of(new RankedHand(PokerHandType.THREE_OF_A_KIND, cardList.subList(0, 5).toArray(Card[]::new)));
                } else if (dupes.contains(2)) {
                    if (dupes.stream().filter(integer -> integer.equals(2)).count() == 2) {
                        return Optional.of(new RankedHand(PokerHandType.TWO_PAIR, cardList.subList(0, 5).toArray(Card[]::new)));
                    } else {
                        return Optional.of(new RankedHand(PokerHandType.ONE_PAIR, cardList.subList(0, 5).toArray(Card[]::new)));
                    }
                } else {
                    return Optional.of(new RankedHand(PokerHandType.HIGH_CARD, cardList.subList(0, 5).toArray(Card[]::new)));
                }
            }
        }
        return Optional.empty();
    }


    /**
     *
     * @param cards sorted in descending order
     * @return
     */
    public static Optional<Card[]> getStraight(Card... cards) {
        if (cards != null && cards.length > 0) {
            List<Card> hand = new ArrayList<>(List.of(cards));
            // reverse ie high card first
            hand.sort((c1, c2) -> Integer.compare(c2.getRankValue(), c1.getRankValue()));
            List<Card> straight = new ArrayList<>();
            Card previous = null;
            for (Card card : hand) {
                if (previous == null) {
                    straight.add(card);
                } else {
                    int diff = card.getRankValue() - previous.getRankValue();
                    if (diff == -1) {
                        straight.add(card);
                    } else {
                        straight.clear();
                        straight.add(card);
                    }
                }
                previous = card;
                if (straight.size() == 5) return Optional.of(straight.toArray(Card[]::new));
            }
        }
        return Optional.empty();
    }

}
