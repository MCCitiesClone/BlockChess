package gg.ethereallabs.blockChess.gui.subgui;

import com.github.bhlangonijr.chesslib.Piece;
import gg.ethereallabs.blockChess.BlockChess;
import gg.ethereallabs.blockChess.game.GameManager;
import gg.ethereallabs.blockChess.gui.GameGUI;
import gg.ethereallabs.blockChess.gui.models.BaseMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class PromotionGUI extends BaseMenu {
    private final GameGUI gameGUI;
    private final Player player;
    private final Consumer<Piece> onPieceChosen;

    public PromotionGUI(GameGUI gameGUI, Player player, Consumer<Piece> onPieceChosen) {
        super("Promotion", 27);
        this.gameGUI = gameGUI;
        this.player = player;
        this.onPieceChosen = onPieceChosen;
        GameManager.playersPromoting.put(player.getUniqueId(), this);
    }

    @Override
    public void draw(Player p) {
        Map<String, Integer> pieceToSlot = Map.of("q", 10, "r", 12, "n", 14, "b", 16);

        pieceToSlot.forEach((piece, slot) -> {
            Material material = gameGUI.playerIsWhite
                    ? BlockChess.whitePiecesByChar.get(piece)
                    : BlockChess.blackPiecesByChar.get(piece);

            if (material == null) return;

            String name = BlockChess.instance.fenToName.getOrDefault(piece, "Unknown");
            var item = createItem(Component.text(name), material, List.of(
                Component.text("Promote").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.RED)
            ), 1);

            var meta = item.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(449);
                item.setItemMeta(meta);
            }

            if (inv != null) inv.setItem(slot, item);
        });
    }

    @Override
    public void handleClick(Player p, int slot, InventoryClickEvent e) {
        Piece chosenPiece = switch (slot) {
            case 10 -> gameGUI.playerIsWhite ? Piece.WHITE_QUEEN  : Piece.BLACK_QUEEN;
            case 12 -> gameGUI.playerIsWhite ? Piece.WHITE_ROOK   : Piece.BLACK_ROOK;
            case 14 -> gameGUI.playerIsWhite ? Piece.WHITE_KNIGHT : Piece.BLACK_KNIGHT;
            case 16 -> gameGUI.playerIsWhite ? Piece.WHITE_BISHOP : Piece.BLACK_BISHOP;
            default -> null;
        };
        if (chosenPiece != null && p != null) {
            // Remove from map before closing so PlayerListener.onInventoryClose doesn't
            // treat this programmatic close as the player dismissing the GUI.
            GameManager.playersPromoting.remove(p.getUniqueId());
            onPieceChosen.accept(chosenPiece);
            p.closeInventory();
            gameGUI.open(p);
        }
    }
}
