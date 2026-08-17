package nazuna.gomoku.service;

import nazuna.gomoku.core.Board;
import nazuna.gomoku.core.RenjuReferee;
import nazuna.gomoku.search.GomokuAI;
import nazuna.gomoku.search.SearchResult;

import java.text.SimpleDateFormat;
import java.util.*;

public final class GameEngine {

    public enum Status {
        IN_PROGRESS,
        BLACK_WIN,
        WHITE_WIN,
        BLACK_FOUL_LOSS,
        DRAW
    }

    private final Board board = new Board();
    private final GomokuAI ai = new GomokuAI();

    private final List<Integer> redoStack = new ArrayList<>();

    private int humanColor = Board.BLACK;
    private int ruleMode = RenjuReferee.RULE_RENJU;
    private Status status = Status.IN_PROGRESS;
    private SearchResult lastAiResult = new SearchResult();

    public GameEngine() {
        startNewGame(Board.BLACK, RenjuReferee.RULE_RENJU, 3000);
    }

    public synchronized void startNewGame(int humanColor, int ruleMode, long timeLimitMs) {
        this.humanColor = humanColor;
        this.ruleMode = ruleMode;
        this.status = Status.IN_PROGRESS;
        this.redoStack.clear();
        this.board.reset();
        this.lastAiResult = new SearchResult();

        ai.setRuleMode(ruleMode);
        ai.setTimeLimitMs(timeLimitMs);
        ai.clearHistory();

        if (humanColor == Board.WHITE) {
            makeAiMove();
        }
    }

    public synchronized boolean makeHumanMove(int cellIndex) {
        if (status != Status.IN_PROGRESS) {
            return false;
        }
        if (board.getSideToMove() != humanColor) {
            return false;
        }

        int side = humanColor;

        if (side == Board.BLACK && RenjuReferee.isFoul(board, cellIndex, Board.BLACK, ruleMode)) {
            board.makeMove(cellIndex);
            status = Status.BLACK_FOUL_LOSS;
            redoStack.clear();
            return true;
        }

        boolean ok = board.makeMove(cellIndex);
        if (!ok) return false;

        redoStack.clear();
        checkGameStatus(cellIndex, side);
        return true;
    }

    public synchronized SearchResult makeAiMove() {
        if (status != Status.IN_PROGRESS) {
            return lastAiResult;
        }

        int side = board.getSideToMove();
        SearchResult result = ai.findBestMove(board);
        this.lastAiResult = result;

        if (result != null && result.bestMove >= 0 && board.isEmpty(result.bestMove)) {
            int move = result.bestMove;

            if (side == Board.BLACK && RenjuReferee.isFoul(board, move, Board.BLACK, ruleMode)) {
                board.makeMove(move);
                status = Status.BLACK_FOUL_LOSS;
                return result;
            }

            board.makeMove(move);
            redoStack.clear();
            checkGameStatus(move, side);
        } else {
            status = (side == Board.BLACK) ? Status.WHITE_WIN : Status.BLACK_WIN;
        }

        return lastAiResult;
    }

    public synchronized boolean undo(int steps) {
        if (board.getHistoryCount() < steps) return false;

        for (int i = 0; i < steps; i++) {
            if (board.getHistoryCount() > 0) {
                int lastMove = board.getLastMove();
                board.undoMove();
                redoStack.add(lastMove);
            }
        }
        status = Status.IN_PROGRESS;
        return true;
    }

    public synchronized boolean redo(int steps) {
        if (redoStack.isEmpty()) return false;

        for (int i = 0; i < steps; i++) {
            if (!redoStack.isEmpty()) {
                int move = redoStack.remove(redoStack.size() - 1);
                int side = board.getSideToMove();
                board.makeMove(move);
                checkGameStatus(move, side);
            }
        }
        return true;
    }

    private void checkGameStatus(int lastMove, int lastColor) {
        if (RenjuReferee.checkWin(board, lastMove, lastColor, ruleMode)) {
            status = (lastColor == Board.BLACK) ? Status.BLACK_WIN : Status.WHITE_WIN;
        } else if (board.getHistoryCount() >= Board.CELL_COUNT) {
            status = Status.DRAW;
        }
    }

