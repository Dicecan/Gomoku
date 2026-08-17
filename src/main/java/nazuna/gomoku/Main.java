package nazuna.gomoku;

import nazuna.gomoku.service.GameEngine;
import nazuna.gomoku.service.GomokuHttpServer;

import java.awt.Desktop;
import java.net.URI;

public final class Main {

    public static void main(String[] args) {
        GameEngine gameEngine = new GameEngine();
        GomokuHttpServer server = new GomokuHttpServer(gameEngine);

        try {
            int port = server.start(8080);
            String url = "http://localhost:" + port + "/";
            System.out.println("Gomoku server running at: " + url);

            openBrowser(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return;
            }

            String os = System.getProperty("os.name").toLowerCase();
            Runtime rt = Runtime.getRuntime();
            if (os.contains("win")) {
                rt.exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            } else if (os.contains("mac")) {
                rt.exec(new String[]{"open", url});
            } else if (os.contains("nix") || os.contains("nux")) {
                rt.exec(new String[]{"xdg-open", url});
            }
        } catch (Exception ignored) {}
    }
}