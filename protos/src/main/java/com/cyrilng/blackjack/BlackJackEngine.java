package com.cyrilng.blackjack;

import com.cyrilng.DeckUtils;
import com.cyrilng.blackjack.handlers.ActionHandler;
import com.cyrilng.game.ActionType;
import com.cyrilng.game.GameAction;
import com.cyrilng.game.GameState;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BlackJackEngine {
    private final DeckUtils deckUtils;
    private final Map<ActionType, ActionHandler> handlerMap;

    public BlackJackEngine(DeckUtils deckUtils, List<ActionHandler> handlers) {
        this.deckUtils = deckUtils;
        this.handlerMap = new EnumMap<>(handlers.stream().collect(
                Collectors.toMap(ActionHandler::handles, actionHandler -> actionHandler)
        ));
    }

    private static boolean isValid(GameState gameState, GameAction action) {
        var actions = gameState.getPlayerActionsList();
        return actions.contains(action);
    }

    public GameState processAction(GameState gameState, GameAction action) {
        // valid action?
        if (!isValid(gameState, action)) {
            return gameState;
        }

        switch (action.getType()) {
            case JOIN -> {
            }
            case QUIT -> {
            }
            case HIT -> {
            }
            case STAND -> {
            }
            case SPLIT -> {
            }
            case DOUBLE_DOWN -> {
            }
            case SURRENDER -> {
            }
            case UNRECOGNIZED -> {
            }
        }
        // do something
        if (handlerMap.containsKey(action.getType())) {
            return handlerMap.get(action.getType()).handle(gameState, action);
        } else {
            return gameState;
        }
    }
}
