package com.cyrilng;

import com.cyrilng.cards.Deck;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DeckTest {
    static DeckUtils deckUtils;

    @BeforeAll
    public static void setUp() {
        deckUtils = new DeckUtils();
    }

    @Test
    public void checkNumberOfCardsInDeck() {
        Deck deck = deckUtils.createNewDeck(0);
        Assertions.assertEquals(52, deck.getCardsList().size());

        Deck deck2 = deckUtils.createNewDeck(2);
        Assertions.assertEquals(54, deck2.getCardsList().size());

        Deck deck3 = deckUtils.createNewDeck(4);
        Assertions.assertEquals(56, deck3.getCardsList().size());

        Deck deck4 = deckUtils.createNewDeck(-4);
        Assertions.assertEquals(52, deck4.getCardsList().size());
    }

    @Test
    public void checkShuffle() {
        Deck deck = deckUtils.createNewDeck(0);
        Deck shuffled = deckUtils.shuffle(deck);

        boolean atLeastOneDiff = false;
        for (int i = 0; i < deck.getCardsCount(); i++) {
            atLeastOneDiff = atLeastOneDiff || !deck.getCards(i).equals(shuffled.getCards(i));
        }
        assertTrue(atLeastOneDiff, "expecting card order to be different after shuffle");
    }
}
