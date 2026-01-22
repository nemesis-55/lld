package com.tictok.lld.enums;

import java.util.List;

public enum PlayerStatus {
    ONLINE(List.of(GameType.SINGLE_PLAYER, GameType.MULTIPLAYER)),
    OFFLINE(List.of(GameType.SINGLE_PLAYER)),
    IN_GAME(List.of());

    private final List<GameType> allowedGameTypes;

    PlayerStatus(List<GameType> allowedGameTypes) {
        this.allowedGameTypes = allowedGameTypes;
    }

    public List<GameType> getAllowedGameTypes() {
        return allowedGameTypes;
    }

}
