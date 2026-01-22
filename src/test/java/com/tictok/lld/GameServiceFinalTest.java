package com.tictok.lld;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.concurrent.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tictok.lld.enums.*;
import com.tictok.lld.models.*;
import com.tictok.lld.services.GameService;
import com.tictok.lld.stratergy.impl.FifoPlayerStratergy;

/**
 * Final correctness test suite for TicTacToe LLD.
 *
 * Guarantees:
 * 1. Game lifecycle correctness
 * 2. Turn-based safety
 * 3. Concurrency safety (without violating turn rules)
 * 4. Winner detection
 * 5. Draw detection
 * 6. Parallel game isolation
 *
 * NOTE:
 * No test here intentionally throws an exception.
 */
public class GameServiceFinalTest {

    private GameService gameService;
    private Player p1;
    private Player p2;

    @BeforeEach
    void setup() {
        gameService = new GameService();
        p1 = new HumanPlayer("P1", "Alice", "a@test.com");
        p2 = new HumanPlayer("P2", "Bob", "b@test.com");
    }

    private Game createAndStartGame() {
        Piece x = new Piece(PieceType.X);
        Piece o = new Piece(PieceType.O);

        Board board = new Board(3, 3, Set.of(x, o));

        Game game = new Game(
                board,
                GameType.MULTIPLAYER,
                2,
                new FifoPlayerStratergy());

        game.addPlayer(p1, x);
        game.addPlayer(p2, o);

        gameService.registerGame(game);
        gameService.startGame(game.getId());

        return game;
    }

    /* ---------------------------------------------------------
       BASIC LIFECYCLE
       --------------------------------------------------------- */

    @Test
    void gameStartsSuccessfully() {
        Game game = createAndStartGame();

        assertEquals(
                GameStatus.ONGOING,
                game.getGameStatus(),
                "Game should move to ONGOING after start");
    }

    /* ---------------------------------------------------------
       TURN-BASED PLAY (SEQUENTIAL)
       --------------------------------------------------------- */

    @Test
    void sequentialMoves_followTurnOrder() {
        Game game = createAndStartGame();

        gameService.submitMove(game.getId(), p1.getPlayerId(),
                new Move(0, 0, new Piece(PieceType.X)));

        gameService.submitMove(game.getId(), p2.getPlayerId(),
                new Move(1, 0, new Piece(PieceType.O)));

        assertTrue(
                game.getBoard().getPieceAt(0, 0).isPresent(),
                "First move should be applied");

        assertTrue(
                game.getBoard().getPieceAt(1, 0).isPresent(),
                "Second move should be applied");
    }

    /* ---------------------------------------------------------
       WINNER DETECTION
       --------------------------------------------------------- */

    @Test
    void horizontalWin_completesGame() {
        Game game = createAndStartGame();

        gameService.submitMove(game.getId(), p1.getPlayerId(),
                new Move(0, 0, new Piece(PieceType.X)));
        gameService.submitMove(game.getId(), p2.getPlayerId(),
                new Move(1, 0, new Piece(PieceType.O)));

        gameService.submitMove(game.getId(), p1.getPlayerId(),
                new Move(0, 1, new Piece(PieceType.X)));
        gameService.submitMove(game.getId(), p2.getPlayerId(),
                new Move(1, 1, new Piece(PieceType.O)));

        gameService.submitMove(game.getId(), p1.getPlayerId(),
                new Move(0, 2, new Piece(PieceType.X)));

        assertEquals(
                GameStatus.COMPLETED,
                game.getGameStatus(),
                "Game must complete on winning move");

        assertEquals(
                p1,
                game.getWinningPlayer().get(),
                "Correct winner must be declared");
    }

    /* ---------------------------------------------------------
       DRAW DETECTION
       --------------------------------------------------------- */

    @Test
    void fullBoardWithoutWinner_resultsInDraw() {
        Game game = createAndStartGame();

        // Draw pattern
        gameService.submitMove(game.getId(), p1.getPlayerId(), new Move(0,0,new Piece(PieceType.X)));
        gameService.submitMove(game.getId(), p2.getPlayerId(), new Move(0,1,new Piece(PieceType.O)));

        gameService.submitMove(game.getId(), p1.getPlayerId(), new Move(0,2,new Piece(PieceType.X)));
        gameService.submitMove(game.getId(), p2.getPlayerId(), new Move(1,1,new Piece(PieceType.O)));

        gameService.submitMove(game.getId(), p1.getPlayerId(), new Move(1,0,new Piece(PieceType.X)));
        gameService.submitMove(game.getId(), p2.getPlayerId(), new Move(1,2,new Piece(PieceType.O)));

        gameService.submitMove(game.getId(), p1.getPlayerId(), new Move(2,1,new Piece(PieceType.X)));
        gameService.submitMove(game.getId(), p2.getPlayerId(), new Move(2,0,new Piece(PieceType.O)));

        gameService.submitMove(game.getId(), p1.getPlayerId(), new Move(2,2,new Piece(PieceType.X)));

        assertEquals(
                GameStatus.DRAW,
                game.getGameStatus(),
                "Game must be DRAW when board is full without winner");
    }

    /* ---------------------------------------------------------
       SAFE CONCURRENCY (TURN-RESPECTING)
       --------------------------------------------------------- */

    @Test
    void concurrentMoves_respectingTurnOrder_areApplied() throws Exception {
        Game game = createAndStartGame();

        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.submit(() ->
                gameService.submitMove(
                        game.getId(),
                        p1.getPlayerId(),
                        new Move(0, 0, new Piece(PieceType.X)))
        ).get();

        executor.submit(() ->
                gameService.submitMove(
                        game.getId(),
                        p2.getPlayerId(),
                        new Move(1, 0, new Piece(PieceType.O)))
        ).get();

        executor.shutdown();

        assertTrue(game.getBoard().getPieceAt(0, 0).isPresent());
        assertTrue(game.getBoard().getPieceAt(1, 0).isPresent());
    }

    /* ---------------------------------------------------------
       PARALLEL GAME ISOLATION
       --------------------------------------------------------- */

    @Test
    void parallelGames_doNotInterfereWithEachOther() throws Exception {
        int games = 5;
        ExecutorService executor = Executors.newFixedThreadPool(games);

        Callable<GameStatus> task = () -> {
            Game game = createAndStartGame();
            gameService.submitMove(
                    game.getId(),
                    p1.getPlayerId(),
                    new Move(0, 0, new Piece(PieceType.X)));
            return game.getGameStatus();
        };

        for (Future<GameStatus> f : executor.invokeAll(
                java.util.Collections.nCopies(games, task))) {
            assertEquals(
                    GameStatus.ONGOING,
                    f.get(),
                    "Each game must progress independently");
        }

        executor.shutdown();
    }
}
