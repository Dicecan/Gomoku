package nazuna.gomoku.search;

import nazuna.gomoku.core.Board;
import nazuna.gomoku.core.PatternTable;
import nazuna.gomoku.core.RenjuReferee;

public final class PVSSearch {

    public static final int MAX_SEARCH_DEPTH = 32;

    private final TranspositionTable tt;
    private final MoveList[] moveBuffers = new MoveList[MAX_SEARCH_DEPTH];
    private final VCFEngine vcfEngine = new VCFEngine();
    private final VCTEngine vctEngine = new VCTEngine();

    private final int[][] killerMoves = new int[MAX_SEARCH_DEPTH][2];
    private final int[][] counterMoves = new int[Board.CELL_COUNT][3];
    private final int[][] historyTable = new int[3][Board.CELL_COUNT];

    private final int[][] pvTable = new int[MAX_SEARCH_DEPTH][MAX_SEARCH_DEPTH];
    private final int[] pvLength = new int[MAX_SEARCH_DEPTH];

    private long nodeCount = 0;
    private long startTimeMs = 0;
    private long timeLimitMs = 0;
    private boolean stopRequested = false;
    private int currentRuleMode = RenjuReferee.RULE_RENJU;

    public PVSSearch(TranspositionTable sharedTT) {
        this.tt = (sharedTT != null) ? sharedTT : new TranspositionTable();
        for (int i = 0; i < MAX_SEARCH_DEPTH; i++) {
            moveBuffers[i] = new MoveList();
        }
    }

    public void stopSearch() {
        this.stopRequested = true;
    }

    public SearchResult search(Board board, int searchLimitDepth, long maxTimeMs, int ruleMode) {
        this.nodeCount = 0;
        this.startTimeMs = System.currentTimeMillis();
        this.timeLimitMs = maxTimeMs;
        this.stopRequested = false;
        this.currentRuleMode = ruleMode;

        for (int i = 0; i < MAX_SEARCH_DEPTH; i++) {
            killerMoves[i][0] = -1;
            killerMoves[i][1] = -1;
            pvLength[i] = 0;
        }

        SearchResult result = OpeningBook.findBookMove(board);
        if (result != null) {
            result.elapsedMs = System.currentTimeMillis() - startTimeMs;
            result.nodeCount = 1;
            result.nps = 1000;
            return result;
        }

        int side = board.getSideToMove();
        int opp = (side == Board.BLACK) ? Board.WHITE : Board.BLACK;

        if (vcfEngine.findVCF(board, side, ruleMode, 28)) {
            result = new SearchResult();
            result.bestMove = vcfEngine.getVcfFirstMove();
            result.score = Evaluator.WIN_SCORE - 10;
            result.depth = vcfEngine.getVcfPathLength();
            result.isVCF = true;
            result.nodeCount = nodeCount;
            result.elapsedMs = Math.max(1, System.currentTimeMillis() - startTimeMs);
            result.nps = (nodeCount * 1000L) / result.elapsedMs;
            int[] path = vcfEngine.getVcfPath();
            for (int i = 0; i < vcfEngine.getVcfPathLength(); i++) {
                result.pvLine.add(path[i]);
            }
            return result;
        }

        if (vctEngine.findVCT(board, side, ruleMode, 12)) {
            result = new SearchResult();
            result.bestMove = vctEngine.getVctFirstMove();
            result.score = Evaluator.WIN_SCORE - 50;
            result.depth = vctEngine.getVctPathLength();
            result.isVCF = true;
            result.nodeCount = nodeCount;
            result.elapsedMs = Math.max(1, System.currentTimeMillis() - startTimeMs);
            result.nps = (nodeCount * 1000L) / result.elapsedMs;
            int[] path = vctEngine.getVctPath();
            for (int i = 0; i < vctEngine.getVctPathLength(); i++) {
                result.pvLine.add(path[i]);
            }
            return result;
        }

        int bestMove = -1;
        int bestScore = 0;
        int completedDepth = 0;

        int alpha = -Evaluator.WIN_SCORE;
        int beta = Evaluator.WIN_SCORE;

        for (int depth = 2; depth <= searchLimitDepth; depth++) {
            pvLength[0] = 0;

            if (depth >= 4 && Math.abs(bestScore) < Evaluator.WIN_SCORE - 2000) {
                int delta = 45;
                alpha = Math.max(-Evaluator.WIN_SCORE, bestScore - delta);
                beta = Math.min(Evaluator.WIN_SCORE, bestScore + delta);
            } else {
                alpha = -Evaluator.WIN_SCORE;
                beta = Evaluator.WIN_SCORE;
            }

            int score = pvsRoot(board, depth, alpha, beta);

            if (!stopRequested && (score <= alpha || score >= beta)) {
                score = pvsRoot(board, depth, -Evaluator.WIN_SCORE, Evaluator.WIN_SCORE);
            }

            if (stopRequested) {
                break;
            }

            bestScore = score;
            completedDepth = depth;

            if (pvLength[0] > 0) {
                bestMove = pvTable[0][0];
            }

            if (bestScore >= Evaluator.WIN_SCORE - 1000 || bestScore <= -Evaluator.WIN_SCORE + 1000) {
                break;
            }

            if (timeLimitMs > 0 && (System.currentTimeMillis() - startTimeMs) > (timeLimitMs * 0.75)) {
                break;
            }
        }

        if (bestMove == -1) {
            MoveList rootMoves = moveBuffers[0];
            generateMoves(board, rootMoves, 0, -1, side);
            if (rootMoves.size > 0) {
                bestMove = rootMoves.pickBest(0);
            } else {
                bestMove = Board.toIndex(7, 7);
            }
        }

        result = new SearchResult();
        result.bestMove = bestMove;
        result.score = bestScore;
        result.depth = completedDepth;
        result.nodeCount = nodeCount;
        long elapsed = System.currentTimeMillis() - startTimeMs;
        result.elapsedMs = Math.max(1, elapsed);
        result.nps = (nodeCount * 1000L) / result.elapsedMs;

        for (int i = 0; i < pvLength[0]; i++) {
            result.pvLine.add(pvTable[0][i]);
        }

        return result;
    }

