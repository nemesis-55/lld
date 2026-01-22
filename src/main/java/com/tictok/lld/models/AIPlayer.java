package com.tictok.lld.models;

import java.util.Optional;

import com.tictok.lld.enums.PlayerType;
import com.tictok.lld.stratergy.IAIStratergy;
import com.tictok.lld.stratergy.impl.RandomStratergy;

public class AIPlayer extends Player {
    private final IAIStratergy stratergy;

    public AIPlayer(String name) {
        super(name, "ai", PlayerType.AI);
        this.stratergy = new RandomStratergy();
    }

    public IAIStratergy getStratergy() {
        return stratergy;
    }

    @Override
    public Optional<Move> getMove(final Board board, final Piece piece) {
        return stratergy.makeMove(board, piece);
    }

}
