package gg.ethereallabs.blockChess.engine;

import gg.ethereallabs.blockChess.BlockChess;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class EngineEnemies {
    private EngineEnemies() {}

    private static ItemStack createEnemy(Component name, int elo, List<Component> lore) {
        ItemStack item = new ItemStack(Material.IRON_NUGGET);
        var meta = item.getItemMeta();
        String rating = elo == 9999 ? "<red><obf>9999</obf><gray>" : "<yellow>" + elo + "<gray>";
        if (meta != null) {
            meta.customName(
                name.append(BlockChess.mm.deserialize(" <gray>(" + rating + ")"))
                    .decoration(TextDecoration.ITALIC, false)
            );
            meta.lore(lore);
            meta.setCustomModelData(449);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static final List<EnemyData> enemiesData = List.of(
        new EnemyData("Sheep",    "<white>",       200,  "<gray>A timid pawn of the plains."),
        new EnemyData("Chicken",  "<white>",       500,  "<gray>Fast to flee, but quicker to strike."),
        new EnemyData("Cow",      "<white>",       820,  "<gray>Strong, steady, and stubborn."),
        new EnemyData("Pig",      "<light_purple>",1060, "<gray>Don't underestimate that snout."),
        new EnemyData("Zombie",   "<green>",       1350, "<gray>Rises again after every loss."),
        new EnemyData("Skeleton", "<white>",       1600, "<gray>Every move—calculated to the bone."),
        new EnemyData("Creeper",  "<green>",       1900, "<gray>Explodes with tactical precision."),
        new EnemyData("Enderman", "<dark_purple>", 2200, "<gray>Teleports between moves unseen."),
        new EnemyData("Pillager", "<dark_gray>",   2500, "<gray>Fights dirty and never alone."),
        new EnemyData("Villager", "<gold>",        2800, "<gray>Trades wisdom for victory."),
        new EnemyData("Steve",    "<blue>",        3050, "<gray>The default... yet undefeated."),
        new EnemyData("Herobrine","<red>",         9999, "<dark_red>A legend beyond mortal play.")
    );

    public static final List<ItemStack> enemies;
    public static final List<Integer> slotOrder = List.of(10, 12, 14, 16, 28, 30, 32, 34, 46, 48, 50, 52);
    public static final Map<Integer, ItemStack> enemySlotMap;

    static {
        List<ItemStack> items = new ArrayList<>();
        for (EnemyData data : enemiesData) {
            items.add(createEnemy(
                BlockChess.mm.deserialize(data.color() + data.name()),
                data.elo(),
                List.of(BlockChess.mm.deserialize(data.lore()))
            ));
        }
        enemies = Collections.unmodifiableList(items);

        Map<Integer, ItemStack> map = new LinkedHashMap<>();
        for (int i = 0; i < slotOrder.size() && i < enemies.size(); i++) {
            map.put(slotOrder.get(i), enemies.get(i));
        }
        enemySlotMap = Collections.unmodifiableMap(map);
    }
}
