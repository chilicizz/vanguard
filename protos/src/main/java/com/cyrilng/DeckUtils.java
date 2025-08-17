package com.cyrilng;

import com.cyrilng.cards.Card;
import com.cyrilng.cards.Deck;
import com.cyrilng.cards.Rank;
import com.cyrilng.cards.Suit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class DeckUtils {
    private final Card[] cards;

    public DeckUtils() {
        // initialise the cards
        List<Card> temp = new ArrayList<>(56);
        for (Suit suit : Suit.values()) {
            if (!Suit.UNRECOGNIZED.equals(suit)) {
                for (Rank rank : Rank.values()) {
                    if (!Rank.UNRECOGNIZED.equals(rank)) {
                        temp.add(Card.newBuilder().setRank(rank).setSuit(suit).build());
                    }
                }
            }
        }
        cards = temp.toArray(Card[]::new);
    }

    public Deck createNewDeck(int jokers) {
        AtomicInteger i = new AtomicInteger(Math.clamp(jokers, 0, 4));
        return Deck.newBuilder().addAllCards(
                Arrays.stream(cards).filter(card -> !Rank.JOKER.equals(card.getRank()) || i.getAndDecrement() > 0)
                        .collect(Collectors.collectingAndThen(Collectors.toList(),
                                        collected -> {
                                            Collections.shuffle(collected);
                                            return collected;
                                        }
                                )
                        )
        ).build();
    }

    public Deck shuffle(Deck deck) {
        return Deck.newBuilder().addAllCards(
                deck.getCardsList().stream()
                        .collect(Collectors.collectingAndThen(Collectors.toList(),
                                        collected -> {
                                            Collections.shuffle(collected);
                                            return collected;
                                        }
                                )
                        )
        ).build();
    }
}
