package nazuna.gomoku.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public final class GomokuHttpServer {

    private final GameEngine gameEngine;
    private HttpServer server;

    public GomokuHttpServer(GameEngine gameEngine) {
        this.gameEngine = gameEngine;
    }

    public int start(int preferredPort) throws IOException {
        int port = preferredPort;
        while (true) {
            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                break;
            } catch (IOException e) {
                port++;
                if (port > preferredPort + 50) {
                    throw e;
                }
            }
        }

        server.setExecutor(Executors.newCachedThreadPool());

        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/state", this::handleGetState);
        server.createContext("/api/move", this::handleMakeMove);
        server.createContext("/api/ai-move", this::handleAiMove);
        server.createContext("/api/new-game", this::handleNewGame);
        server.createContext("/api/undo", this::handleUndo);
        server.createContext("/api/redo", this::handleRedo);
        server.createContext("/api/sgf/export", this::handleExportSgf);
        server.createContext("/api/sgf/import", this::handleImportSgf);

        server.start();
        return port;
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handleGetState(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
            return;
        }
        String json = gameEngine.toJsonState();
        sendResponse(exchange, 200, json, "application/json; charset=UTF-8");
    }

    private void handleMakeMove(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
            return;
        }

        String body = readRequestBody(exchange);
        int moveIndex = -1;

        if (body.contains("\"index\"")) {
            int idx = body.indexOf("\"index\"");
            int colon = body.indexOf(":", idx);
            int end = body.indexOf("}", colon);
            if (end == -1) end = body.indexOf(",", colon);
            String valStr = body.substring(colon + 1, end).replaceAll("[^0-9]", "").trim();
            if (!valStr.isEmpty()) {
                moveIndex = Integer.parseInt(valStr);
            }
        } else if (body.contains("\"x\"") && body.contains("\"y\"")) {
            int x = extractJsonInt(body, "x");
            int y = extractJsonInt(body, "y");
            if (x >= 0 && y >= 0) {
                moveIndex = y * 15 + x;
            }
        }

        if (moveIndex >= 0 && moveIndex < 225) {
            gameEngine.makeHumanMove(moveIndex);
        }

        String json = gameEngine.toJsonState();
        sendResponse(exchange, 200, json, "application/json; charset=UTF-8");
    }

    private void handleAiMove(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
            return;
        }
        gameEngine.makeAiMove();
        String json = gameEngine.toJsonState();
        sendResponse(exchange, 200, json, "application/json; charset=UTF-8");
    }

    private void handleNewGame(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
            return;
        }

        String body = readRequestBody(exchange);
        int humanColor = extractJsonInt(body, "humanColor", 1);
        int ruleMode = extractJsonInt(body, "ruleMode", 0);
        int timeLimitSec = extractJsonInt(body, "timeLimitSec", 3);

        gameEngine.startNewGame(humanColor, ruleMode, timeLimitSec * 1000L);

        String json = gameEngine.toJsonState();
        sendResponse(exchange, 200, json, "application/json; charset=UTF-8");
    }

    private void handleUndo(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
            return;
        }
        String body = readRequestBody(exchange);
        int steps = extractJsonInt(body, "steps", 2);
        gameEngine.undo(steps);

        String json = gameEngine.toJsonState();
        sendResponse(exchange, 200, json, "application/json; charset=UTF-8");
    }

    private void handleRedo(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
            return;
        }
        String body = readRequestBody(exchange);
        int steps = extractJsonInt(body, "steps", 1);
        gameEngine.redo(steps);

        String json = gameEngine.toJsonState();
        sendResponse(exchange, 200, json, "application/json; charset=UTF-8");
    }

    private void handleExportSgf(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
            return;
        }
        String sgf = gameEngine.exportSGF();
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"gomoku_game.sgf\"");
        sendResponse(exchange, 200, sgf, "application/x-go-sgf; charset=UTF-8");
    }

    private void handleImportSgf(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
            return;
        }
        String sgf = readRequestBody(exchange);
        gameEngine.importSGF(sgf);

        String json = gameEngine.toJsonState();
        sendResponse(exchange, 200, json, "application/json; charset=UTF-8");
    }

    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if ("/".equals(path)) {
                path = "/index.html";
            }

            try (InputStream is = getClass().getResourceAsStream("/web" + path)) {
                if (is == null) {
                    sendResponse(exchange, 404, "404 Not Found", "text/plain");
                    return;
                }

                byte[] bytes = is.readAllBytes();
                String contentType = "text/html; charset=UTF-8";
                if (path.endsWith(".css")) contentType = "text/css; charset=UTF-8";
                if (path.endsWith(".js")) contentType = "application/javascript; charset=UTF-8";
                if (path.endsWith(".png")) contentType = "image/png";
                if (path.endsWith(".svg")) contentType = "image/svg+xml";

                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String responseText, String contentType) throws IOException {
        byte[] bytes = responseText.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static int extractJsonInt(String json, String key) {
        return extractJsonInt(json, key, -1);
    }

    private static int extractJsonInt(String json, String key, int defaultValue) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx == -1) return defaultValue;

        int colon = json.indexOf(":", idx);
        if (colon == -1) return defaultValue;

        int end = json.indexOf(",", colon);
        if (end == -1) end = json.indexOf("}", colon);
        if (end == -1) end = json.length();

        String val = json.substring(colon + 1, end).replaceAll("[^0-9-]", "").trim();
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
