package nazuna.gomoku.search;

import nazuna.gomoku.core.Board;

import java.util.HashMap;
import java.util.Map;

public final class OpeningBook {

    private static final Map<String, Integer> BOOK_MOVES = new HashMap<>();
    private static final Map<String, String> OPENING_NAMES = new HashMap<>();

    private static int transformCoord(int x, int y, int symmetry) {
        int nx = x, ny = y;
        switch (symmetry) {
            case 0 -> { nx = x; ny = y; }
            case 1 -> { nx = 14 - y; ny = x; }
            case 2 -> { nx = 14 - x; ny = 14 - y; }
            case 3 -> { nx = y; ny = 14 - x; }
            case 4 -> { nx = 14 - x; ny = y; }
            case 5 -> { nx = x; ny = 14 - y; }
            case 6 -> { nx = y; ny = x; }
            case 7 -> { nx = 14 - y; ny = 14 - x; }
        }
        return ny * 15 + nx;
    }

    private static int inverseTransformCoord(int x, int y, int symmetry) {
        for (int origY = 0; origY < 15; origY++) {
            for (int origX = 0; origX < 15; origX++) {
                if (transformCoord(origX, origY, symmetry) == y * 15 + x) {
                    return origY * 15 + origX;
                }
            }
        }
        return y * 15 + x;
    }

    static {
        initStandardOpenings();
    }

    private static void registerPattern(String sequence, int nextMove, String name) {
        BOOK_MOVES.put(sequence, nextMove);
        if (name != null && !name.isEmpty()) {
            OPENING_NAMES.put(sequence, name);
        }
    }

    private static void initStandardOpenings() {
        int h8 = 7 * 15 + 7;
        registerPattern("", h8, "天元开局");

        int h9 = 8 * 15 + 7;
        registerPattern("112", h9, "直指开局");

        // 13 Direct Openings
        registerPattern("112,127", 8 * 15 + 8, "花月局");
        registerPattern("112,127,128", 9 * 15 + 9, "花月定式");
        registerPattern("112,127,128,144", 6 * 15 + 6, "花月必胜变例");

        registerPattern("112,127", 9 * 15 + 7, "浦月局");
        registerPattern("112,127,142", 8 * 15 + 8, "浦月定式");

        registerPattern("112,127", 7 * 15 + 8, "溪月局");
        registerPattern("112,127,113", 8 * 15 + 8, "溪月定式");

        registerPattern("112,127", 7 * 15 + 9, "峡月局");
        registerPattern("112,127", 8 * 15 + 9, "恒星局");
        registerPattern("112,127", 9 * 15 + 9, "水月局");
        registerPattern("112,127", 6 * 15 + 8, "云月局");
        registerPattern("112,127", 8 * 15 + 6, "雨月局");
        registerPattern("112,127", 6 * 15 + 9, "金星局");
        registerPattern("112,127", 9 * 15 + 8, "松月局");
        registerPattern("112,127,143", 6 * 15 + 7, "松月定式");
        registerPattern("112,127", 9 * 15 + 6, "丘月局");
        registerPattern("112,127", 7 * 15 + 6, "新月局");
        registerPattern("112,127", 6 * 15 + 6, "瑞星局");
        registerPattern("112,127,96", 8 * 15 + 8, "瑞星平衡定式");

        // Indirect Openings
        registerPattern("112,128", 9 * 15 + 8, "长星局");
        registerPattern("112,128,143", 7 * 15 + 9, "长星定式");
        registerPattern("112,128", 8 * 15 + 7, "浦月局");
        registerPattern("112,128,113", 8 * 15 + 9, "浦月定式");
        registerPattern("112,128", 7 * 15 + 9, "岚月局");
        registerPattern("112,128", 9 * 15 + 9, "明星局");
        registerPattern("112,128", 9 * 15 + 7, "银月局");
        registerPattern("112,128", 6 * 15 + 8, "斜月局");
    }

    public static SearchResult findBookMove(Board board) {
        int historyCount = board.getHistoryCount();
        if (historyCount > 8) {
            return null;
        }

        if (historyCount == 0) {
            SearchResult res = new SearchResult();
            res.bestMove = 7 * 15 + 7;
            res.isBook = true;
            res.openingName = "天元开局";
            return res;
        }

        for (int sym = 0; sym < 8; sym++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < historyCount; i++) {
                int move = board.getHistoryMove(i);
                int x = Board.toX(move);
                int y = Board.toY(move);
                int transMove = transformCoord(x, y, sym);
                if (i > 0) sb.append(',');
                sb.append(transMove);
            }

            String seq = sb.toString();
            Integer nextTransMove = BOOK_MOVES.get(seq);
            if (nextTransMove != null) {
                int ntx = Board.toX(nextTransMove);
                int nty = Board.toY(nextTransMove);
                int actualMove = inverseTransformCoord(ntx, nty, sym);

                if (board.isEmpty(actualMove)) {
                    SearchResult res = new SearchResult();
                    res.bestMove = actualMove;
                    res.isBook = true;
                    res.openingName = OPENING_NAMES.getOrDefault(seq, "连珠定式");
                    res.score = 500;
                    res.depth = 1;
                    return res;
                }
            }
        }

        return null;
    }
}
