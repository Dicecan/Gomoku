package nazuna.gomoku.search;

public final class MoveList {
    public static final int MAX_MOVES = 225;

    public final int[] moves = new int[MAX_MOVES];
    public final int[] scores = new int[MAX_MOVES];
    public int size = 0;

    public void clear() {
        size = 0;
    }

    public void add(int move, int score) {
        moves[size] = move;
        scores[size] = score;
        size++;
    }

    public int pickBest(int fromIndex) {
        int bestIdx = fromIndex;
        int bestScore = scores[fromIndex];

        for (int i = fromIndex + 1; i < size; i++) {
            if (scores[i] > bestScore) {
                bestScore = scores[i];
                bestIdx = i;
            }
        }

        if (bestIdx != fromIndex) {
            int tmpMove = moves[fromIndex];
            moves[fromIndex] = moves[bestIdx];
            moves[bestIdx] = tmpMove;

            int tmpScore = scores[fromIndex];
            scores[fromIndex] = scores[bestIdx];
            scores[bestIdx] = tmpScore;
        }

        return moves[fromIndex];
    }
}
