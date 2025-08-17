package com.cyrilng.blackjack.handlers;

import com.cyrilng.game.ActionType;
import com.cyrilng.game.GameAction;
import com.cyrilng.game.GameState;

public interface ActionHandler {
    ActionType handles();

    GameState handle(GameState gameState, GameAction action);
}
