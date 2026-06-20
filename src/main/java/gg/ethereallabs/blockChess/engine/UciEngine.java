package gg.ethereallabs.blockChess.engine;

import gg.ethereallabs.blockChess.utils.SyncHelper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class UciEngine {
    private final String executablePath;
    private Process process;
    private OutputStreamWriter writer;
    private BufferedReader reader;
    private final LinkedBlockingQueue<String> lines = new LinkedBlockingQueue<>();

    public UciEngine(String executablePath) {
        this.executablePath = executablePath;
    }

    public void start() throws Exception {
        ProcessBuilder pb = new ProcessBuilder(executablePath);
        pb.redirectErrorStream(true);
        process = pb.start();
        writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8);
        reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

        Thread t = new Thread(() -> {
            try {
                while (true) {
                    String line = reader.readLine();
                    if (line == null) break;
                    lines.put(line);
                }
            } catch (Exception ignored) {}
        });
        t.setDaemon(true);
        t.start();
    }

    public void stop() {
        try { send("quit"); } catch (Exception ignored) {}
        if (process != null) process.destroy();
    }

    public void send(String cmd) {
        if (writer == null) return;
        try {
            writer.write(cmd);
            writer.write("\n");
            writer.flush();
        } catch (Exception ignored) {}
    }

    public void initLevel(Integer enemyIndex) {
        send("uci");
        waitFor("uciok", 5000);
        send("setoption name UCI_LimitStrength value false");

        if (enemyIndex != null) {
            String elo = switch (enemyIndex) {
                case 1 -> "200";
                case 2 -> "500";
                case 3 -> "820";
                case 4 -> "1060";
                case 5 -> "1350";
                case 6 -> "1600";
                case 7 -> "1900";
                case 8 -> "2200";
                case 9 -> "2500";
                case 10 -> "2800";
                case 11 -> "3050";
                default -> null;
            };
            if (elo != null) {
                send("setoption name UCI_LimitStrength value true");
                send("setoption name UCI_Elo value " + elo);
            }
        }

        send("isready");
        waitFor("readyok", 5000);
        send("ucinewgame");
    }

    public void init(Integer skillLevel) {
        send("uci");
        waitFor("uciok", 5000);
        if (skillLevel != null) {
            send("setoption name Skill_Level value " + skillLevel);
        }
        send("isready");
        waitFor("readyok", 5000);
        send("ucinewgame");
    }

    public void positionFen(String fen) {
        send("position fen " + fen);
    }

    public void goBestMoveWTimeBTime(long wtimeMs, long btimeMs, long wincMs, long bincMs, Consumer<String> callback) {
        CompletableFuture.runAsync(() -> {
            send("go wtime " + wtimeMs + " btime " + btimeMs + " winc " + wincMs + " binc " + bincMs);
            String move = waitBestMove(30000);
            SyncHelper.runSync(() -> callback.accept(move));
        });
    }

    public void goBestMoveMovetime(long movetimeMs, Consumer<String> callback) {
        CompletableFuture.runAsync(() -> {
            send("go movetime " + movetimeMs);
            String move = waitBestMove(30000);
            SyncHelper.runSync(() -> callback.accept(move));
        });
    }

    private String waitBestMove(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            String line = lines.poll();
            if (line == null) continue;
            if (line.startsWith("bestmove")) {
                String[] parts = line.split(" ");
                return parts.length > 1 ? parts[1] : null;
            }
        }
        throw new RuntimeException("UCI engine timeout waiting for bestmove");
    }

    private boolean waitFor(String token, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            String line = lines.poll();
            if (line == null) continue;
            if (line.contains(token)) return true;
        }
        return false;
    }
}