    private int pvsRoot(Board board, int depth, int alpha, int beta) {
        int side = board.getSideToMove();
        long hash = board.getZobristHash();
        long entry = tt.probe(hash);
        int ttMove = -1;

        if (entry != 0L) {
            ttMove = TranspositionTable.extractBestMove(entry);
        }

        MoveList moves = moveBuffers[0];
        generateMoves(board, moves, 0, ttMove, side);

        if (moves.size == 0) {
            return 0;
        }

        int bestMove = -1;
        int bestScore = -Evaluator.WIN_SCORE;

        for (int i = 0; i < moves.size; i++) {
            int move = moves.pickBest(i);

            board.makeMove(move);

            if (RenjuReferee.checkWin(board, move, side, currentRuleMode)) {
                board.undoMove();
                pvTable[0][0] = move;
                pvLength[0] = 1;
                tt.store(hash, depth, TranspositionTable.FLAG_EXACT, Evaluator.WIN_SCORE, move);
                return Evaluator.WIN_SCORE;
            }

            int score;
            if (i == 0) {
                score = -pvs(board, depth - 1, 1, -beta, -alpha);
            } else {
                score = -pvs(board, depth - 1, 1, -alpha - 1, -alpha);
                if (score > alpha && score < beta) {
                    score = -pvs(board, depth - 1, 1, -beta, -score);
                }
            }

            board.undoMove();

            if (stopRequested) {
                return bestScore;
            }

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
                pvTable[0][0] = move;
                if (pvLength[1] > 0) {
                    System.arraycopy(pvTable[1], 0, pvTable[0], 1, pvLength[1]);
                    pvLength[0] = pvLength[1] + 1;
                } else {
                    pvLength[0] = 1;
                }
            }

            if (score > alpha) {
                alpha = score;
                if (alpha >= beta) {
                    break;
                }
            }
        }

        if (bestMove != -1 && !stopRequested) {
            tt.store(hash, depth, TranspositionTable.FLAG_EXACT, bestScore, bestMove);
        }

