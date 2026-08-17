package nazuna.gomoku.search;

import nazuna.gomoku.core.Board;
import nazuna.gomoku.core.RenjuReferee;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VCFEngineTest {

    @Test
    public void testSimpleVCFSearch() {
        Board board = new Board();
        board.makeMove(Board.toIndex(7, 7)); // B
        board.makeMove(Board.toIndex(0, 0)); // W
        board.makeMove(Board.toIndex(7, 8)); // B
        board.makeMove(Board.toIndex(0, 1)); // W
        board.makeMove(Board.toIndex(7, 9)); // B
        board.makeMove(Board.toIndex(0, 2)); // W

        board.makeMove(Board.toIndex(5, 10)); // B
        board.makeMove(Board.toIndex(0, 3));  // W
        board.makeMove(Board.toIndex(6, 10)); // B
        board.makeMove(Board.toIndex(0, 4));  // W
        board.makeMove(Board.toIndex(8, 10)); // B
        board.makeMove(Board.toIndex(0, 5));  // W

        VCFEngine vcf = new VCFEngine();
        boolean found = vcf.findVCF(board, Board.BLACK, RenjuReferee.RULE_RENJU, 10);
        assertTrue(found);
        assertTrue(vcf.getVcfFirstMove() == Board.toIndex(7, 6) || vcf.getVcfFirstMove() == Board.toIndex(7, 10));
    }

    @Test
    public void testAISearchSpeedAndDepth() {
        Board board = new Board();
        board.makeMove(Board.toIndex(7, 7));
        board.makeMove(Board.toIndex(6, 7));
        board.makeMove(Board.toIndex(8, 6));

        GomokuAI ai = new GomokuAI(2);
        ai.setTimeLimitMs(1000);
        ai.setMaxDepth(14);

        SearchResult result = ai.findBestMove(board);

        assertNotNull(result);
        assertTrue(result.bestMove >= 0 && result.bestMove < 225);
        assertTrue(result.depth >= 6 || result.isVCF || result.isBook);
    }
}
