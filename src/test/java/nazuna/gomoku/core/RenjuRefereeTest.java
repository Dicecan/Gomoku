package nazuna.gomoku.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RenjuRefereeTest {

    private Board board;

    @BeforeEach
    public void setup() {
        board = new Board();
    }

    @Test
    public void testOverlineFoulForBlack() {
        board.makeMove(Board.toIndex(7, 3));
        board.makeMove(Board.toIndex(0, 0));
        board.makeMove(Board.toIndex(7, 4));
        board.makeMove(Board.toIndex(0, 1));
        board.makeMove(Board.toIndex(7, 5));
        board.makeMove(Board.toIndex(0, 2));
        board.makeMove(Board.toIndex(7, 6));
        board.makeMove(Board.toIndex(0, 3));
        board.makeMove(Board.toIndex(7, 7));
        board.makeMove(Board.toIndex(0, 4));

        int overlineMove = Board.toIndex(7, 8);
        int foul = RenjuReferee.checkFoul(board, overlineMove, Board.BLACK, RenjuReferee.RULE_RENJU);
        assertEquals(RenjuReferee.FOUL_OVERLINE, foul);

        int whiteFoul = RenjuReferee.checkFoul(board, overlineMove, Board.WHITE, RenjuReferee.RULE_RENJU);
        assertEquals(RenjuReferee.FOUL_NONE, whiteFoul);

        int freeFoul = RenjuReferee.checkFoul(board, overlineMove, Board.BLACK, RenjuReferee.RULE_FREESTYLE);
        assertEquals(RenjuReferee.FOUL_NONE, freeFoul);
    }

    @Test
    public void testDoubleFourFoulForBlack() {
        board.makeMove(Board.toIndex(7, 5)); // B
        board.makeMove(Board.toIndex(0, 0)); // W
        board.makeMove(Board.toIndex(7, 6)); // B
        board.makeMove(Board.toIndex(0, 1)); // W
        board.makeMove(Board.toIndex(7, 8)); // B
        board.makeMove(Board.toIndex(0, 2)); // W

        board.makeMove(Board.toIndex(5, 7)); // B
        board.makeMove(Board.toIndex(0, 3)); // W
        board.makeMove(Board.toIndex(6, 7)); // B
        board.makeMove(Board.toIndex(0, 4)); // W
        board.makeMove(Board.toIndex(8, 7)); // B
        board.makeMove(Board.toIndex(0, 5)); // W

        int center = Board.toIndex(7, 7);
        int foul = RenjuReferee.checkFoul(board, center, Board.BLACK, RenjuReferee.RULE_RENJU);
        assertEquals(RenjuReferee.FOUL_DOUBLE_FOUR, foul);
    }

    @Test
    public void testDoubleThreeFoulForBlack() {
        board.makeMove(Board.toIndex(7, 6)); // B
        board.makeMove(Board.toIndex(0, 0)); // W
        board.makeMove(Board.toIndex(7, 8)); // B
        board.makeMove(Board.toIndex(0, 1)); // W

        board.makeMove(Board.toIndex(6, 7)); // B
        board.makeMove(Board.toIndex(0, 2)); // W
        board.makeMove(Board.toIndex(8, 7)); // B
        board.makeMove(Board.toIndex(0, 3)); // W

        int center = Board.toIndex(7, 7);
        int foul = RenjuReferee.checkFoul(board, center, Board.BLACK, RenjuReferee.RULE_RENJU);
        assertEquals(RenjuReferee.FOUL_DOUBLE_THREE, foul);
    }

    @Test
    public void testFiveOverridesFoul() {
        board.makeMove(Board.toIndex(7, 4)); // B
        board.makeMove(Board.toIndex(0, 0)); // W
        board.makeMove(Board.toIndex(7, 5)); // B
        board.makeMove(Board.toIndex(0, 1)); // W
        board.makeMove(Board.toIndex(7, 6)); // B
        board.makeMove(Board.toIndex(0, 2)); // W
        board.makeMove(Board.toIndex(7, 8)); // B
        board.makeMove(Board.toIndex(0, 3)); // W

        board.makeMove(Board.toIndex(5, 7)); // B
        board.makeMove(Board.toIndex(0, 4)); // W
        board.makeMove(Board.toIndex(6, 7)); // B
        board.makeMove(Board.toIndex(0, 5)); // W
        board.makeMove(Board.toIndex(8, 7)); // B
        board.makeMove(Board.toIndex(0, 6)); // W

        int center = Board.toIndex(7, 7);
        int foul = RenjuReferee.checkFoul(board, center, Board.BLACK, RenjuReferee.RULE_RENJU);
        assertEquals(RenjuReferee.FOUL_NONE, foul);

        board.makeMove(center);
        assertTrue(RenjuReferee.checkWin(board, center, Board.BLACK, RenjuReferee.RULE_RENJU));
    }
}
