package com.tictok.lld.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.tictok.lld.enums.GameStatus;
import com.tictok.lld.models.Game;
import com.tictok.lld.models.Move;
import com.tictok.lld.models.Player;

public class GameService {

    private final Map<String, Game> activeGames = new ConcurrentHashMap<>();

    public void registerGame(Game game) {
        activeGames.put(game.getId(), game);
    }

    public void startGame(String gameId) {
        Game game = getGame(gameId);

        synchronized (game) {
            if (game.getGameStatus() != GameStatus.LOBBYING) {
                throw new IllegalStateException("Game cannot be started");
            }
            game.start();
        }
    }

    public void submitMove(String gameId, String playerId, Move move) {
        Game game = getGame(gameId);

        synchronized (game) {

            validateTurn(game, playerId);

            // Idempotency + ordering handled inside Game
            game.applyMove(move);

            Player currentPlayer = game.getCurrentTurn()
                    .orElseThrow(() -> new IllegalStateException("Current turn missing"));

            if (game.isWinnerDecided(move)) {
                game.complete(currentPlayer);
            } else if (game.isDraw()) {
                game.terminateAsDraw();
            } else {
                game.switchTurn();
            }
        }
    }

    private void validateTurn(Game game, String playerId) {
        if (game.getGameStatus() != GameStatus.ONGOING) {
            throw new IllegalStateException("Game is not active");
        }

        Player currentTurn = game.getCurrentTurn()
                .orElseThrow(() -> new IllegalStateException("Current turn not set"));

        if (!currentTurn.getPlayerId().equals(playerId)) {
            throw new IllegalStateException("Not player's turn");
        }
    }

    private Game getGame(String gameId) {
        Game game = activeGames.get(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found");
        }
        return game;
    }
}
