package com.tictok.lld.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import lombok.Getter;

public final class Board {

    @Getter
    private final int size;
    private final int winLength;

    private final Piece[][] grid;
    private int filledCells = 0;

    private final Map<Piece, DirectionCount[][]> counters = new HashMap<>();

    public Board(int size, int winLength, Set<Piece> pieces) {
        if (size <= 0 || winLength <= 0 || winLength > size) {
            throw new IllegalArgumentException("Invalid board or win length");
        }
        this.size = size;
        this.winLength = winLength;
        this.grid = new Piece[size][size];

        for (Piece piece : pieces) {
            DirectionCount[][] pieceCounters = new DirectionCount[size][size];
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    pieceCounters[i][j] = new DirectionCount();
                }
            }
            counters.put(piece, pieceCounters);
        }
    }


    public synchronized void applyMove(Move move) {
        validateMove(move);

        int r = move.getRow();
        int c = move.getCol();
        Piece piece = move.getPiece();

        grid[r][c] = piece;
        filledCells++;

        updateCounters(r, c, piece);
    }

    private void validateMove(Move move) {
        int r = move.getRow();
        int c = move.getCol();

        if (r < 0 || r >= size || c < 0 || c >= size) {
            throw new IllegalArgumentException("Move out of bounds");
        }
        if (grid[r][c] != null) {
            throw new IllegalStateException("Cell already occupied");
        }
    }


    private void updateCounters(int r, int c, Piece piece) {
        DirectionCount[][] dc = counters.get(piece);

        dc[r][c].horizontal =
                1 + getHorizontal(dc, r, c - 1)
                  + getHorizontal(dc, r, c + 1);

        dc[r][c].vertical =
                1 + getVertical(dc, r - 1, c)
                  + getVertical(dc, r + 1, c);

        dc[r][c].diagonal =
                1 + getDiagonal(dc, r - 1, c - 1)
                  + getDiagonal(dc, r + 1, c + 1);

        dc[r][c].antiDiagonal =
                1 + getAntiDiagonal(dc, r - 1, c + 1)
                  + getAntiDiagonal(dc, r + 1, c - 1);
    }

    private int getHorizontal(DirectionCount[][] dc, int r, int c) {
        return isInside(r, c) ? dc[r][c].horizontal : 0;
    }

    private int getVertical(DirectionCount[][] dc, int r, int c) {
        return isInside(r, c) ? dc[r][c].vertical : 0;
    }

    private int getDiagonal(DirectionCount[][] dc, int r, int c) {
        return isInside(r, c) ? dc[r][c].diagonal : 0;
    }

    private int getAntiDiagonal(DirectionCount[][] dc, int r, int c) {
        return isInside(r, c) ? dc[r][c].antiDiagonal : 0;
    }

    private boolean isInside(int r, int c) {
        return r >= 0 && r < size && c >= 0 && c < size;
    }

    public synchronized boolean hasWinner(Move lastMove) {
        DirectionCount d = counters
                .get(lastMove.getPiece())
                [lastMove.getRow()][lastMove.getCol()];

        return d.horizontal >= winLength
            || d.vertical >= winLength
            || d.diagonal >= winLength
            || d.antiDiagonal >= winLength;
    }


    public boolean isFull() {
        return filledCells == size * size;
    }

    public Optional<Piece> getPieceAt(int row, int col) {
        if (!isInside(row, col)) {
            return Optional.empty();
        }
        return Optional.ofNullable(grid[row][col]);
    }
}