        return bestScore;
    }

    private int pvs(Board board, int depth, int ply, int alpha, int beta) {
        nodeCount++;

        if ((nodeCount & 1023) == 0) {
            if (timeLimitMs > 0 && (System.currentTimeMillis() - startTimeMs) >= timeLimitMs) {
                stopRequested = true;
                return alpha;
            }
        }

        int side = board.getSideToMove();
        long hash = board.getZobristHash();

        long entry = tt.probe(hash);
        int ttMove = -1;

        if (entry != 0L) {
            int ttDepth = TranspositionTable.extractDepth(entry);
            if (ttDepth >= depth) {
                int ttFlag = TranspositionTable.extractFlag(entry);
                int ttScore = TranspositionTable.extractScore(entry);
                if (ttFlag == TranspositionTable.FLAG_EXACT) {
                    return ttScore;
                } else if (ttFlag == TranspositionTable.FLAG_LOWERBOUND && ttScore >= beta) {
                    return ttScore;
                } else if (ttFlag == TranspositionTable.FLAG_UPPERBOUND && ttScore <= alpha) {
                    return ttScore;
                }
            }
            ttMove = TranspositionTable.extractBestMove(entry);
        }

        if (depth <= 0 || ply >= MAX_SEARCH_DEPTH - 1) {
            return Evaluator.evaluate(board, side, currentRuleMode);
        }

        int lastMove = board.getLastMove();
        int extension = 0;
        if (lastMove >= 0) {
            int oppColor = (side == Board.BLACK) ? Board.WHITE : Board.BLACK;
            for (int d = 0; d < 4; d++) {
                int key = board.getLineKey(lastMove, d, oppColor);
                byte pat = PatternTable.getPattern(key);
                if (pat == PatternTable.RUSH_FOUR || pat == PatternTable.OPEN_THREE) {
                    extension = 1;
                    break;
                }
            }
        }

        MoveList moves = moveBuffers[ply];
        generateMoves(board, moves, ply, ttMove, side);

        if (moves.size == 0) {
            return 0;
        }

        pvLength[ply] = 0;
        int bestMove = -1;
        int initialAlpha = alpha;

        for (int i = 0; i < moves.size; i++) {
            int move = moves.pickBest(i);

            board.makeMove(move);

            if (RenjuReferee.checkWin(board, move, side, currentRuleMode)) {
                board.undoMove();
                pvTable[ply][0] = move;
                pvLength[ply] = 1;
                return Evaluator.WIN_SCORE - ply;
            }

            int score;
            int nextDepth = depth - 1 + extension;

            if (i == 0) {
                score = -pvs(board, nextDepth, ply + 1, -beta, -alpha);
            } else {
                score = -pvs(board, nextDepth, ply + 1, -alpha - 1, -alpha);
                if (score > alpha && score < beta) {
                    score = -pvs(board, nextDepth, ply + 1, -beta, -score);
                }
            }

            board.undoMove();

            if (stopRequested) {
                return alpha;
            }

            if (score > alpha) {
                alpha = score;
                bestMove = move;

                pvTable[ply][0] = move;
                if (pvLength[ply + 1] > 0) {
                    System.arraycopy(pvTable[ply + 1], 0, pvTable[ply], 1, pvLength[ply + 1]);
                    pvLength[ply] = pvLength[ply + 1] + 1;
                } else {
                    pvLength[ply] = 1;
                }

                if (alpha >= beta) {
                    if (killerMoves[ply][0] != move) {
                        killerMoves[ply][1] = killerMoves[ply][0];
                        killerMoves[ply][0] = move;
                    }
                    if (lastMove >= 0) {
                        counterMoves[lastMove][side] = move;
                    }
                    historyTable[side][move] += depth * depth;
                    break;
                }
            }
        }

        int flag = TranspositionTable.FLAG_EXACT;
        if (alpha <= initialAlpha) {
            flag = TranspositionTable.FLAG_UPPERBOUND;
        } else if (alpha >= beta) {
            flag = TranspositionTable.FLAG_LOWERBOUND;
        }

        if (!stopRequested) {
            tt.store(hash, depth, flag, alpha, bestMove);
        }

        return alpha;
    }

    private void generateMoves(Board board, MoveList list, int ply, int ttMove, int side) {
        list.clear();
        int opp = (side == Board.BLACK) ? Board.WHITE : Board.BLACK;
        int lastMove = board.getLastMove();
        int counterMove = (lastMove >= 0) ? counterMoves[lastMove][side] : -1;

        for (int i = 0; i < Board.CELL_COUNT; i++) {
            if (!board.isEmpty(i) || !board.hasNeighbor(i)) {
                continue;
            }

            int baseScore = Evaluator.scoreMove(board, i, side, opp, currentRuleMode);
            if (baseScore <= -1_000_000_000) {
                continue;
            }

            int orderScore = baseScore;

            if (i == ttMove) {
                orderScore += 1_000_000_000;
            } else if (killerMoves[ply][0] == i) {
                orderScore += 50_000_000;
            } else if (killerMoves[ply][1] == i) {
                orderScore += 40_000_000;
            } else if (counterMove == i) {
                orderScore += 35_000_000;
            } else {
                orderScore += historyTable[side][i];
            }

            list.add(i, orderScore);
        }
    }
}
