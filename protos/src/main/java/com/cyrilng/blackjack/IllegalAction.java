package com.cyrilng.blackjack;

import com.cyrilng.game.GameAction;
import com.cyrilng.game.GameState;

public class IllegalAction extends RuntimeException {

    private final GameState gameState;
    private final GameAction action;

    public IllegalAction(String message, GameState gameState, GameAction action) {
        super(message);
        this.gameState = gameState;
        this.action = action;
    }
}
