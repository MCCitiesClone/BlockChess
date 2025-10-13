package gg.ethereallabs.blockChess.engine

import gg.ethereallabs.blockChess.BlockChess
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

data class EnemyData(
    val name: String,
    val color: String,
    val elo: Int,
    val lore: String
)

class EngineEnemies {
    companion object {

        private fun createEnemy(
            name: Component?,
            elo: Int,
            lore: MutableList<Component?>?
        ): ItemStack {
            val item = ItemStack(Material.IRON_NUGGET)
            val meta = item.itemMeta
            var rating = "<yellow>$elo<gray>"
            if(elo == 9999) rating = "<red><obf>9999</obf><gray>"
            if (meta != null) {
                meta.customName(
                    name
                        ?.append(BlockChess.mm.deserialize(" <gray>($rating)"))
                        ?.decoration(TextDecoration.ITALIC, false)
                )
                meta.lore(lore)
                meta.setCustomModelData(449)
                item.setItemMeta(meta)
            }
            return item
        }

        val enemiesData = listOf(
            EnemyData("Sheep", "<white>", 200, "<gray>A timid pawn of the plains."),
            EnemyData("Chicken", "<white>", 500, "<gray>Fast to flee, but quicker to strike."),
            EnemyData("Cow", "<white>", 820, "<gray>Strong, steady, and stubborn."),
            EnemyData("Pig", "<light_purple>", 1060, "<gray>Don’t underestimate that snout."),
            EnemyData("Zombie", "<green>", 1350, "<gray>Rises again after every loss."),
            EnemyData("Skeleton", "<white>", 1600, "<gray>Every move—calculated to the bone."),
            EnemyData("Creeper", "<green>", 1900, "<gray>Explodes with tactical precision."),
            EnemyData("Enderman", "<dark_purple>", 2200, "<gray>Teleports between moves unseen."),
            EnemyData("Pillager", "<dark_gray>", 2500, "<gray>Fights dirty and never alone."),
            EnemyData("Villager", "<gold>", 2800, "<gray>Trades wisdom for victory."),
            EnemyData("Steve", "<blue>", 3050, "<gray>The default... yet undefeated."),
            EnemyData("Herobrine", "<red>", 9999, "<dark_red>A legend beyond mortal play.")
        )

        val enemies: List<ItemStack> = enemiesData.map { data ->
            createEnemy(
                BlockChess.mm.deserialize("${data.color}${data.name}"),
                data.elo,
                mutableListOf(BlockChess.mm.deserialize(data.lore))
            )
        }

        val slotOrder = listOf(10, 12, 14, 16, 28, 30, 32, 34, 46, 48, 50, 52)

        val enemySlotMap: Map<Int, ItemStack> = slotOrder.zip(enemies).toMap()
    }
}
