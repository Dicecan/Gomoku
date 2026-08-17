package nazuna.gomoku.core;

public final class PatternTable {
    public static final int NONE = 0;
    public static final int SLEEP_TWO = 1;
    public static final int OPEN_TWO = 2;
    public static final int SLEEP_THREE = 3;
    public static final int OPEN_THREE = 4;
    public static final int RUSH_FOUR = 5;
    public static final int OPEN_FOUR = 6;
    public static final int FIVE = 7;
    public static final int OVERLINE = 8;

    public static final int SCORE_FIVE = 100_000_000;
    public static final int SCORE_OPEN_FOUR = 10_000_000;
    public static final int SCORE_RUSH_FOUR = 120_000;
    public static final int SCORE_OPEN_THREE = 100_000;
    public static final int SCORE_SLEEP_THREE = 8_000;
    public static final int SCORE_OPEN_TWO = 5_000;
    public static final int SCORE_SLEEP_TWO = 500;

    private static final int[] POW3 = new int[9];
    public static final int TABLE_SIZE = 19683;

    private static final byte[] PATTERNS = new byte[TABLE_SIZE];
    private static final int[] SCORES = new int[TABLE_SIZE];
    private static final byte[] KEY_OFFSETS = new byte[TABLE_SIZE];

    static {
        POW3[0] = 1;
        for (int i = 1; i < 9; i++) {
            POW3[i] = POW3[i - 1] * 3;
        }
        initPatternTable();
    }

    private static void initPatternTable() {
        int[] line = new int[9];
        for (int key = 0; key < TABLE_SIZE; key++) {
            int temp = key;
            for (int i = 0; i < 9; i++) {
                line[i] = temp % 3;
                temp /= 3;
            }

            if (line[4] != 1) {
                PATTERNS[key] = NONE;
                SCORES[key] = 0;
                KEY_OFFSETS[key] = 0;
                continue;
            }

            byte pattern = evaluateLinePattern(line);
            PATTERNS[key] = pattern;
            SCORES[key] = patternToScore(pattern);
            KEY_OFFSETS[key] = findKeyPointOffset(line, pattern);
        }
    }

    private static byte evaluateLinePattern(int[] line) {
        int left = 4;
        while (left > 0 && line[left - 1] == 1) left--;
        int right = 4;
        while (right < 8 && line[right + 1] == 1) right++;
        int contiguousLen = right - left + 1;

        if (contiguousLen >= 6) {
            return OVERLINE;
        }
        if (contiguousLen == 5) {
            return FIVE;
        }

        int fiveWinningMoves = 0;
        for (int i = 0; i < 9; i++) {
            if (line[i] == 0) {
                line[i] = 1;
                if (hasExactFive(line)) {
                    fiveWinningMoves++;
                }
                line[i] = 0;
            }
        }

        if (fiveWinningMoves >= 2) {
            return OPEN_FOUR;
        }
        if (fiveWinningMoves == 1) {
            return RUSH_FOUR;
        }

        int openFourMoves = 0;
        for (int i = 0; i < 9; i++) {
            if (line[i] == 0) {
                line[i] = 1;
                if (hasOpenFour(line)) {
                    openFourMoves++;
                }
                line[i] = 0;
            }
        }
        if (openFourMoves >= 1) {
            return OPEN_THREE;
        }

        int rushFourMoves = 0;
        for (int i = 0; i < 9; i++) {
            if (line[i] == 0) {
                line[i] = 1;
                if (hasRushFour(line)) {
                    rushFourMoves++;
                }
                line[i] = 0;
            }
        }
        if (rushFourMoves >= 1 && hasSpaceForFive(line)) {
            return SLEEP_THREE;
        }

        int openThreeMoves = 0;
        for (int i = 0; i < 9; i++) {
            if (line[i] == 0) {
                line[i] = 1;
                if (hasOpenThree(line)) {
                    openThreeMoves++;
                }
                line[i] = 0;
            }
        }
        if (openThreeMoves >= 1 && hasSpaceForFive(line)) {
            return OPEN_TWO;
        }

        int sleepThreeMoves = 0;
        for (int i = 0; i < 9; i++) {
            if (line[i] == 0) {
                line[i] = 1;
                if (hasSleepThree(line)) {
                    sleepThreeMoves++;
                }
                line[i] = 0;
            }
        }
        if (sleepThreeMoves >= 1 && hasSpaceForFive(line)) {
            return SLEEP_TWO;
        }

        return NONE;
    }

