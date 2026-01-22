package com.tictok.lld.models;

import java.util.Optional;

import com.tictok.lld.enums.PlayerType;
import com.tictok.lld.stratergy.IAIStratergy;
import com.tictok.lld.stratergy.impl.AdvancedStratergy;
import com.tictok.lld.stratergy.impl.RandomStratergy;

public class AI extends Player {

    private final IAIStratergy strategy;
    private final int difficulty;

    public AI(String playerId, String name, int difficulty) {
        super(playerId, name, PlayerType.AI);
        this.difficulty = difficulty;
        this.strategy = difficulty < 5 ? new RandomStratergy() : new AdvancedStratergy();
    }

    @Override
    public Optional<Move> getMove(Board board, Piece piece) {
        return strategy.makeMove(board, piece);
    }

}
