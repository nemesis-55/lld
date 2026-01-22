package com.tictok.lld.stratergy.impl;

import java.util.Optional;

import com.tictok.lld.models.Board;
import com.tictok.lld.models.Move;
import com.tictok.lld.models.Piece;
import com.tictok.lld.stratergy.IAIStratergy;

public class AdvancedStratergy implements IAIStratergy {

    @Override
    public Optional<Move> makeMove(Board board, Piece piece) {
        // Advanced strategy implementation
        return Optional.empty();
    }

}
