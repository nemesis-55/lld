package com.tictok.lld.models;

import com.tictok.lld.enums.PieceType;

import lombok.Getter;

@Getter
public class Piece {

    private final PieceType pieceType;

    public Piece(PieceType pieceType) {
        this.pieceType = pieceType;
    }

}
