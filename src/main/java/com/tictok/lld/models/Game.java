package com.tictok.lld.models;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.tictok.lld.enums.GameStatus;
import com.tictok.lld.enums.GameType;
import com.tictok.lld.stratergy.IPlayerSelectionStratergy;

import lombok.Getter;

@Getter
public class Game {

    private final String id;
    private final Board board;
    private final int numberOfPlayers;
    private final GameType gameType;

    private final Map<Player, Piece> playerPieceMap = new ConcurrentHashMap<>();
    private final IPlayerSelectionStratergy playerSelectionStrategy;

    private Player currentTurn;
    private GameStatus gameStatus;
    private Player winningPlayer;

    public Game(
            Board board,
            GameType gameType,
            int numberOfPlayers,
            IPlayerSelectionStratergy playerSelectionStrategy) {
        this.id = UUID.randomUUID().toString();
        this.board = board;
        this.gameType = gameType;
        this.numberOfPlayers = numberOfPlayers;
        this.playerSelectionStrategy = playerSelectionStrategy;
        this.gameStatus = GameStatus.LOBBYING;
    }

    public synchronized void start() {
        if (gameStatus != GameStatus.LOBBYING) {
            throw new IllegalStateException("Game already started");
        }

        if (playerPieceMap.size() != numberOfPlayers) {
            throw new IllegalStateException("Not enough players to start");
        }

        this.currentTurn = playerSelectionStrategy
                .selectPlayer(playerPieceMap.keySet(), Optional.empty())
                .orElseThrow(() -> new IllegalStateException("No starting player found"));

        this.gameStatus = GameStatus.ONGOING;
    }

    public synchronized void complete(Player winner) {
        this.winningPlayer = winner;
        this.gameStatus = GameStatus.COMPLETED;
    }

    public synchronized void terminateAsDraw() {
        this.gameStatus = GameStatus.DRAW;
    }

    public synchronized void addPlayer(Player player, Piece piece) {
        if (gameStatus != GameStatus.LOBBYING) {
            throw new IllegalStateException("Cannot add players after game start");
        }
        if (playerPieceMap.size() >= numberOfPlayers) {
            throw new IllegalStateException("Game already has max players");
        }
        playerPieceMap.put(player, piece);
    }

    public synchronized void removePlayer(Player player) {
        playerPieceMap.remove(player);
    }

    public synchronized void applyMove(Move move) {
        if (gameStatus != GameStatus.ONGOING) {
            throw new IllegalStateException("Game not in progress");
        }
        board.applyMove(move);
    }

    public synchronized boolean isWinnerDecided(final Move move) {
        return board.hasWinner(move);
    }

    public synchronized boolean isDraw() {
        return board.isFull();
    }

    public synchronized void switchTurn() {
        for (Player player : playerPieceMap.keySet()) {
            if (!player.equals(currentTurn)) {
                currentTurn = player;
                return;
            }
        }
    }

    public Optional<Player> getCurrentTurn() {
        return Optional.ofNullable(currentTurn);
    }

    public Optional<Player> getWinningPlayer() {
        return Optional.ofNullable(winningPlayer);
    }
}
