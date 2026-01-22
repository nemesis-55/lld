package com.tictok.lld.stratergy.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import com.tictok.lld.models.Board;
import com.tictok.lld.models.Move;
import com.tictok.lld.models.Piece;
import com.tictok.lld.stratergy.IAIStratergy;

public class RandomStratergy implements IAIStratergy {

    @Override
    public Optional<Move> makeMove(final Board board, Piece piece) {
        final List<int[]> emptySpots = new ArrayList<>();
        for (int row = 0; row < board.getSize(); row++) {
            for (int col = 0; col < board.getSize(); col++) {
                if (board.getPieceAt(row, col).isEmpty()) {
                    emptySpots.add(new int[]{row, col});
                }
            }
        }

        if (emptySpots.isEmpty()) {
            return Optional.empty();
        }

        int[] spot = emptySpots.get(new Random().nextInt(emptySpots.size()));
        Move move = new Move(spot[0], spot[1], piece);
        return Optional.of(move);
    }

}
