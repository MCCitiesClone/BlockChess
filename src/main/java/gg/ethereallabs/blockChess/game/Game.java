package gg.ethereallabs.blockChess.game;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveList;
import gg.ethereallabs.blockChess.BlockChess;
import gg.ethereallabs.blockChess.config.Config;
import gg.ethereallabs.blockChess.data.LocalStorage;
import gg.ethereallabs.blockChess.data.PlayerData;
import gg.ethereallabs.blockChess.elo.EloManager;
import gg.ethereallabs.blockChess.engine.EnemyData;
import gg.ethereallabs.blockChess.engine.UciEngine;
import gg.ethereallabs.blockChess.gui.GameGUI;
import gg.ethereallabs.blockChess.utils.SyncHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Game {

    public final Board board = new Board();
    public Player white = null;
    public Player black = null;
    public Player drawRequester = null;
    public MoveList moveList = new MoveList();
    public List<Piece> whiteEaten = new ArrayList<>();
    public List<Piece> blackEaten = new ArrayList<>();
    private boolean ended = false;

    public Move lastMove = null;

    private int taskId = -1;
    public long whiteTimeMs = 5L * 60 * 1000;
    public long blackTimeMs = 5L * 60 * 1000;
    private long lastTickMs = System.currentTimeMillis();

    public GameGUI guiWhite = null;
    public GameGUI guiBlack = null;

    public boolean againstBot = false;
    private Side engineSide = null;
    private UciEngine engine = null;
    private boolean engineThinking = false;
    private Integer engineSkill = null;
    public EnemyData botData = null;

    private ResultType matchResult = null;

    public enum ResultType {
        WHITE_WIN, BLACK_WIN,
        DRAW_STALEMATE, DRAW_REPETITION, DRAW_INSUFFICIENT, DRAW_100MOVES,
        TIMEOUT_WHITE, TIMEOUT_BLACK,
        WHITE_RESIGN, BLACK_RESIGN, DRAW,
        WHITE_FORFEIT, BLACK_FORFEIT
    }

    public boolean isEnded() { return ended; }

    // ─── GAME START ───────────────────────────────────────────────────────────

    public void start(Player pWhite, Player pBlack) {
        setupBoard();
        white = pWhite;
        black = pBlack;
        guiWhite = new GameGUI(this, true);
        guiBlack = new GameGUI(this, false);
        guiWhite.open(pWhite);
        guiBlack.open(pBlack);
        startTimer();
    }

    public void startAgainstBot(Player human, int difficulty, boolean humanIsWhite) {
        if (!Config.botEnabled) {
            BlockChess.instance.sendMessage("<red>Bots are currently disabled!", human);
            human.playSound(human.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        setupBoard();
        againstBot = true;
        engineSkill = difficulty;
        engineSide = humanIsWhite ? Side.BLACK : Side.WHITE;

        if (humanIsWhite) {
            white = human;
            guiWhite = new GameGUI(this, true);
            guiWhite.open(human);
        } else {
            black = human;
            guiBlack = new GameGUI(this, false);
            guiBlack.open(human);
        }

        engine = new UciEngine(Config.enginePath);
        try {
            engine.start();
            engine.initLevel(engineSkill);
        } catch (Exception ex) {
            human.sendMessage(BlockChess.mm.deserialize("<red>Impossible to start chess engine: " + ex.getMessage()));
        }

        startTimer();

        if (board.getSideToMove() == engineSide) triggerEngineMove();
    }

    private void setupBoard() {
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }

    // ─── GAME END ─────────────────────────────────────────────────────────────

    public void finalizeGame(ResultType result) {
        if (ended) return;
        ended = true;
        stop();

        matchResult = result;
        sendPGN();

        if (againstBot) {
            handleBotResult(result);
        } else {
            handlePlayerResult(result);
        }

        GameManager.end(this);
    }

    private void sendPGN() {
        String pgn = moveList.toString();
        Component pgnMsg = Component.text("PGN (Click to copy): ", NamedTextColor.GRAY)
                .append(
                    Component.text(pgn, NamedTextColor.YELLOW)
                        .clickEvent(ClickEvent.copyToClipboard(pgn))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to copy PGN")))
                );
        if (white != null && white.isOnline()) white.sendMessage(pgnMsg);
        if (black != null && black.isOnline()) black.sendMessage(pgnMsg);
    }

    private void handlePlayerResult(ResultType result) {
        if (white == null || black == null) return;
        Player w = white;
        Player b = black;

        PlayerData wData = EloManager.players.computeIfAbsent(w.getUniqueId(), k -> new PlayerData());
        PlayerData bData = EloManager.players.computeIfAbsent(b.getUniqueId(), k -> new PlayerData());

        switch (result) {
            case WHITE_WIN    -> announceResult(w, b, wData, bData, 1.0, 0.0, "🏆", "❌", "by Checkmate");
            case BLACK_WIN    -> announceResult(b, w, bData, wData, 1.0, 0.0, "🏆", "❌", "by Checkmate");
            case TIMEOUT_BLACK -> announceResult(w, b, wData, bData, 1.0, 0.0, "🏆", "❌", "by time");
            case TIMEOUT_WHITE -> announceResult(b, w, bData, wData, 1.0, 0.0, "🏆", "❌", "by time");
            case WHITE_RESIGN  -> announceResult(b, w, bData, wData, 1.0, 0.0, "🏆", "❌", "by resignation");
            case BLACK_RESIGN  -> announceResult(w, b, wData, bData, 1.0, 0.0, "🏆", "❌", "by resignation");
            case WHITE_FORFEIT -> announceResult(b, w, bData, wData, 1.0, 0.0, "🏆", "❌", "by abandonment");
            case BLACK_FORFEIT -> announceResult(w, b, wData, bData, 1.0, 0.0, "🏆", "❌", "by abandonment");
            case DRAW_STALEMATE  -> announceDraw(w, b, wData, bData, "⚖", "by stalemate");
            case DRAW_REPETITION -> announceDraw(w, b, wData, bData, "🔁", "by repetition");
            case DRAW_INSUFFICIENT -> announceDraw(w, b, wData, bData, "🪶", "by insufficient material");
            case DRAW_100MOVES   -> announceDraw(w, b, wData, bData, "⏳", "by 50 move rule");
            case DRAW            -> announceDraw(w, b, wData, bData, "⚖", "by agreed draw");
        }

        LocalStorage.savePlayerData(w);
        LocalStorage.savePlayerData(b);
    }

    private void handleBotResult(ResultType result) {
        Player human = white != null ? white : black;
        if (human == null) return;
        boolean humanIsWhite = human == white;

        switch (result) {
            case WHITE_WIN  -> sendHumanGameMessage(human, humanIsWhite,  "🏆", "❌", "by Checkmate");
            case BLACK_WIN  -> sendHumanGameMessage(human, !humanIsWhite, "🏆", "❌", "by Checkmate");
            case TIMEOUT_WHITE -> sendHumanGameMessage(human, !humanIsWhite, "⏳", "⏳", "by time");
            case TIMEOUT_BLACK -> sendHumanGameMessage(human, humanIsWhite,  "⏳", "⏳", "by time");
            case WHITE_RESIGN, BLACK_RESIGN -> sendHumanGameMessage(human, false, "❌", "❌", "by resignation");
            case WHITE_FORFEIT, BLACK_FORFEIT -> sendHumanGameMessage(human, false, "❌", "❌", "by abandonment");
            case DRAW_STALEMATE  -> sendDrawMessage(human, "⚖", "by stalemate");
            case DRAW_REPETITION -> sendDrawMessage(human, "🔁", "by repetition");
            case DRAW_INSUFFICIENT -> sendDrawMessage(human, "🪶", "by insufficient material");
            case DRAW_100MOVES   -> sendDrawMessage(human, "⏳", "by 50 move rule");
            case DRAW            -> sendDrawMessage(human, "⚖", "by agreed draw");
        }
    }

    private void sendHumanGameMessage(Player human, boolean won, String wEmoji, String lEmoji, String reason) {
        if (!human.isOnline()) return;
        String botName = botData != null ? botData.color() + botData.name() : "Bot";
        if (won) {
            BlockChess.instance.sendMessage("<bold><#f3fa6b>" + wEmoji + "</bold> You won against " + botName + " <gray>(" + reason + ")", human);
        } else {
            BlockChess.instance.sendMessage("<bold><red>" + lEmoji + "</bold> You lost against " + botName + " <gray>(" + reason + ")", human);
        }
    }

    private void sendDrawMessage(Player human, String emoji, String reason) {
        if (!human.isOnline()) return;
        human.sendMessage(BlockChess.mm.deserialize("<yellow>" + emoji + " " + reason + "</yellow>"));
    }

    // ─── ELO / MESSAGE HELPERS ────────────────────────────────────────────────

    private void announceResult(Player winner, Player loser, PlayerData wData, PlayerData lData,
                                 double wScore, double lScore, String wEmoji, String lEmoji, String cause) {
        int wGain = EloManager.updateElo(wData, lData, wScore);
        int bGain = EloManager.updateElo(lData, wData, lScore);
        wData.wins++;
        lData.losses++;
        String wGainStr = wGain >= 0 ? "+" + wGain : String.valueOf(wGain);
        String bGainStr = bGain >= 0 ? "+" + bGain : String.valueOf(bGain);
        String wName = EloManager.getChessistName(winner);
        String lName = EloManager.getChessistName(loser);
        int wElo = wData.rating;
        int lElo = lData.rating;

        if (winner.isOnline()) {
            BlockChess.instance.sendMessage("<bold><#f3fa6b>" + wEmoji + "</bold> " + wName + " <#faff9c>(" + wElo + ") <#97ff82>" + wGainStr + " <gray>- " + lName + " <#faff9c>(" + lElo + ")<red> " + bGainStr, winner);
            BlockChess.instance.sendMessage("<bold>You Won", winner);
            BlockChess.instance.sendMessage("<gray>" + cause, winner);
        }

        if (loser.isOnline()) {
            BlockChess.instance.sendMessage("<bold><red>" + lEmoji + "</bold> " + lName + " <#faff9c>(" + lElo + ") <red>" + bGainStr + " <gray>- " + wName + " <#faff9c>(" + wElo + ")<#97ff82> " + wGainStr, loser);
            BlockChess.instance.sendMessage("<bold>You Lost", loser);
            BlockChess.instance.sendMessage("<gray>" + cause, loser);
        }
    }

    private void announceDraw(Player w, Player b, PlayerData wData, PlayerData bData, String emoji, String cause) {
        int wGain = EloManager.updateElo(wData, bData, 0.5);
        int bGain = EloManager.updateElo(bData, wData, 0.5);
        wData.draws++;
        bData.draws++;

        String wName = EloManager.getChessistName(w);
        String bName = EloManager.getChessistName(b);
        int wElo = wData.rating;
        int bElo = bData.rating;
        String wGainStr = wGain >= 0 ? "+" + wGain : String.valueOf(wGain);
        String bGainStr = bGain >= 0 ? "+" + bGain : String.valueOf(bGain);

        if (w.isOnline()) {
            BlockChess.instance.sendMessage("<#f3fa6b>" + emoji + " " + wName + " <#faff9c>(" + wElo + ") <gray>" + wGainStr + " - " + bName + " <#faff9c>(" + bElo + ") <gray>" + bGainStr, w);
            BlockChess.instance.sendMessage("<gray>" + cause, w);
        }

        if (b.isOnline()) {
            BlockChess.instance.sendMessage("<#f3fa6b>" + emoji + " " + bName + " <#faff9c>(" + bElo + ") <gray>" + bGainStr + " - " + wName + " <#faff9c>(" + wElo + ") <gray>" + wGainStr, b);
            BlockChess.instance.sendMessage("<gray>" + cause, b);
        }
    }

    // ─── TIMER & MOVE HANDLING ────────────────────────────────────────────────

    public void stop() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        taskId = -1;
        if (againstBot && engine != null) {
            try { engine.stop(); } catch (Exception ignored) {}
        }
    }

    private void startTimer() {
        lastTickMs = System.currentTimeMillis();
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(BlockChess.instance, () -> {
            long now = System.currentTimeMillis();
            long delta = now - lastTickMs;
            lastTickMs = now;

            if (board.getSideToMove() == Side.WHITE) whiteTimeMs -= delta;
            else blackTimeMs -= delta;

            if (whiteTimeMs <= 0 || blackTimeMs <= 0) {
                finalizeGame(whiteTimeMs <= 0 ? ResultType.TIMEOUT_WHITE : ResultType.TIMEOUT_BLACK);
            } else {
                if (guiWhite != null) guiWhite.updateClock();
                if (guiBlack != null) guiBlack.updateClock();
                if (againstBot && board.getSideToMove() == engineSide && !engineThinking) triggerEngineMove();
            }
        }, 20L, 20L);
    }

    public void onMoveMade(Move move) {
        lastMove = move;
        if (guiWhite != null) guiWhite.draw(white);
        if (guiBlack != null) guiBlack.draw(black);
        moveList.add(move);

        Sound sound = Sound.BLOCK_NETHER_WOOD_BREAK;
        if (board.isKingAttacked()) {
            sound = Sound.UI_BUTTON_CLICK;
        }

        Sound finalSound = sound;
        List.of(white, black).stream()
                .filter(p -> p != null && p.isOnline())
                .forEach(p -> p.playSound(p.getLocation(), finalSound, 1f, 1f));

        if (board.isStaleMate()) {
            finalizeGame(ResultType.DRAW_STALEMATE);
        } else if (board.isMated()) {
            finalizeGame(board.getSideToMove() == Side.WHITE ? ResultType.BLACK_WIN : ResultType.WHITE_WIN);
        } else if (board.isRepetition()) {
            finalizeGame(ResultType.DRAW_REPETITION);
        } else if (board.isInsufficientMaterial()) {
            finalizeGame(ResultType.DRAW_INSUFFICIENT);
        } else if (board.getHalfMoveCounter() >= 100) {
            finalizeGame(ResultType.DRAW_100MOVES);
        }
    }

    // ─── ENGINE LOGIC ─────────────────────────────────────────────────────────

    private void triggerEngineMove() {
        if (!againstBot || engine == null || engineThinking) return;
        engineThinking = true;

        String fen = board.getFen();
        long wtime = whiteTimeMs;
        long btime = blackTimeMs;

        Player human = engineSide == Side.WHITE ? black : white;
        if (human != null && human.isOnline() && botData != null) {
            BlockChess.instance.sendMessage(botData.color() + botData.name() + " <gray>is thinking...", human);
        }

        engine.positionFen(fen);

        int skill = engineSkill != null ? engineSkill : 5;

        CompletableFuture.runAsync(() -> {
            try {
                long movetime = Math.max(50, Math.min(200, skill * 50L));
                engine.goBestMoveMovetime(movetime, bestMove -> {
                    SyncHelper.runSync(() -> {
                        try {
                            Move m = uciToLegalMove(bestMove);
                            if (m != null) {
                                board.doMove(m);
                                onMoveMade(m);
                            }
                        } catch (Exception ignored) {}
                        engineThinking = false;
                    });
                });
            } catch (Exception e) {
                long alloc = Math.max(100, Math.min(2000, (board.getSideToMove() == Side.WHITE ? wtime : btime) / 20));
                engine.goBestMoveMovetime(alloc, bestMove -> {
                    SyncHelper.runSync(() -> {
                        try {
                            Move m = uciToLegalMove(bestMove);
                            if (m != null) {
                                board.doMove(m);
                                onMoveMade(m);
                            }
                        } catch (Exception ignored) {}
                        engineThinking = false;
                    });
                });
            }
        });
    }

    private Move uciToLegalMove(String uci) {
        if (uci == null || uci.isBlank() || uci.length() < 4) return null;
        Square from = Square.squareAt((uci.charAt(1) - '1') * 8 + (uci.charAt(0) - 'a'));
        Square to   = Square.squareAt((uci.charAt(3) - '1') * 8 + (uci.charAt(2) - 'a'));
        Piece promo = uci.length() >= 5 ? promoPieceFromChar(uci.charAt(4), board.getSideToMove()) : Piece.NONE;

        for (Move m : board.legalMoves()) {
            if (m.getFrom() == from && m.getTo() == to &&
                    (promo == Piece.NONE || m.getPromotion() == promo)) {
                return m;
            }
        }
        return null;
    }

    private Piece promoPieceFromChar(char c, Side side) {
        return switch (Character.toLowerCase(c)) {
            case 'q' -> side == Side.WHITE ? Piece.WHITE_QUEEN  : Piece.BLACK_QUEEN;
            case 'r' -> side == Side.WHITE ? Piece.WHITE_ROOK   : Piece.BLACK_ROOK;
            case 'b' -> side == Side.WHITE ? Piece.WHITE_BISHOP : Piece.BLACK_BISHOP;
            case 'n' -> side == Side.WHITE ? Piece.WHITE_KNIGHT : Piece.BLACK_KNIGHT;
            default  -> Piece.NONE;
        };
    }

    public GameGUI getPlayerGUI(Player player) {
        if (player == white) return guiWhite;
        if (player == black) return guiBlack;
        return null;
    }
}
