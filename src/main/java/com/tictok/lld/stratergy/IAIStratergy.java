package com.tictok.lld.stratergy;

import java.util.Optional;

import com.tictok.lld.models.Board;
import com.tictok.lld.models.Move;
import com.tictok.lld.models.Piece;

public interface IAIStratergy {
    Optional<Move> makeMove(Board board, Piece piece);
}
