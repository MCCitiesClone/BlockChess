package gg.ethereallabs.blockChess.gui;

import gg.ethereallabs.blockChess.BlockChess;
import gg.ethereallabs.blockChess.config.Config;
import gg.ethereallabs.blockChess.gui.models.BaseMenu;
import gg.ethereallabs.blockChess.matchmaking.MatchmakingManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class MainGUI extends BaseMenu {
    public MainGUI() {
        super("<shift:-48>ⷀ", 54);
    }

    @Override
    public void draw(Player p) {
        var globalOnlineItem = createItem(
            BlockChess.mm.deserialize("<blue>🌐 Global Online"),
            Material.IRON_NUGGET,
            List.of(
                BlockChess.mm.deserialize("<gray>Challenge other players <yellow>across the network<gray>!"),
                BlockChess.mm.deserialize("<red>Currently disabled!")
            ),
            1
        );
        var globalMeta = globalOnlineItem.getItemMeta();
        if (globalMeta != null) {
            globalMeta.setCustomModelData(449);
            globalOnlineItem.setItemMeta(globalMeta);
        }

        var onlineItem = createItem(
            Component.text("⚔ Online", NamedTextColor.YELLOW),
            Material.IRON_NUGGET,
            List.of(
                BlockChess.mm.deserialize("<gray>Challenge another player"),
                BlockChess.mm.deserialize("<gray>and begin a strategic duel of minds.")
            ),
            1
        );
        var onlineMeta = onlineItem.getItemMeta();
        if (onlineMeta != null) {
            onlineMeta.setCustomModelData(449);
            onlineItem.setItemMeta(onlineMeta);
        }

        var botItem = createItem(
            Component.text("🤖 Bot", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false),
            Material.IRON_NUGGET,
            List.of(
                BlockChess.mm.deserialize("<gray>Face the machine!"),
                BlockChess.mm.deserialize("<gray>Choose from a list of AI opponents,"),
                BlockChess.mm.deserialize("<gray>each with their own rating and skill.")
            ),
            1
        );
        var botMeta = botItem.getItemMeta();
        if (botMeta != null) {
            botMeta.setCustomModelData(449);
            botItem.setItemMeta(botMeta);
        }

        for (int slot = 0; slot < size; slot++) {
            int col = slot % 9;
            if (col <= 2 && inv != null) inv.setItem(slot, globalOnlineItem);
            else if (col <= 5 && inv != null) inv.setItem(slot, onlineItem);
            else if (inv != null) inv.setItem(slot, botItem);
        }
    }

    @Override
    public void handleClick(Player p, int slot, InventoryClickEvent e) {
        if (slot > 53 || p == null) return;

        int col = slot % 9;
        if (col <= 2) {
            BlockChess.instance.sendMessage("<red>Global Online is currently disabled!", p);
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        } else if (col <= 5) {
            p.closeInventory();
            MatchmakingManager.joinQueue(p);
        } else {
            if (!Config.botEnabled) {
                BlockChess.instance.sendMessage("<red>Bots are currently disabled!", p);
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            new CpuGUI().open(p);
        }
    }
}
