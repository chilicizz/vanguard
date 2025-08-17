package com.cyrilng.blackjack;

import com.cyrilng.HandValueRules;
import com.cyrilng.cards.Card;
import com.cyrilng.cards.Rank;

import java.util.Map;

public class BlackJackHandRules implements HandValueRules {

    private static final Map<Rank, Integer> VALUES = Map.ofEntries(
            Map.entry(Rank.ACE, 1),
            Map.entry(Rank.JACK, 10),
            Map.entry(Rank.QUEEN, 10),
            Map.entry(Rank.KING, 10)
    );

    @Override
    public int value(Card... cards) {
        if (cards == null || cards.length == 0) {
            return 0;
        } else {
            boolean hasAce = false;
            int total = 0;
            for (Card card : cards) {
                total += VALUES.getOrDefault(card.getRank(), card.getRankValue());
                hasAce = hasAce | Rank.ACE.equals(card.getRank());
            }
            return (hasAce && total <= 11) ? total + 10 : total;
        }
    }
}
