package com.tictok.lld.models;

import java.util.Optional;

import com.tictok.lld.enums.PlayerType;

import lombok.Getter;

@Getter
public abstract class Player {
    private final String playerId;
    private final String name;
    private final PlayerType playerType;
    public Player(final String playerId, final String name, final PlayerType playerType) {
        this.playerId = playerId;
        this.name = name;
        this.playerType = playerType;
    }

    public abstract Optional<Move> getMove(final Board board, final Piece piece);
}
