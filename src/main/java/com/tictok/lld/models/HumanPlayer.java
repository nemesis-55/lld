package com.tictok.lld.models;

import java.util.Optional;

import com.tictok.lld.enums.PlayerType;

import lombok.Getter;

public class HumanPlayer  extends Player {

    private static final int MAX_RANKING = 1000;
    private static final int MIN_RANKING = 0;
    @Getter
    private final int ranking;
    @Getter
    private final String email;

    public HumanPlayer(String playerId, String name, String email) {
        super(playerId, name, PlayerType.HUMAN);
        this.email = email;
        this.ranking = MIN_RANKING;
    }

    @Override
    public Optional<Move> getMove(Board board, Piece piece) {
        try (java.util.Scanner scanner = new java.util.Scanner(System.in)) {
            System.out.printf("Enter %s move (row and column): ", this.getName());
            int row = scanner.nextInt();
            int col = scanner.nextInt();
            return Optional.of(new Move(row, col, piece));
        } catch (Exception e) {
            System.out.println("Failed to read move input. " + e.getMessage());
            return Optional.empty();
        }
    }
}