    private static boolean hasExactFive(int[] line) {
        int maxConsecutive = 0;
        int current = 0;
        for (int i = 0; i < 9; i++) {
            if (line[i] == 1) {
                current++;
                if (current > maxConsecutive) maxConsecutive = current;
            } else {
                current = 0;
            }
        }
        return maxConsecutive == 5;
    }

    private static boolean hasOpenFour(int[] line) {
        for (int i = 0; i <= 3; i++) {
            if (line[i] == 0 && line[i + 1] == 1 && line[i + 2] == 1 &&
                line[i + 3] == 1 && line[i + 4] == 1 && line[i + 5] == 0) {
                boolean leftFree = (i == 0 || line[i - 1] != 1);
                boolean rightFree = (i + 5 == 8 || line[i + 6] != 1);
                if (leftFree && rightFree) return true;
            }
        }
        return false;
    }

    private static boolean hasRushFour(int[] line) {
        int fiveMoves = 0;
        for (int i = 0; i < 9; i++) {
            if (line[i] == 0) {
                line[i] = 1;
                if (hasExactFive(line)) fiveMoves++;
                line[i] = 0;
            }
        }
        return fiveMoves == 1;
    }

    private static boolean hasOpenThree(int[] line) {
        for (int i = 0; i < 9; i++) {
            if (line[i] == 0) {
                line[i] = 1;
                if (hasOpenFour(line)) {
                    line[i] = 0;
                    return true;
                }
                line[i] = 0;
            }
        }
        return false;
    }

    private static boolean hasSleepThree(int[] line) {
        for (int i = 0; i < 9; i++) {
            if (line[i] == 0) {
                line[i] = 1;
                if (hasRushFour(line)) {
                    line[i] = 0;
                    return true;
                }
                line[i] = 0;
            }
        }
        return false;
    }

    private static boolean hasSpaceForFive(int[] line) {
        for (int i = 0; i <= 4; i++) {
            boolean blocked = false;
            for (int k = 0; k < 5; k++) {
                if (line[i + k] == 2) {
                    blocked = true;
                    break;
                }
            }
            if (!blocked) return true;
        }
        return false;
    }

    private static byte findKeyPointOffset(int[] line, byte pattern) {
        if (pattern == RUSH_FOUR || pattern == OPEN_FOUR) {
            for (int i = 0; i < 9; i++) {
                if (line[i] == 0) {
                    line[i] = 1;
                    boolean five = hasExactFive(line);
                    line[i] = 0;
                    if (five) {
                        return (byte) (i - 4);
                    }
                }
            }
        }
        return 0;
    }

    public static int patternToScore(byte pattern) {
        return switch (pattern) {
            case FIVE -> SCORE_FIVE;
            case OPEN_FOUR -> SCORE_OPEN_FOUR;
            case RUSH_FOUR -> SCORE_RUSH_FOUR;
            case OPEN_THREE -> SCORE_OPEN_THREE;
            case SLEEP_THREE -> SCORE_SLEEP_THREE;
            case OPEN_TWO -> SCORE_OPEN_TWO;
            case SLEEP_TWO -> SCORE_SLEEP_TWO;
            default -> 0;
        };
    }

    public static byte getPattern(int key) {
        return PATTERNS[key];
    }

    public static int getScore(int key) {
        return SCORES[key];
    }

    public static int getKeyOffset(int key) {
        return KEY_OFFSETS[key];
    }

    public static int packKey(int c0, int c1, int c2, int c3, int c4, int c5, int c6, int c7, int c8) {
        return c0 + c1 * 3 + c2 * 9 + c3 * 27 + c4 * 81 + c5 * 243 + c6 * 729 + c7 * 2187 + c8 * 6561;
    }
}