    public synchronized String exportSGF() {
        StringBuilder sb = new StringBuilder();
        sb.append("(;GM[1]SZ[15]RU[").append(ruleMode == RenjuReferee.RULE_RENJU ? "Renju" : "Freestyle").append("]");
        sb.append("KM[0.0]PW[AI]PB[Player]DT[").append(new SimpleDateFormat("yyyy-MM-dd").format(new Date())).append("]");

        int count = board.getHistoryCount();
        for (int i = 0; i < count; i++) {
            int move = board.getHistoryMove(i);
            int x = Board.toX(move);
            int y = Board.toY(move);
            char col = (char) ('a' + x);
            char row = (char) ('a' + y);
            String tag = (i % 2 == 0) ? ";B[" : ";W[";
            sb.append(tag).append(col).append(row).append("]");
        }
        sb.append(")");
        return sb.toString();
    }

    public synchronized boolean importSGF(String sgf) {
        if (sgf == null || sgf.isEmpty()) return false;

        board.reset();
        redoStack.clear();
        status = Status.IN_PROGRESS;

        int idx = 0;
        while (idx < sgf.length()) {
            int bPos = sgf.indexOf(";B[", idx);
            int wPos = sgf.indexOf(";W[", idx);

            if (bPos == -1 && wPos == -1) break;

            int targetPos;
            int color;
            if (bPos != -1 && (wPos == -1 || bPos < wPos)) {
                targetPos = bPos + 3;
                color = Board.BLACK;
            } else {
                targetPos = wPos + 3;
                color = Board.WHITE;
            }

            if (targetPos + 2 <= sgf.length()) {
                char cx = sgf.charAt(targetPos);
                char cy = sgf.charAt(targetPos + 1);
                int x = cx - 'a';
                int y = cy - 'a';
                if (x >= 0 && x < Board.SIZE && y >= 0 && y < Board.SIZE) {
                    int move = Board.toIndex(x, y);
                    board.makeMove(move);
                    checkGameStatus(move, color);
                }
            }
            idx = targetPos + 2;
        }
        return true;
    }

    public synchronized String toJsonState() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"status\":\"").append(status.name()).append("\",");
        sb.append("\"sideToMove\":").append(board.getSideToMove()).append(",");
        sb.append("\"humanColor\":").append(humanColor).append(",");
        sb.append("\"ruleMode\":").append(ruleMode).append(",");
        sb.append("\"lastMove\":").append(board.getLastMove()).append(",");
        sb.append("\"canUndo\":").append(board.getHistoryCount() >= 2).append(",");
        sb.append("\"canRedo\":").append(!redoStack.isEmpty()).append(",");

        sb.append("\"board\":[");
        for (int i = 0; i < Board.CELL_COUNT; i++) {
            if (i > 0) sb.append(",");
            sb.append(board.get(i));
        }
        sb.append("],");

        sb.append("\"history\":[");
        for (int i = 0; i < board.getHistoryCount(); i++) {
            if (i > 0) sb.append(",");
            sb.append(board.getHistoryMove(i));
        }
        sb.append("],");

        sb.append("\"ai\":{");
        sb.append("\"bestMove\":").append(lastAiResult.bestMove).append(",");
        sb.append("\"score\":").append(lastAiResult.score).append(",");
        sb.append("\"winRate\":").append(String.format(Locale.US, "%.1f", lastAiResult.getWinRate())).append(",");
        sb.append("\"depth\":").append(lastAiResult.depth).append(",");
        sb.append("\"nodes\":").append(lastAiResult.nodeCount).append(",");
        sb.append("\"nps\":").append(lastAiResult.nps).append(",");
        sb.append("\"elapsedMs\":").append(lastAiResult.elapsedMs).append(",");
        sb.append("\"isVCF\":").append(lastAiResult.isVCF).append(",");
        sb.append("\"isBook\":").append(lastAiResult.isBook).append(",");
        sb.append("\"openingName\":\"").append(lastAiResult.openingName != null ? lastAiResult.openingName : "").append("\",");
        sb.append("\"pv\":[");
        if (lastAiResult.pvLine != null) {
            for (int i = 0; i < lastAiResult.pvLine.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(lastAiResult.pvLine.get(i));
            }
        }
        sb.append("]");
        sb.append("}");

        sb.append("}");
        return sb.toString();
    }

    public Board getBoard() {
        return board;
    }

    public Status getStatus() {
        return status;
    }
}
