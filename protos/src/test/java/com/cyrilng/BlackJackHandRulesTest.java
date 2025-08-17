package com.cyrilng;

import com.cyrilng.blackjack.BlackJackHandRules;
import com.cyrilng.cards.Card;
import com.cyrilng.cards.Rank;
import com.cyrilng.cards.Suit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlackJackHandRulesTest {
    static BlackJackHandRules rules;

    @BeforeAll
    static void setUp() {
        rules = new BlackJackHandRules();
    }

    @Test
    void validateValues() {
        Map<List<Card>, Integer> expected = Map.ofEntries(
                Map.entry(List.of(Card.newBuilder().setRank(Rank.ACE).setSuit(Suit.CLUBS).build()), 11),
                Map.entry(List.of(Card.newBuilder().setRank(Rank.JACK).setSuit(Suit.CLUBS).build()), 10),
                Map.entry(List.of(Card.newBuilder().setRank(Rank.QUEEN).setSuit(Suit.CLUBS).build()), 10),
                Map.entry(List.of(Card.newBuilder().setRank(Rank.KING).setSuit(Suit.CLUBS).build()), 10),
                Map.entry(List.of(
                        Card.newBuilder().setRank(Rank.ACE).setSuit(Suit.DIAMONDS).build(),
                        Card.newBuilder().setRank(Rank.JACK).setSuit(Suit.CLUBS).build()), 21),
                Map.entry(List.of(
                        Card.newBuilder().setRank(Rank.ACE).setSuit(Suit.DIAMONDS).build(),
                        Card.newBuilder().setRank(Rank.ACE).setSuit(Suit.CLUBS).build()), 12),
                Map.entry(List.of(
                        Card.newBuilder().setRank(Rank.TEN).setSuit(Suit.DIAMONDS).build(),
                        Card.newBuilder().setRank(Rank.JACK).setSuit(Suit.CLUBS).build()), 20),
                Map.entry(List.of(
                        Card.newBuilder().setRank(Rank.EIGHT).setSuit(Suit.DIAMONDS).build(),
                        Card.newBuilder().setRank(Rank.ACE).setSuit(Suit.DIAMONDS).build(),
                        Card.newBuilder().setRank(Rank.ACE).setSuit(Suit.CLUBS).build()), 20)
        );
        for (Map.Entry<List<Card>, Integer> testCase : expected.entrySet()) {
            int expectedVal = testCase.getValue();
            List<Card> cards = testCase.getKey();
            assertEquals(expectedVal, rules.value(cards), "Expected value: " + expectedVal + " for " + cards);
        }
    }
}