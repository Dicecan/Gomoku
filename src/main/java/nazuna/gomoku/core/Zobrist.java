package nazuna.gomoku.core;

import java.util.Random;

public final class Zobrist {
    public static final int BOARD_SIZE = 15;
    public static final int CELL_COUNT = BOARD_SIZE * BOARD_SIZE;

    private static final long[][] PIECE_KEYS = new long[3][CELL_COUNT];
    private static final long BLACK_TO_MOVE_KEY;

    static {
        Random rand = new Random(0x19980817L);
        for (int p = 1; p <= 2; p++) {
            for (int i = 0; i < CELL_COUNT; i++) {
                PIECE_KEYS[p][i] = rand.nextLong();
            }
        }
        BLACK_TO_MOVE_KEY = rand.nextLong();
    }

    public static long getPieceKey(int piece, int cellIndex) {
        return PIECE_KEYS[piece][cellIndex];
    }

    public static long getTurnKey() {
        return BLACK_TO_MOVE_KEY;
    }
}
