package nazuna.gomoku.search;

import nazuna.gomoku.core.Board;
import nazuna.gomoku.core.PatternTable;
import nazuna.gomoku.core.RenjuReferee;

public final class VCTEngine {

    public static final int MAX_VCT_DEPTH = 16;

    private final int[][] attackMoves = new int[MAX_VCT_DEPTH][64];
    private final int[][] defenseMoves = new int[MAX_VCT_DEPTH][64];
    private final int[] attackCounts = new int[MAX_VCT_DEPTH];

    private final VCFEngine vcfEngine = new VCFEngine();

    private final int[] vctPath = new int[MAX_VCT_DEPTH];
    private int vctPathLength = 0;

    private int vctNodes = 0;
    private static final int MAX_VCT_NODES = 40_000;

    public int getVctPathLength() {
        return vctPathLength;
    }

    public int getVctFirstMove() {
        return vctPathLength > 0 ? vctPath[0] : -1;
    }

    public int[] getVctPath() {
        return vctPath;
    }

    public boolean findVCT(Board board, int attackerColor, int ruleMode, int maxDepth) {
        vctPathLength = 0;
        vctNodes = 0;
        int defenderColor = (attackerColor == Board.BLACK) ? Board.WHITE : Board.BLACK;
        int limit = Math.min(maxDepth, MAX_VCT_DEPTH - 2);

        if (vcfEngine.findVCF(board, attackerColor, ruleMode, 24)) {
            vctPathLength = vcfEngine.getVcfPathLength();
            System.arraycopy(vcfEngine.getVcfPath(), 0, vctPath, 0, vctPathLength);
            return true;
        }

        return searchVCT(board, attackerColor, defenderColor, 0, limit, ruleMode);
    }

    private boolean searchVCT(Board board, int attacker, int defender, int depth, int maxDepth, int ruleMode) {
        vctNodes++;
        if (vctNodes > MAX_VCT_NODES || depth >= maxDepth) {
            return false;
        }

        if (vcfEngine.findVCF(board, attacker, ruleMode, 16)) {
            int vcfLen = vcfEngine.getVcfPathLength();
            int[] vcfP = vcfEngine.getVcfPath();
            for (int i = 0; i < vcfLen && (depth + i) < MAX_VCT_DEPTH; i++) {
                vctPath[depth + i] = vcfP[i];
            }
            vctPathLength = Math.min(MAX_VCT_DEPTH, depth + vcfLen);
            return true;
        }

        int count = generateAttackerThreats(board, attacker, ruleMode, depth);
        if (count == 0) {
            return false;
        }

        int[] aMoves = attackMoves[depth];

        for (int i = 0; i < count; i++) {
            int aMove = aMoves[i];

            board.makeMove(aMove);

            if (board.checkFiveOrOverline(aMove, defender)) {
                board.undoMove();
                continue;
            }

            if (vcfEngine.findVCF(board, defender, ruleMode, 8)) {
                board.undoMove();
                continue;
            }

            int defCount = generateDefenderResponses(board, defender, aMove, ruleMode, depth);
            boolean allDefensesFail = (defCount > 0);

            int[] dMoves = defenseMoves[depth];
            for (int d = 0; d < defCount; d++) {
                int dMove = dMoves[d];
                if (!board.isEmpty(dMove)) continue;

                board.makeMove(dMove);

                boolean defenderLost = (attacker == Board.WHITE)
                        ? board.checkFiveOrOverline(dMove, Board.WHITE)
                        : (board.checkFive(dMove, Board.BLACK) && !RenjuReferee.isFoul(board, dMove, Board.BLACK, ruleMode));

                boolean subWin = false;
                if (!defenderLost) {
                    subWin = searchVCT(board, attacker, defender, depth + 2, maxDepth, ruleMode);
                }

                board.undoMove();

                if (!subWin) {
                    allDefensesFail = false;
                    break;
                }
            }

            board.undoMove();

            if (allDefensesFail) {
                vctPath[depth] = aMove;
                if (vctPathLength <= depth) {
                    vctPathLength = depth + 1;
                }
                return true;
            }
        }

        return false;
    }

    private int generateAttackerThreats(Board board, int attacker, int ruleMode, int depth) {
        int count = 0;
        int[] moves = attackMoves[depth];

        for (int idx = 0; idx < Board.CELL_COUNT; idx++) {
            if (!board.isEmpty(idx) || !board.hasNeighbor(idx)) continue;

            if (attacker == Board.BLACK && RenjuReferee.isFoul(board, idx, Board.BLACK, ruleMode)) {
                continue;
            }

            int threatWeight = 0;
            for (int d = 0; d < 4; d++) {
                int key = board.getLineKey(idx, d, attacker);
                byte pat = PatternTable.getPattern(key);
                switch (pat) {
                    case PatternTable.FIVE, PatternTable.OPEN_FOUR -> threatWeight += 1000;
                    case PatternTable.RUSH_FOUR -> threatWeight += 100;
                    case PatternTable.OPEN_THREE -> threatWeight += 50;
                }
            }

            if (threatWeight >= 50) {
                moves[count++] = idx;
                if (count >= 64) break;
            }
        }

        attackCounts[depth] = count;
        return count;
    }

    private int generateDefenderResponses(Board board, int defender, int lastAttackMove, int ruleMode, int depth) {
        int count = 0;
        int[] defs = defenseMoves[depth];
        int oppColor = (defender == Board.BLACK) ? Board.WHITE : Board.BLACK;

        int ax = Board.toX(lastAttackMove);
        int ay = Board.toY(lastAttackMove);

        for (int d = 0; d < 4; d++) {
            int key = board.getLineKey(lastAttackMove, d, oppColor);
            byte pat = PatternTable.getPattern(key);
            if (pat == PatternTable.RUSH_FOUR || pat == PatternTable.OPEN_FOUR) {
                int offset = PatternTable.getKeyOffset(key);
                int dx = Board.DX[d];
                int dy = Board.DY[d];
                int fx = ax + offset * dx;
                int fy = ay + offset * dy;
                if (board.isInside(fx, fy) && board.isEmpty(Board.toIndex(fx, fy))) {
                    defs[count++] = Board.toIndex(fx, fy);
                    return count;
                }
            }
        }

        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                int nx = ax + dx;
                int ny = ay + dy;
                if (board.isInside(nx, ny)) {
                    int idx = Board.toIndex(nx, ny);
                    if (board.isEmpty(idx)) {
                        if (defender == Board.BLACK && RenjuReferee.isFoul(board, idx, Board.BLACK, ruleMode)) {
                            continue;
                        }
                        defs[count++] = idx;
                        if (count >= 16) break;
                    }
                }
            }
            if (count >= 16) break;
        }

        return count;
    }
}
