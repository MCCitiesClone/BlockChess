package gg.ethereallabs.blockChess.gui;

import gg.ethereallabs.blockChess.BlockChess;
import gg.ethereallabs.blockChess.engine.EngineEnemies;
import gg.ethereallabs.blockChess.game.GameManager;
import gg.ethereallabs.blockChess.gui.models.BaseMenu;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class CpuGUI extends BaseMenu {
    public CpuGUI() {
        super("<shift:-48>ꀂ", 54);
    }

    public void drawEnemies() {
        EngineEnemies.enemySlotMap.forEach((slot, item) -> {
            if (inv != null) inv.setItem(slot, item);
        });
    }

    public void drawControl() {
        var backButton = createItem(
            BlockChess.mm.deserialize("<aqua>Go Back").decoration(TextDecoration.ITALIC, false),
            Material.IRON_NUGGET,
            List.of(BlockChess.mm.deserialize("<gray>Go back to main menu").decoration(TextDecoration.ITALIC, false)),
            1
        );
        var backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setCustomModelData(449);
            backButton.setItemMeta(backMeta);
        }
        if (inv != null) inv.setItem(4, backButton);
    }

    @Override
    public void draw(Player p) {
        drawControl();
        drawEnemies();
    }

    @Override
    public void handleClick(Player p, int slot, InventoryClickEvent e) {
        if (slot == 4) {
            if (p != null) new MainGUI().open(p);
            return;
        }

        int index = EngineEnemies.slotOrder.indexOf(slot);
        if (index == -1) return;

        var enemyData = EngineEnemies.enemiesData.get(index);
        int skillLevel = index + 1;

        if (p != null) GameManager.startBot(p, skillLevel, enemyData);
    }
}
