package gg.ethereallabs.blockChess.gui;

import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import gg.ethereallabs.blockChess.BlockChess;
import gg.ethereallabs.blockChess.config.Config;
import gg.ethereallabs.blockChess.game.Game;
import gg.ethereallabs.blockChess.gui.models.BaseMenu;
import gg.ethereallabs.blockChess.gui.subgui.AcceptDrawGUI;
import gg.ethereallabs.blockChess.gui.subgui.PromotionGUI;
import gg.ethereallabs.blockChess.gui.subgui.RequestDrawGUI;
import gg.ethereallabs.blockChess.gui.subgui.SurrendGUI;
import gg.ethereallabs.blockChess.utils.InventorySerializer;
import gg.ethereallabs.blockChess.utils.SyncHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class GameGUI extends BaseMenu {
    public final Game game;
    public final boolean playerIsWhite;

    private Square selected = null;
    private List<Move> legalFromSelected = new ArrayList<>();

    public GameGUI(Game game, boolean playerIsWhite) {
        super(!Config.resourcepack ? "BlockChess" : (playerIsWhite ? "<shift:-48>⸿" : "<shift:-48>⸘"), 54);
        this.game = game;
        this.playerIsWhite = playerIsWhite;
        InventorySerializer.saveInventoryToPDC(playerIsWhite ? game.white : game.black);
    }

    private void clearInventories(Player p) {
        if (inv != null) inv.clear();
        if (p != null) p.getInventory().clear();
    }

    private void renderStaticControls() {
        if (inv != null) {
            inv.setItem(26, getClockItem());
            inv.setItem(53, getSurrendItem());
            inv.setItem(44, getDrawItem());
        }
    }

    private void renderCapturedForPerspective(Player p) {
        if (playerIsWhite) giveCapturedPieces(p, game.blackEaten);
        else giveCapturedPieces(p, game.whiteEaten);
    }

    private ItemStack createPieceItem(Piece piece, Square square) {
        if (piece.getPieceType() == null || piece.getPieceType().name().equals("NONE")) return null;
        String fen = piece.getFenSymbol().toLowerCase();
        Material material = piece.getPieceSide() == Side.WHITE
                ? BlockChess.whitePiecesByChar.get(fen)
                : BlockChess.blackPiecesByChar.get(fen);
        if (material == null) return null;

        String name = BlockChess.instance.fenToName.getOrDefault(fen, "Unknown");
        ItemStack item = createItem(Component.text(name), material, List.of(
            Component.text(square.name()).decoration(TextDecoration.ITALIC, false)
        ), 1);

        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(449);
            meta.getPersistentDataContainer().set(BlockChess.CHESS_PIECE_KEY, PersistentDataType.BOOLEAN, true);
            if (item.getType() == Material.LEATHER_HORSE_ARMOR && meta instanceof LeatherArmorMeta leatherMeta) {
                leatherMeta.setColor(null);
                item.setItemMeta(leatherMeta);
            } else {
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    private void placeItemAtVisual(Player p, int visualRank, int visualFile, ItemStack item) {
        if (visualRank < 6) {
            int chestSlot = visualRank * 9 + visualFile;
            if (inv != null) inv.setItem(chestSlot, item);
        } else {
            int playerInvRow = visualRank - 6;
            int playerSlot = playerInvRow * 9 + visualFile + 9;
            if (p != null) p.getInventory().setItem(playerSlot, item);
        }
    }

    private void renderBoardPieces(Player p) {
        var board = game.board;
        for (int visualRank = 0; visualRank <= 7; visualRank++) {
            for (int visualFile = 0; visualFile <= 7; visualFile++) {
                int boardRank = playerIsWhite ? 7 - visualRank : visualRank;
                int boardFile = playerIsWhite ? visualFile : 7 - visualFile;
                int squareIndex = boardRank * 8 + boardFile;
                Square square = Square.squareAt(squareIndex);
                Piece piece = board.getPiece(square);
                if (piece == null) continue;
                ItemStack item = createPieceItem(piece, square);
                if (item == null) continue;
                placeItemAtVisual(p, visualRank, visualFile, item);
            }
        }
    }

    private String formatTime(long ms) {
        long totalSec = (ms > 0 ? ms : 0L) / 1000;
        long m = totalSec / 60;
        long s = totalSec % 60;
        return String.format("%d:%02d", m, s);
    }

    private Square squareFromRawSlot(int slot) {
        if (slot >= 0 && slot <= 53) {
            int visualRank = slot / 9;
            int visualFile = slot % 9;
            if (visualFile == 8) return null;
            int boardRank = playerIsWhite ? 7 - visualRank : visualRank;
            int boardFile = playerIsWhite ? visualFile : 7 - visualFile;
            return Square.squareAt(boardRank * 8 + boardFile);
        }
        if (slot >= 54 && slot <= 89) {
            int idx = slot - 54;
            int bottomRow = idx / 9;
            int bottomCol = idx % 9;
            if (bottomCol == 8) return null;
            if (bottomRow < 0 || bottomRow > 1) return null;
            int visualRank = 6 + bottomRow;
            int boardRank = playerIsWhite ? 7 - visualRank : visualRank;
            int boardFile = playerIsWhite ? bottomCol : 7 - bottomCol;
            return Square.squareAt(boardRank * 8 + boardFile);
        }
        return null;
    }

    private void setOverlayAtSquare(Player p, Square square, ItemStack item) {
        int squareIndex = square.ordinal();
        int boardRank = squareIndex / 8;
        int boardFile = squareIndex % 8;
        int visualRank = playerIsWhite ? 7 - boardRank : boardRank;
        int visualFile = playerIsWhite ? boardFile : 7 - boardFile;

        if (visualRank < 6) {
            int chestSlot = visualRank * 9 + visualFile;
            if (inv != null) inv.setItem(chestSlot, item);
        } else {
            int playerInvRow = visualRank - 6;
            int playerSlot = playerInvRow * 9 + visualFile + 9;
            if (p != null) p.getInventory().setItem(playerSlot, item);
        }
    }

    public ItemStack getClockItem() {
        Material mat = Config.resourcepack ? Material.IRON_NUGGET : Material.CLOCK;
        ItemStack clockItem = createItem(Component.text("Remaining Time"), mat, List.of(
            BlockChess.mm.deserialize("<gray>White: <yellow>" + formatTime(game.whiteTimeMs)),
            BlockChess.mm.deserialize("<gray>Black: <yellow>" + formatTime(game.blackTimeMs))
        ), 1);
        var meta = clockItem.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(449);
            clockItem.setItemMeta(meta);
        }
        return clockItem;
    }

    public ItemStack getSurrendItem() {
        Material mat = Config.resourcepack ? Material.IRON_INGOT : Material.WHITE_BANNER;
        ItemStack item = createItem(Component.text("Surrender"), mat, List.of(), 1);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(449);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack getDrawItem() {
        Material mat = Config.resourcepack ? Material.GOLD_INGOT : Material.GRAY_BANNER;
        ItemStack item = createItem(Component.text("Request Draw"), mat, List.of(), 1);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(449);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void giveCapturedPieces(Player player, List<Piece> pieces) {
        if (player == null) return;
        pieces.sort(null);

        Map<String, Integer> pieceToSlot = new HashMap<>(Map.of(
            "p", 0, "n", 1, "b", 2, "r", 3, "q", 4
        ));

        // distinct pieces
        List<Piece> seen = new ArrayList<>();
        for (Piece piece : pieces) {
            if (!seen.contains(piece)) seen.add(piece);
        }

        for (Piece piece : seen) {
            String fen = piece.getFenSymbol().toLowerCase();
            Material material = piece.getPieceSide() == Side.WHITE
                    ? BlockChess.whitePiecesByChar.get(fen)
                    : BlockChess.blackPiecesByChar.get(fen);
            if (material == null) continue;

            int amount = 0;
            for (Piece p : pieces) if (p == piece) amount++;

            Integer slot = pieceToSlot.get(fen);
            if (slot == null) continue;

            String name = BlockChess.instance.fenToName.getOrDefault(fen, "Unknown");
            ItemStack item = createItem(Component.text(name), material, List.of(
                Component.text("Eaten").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.RED)
            ), amount);

            var meta = item.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(449);
                meta.getPersistentDataContainer().set(BlockChess.CHESS_PIECE_KEY, PersistentDataType.BOOLEAN, true);
                item.setItemMeta(meta);
            }
            player.getInventory().setItem(slot, item);
        }
    }

    public void updateClock() {
        if (inv != null) inv.setItem(26, getClockItem());
    }

    public void displayDrawRequest() {
        if (game.drawRequester == null) return;
        Player player = game.drawRequester == game.white ? game.black : game.white;
        if (player == null) return;

        var drawItem = createItem(BlockChess.mm.deserialize("<yellow>Accept Draw?"), Material.GRAY_WOOL,
            List.of(
                BlockChess.mm.deserialize("<yellow>Open a dialog to").decoration(TextDecoration.ITALIC, false),
                BlockChess.mm.deserialize("<green>accept <yellow> or <red> decline <yellow> the draw.").decoration(TextDecoration.ITALIC, false)
            ), 1);

        player.getInventory().setItem(35, drawItem);
    }

    public void renderLastMove() {
        Move move = game.lastMove;
        if (move == null) return;
        var board = game.board;
        Piece piece = board.getPiece(move.getTo());

        String pieceName = piece != null
                ? BlockChess.instance.fenToName.getOrDefault(piece.getFenSymbol().toLowerCase(), "Piece")
                : "Piece";

        ItemStack lastMoveOverlay = createItem(
            BlockChess.mm.deserialize("<yellow>Last Move"),
            Material.YELLOW_STAINED_GLASS_PANE,
            List.of(
                BlockChess.mm.deserialize("<gray>" + pieceName + " <yellow>to <gray>" + move.getTo().name())
                    .decoration(TextDecoration.ITALIC, false)
            ),
            1
        );

        setOverlayAtSquare(game.white, move.getFrom(), lastMoveOverlay);
        setOverlayAtSquare(game.black, move.getFrom(), lastMoveOverlay);

        if (piece == null) return;
        ItemStack movedPieceItem = createPieceItem(piece, move.getTo());
        if (movedPieceItem == null) return;

        var meta = movedPieceItem.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.SHARPNESS, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            movedPieceItem.setItemMeta(meta);
        }

        setOverlayAtSquare(game.white, move.getTo(), movedPieceItem);
        setOverlayAtSquare(game.black, move.getTo(), movedPieceItem);
    }

    @Override
    public void draw(Player p) {
        if (p == null || !p.isOnline()) return;
        clearInventories(p);
        renderStaticControls();
        displayDrawRequest();
        renderCapturedForPerspective(p);
        renderBoardPieces(p);
        renderLastMove();
    }

    @Override
    public void handleClick(Player p, int slot, InventoryClickEvent e) {
        if (game.isEnded()) {
            if (e != null) e.setCancelled(true);
            return;
        }

        if (handleControlClicks(slot, p)) return;

        Square clickedSquare = squareFromRawSlot(slot);
        if (clickedSquare == null) return;

        Side sideViewing = playerIsWhite ? Side.WHITE : Side.BLACK;
        var board = game.board;

        if (selected == null) {
            trySelectPiece(p, board, clickedSquare, sideViewing);
            return;
        }

        if (attemptMove(p, clickedSquare)) return;

        tryReselectOrReset(p, board, clickedSquare, sideViewing);
    }

    private boolean handleControlClicks(int slot, Player p) {
        switch (slot) {
            case 80 -> {
                if (p == null) return true;
                new AcceptDrawGUI(this, p, choice -> {
                    try {
                        if (choice) {
                            game.finalizeGame(Game.ResultType.DRAW);
                        } else {
                            game.drawRequester = null;
                        }
                    } catch (Exception ignored) {}
                }).open(p);
                return true;
            }
            case 53 -> {
                if (p == null) return true;
                new SurrendGUI(this, p, choice -> {
                    try {
                        if (choice) {
                            game.finalizeGame(playerIsWhite ? Game.ResultType.WHITE_RESIGN : Game.ResultType.BLACK_RESIGN);
                        }
                    } catch (Exception ignored) {}
                }).open(p);
                return true;
            }
            case 44 -> {
                if (p == null) return true;
                if (game.againstBot) {
                    BlockChess.instance.sendMessage("<red>You can't request a draw against CPU!", p);
                    return true;
                }
                new RequestDrawGUI(this, p, choice -> {
                    try {
                        if (choice) {
                            game.drawRequester = p;
                            Player target = getOppositePlayer(p);
                            if (target != null && target.isOnline()) {
                                BlockChess.instance.sendMessage("<yellow>Your opponent has requested you a draw!", target);
                            }
                        } else {
                            BlockChess.instance.sendMessage("You have canceled your draw request.", p);
                        }
                    } catch (Exception ignored) {}
                }).open(p);
                return true;
            }
        }
        return false;
    }

    private void trySelectPiece(Player p, com.github.bhlangonijr.chesslib.Board board, Square clickedSquare, Side sideViewing) {
        Piece piece = board.getPiece(clickedSquare);
        if (piece == null || piece.getPieceType() == null || piece.getPieceType().name().equals("NONE")) return;
        if (piece.getPieceSide() != sideViewing) return;
        if (board.getSideToMove() != sideViewing) return;

        computeAndShowLegalMovesFrom(p, board, clickedSquare, piece);
    }

    private void computeAndShowLegalMovesFrom(Player p, com.github.bhlangonijr.chesslib.Board board, Square clickedSquare, Piece piece) {
        CompletableFuture.runAsync(() -> {
            List<Move> moves;
            try { moves = board.legalMoves(); } catch (Exception e) { moves = List.of(); }
            List<Move> legalMoves = moves.stream().filter(m -> m.getFrom() == clickedSquare).toList();
            List<Move> finalMoves = legalMoves;

            Bukkit.getScheduler().runTask(BlockChess.instance, () -> {
                legalFromSelected = new ArrayList<>(finalMoves);
                selected = clickedSquare;

                for (Move mv : legalFromSelected) {
                    Square target = mv.getTo();
                    Piece targetPiece = board.getPiece(target);
                    boolean isCapture = (targetPiece != null && targetPiece.getPieceType() != null && !targetPiece.getPieceType().name().equals("NONE")) ||
                            (board.getEnPassantTarget() == target && piece.getPieceType() != null && piece.getPieceType().name().equals("PAWN"));

                    String overlayNameStr = isCapture
                            ? "Capture " + (targetPiece != null ? BlockChess.instance.fenToName.getOrDefault(targetPiece.getFenSymbol().toLowerCase(), "Piece") : "Piece")
                            : "Move";
                    Component overlayName = Component.text(overlayNameStr);
                    Material overlayMat = isCapture ? Material.RED_STAINED_GLASS_PANE : Material.GREEN_STAINED_GLASS_PANE;
                    ItemStack overlay = createItem(overlayName, overlayMat, List.of(), 1);
                    setOverlayAtSquare(p, target, overlay);
                }
            });
        });
    }

    private boolean attemptMove(Player p, Square clickedSquare) {
        Move targetMove = null;
        for (Move m : legalFromSelected) {
            if (m.getTo() == clickedSquare) { targetMove = m; break; }
        }
        if (targetMove == null) return false;
        performMoveAsync(p, targetMove);
        return true;
    }

    private Player getOppositePlayer(Player player) {
        if (player != game.white && player != game.black) return null;
        return player == game.white ? game.black : game.white;
    }

    private void performMoveAsync(Player p, Move targetMove) {
        CompletableFuture.runAsync(() -> {
            try {
                Piece oldPiece = game.board.getPiece(targetMove.getTo());

                if (targetMove.getPromotion() != Piece.NONE) {
                    SyncHelper.runSync(() -> {
                        if (p == null) return;

                        // Construct PromotionGUI first so playersPromoting is set before
                        // p.openInventory() implicitly closes the GameGUI — otherwise
                        // PlayerListener sees an unguarded GameGUI close and fires the
                        // "can't close" error and re-schedules the board to reopen.
                        new PromotionGUI(this, p, chosenPiece -> {
                            CompletableFuture.runAsync(() -> {
                                try {
                                    Move promoMove = new Move(targetMove.getFrom(), targetMove.getTo(), chosenPiece);
                                    game.board.doMove(promoMove);

                                    SyncHelper.runSync(() -> {
                                        if (oldPiece != Piece.NONE && oldPiece.getPieceType() != null && !oldPiece.getPieceType().name().equals("NONE")) {
                                            if (oldPiece.getPieceSide() == Side.WHITE) game.whiteEaten.add(oldPiece);
                                            else game.blackEaten.add(oldPiece);
                                        }
                                        selected = null;
                                        legalFromSelected = new ArrayList<>();
                                        game.onMoveMade(promoMove);
                                    });
                                } catch (Exception ignored) {}
                            });
                        }).open(p);
                    });
                    return;
                }

                game.board.doMove(targetMove);
                Move finalTargetMove = targetMove;
                SyncHelper.runSync(() -> {
                    if (oldPiece != Piece.NONE && oldPiece.getPieceType() != null && !oldPiece.getPieceType().name().equals("NONE")) {
                        if (oldPiece.getPieceSide() == Side.WHITE) game.whiteEaten.add(oldPiece);
                        else game.blackEaten.add(oldPiece);
                    }
                    selected = null;
                    legalFromSelected = new ArrayList<>();
                    game.onMoveMade(finalTargetMove);
                });
            } catch (Exception ignored) {}
        });
    }

    private void tryReselectOrReset(Player p, com.github.bhlangonijr.chesslib.Board board, Square clickedSquare, Side sideViewing) {
        Piece piece = board.getPiece(clickedSquare);
        if (piece != null && piece.getPieceType() != null && !piece.getPieceType().name().equals("NONE") && piece.getPieceSide() == sideViewing) {
            draw(p);
            computeAndShowLegalMovesFrom(p, board, clickedSquare, piece);
            return;
        }
        selected = null;
        legalFromSelected = new ArrayList<>();
        draw(p);
    }
}
