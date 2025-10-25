package com.cyrilng;

import com.cyrilng.cards.Card;
import com.cyrilng.cards.Rank;
import com.cyrilng.cards.Suit;
import com.cyrilng.game.PokerHandType;
import com.cyrilng.poker.PokerHandRules;
import com.cyrilng.poker.RankedHand;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PokerHandRulesTest {
    static Map<Suit, Map<Rank, Card>> deck;

    @BeforeAll
    public static void setUpAll() {
        deck = new EnumMap<>(Suit.class);
        for (Suit suit : Suit.values()) {
            if (Suit.UNRECOGNIZED != suit) {
                Map<Rank, Card> rankCards = new EnumMap<>(Rank.class);
                for (Rank rank : Rank.values()) {
                    if (Rank.UNRECOGNIZED != rank) {
                        rankCards.put(rank, Card.newBuilder().setSuit(suit).setRank(rank).build());
                    }
                }
                deck.put(suit, rankCards);
            }
        }
    }

    @BeforeEach
    public void setUp() {
    }

    @Test
    public void testGettingAllHandTypes() {
        assertEquals(PokerHandType.STRAIGHT_FLUSH, PokerHandRules.getRankedHand(
                deck.get(Suit.CLUBS).get(Rank.TWO),
                deck.get(Suit.CLUBS).get(Rank.THREE),
                deck.get(Suit.CLUBS).get(Rank.FOUR),
                deck.get(Suit.CLUBS).get(Rank.FIVE),
                deck.get(Suit.CLUBS).get(Rank.SIX)
        ).type());

        assertEquals(PokerHandType.FLUSH, PokerHandRules.getRankedHand(
                deck.get(Suit.SPADES).get(Rank.JACK),
                deck.get(Suit.SPADES).get(Rank.THREE),
                deck.get(Suit.SPADES).get(Rank.NINE),
                deck.get(Suit.SPADES).get(Rank.FOUR),
                deck.get(Suit.SPADES).get(Rank.SIX)
        ).type());

        assertEquals(PokerHandType.STRAIGHT, PokerHandRules.getRankedHand(
                deck.get(Suit.SPADES).get(Rank.TWO),
                deck.get(Suit.DIAMONDS).get(Rank.THREE),
                deck.get(Suit.CLUBS).get(Rank.FIVE),
                deck.get(Suit.HEARTS).get(Rank.FOUR),
                deck.get(Suit.SPADES).get(Rank.SIX)
        ).type());

        assertEquals(PokerHandType.FOUR_OF_A_KIND, PokerHandRules.getRankedHand(
                deck.get(Suit.SPADES).get(Rank.TWO),
                deck.get(Suit.CLUBS).get(Rank.TWO),
                deck.get(Suit.HEARTS).get(Rank.TWO),
                deck.get(Suit.DIAMONDS).get(Rank.TWO),
                deck.get(Suit.CLUBS).get(Rank.SIX)
        ).type());

        assertEquals(PokerHandType.FULL_HOUSE, PokerHandRules.getRankedHand(
                deck.get(Suit.CLUBS).get(Rank.TWO),
                deck.get(Suit.SPADES).get(Rank.TWO),
                deck.get(Suit.CLUBS).get(Rank.FOUR),
                deck.get(Suit.HEARTS).get(Rank.FOUR),
                deck.get(Suit.DIAMONDS).get(Rank.FOUR)
        ).type());

        assertEquals(PokerHandType.TWO_PAIR, PokerHandRules.getRankedHand(
                deck.get(Suit.CLUBS).get(Rank.TWO),
                deck.get(Suit.SPADES).get(Rank.TWO),
                deck.get(Suit.CLUBS).get(Rank.FOUR),
                deck.get(Suit.HEARTS).get(Rank.FOUR),
                deck.get(Suit.CLUBS).get(Rank.SIX)
        ).type());

        assertEquals(PokerHandType.ONE_PAIR, PokerHandRules.getRankedHand(
                deck.get(Suit.CLUBS).get(Rank.TWO),
                deck.get(Suit.SPADES).get(Rank.TWO),
                deck.get(Suit.CLUBS).get(Rank.FOUR),
                deck.get(Suit.HEARTS).get(Rank.FIVE),
                deck.get(Suit.CLUBS).get(Rank.SIX)
        ).type());

        assertEquals(PokerHandType.HIGH_CARD, PokerHandRules.getRankedHand(
                deck.get(Suit.CLUBS).get(Rank.TWO),
                deck.get(Suit.SPADES).get(Rank.FIVE),
                deck.get(Suit.CLUBS).get(Rank.JACK),
                deck.get(Suit.HEARTS).get(Rank.QUEEN),
                deck.get(Suit.DIAMONDS).get(Rank.KING)
        ).type());
    }

    @Test
    public void testHandsAre5Cards() {
        RankedHand hand = PokerHandRules.getRankedHand(
                deck.get(Suit.CLUBS).get(Rank.TWO),
                deck.get(Suit.CLUBS).get(Rank.THREE),
                deck.get(Suit.CLUBS).get(Rank.FOUR),
                deck.get(Suit.CLUBS).get(Rank.FIVE),
                deck.get(Suit.CLUBS).get(Rank.SIX),
                deck.get(Suit.CLUBS).get(Rank.SEVEN),
                deck.get(Suit.DIAMONDS).get(Rank.EIGHT));
        assertEquals(PokerHandType.STRAIGHT_FLUSH, hand.type());
        assertTrue(Arrays.stream(hand.cards()).anyMatch(card -> Rank.SEVEN.equals(card.getRank())));
        assertTrue(Arrays.stream(hand.cards()).anyMatch(card -> Rank.SIX.equals(card.getRank())));
        assertTrue(Arrays.stream(hand.cards()).anyMatch(card -> Rank.FIVE.equals(card.getRank())));
        assertTrue(Arrays.stream(hand.cards()).anyMatch(card -> Rank.FOUR.equals(card.getRank())));
        assertTrue(Arrays.stream(hand.cards()).anyMatch(card -> Rank.THREE.equals(card.getRank())));
        assertFalse(Arrays.stream(hand.cards()).anyMatch(card -> Rank.TWO.equals(card.getRank())));
        assertFalse(Arrays.stream(hand.cards()).anyMatch(card -> Rank.EIGHT.equals(card.getRank())));
        assertEquals(5, hand.cards().length);
    }
}
