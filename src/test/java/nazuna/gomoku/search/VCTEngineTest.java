package nazuna.gomoku.search;

import nazuna.gomoku.core.Board;
import nazuna.gomoku.core.RenjuReferee;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VCTEngineTest {

    @Test
    public void testVCTSearch() {
        Board board = new Board();
        board.makeMove(Board.toIndex(7, 7)); // B
        board.makeMove(Board.toIndex(0, 0)); // W
        board.makeMove(Board.toIndex(7, 8)); // B
        board.makeMove(Board.toIndex(0, 1)); // W
        board.makeMove(Board.toIndex(7, 9)); // B
        board.makeMove(Board.toIndex(0, 2)); // W

        VCTEngine vct = new VCTEngine();
        boolean found = vct.findVCT(board, Board.BLACK, RenjuReferee.RULE_RENJU, 12);
        assertTrue(found);
        assertTrue(vct.getVctFirstMove() >= 0);
    }
}
