package nazuna.gomoku.search;

import nazuna.gomoku.core.Board;
import nazuna.gomoku.core.RenjuReferee;

import java.util.concurrent.*;

public final class GomokuAI {

    private final TranspositionTable sharedTT = new TranspositionTable(22);
    private final int threadCount;
    private final ExecutorService threadPool;

    private int maxDepth = 20;
    private long timeLimitMs = 3000;
    private int ruleMode = RenjuReferee.RULE_RENJU;

    public GomokuAI() {
        this(Math.max(2, Math.min(Runtime.getRuntime().availableProcessors(), 4)));
    }

    public GomokuAI(int threadCount) {
        this.threadCount = threadCount;
        this.threadPool = Executors.newFixedThreadPool(threadCount, r -> {
            Thread t = new Thread(r, "GomokuAI-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    public void setTimeLimitMs(long timeLimitMs) {
        this.timeLimitMs = timeLimitMs;
    }

    public long getTimeLimitMs() {
        return timeLimitMs;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setRuleMode(int ruleMode) {
        this.ruleMode = ruleMode;
    }

    public int getRuleMode() {
        return ruleMode;
    }

    public void clearHistory() {
        sharedTT.clear();
    }

    public SearchResult findBestMove(Board board) {
        sharedTT.newSearch();

        if (threadCount <= 1) {
            PVSSearch searcher = new PVSSearch(sharedTT);
            return searcher.search(board, maxDepth, timeLimitMs, ruleMode);
        }

        SearchResult bookResult = OpeningBook.findBookMove(board);
        if (bookResult != null) {
            return bookResult;
        }

        PVSSearch[] searchers = new PVSSearch[threadCount];
        Future<SearchResult>[] futures = new Future[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadIdx = i;
            searchers[i] = new PVSSearch(sharedTT);
            final Board threadBoard = new Board();
            threadBoard.copyFrom(board);

            futures[i] = threadPool.submit(() -> {
                int depth = (threadIdx == 0) ? maxDepth : Math.max(8, maxDepth - (threadIdx % 2));
                return searchers[threadIdx].search(threadBoard, depth, timeLimitMs, ruleMode);
            });
        }

        SearchResult bestResult = null;
        long totalNodes = 0;

        for (int i = 0; i < threadCount; i++) {
            try {
                SearchResult res = futures[i].get(timeLimitMs + 1000, TimeUnit.MILLISECONDS);
                if (res != null) {
                    totalNodes += res.nodeCount;
                    if (bestResult == null || res.isVCF
                            || (!bestResult.isVCF && (res.depth > bestResult.depth || (res.depth == bestResult.depth && res.score > bestResult.score)))) {
                        bestResult = res;
                    }
                }
            } catch (Exception e) {
                if (searchers[i] != null) {
                    searchers[i].stopSearch();
                }
            }
        }

        if (bestResult != null) {
            bestResult.nodeCount = totalNodes;
            if (bestResult.elapsedMs > 0) {
                bestResult.nps = (totalNodes * 1000L) / bestResult.elapsedMs;
            }
        } else {
            PVSSearch searcher = new PVSSearch(sharedTT);
            bestResult = searcher.search(board, 4, 500, ruleMode);
        }

        return bestResult;
    }

    public void shutdown() {
        threadPool.shutdownNow();
    }
}
