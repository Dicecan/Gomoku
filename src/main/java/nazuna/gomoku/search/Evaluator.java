package nazuna.gomoku.search;

import nazuna.gomoku.core.Board;
import nazuna.gomoku.core.PatternTable;
import nazuna.gomoku.core.RenjuReferee;

public final class Evaluator {

    public static final int WIN_SCORE = 1_000_000_000;
    public static final int LOSS_SCORE = -WIN_SCORE;

    private static final int[] CENTER_BONUS = new int[Board.CELL_COUNT];

    static {
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                int dist = Math.max(Math.abs(x - 7), Math.abs(y - 7));
                CENTER_BONUS[y * Board.SIZE + x] = (7 - dist) * 10;
            }
        }
    }

    public static int evaluate(Board board, int perspectiveColor, int ruleMode) {
        int oppColor = (perspectiveColor == Board.BLACK) ? Board.WHITE : Board.BLACK;

        int myScore = 0;
        int oppScore = 0;

        int myOpenFours = 0, myRushFours = 0, myOpenThrees = 0, mySleepThrees = 0, myOpenTwos = 0;
        int oppOpenFours = 0, oppRushFours = 0, oppOpenThrees = 0, oppSleepThrees = 0, oppOpenTwos = 0;

        int stoneCount = board.getHistoryCount();
        for (int i = 0; i < stoneCount; i++) {
            int idx = board.getHistoryMove(i);
            int piece = board.get(idx);

            for (int d = 0; d < 4; d++) {
                if (piece == perspectiveColor) {
                    int key = board.getLineKey(idx, d, perspectiveColor);
                    byte pat = PatternTable.getPattern(key);
                    switch (pat) {
                        case PatternTable.FIVE -> { return WIN_SCORE; }
                        case PatternTable.OVERLINE -> {
                            if (perspectiveColor == Board.WHITE || ruleMode == RenjuReferee.RULE_FREESTYLE) return WIN_SCORE;
                        }
                        case PatternTable.OPEN_FOUR -> myOpenFours++;
                        case PatternTable.RUSH_FOUR -> myRushFours++;
                        case PatternTable.OPEN_THREE -> myOpenThrees++;
                        case PatternTable.SLEEP_THREE -> mySleepThrees++;
                        case PatternTable.OPEN_TWO -> myOpenTwos++;
                    }
                } else {
                    int key = board.getLineKey(idx, d, oppColor);
                    byte pat = PatternTable.getPattern(key);
                    switch (pat) {
                        case PatternTable.FIVE -> { return LOSS_SCORE; }
                        case PatternTable.OVERLINE -> {
                            if (oppColor == Board.WHITE || ruleMode == RenjuReferee.RULE_FREESTYLE) return LOSS_SCORE;
                        }
                        case PatternTable.OPEN_FOUR -> oppOpenFours++;
                        case PatternTable.RUSH_FOUR -> oppRushFours++;
                        case PatternTable.OPEN_THREE -> oppOpenThrees++;
                        case PatternTable.SLEEP_THREE -> oppSleepThrees++;
                        case PatternTable.OPEN_TWO -> oppOpenTwos++;
                    }
                }
            }
        }

        if (myOpenFours > 0 || myRushFours >= 2 || (myRushFours > 0 && myOpenThrees > 0)) {
            myScore += 50_000_000;
        }
        if (oppOpenFours > 0 || oppRushFours >= 2 || (oppRushFours > 0 && oppOpenThrees > 0)) {
            oppScore += 50_000_000;
        }

        if (myOpenThrees >= 2) {
            myScore += 10_000_000;
        }
        if (oppOpenThrees >= 2) {
            oppScore += 10_000_000;
        }

        myScore += myOpenFours * 8_000_000 + myRushFours * 150_000 + myOpenThrees * 120_000
                + mySleepThrees * 10_000 + myOpenTwos * 3_000;

        oppScore += oppOpenFours * 8_000_000 + oppRushFours * 150_000 + oppOpenThrees * 120_000
                + oppSleepThrees * 10_000 + oppOpenTwos * 3_000;

        return myScore - (int) (oppScore * 1.15);
    }

    public static int scoreMove(Board board, int move, int myColor, int oppColor, int ruleMode) {
        if (myColor == Board.BLACK && RenjuReferee.isFoul(board, move, Board.BLACK, ruleMode)) {
            return -1_000_000_000;
        }

        int attackScore = 0;
        int defenseScore = 0;

        int myFive = 0, myOpenFour = 0, myRushFour = 0, myOpenThree = 0, mySleepThree = 0, myOpenTwo = 0;
        int oppFive = 0, oppOpenFour = 0, oppRushFour = 0, oppOpenThree = 0, oppSleepThree = 0, oppOpenTwo = 0;

        for (int d = 0; d < 4; d++) {
            int attackKey = board.getLineKey(move, d, myColor);
            byte attackPat = PatternTable.getPattern(attackKey);
            switch (attackPat) {
                case PatternTable.FIVE -> myFive++;
                case PatternTable.OVERLINE -> {
                    if (myColor == Board.WHITE || ruleMode == RenjuReferee.RULE_FREESTYLE) myFive++;
                }
                case PatternTable.OPEN_FOUR -> myOpenFour++;
                case PatternTable.RUSH_FOUR -> myRushFour++;
                case PatternTable.OPEN_THREE -> myOpenThree++;
                case PatternTable.SLEEP_THREE -> mySleepThree++;
                case PatternTable.OPEN_TWO -> myOpenTwo++;
            }

            int defenseKey = board.getLineKey(move, d, oppColor);
            byte defensePat = PatternTable.getPattern(defenseKey);
            switch (defensePat) {
                case PatternTable.FIVE -> oppFive++;
                case PatternTable.OVERLINE -> {
                    if (oppColor == Board.WHITE || ruleMode == RenjuReferee.RULE_FREESTYLE) oppFive++;
                }
                case PatternTable.OPEN_FOUR -> oppOpenFour++;
                case PatternTable.RUSH_FOUR -> oppRushFour++;
                case PatternTable.OPEN_THREE -> oppOpenThree++;
                case PatternTable.SLEEP_THREE -> oppSleepThree++;
                case PatternTable.OPEN_TWO -> oppOpenTwo++;
            }
        }

        if (myFive > 0) return 500_000_000;
        if (oppFive > 0) return 200_000_000;
        if (myOpenFour > 0 || myRushFour >= 2) return 100_000_000;
        if (oppOpenFour > 0 || oppRushFour >= 2) return 80_000_000;
        if (myRushFour > 0 && myOpenThree > 0) return 50_000_000;
        if (oppRushFour > 0 && oppOpenThree > 0) return 40_000_000;
        if (myOpenThree >= 2) return 30_000_000;
        if (oppOpenThree >= 2) return 25_000_000;

        attackScore = myRushFour * 250_000 + myOpenThree * 200_000 + mySleepThree * 15_000 + myOpenTwo * 5_000;
        defenseScore = oppRushFour * 200_000 + oppOpenThree * 180_000 + oppSleepThree * 10_000 + oppOpenTwo * 4_000;

        return attackScore + defenseScore + CENTER_BONUS[move];
    }
}
