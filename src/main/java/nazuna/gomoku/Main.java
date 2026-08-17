package nazuna.gomoku;

import nazuna.gomoku.service.GameEngine;
import nazuna.gomoku.service.GomokuHttpServer;

import java.awt.Desktop;
import java.net.URI;
import java.util.Random;

public final class Main {

    private static final String[] EASTER_EGGS = {
        "🦇 「今夜は、まだ終わらないよ。」—— 属于夜晚的对局才刚刚开始。",
        "🌙 「所谓熬夜，是为了追求未曾见过的自由与心动。」",
        "🩸 算杀引擎已潜入夜色，落子无悔，彻夜对决！",
        "🍺 「要不要来罐冰啤酒，下一盘直到天亮的五子棋？」",
        "✨ 蝙蝠掠过棋盘，夜风正盛。祝今晚对弈尽兴！"
    };

    private static final String BANNER = """
    ======================================================================
     _   _    _    ______ _   _ _   _    _        ____  ____   ___  
    | \\ | |  / \\  |__  / | | | | \\ | |  / \\      |  _ \\|  _ \\ / _ \\ 
    |  \\| | / _ \\   / /  | | | |  \\| | / _ \\     | |_) | |_) | | | |
    | |\\  |/ ___ \\ / /_  | |_| | |\\  |/ ___ \\    |  __/|  _ <| |_| |
    |_| \\_/_/   \\_/____|  \\___/|_| \\_/_/   \\_\\   |_|   |_| \\_\\\\___/ 
                                                                     
               NAZUNA GOMOKU / RENJU PRO - GAME ENGINE               
    ======================================================================
      * Name Origin  : 七草 ナズナ (Nanakusa Nazuna) ——《彻夜之歌》
      * Architecture : Java 17+ Core Engine & Modern Canvas WebUI
      * Algorithms   : PVS Search, Aspiration Windows, VCF & VCT Solvers
      * Rules        : International Renju (RIF) / Freestyle Gomoku
      * License      : GNU General Public License v3.0 (GPLv3)
      * Repository   : https://github.com/Dicecan/Gomoku
    ======================================================================
    """;

    public static void main(String[] args) {
        System.out.println(BANNER);

        String easterEgg = EASTER_EGGS[new Random().nextInt(EASTER_EGGS.length)];
        System.out.println("  " + easterEgg + "\n");

        GameEngine gameEngine = new GameEngine();
        GomokuHttpServer server = new GomokuHttpServer(gameEngine);

        try {
            int port = server.start(8080);
            String url = "http://localhost:" + port + "/";

            System.out.println("  [+] Server Status : ONLINE");
            System.out.println("  [+] Access URL    : " + url);
            System.out.println("  [+] Status        : Ready for battle. Opening default browser...\n");
            System.out.println("  Press Ctrl+C to stop server.\n");

            openBrowser(url);
        } catch (Exception e) {
            System.err.println("  [!] Failed to start Gomoku server: " + e.getMessage());
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