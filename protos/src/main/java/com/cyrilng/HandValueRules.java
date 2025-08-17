package com.cyrilng;

import com.cyrilng.cards.Card;
import com.cyrilng.cards.Hand;

import java.util.List;

public interface HandValueRules {
    default int value(Hand hand) {
        return hand == null ? 0 : value(hand.getCardsList());
    }

    default int value(List<Card> cardList) {
        return cardList == null ? 0 : value(cardList.toArray(Card[]::new));
    }

    int value(Card... cards);
}
