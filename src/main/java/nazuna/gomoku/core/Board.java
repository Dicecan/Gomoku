package nazuna.gomoku.core;

import java.util.Arrays;

public final class Board {
    public static final int SIZE = 15;
    public static final int CELL_COUNT = SIZE * SIZE;

    public static final int EMPTY = 0;
    public static final int BLACK = 1;
    public static final int WHITE = 2;

    public static final int[] DX = {1, 0, 1, 1};
    public static final int[] DY = {0, 1, 1, -1};

    private final byte[] cells = new byte[CELL_COUNT];
    private final byte[] neighborCount = new byte[CELL_COUNT];
    private final int[] history = new int[CELL_COUNT];
    private int historyCount = 0;

    private int sideToMove = BLACK;
    private long zobristHash = 0L;

    private static final short[][][] LINE_INDICES = new short[CELL_COUNT][4][9];

    static {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int idx = y * SIZE + x;
                for (int d = 0; d < 4; d++) {
                    int dx = DX[d];
                    int dy = DY[d];
                    for (int offset = -4; offset <= 4; offset++) {
                        int nx = x + offset * dx;
                        int ny = y + offset * dy;
                        if (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE) {
                            LINE_INDICES[idx][d][offset + 4] = (short) (ny * SIZE + nx);
                        } else {
                            LINE_INDICES[idx][d][offset + 4] = -1;
                        }
                    }
                }
            }
        }
    }

    public Board() {
        reset();
    }

    public void reset() {
        Arrays.fill(cells, (byte) EMPTY);
        Arrays.fill(neighborCount, (byte) 0);
        historyCount = 0;
        sideToMove = BLACK;
        zobristHash = Zobrist.getTurnKey();
    }

    public void copyFrom(Board other) {
        System.arraycopy(other.cells, 0, this.cells, 0, CELL_COUNT);
        System.arraycopy(other.neighborCount, 0, this.neighborCount, 0, CELL_COUNT);
        System.arraycopy(other.history, 0, this.history, 0, other.historyCount);
        this.historyCount = other.historyCount;
        this.sideToMove = other.sideToMove;
        this.zobristHash = other.zobristHash;
    }

    public int get(int x, int y) {
        return cells[y * SIZE + x];
    }

    public int get(int index) {
        return cells[index];
    }

    public int getSideToMove() {
        return sideToMove;
    }

    public long getZobristHash() {
        return zobristHash;
    }

    public int getHistoryCount() {
        return historyCount;
    }

    public int getLastMove() {
        return historyCount > 0 ? history[historyCount - 1] : -1;
    }

    public int getHistoryMove(int index) {
        return history[index];
    }

    public boolean isInside(int x, int y) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE;
    }

    public boolean isEmpty(int index) {
        return cells[index] == EMPTY;
    }

    public boolean hasNeighbor(int index) {
        return neighborCount[index] > 0;
    }

    public boolean makeMove(int index) {
        if (index < 0 || index >= CELL_COUNT || cells[index] != EMPTY) {
            return false;
        }

        int piece = sideToMove;
        cells[index] = (byte) piece;
        history[historyCount++] = index;

        zobristHash ^= Zobrist.getPieceKey(piece, index);
        zobristHash ^= Zobrist.getTurnKey();

        updateNeighbors(index, 1);
        sideToMove = (piece == BLACK) ? WHITE : BLACK;
        return true;
    }

    public void undoMove() {
        if (historyCount == 0) return;

        int index = history[--historyCount];
        int piece = cells[index];
        cells[index] = EMPTY;

        zobristHash ^= Zobrist.getPieceKey(piece, index);
        zobristHash ^= Zobrist.getTurnKey();

        updateNeighbors(index, -1);
        sideToMove = piece;
    }

    private void updateNeighbors(int index, int delta) {
        int cx = index % SIZE;
        int cy = index / SIZE;

        int minX = Math.max(0, cx - 2);
        int maxX = Math.min(SIZE - 1, cx + 2);
        int minY = Math.max(0, cy - 2);
        int maxY = Math.min(SIZE - 1, cy + 2);

        for (int y = minY; y <= maxY; y++) {
            int rowOffset = y * SIZE;
            for (int x = minX; x <= maxX; x++) {
                if (x == cx && y == cy) continue;
                neighborCount[rowOffset + x] += delta;
            }
        }
    }

    public int getLineKey(int cellIndex, int dir, int perspectiveColor) {
        short[] indices = LINE_INDICES[cellIndex][dir];
        int oppColor = (perspectiveColor == BLACK) ? WHITE : BLACK;

        int c0 = getCellPerspective(indices[0], perspectiveColor, oppColor);
        int c1 = getCellPerspective(indices[1], perspectiveColor, oppColor);
        int c2 = getCellPerspective(indices[2], perspectiveColor, oppColor);
        int c3 = getCellPerspective(indices[3], perspectiveColor, oppColor);
        int c4 = 1;
        int c5 = getCellPerspective(indices[5], perspectiveColor, oppColor);
        int c6 = getCellPerspective(indices[6], perspectiveColor, oppColor);
        int c7 = getCellPerspective(indices[7], perspectiveColor, oppColor);
        int c8 = getCellPerspective(indices[8], perspectiveColor, oppColor);

        return PatternTable.packKey(c0, c1, c2, c3, c4, c5, c6, c7, c8);
    }

    private int getCellPerspective(int cellIdx, int meColor, int oppColor) {
        if (cellIdx < 0) return 2;
        int val = cells[cellIdx];
        if (val == EMPTY) return 0;
        if (val == meColor) return 1;
        return 2;
    }

    public int countContiguous(int cellIndex, int color, int dir) {
        int x = toX(cellIndex);
        int y = toY(cellIndex);
        int dx = DX[dir];
        int dy = DY[dir];

        int count = 1;
        int step = 1;
        while (true) {
            int nx = x + step * dx;
            int ny = y + step * dy;
            if (isInside(nx, ny) && get(nx, ny) == color) {
                count++;
                step++;
            } else {
                break;
            }
        }

        step = 1;
        while (true) {
            int nx = x - step * dx;
            int ny = y - step * dy;
            if (isInside(nx, ny) && get(nx, ny) == color) {
                count++;
                step++;
            } else {
                break;
            }
        }
        return count;
    }

    public boolean checkFive(int cellIndex, int color) {
        for (int d = 0; d < 4; d++) {
            if (countContiguous(cellIndex, color, d) == 5) {
                return true;
            }
        }
        return false;
    }

    public boolean checkFiveOrOverline(int cellIndex, int color) {
        for (int d = 0; d < 4; d++) {
            if (countContiguous(cellIndex, color, d) >= 5) {
                return true;
            }
        }
        return false;
    }

    public static int toIndex(int x, int y) {
        return y * SIZE + x;
    }

    public static int toX(int index) {
        return index % SIZE;
    }

    public static int toY(int index) {
        return index / SIZE;
    }
}
