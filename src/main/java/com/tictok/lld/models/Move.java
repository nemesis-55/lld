package com.tictok.lld.models;

import lombok.Getter;

@Getter
public class Move {

    private final int row;
    private final int col;
    private final Piece piece;
    public Move(final int row, final int col, final Piece piece) {
        this.row = row;
        this.col = col;
        this.piece = piece;
    }

}
