package nazuna.gomoku.search;

import nazuna.gomoku.core.Board;
import nazuna.gomoku.core.PatternTable;
import nazuna.gomoku.core.RenjuReferee;

public final class VCFEngine {

    public static final int MAX_VCF_DEPTH = 40;

    private final int[][] candidateMoves = new int[MAX_VCF_DEPTH][64];
    private final int[][] defensePoints = new int[MAX_VCF_DEPTH][64];
    private final int[] candidateCounts = new int[MAX_VCF_DEPTH];

    private final int[] vcfPath = new int[MAX_VCF_DEPTH];
    private int vcfPathLength = 0;

    public int getVcfPathLength() {
        return vcfPathLength;
    }

    public int getVcfFirstMove() {
        return vcfPathLength > 0 ? vcfPath[0] : -1;
    }

    public int[] getVcfPath() {
        return vcfPath;
    }

    public boolean findVCF(Board board, int attackerColor, int ruleMode, int maxDepth) {
        vcfPathLength = 0;
        int defenderColor = (attackerColor == Board.BLACK) ? Board.WHITE : Board.BLACK;
        int depthLimit = Math.min(maxDepth, MAX_VCF_DEPTH - 2);

        return searchVCF(board, attackerColor, defenderColor, 0, depthLimit, ruleMode);
    }

    private boolean searchVCF(Board board, int attacker, int defender, int depth, int maxDepth, int ruleMode) {
        if (depth >= maxDepth) {
            return false;
        }

        int directWinMove = findDirectWinMove(board, attacker, ruleMode);
        if (directWinMove != -1) {
            vcfPath[depth] = directWinMove;
            vcfPathLength = depth + 1;
            return true;
        }

        int count = generateFourMoves(board, attacker, ruleMode, depth);
        if (count == 0) {
            return false;
        }

        int[] moves = candidateMoves[depth];
        int[] defenses = defensePoints[depth];

        for (int i = 0; i < count; i++) {
            int attackMove = moves[i];
            int defenseMove = defenses[i];

            if (defenseMove == -1) {
                vcfPath[depth] = attackMove;
                vcfPathLength = depth + 1;
                return true;
            }

            if (defenseMove < 0 || defenseMove >= Board.CELL_COUNT || !board.isEmpty(defenseMove)) {
                continue;
            }

            board.makeMove(attackMove);

            if (board.checkFiveOrOverline(attackMove, defender)) {
                board.undoMove();
                continue;
            }

            board.makeMove(defenseMove);

            boolean defenderWon = (defender == Board.WHITE)
                    ? board.checkFiveOrOverline(defenseMove, Board.WHITE)
                    : (board.checkFive(defenseMove, Board.BLACK) && !RenjuReferee.isFoul(board, defenseMove, Board.BLACK, ruleMode));

            boolean found = false;
            if (!defenderWon) {
                found = searchVCF(board, attacker, defender, depth + 2, maxDepth, ruleMode);
            }

            board.undoMove();
            board.undoMove();

            if (found) {
                vcfPath[depth] = attackMove;
                vcfPath[depth + 1] = defenseMove;
                return true;
            }
        }

        return false;
    }

    private int findDirectWinMove(Board board, int attacker, int ruleMode) {
        for (int i = 0; i < Board.CELL_COUNT; i++) {
            if (board.isEmpty(i) && board.hasNeighbor(i)) {
                if (attacker == Board.BLACK) {
                    if (board.checkFive(i, Board.BLACK) && !RenjuReferee.isFoul(board, i, Board.BLACK, ruleMode)) {
                        return i;
                    }
                } else {
                    if (board.checkFiveOrOverline(i, Board.WHITE)) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    private int generateFourMoves(Board board, int attacker, int ruleMode, int depth) {
        int count = 0;
        int[] moves = candidateMoves[depth];
        int[] defenses = defensePoints[depth];

        for (int move = 0; move < Board.CELL_COUNT; move++) {
            if (!board.isEmpty(move) || !board.hasNeighbor(move)) {
                continue;
            }

            if (attacker == Board.BLACK && RenjuReferee.isFoul(board, move, Board.BLACK, ruleMode)) {
                continue;
            }

            int openFourCount = 0;
            int rushFourCount = 0;
            int keyDefensePoint = -1;

            int mx = Board.toX(move);
            int my = Board.toY(move);

            for (int d = 0; d < 4; d++) {
                int key = board.getLineKey(move, d, attacker);
                byte pat = PatternTable.getPattern(key);

                if (pat == PatternTable.OPEN_FOUR) {
                    openFourCount++;
                } else if (pat == PatternTable.RUSH_FOUR) {
                    rushFourCount++;
                    int offset = PatternTable.getKeyOffset(key);
                    int dx = Board.DX[d];
                    int dy = Board.DY[d];
                    int fx = mx + offset * dx;
                    int fy = my + offset * dy;
                    if (board.isInside(fx, fy)) {
                        keyDefensePoint = Board.toIndex(fx, fy);
                    }
                }
            }

            if (openFourCount > 0) {
                moves[count] = move;
                defenses[count] = -1;
                count++;
                if (count >= 64) break;
            } else if (rushFourCount > 0 && keyDefensePoint != -1 && board.isEmpty(keyDefensePoint)) {
                moves[count] = move;
                defenses[count] = keyDefensePoint;
                count++;
                if (count >= 64) break;
            }
        }

        candidateCounts[depth] = count;
        return count;
    }
}
