package nazuna.gomoku.search;

import java.util.ArrayList;
import java.util.List;

public final class SearchResult {
    public int bestMove = -1;
    public int score = 0;
    public int depth = 0;
    public long nodeCount = 0;
    public long elapsedMs = 0;
    public long nps = 0;
    public boolean isVCF = false;
    public boolean isBook = false;
    public String openingName = "";
    public List<Integer> pvLine = new ArrayList<>();

    public double getWinRate() {
        if (score >= 400_000_000) return 100.0;
        if (score <= -400_000_000) return 0.0;
        double k = 0.000008;
        double winProb = 1.0 / (1.0 + Math.exp(-k * score));
        return Math.max(0.1, Math.min(99.9, Math.round(winProb * 1000.0) / 10.0));
    }
}
