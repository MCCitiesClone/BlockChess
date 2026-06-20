package gg.ethereallabs.blockChess.gui.subgui;

import gg.ethereallabs.blockChess.BlockChess;
import gg.ethereallabs.blockChess.game.GameManager;
import gg.ethereallabs.blockChess.gui.GameGUI;
import gg.ethereallabs.blockChess.gui.models.BaseMenu;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.function.Consumer;

public class RequestDrawGUI extends BaseMenu {
    private final GameGUI gameGUI;
    private final Player player;
    private final Consumer<Boolean> choice;

    public RequestDrawGUI(GameGUI gameGUI, Player player, Consumer<Boolean> choice) {
        super("Request Draw", 27);
        this.gameGUI = gameGUI;
        this.player = player;
        this.choice = choice;
        GameManager.playersRequestingDraw.put(player.getUniqueId(), this);
    }

    @Override
    public void draw(Player p) {
        var acceptItem = createItem(
            BlockChess.mm.deserialize("<green>Request Draw"), Material.GREEN_WOOL,
            List.of(BlockChess.mm.deserialize("<gray>Request a draw").decoration(TextDecoration.ITALIC, false)),
            1
        );
        var cancelItem = createItem(
            BlockChess.mm.deserialize("<red>Cancel"), Material.RED_WOOL,
            List.of(BlockChess.mm.deserialize("<gray>Don't request a draw").decoration(TextDecoration.ITALIC, false)),
            1
        );
        if (inv != null) {
            inv.setItem(12, acceptItem);
            inv.setItem(14, cancelItem);
        }
    }

    @Override
    public void handleClick(Player p, int slot, InventoryClickEvent e) {
        Boolean draw = switch (slot) {
            case 12 -> true;
            case 14 -> false;
            default -> null;
        };
        if (draw != null && p != null) {
            choice.accept(draw);
            p.closeInventory();
            gameGUI.open(p);
            if (draw) {
                BlockChess.instance.sendMessage("<gray>You've requested a draw.", p);
            }
        }
    }
}
