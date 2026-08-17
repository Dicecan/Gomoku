package nazuna.gomoku.search;

public final class TranspositionTable {

    public static final int FLAG_EXACT = 0;
    public static final int FLAG_LOWERBOUND = 1;
    public static final int FLAG_UPPERBOUND = 2;

    private static final int DEFAULT_POWER = 22;
    private final int size;
    private final int mask;

    private final long[] keys;
    private final long[] entries;
    private int currentAge = 0;

    public TranspositionTable() {
        this(DEFAULT_POWER);
    }

    public TranspositionTable(int power) {
        this.size = 1 << power;
        this.mask = this.size - 1;
        this.keys = new long[this.size];
        this.entries = new long[this.size];
    }

    public void clear() {
        for (int i = 0; i < size; i++) {
            keys[i] = 0L;
            entries[i] = 0L;
        }
        currentAge = 0;
    }

    public void newSearch() {
        currentAge = (currentAge + 1) & 0xF;
    }

    public void store(long zobristKey, int depth, int flag, int score, int bestMove) {
        int index = (int) (zobristKey & mask);
        long existingKey = keys[index];
        long existingEntry = entries[index];

        if (existingKey != 0L && existingKey == zobristKey) {
            int oldDepth = (int) ((existingEntry >>> 48) & 0xFF);
            if (depth < oldDepth && ((existingEntry >>> 40) & 0xF) == currentAge) {
                return;
            }
        }

        long packed = ((long) (depth & 0xFF) << 48)
                    | ((long) (flag & 0x0F) << 44)
                    | ((long) (bestMove & 0xFF) << 36)
                    | ((long) (currentAge & 0x0F) << 32)
                    | ((long) score & 0xFFFFFFFFL);

        keys[index] = zobristKey;
        entries[index] = packed;
    }

    public long probe(long zobristKey) {
        int index = (int) (zobristKey & mask);
        if (keys[index] == zobristKey) {
            return entries[index];
        }
        return 0L;
    }

    public static int extractDepth(long entry) {
        return (int) ((entry >>> 48) & 0xFF);
    }

    public static int extractFlag(long entry) {
        return (int) ((entry >>> 44) & 0x0F);
    }

    public static int extractBestMove(long entry) {
        return (int) ((entry >>> 36) & 0xFF);
    }

    public static int extractScore(long entry) {
        return (int) (entry & 0xFFFFFFFFL);
    }
}
