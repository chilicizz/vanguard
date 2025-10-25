package com.cyrilng.poker;

import com.cyrilng.cards.Card;
import com.cyrilng.game.PokerHandType;

public record RankedHand(PokerHandType type, Card[] cards) implements Comparable<RankedHand> {

    @Override
    public int compareTo(RankedHand o) {
        return this.type().compareTo(o.type());
    }
}
