package nazuna.gomoku.core;

public final class RenjuReferee {

    public static final int FOUL_NONE = 0;
    public static final int FOUL_OVERLINE = 1;
    public static final int FOUL_DOUBLE_FOUR = 2;
    public static final int FOUL_DOUBLE_THREE = 3;

    public static final int RULE_RENJU = 0;
    public static final int RULE_FREESTYLE = 1;

    public static int checkFoul(Board board, int move, int color, int ruleMode) {
        if (ruleMode == RULE_FREESTYLE || color != Board.BLACK) {
            return FOUL_NONE;
        }
        return checkBlackFoulInternal(board, move, 0);
    }

    public static boolean isFoul(Board board, int move, int color, int ruleMode) {
        return checkFoul(board, move, color, ruleMode) != FOUL_NONE;
    }

    private static int checkBlackFoulInternal(Board board, int move, int depth) {
        if (depth > 2) {
            return FOUL_NONE;
        }

        boolean hasOverline = false;
        boolean hasExactFive = false;

        for (int d = 0; d < 4; d++) {
            int len = board.countContiguous(move, Board.BLACK, d);
            if (len >= 6) {
                hasOverline = true;
            } else if (len == 5) {
                hasExactFive = true;
            }
        }

        if (hasOverline) {
            return FOUL_OVERLINE;
        }

        if (hasExactFive) {
            return FOUL_NONE;
        }

        int fourCount = countFours(board, move);
        if (fourCount >= 2) {
            return FOUL_DOUBLE_FOUR;
        }

        int openThreeCount = countOpenThrees(board, move, depth);
        if (openThreeCount >= 2) {
            return FOUL_DOUBLE_THREE;
        }

        return FOUL_NONE;
    }

    public static int countFours(Board board, int move) {
        int fourCount = 0;
        int x = Board.toX(move);
        int y = Board.toY(move);

        for (int d = 0; d < 4; d++) {
            int key = board.getLineKey(move, d, Board.BLACK);
            byte pat = PatternTable.getPattern(key);
            if (pat == PatternTable.OPEN_FOUR || pat == PatternTable.RUSH_FOUR) {
                fourCount++;
            } else {
                int dx = Board.DX[d];
                int dy = Board.DY[d];
                int winningPoints = 0;

                for (int offset = -4; offset <= 4; offset++) {
                    if (offset == 0) continue;
                    int nx = x + offset * dx;
                    int ny = y + offset * dy;
                    if (board.isInside(nx, ny) && board.get(nx, ny) == Board.EMPTY) {
                        int testIdx = Board.toIndex(nx, ny);
                        if (checkLineFiveWithTwoStones(board, move, testIdx, d)) {
                            winningPoints++;
                        }
                    }
                }
                if (winningPoints >= 2 && pat == PatternTable.NONE) {
                    fourCount += winningPoints;
                }
            }
        }
        return fourCount;
    }

    private static boolean checkLineFiveWithTwoStones(Board board, int stone1, int stone2, int dir) {
        int x1 = Board.toX(stone1);
        int y1 = Board.toY(stone1);
        int dx = Board.DX[dir];
        int dy = Board.DY[dir];

        int consecutive = 0;
        int maxConsecutive = 0;

        for (int offset = -4; offset <= 4; offset++) {
            int nx = x1 + offset * dx;
            int ny = y1 + offset * dy;
            if (!board.isInside(nx, ny)) {
                consecutive = 0;
                continue;
            }
            int idx = Board.toIndex(nx, ny);
            if (idx == stone1 || idx == stone2 || board.get(idx) == Board.BLACK) {
                consecutive++;
                if (consecutive > maxConsecutive) maxConsecutive = consecutive;
            } else {
                consecutive = 0;
            }
        }
        return maxConsecutive == 5;
    }

    public static int countOpenThrees(Board board, int move, int depth) {
        int openThreeCount = 0;
        int x = Board.toX(move);
        int y = Board.toY(move);

        for (int d = 0; d < 4; d++) {
            int key = board.getLineKey(move, d, Board.BLACK);
            byte pat = PatternTable.getPattern(key);

            if (pat == PatternTable.OPEN_THREE) {
                int dx = Board.DX[d];
                int dy = Board.DY[d];
                boolean hasLegalOpenFourPoint = false;

                for (int offset = -4; offset <= 4; offset++) {
                    if (offset == 0) continue;
                    int nx = x + offset * dx;
                    int ny = y + offset * dy;
                    if (board.isInside(nx, ny) && board.get(nx, ny) == Board.EMPTY) {
                        int testIdx = Board.toIndex(nx, ny);
                        if (checkLineOpenFourWithTwoStones(board, move, testIdx, d)) {
                            if (isPointLegalForBlack(board, move, testIdx, depth + 1)) {
                                hasLegalOpenFourPoint = true;
                                break;
                            }
                        }
                    }
                }

                if (hasLegalOpenFourPoint) {
                    openThreeCount++;
                }
            }
        }
        return openThreeCount;
    }

    private static boolean checkLineOpenFourWithTwoStones(Board board, int move, int testIdx, int dir) {
        int x1 = Board.toX(move);
        int y1 = Board.toY(move);
        int dx = Board.DX[dir];
        int dy = Board.DY[dir];

        int[] line = new int[9];
        for (int i = 0; i < 9; i++) {
            int offset = i - 4;
            int nx = x1 + offset * dx;
            int ny = y1 + offset * dy;
            if (!board.isInside(nx, ny)) {
                line[i] = 2;
            } else {
                int idx = Board.toIndex(nx, ny);
                if (idx == move || idx == testIdx) {
                    line[i] = 1;
                } else {
                    int val = board.get(idx);
                    if (val == Board.EMPTY) line[i] = 0;
                    else if (val == Board.BLACK) line[i] = 1;
                    else line[i] = 2;
                }
            }
        }

        int fiveCount = 0;
        for (int i = 0; i < 9; i++) {
            if (line[i] == 0) {
                line[i] = 1;
                int maxConsec = 0;
                int cur = 0;
                for (int k = 0; k < 9; k++) {
                    if (line[k] == 1) {
                        cur++;
                        if (cur > maxConsec) maxConsec = cur;
                    } else {
                        cur = 0;
                    }
                }
                if (maxConsec == 5) fiveCount++;
                line[i] = 0;
            }
        }
        return fiveCount >= 2;
    }

    private static boolean isPointLegalForBlack(Board board, int firstMove, int secondMove, int nextDepth) {
        board.makeMove(firstMove);
        int foul = checkBlackFoulInternal(board, secondMove, nextDepth);
        board.undoMove();
        return foul == FOUL_NONE;
    }

    public static boolean checkWin(Board board, int lastMove, int color, int ruleMode) {
        if (ruleMode == RULE_FREESTYLE) {
            return board.checkFiveOrOverline(lastMove, color);
        }

        if (color == Board.WHITE) {
            return board.checkFiveOrOverline(lastMove, Board.WHITE);
        } else {
            return board.checkFive(lastMove, Board.BLACK);
        }
    }
}
