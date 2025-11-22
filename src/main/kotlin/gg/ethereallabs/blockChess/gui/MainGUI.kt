package gg.ethereallabs.blockChess.gui

import gg.ethereallabs.blockChess.BlockChess
import gg.ethereallabs.blockChess.config.Config
import gg.ethereallabs.blockChess.gui.models.BaseMenu
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

class MainGUI : BaseMenu("<shift:-48>ⷀ", 54) {
    override fun draw(p: Player?) {
        val globalOnlineItem = createItem(
            BlockChess.mm.deserialize("<blue>\uD83C\uDF10 Global Online"),
            Material.IRON_NUGGET,
            mutableListOf(
                BlockChess.mm.deserialize("<gray>Challenge other players <yellow>across the network<gray>!")
            ),
            1
        )
        val globalMeta = globalOnlineItem.itemMeta
        globalMeta.setCustomModelData(449)
        globalOnlineItem.itemMeta = globalMeta
        val onlineItem = createItem(
            Component.text("⚔ Online", NamedTextColor.YELLOW),
            Material.IRON_NUGGET,
            mutableListOf(
                BlockChess.mm.deserialize("<gray>Challenge another player"),
                BlockChess.mm.deserialize("<gray>and begin a strategic duel of minds.")
            ),
            1
        )
        val onlineMeta = onlineItem.itemMeta
        onlineMeta.setCustomModelData(449)
        onlineItem.itemMeta = onlineMeta
        val botItem = createItem(
            Component.text("\uD83E\uDD16 Bot", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false),
            Material.IRON_NUGGET,
            mutableListOf(
                BlockChess.mm.deserialize("<gray>Face the machine!"),
                BlockChess.mm.deserialize("<gray>Choose from a list of AI opponents,"),
                BlockChess.mm.deserialize("<gray>each with their own rating and skill."),
            ),
            1
        )
        val botMeta = botItem.itemMeta
        botMeta.setCustomModelData(449)
        botItem.itemMeta = botMeta

        for (slot in 0 until size) {
            val col = slot % 9
            when (col) {
                in 0..2 -> inv?.setItem(slot, globalOnlineItem)
                in 3..5 -> inv?.setItem(slot, onlineItem)
                in 6..8 -> inv?.setItem(slot, botItem)
            }
        }
    }

    override fun handleClick(
        p: Player?,
        slot: Int,
        e: InventoryClickEvent?
    ) {
        if(slot > 53) return
        when (slot % 9) {
            in 0..2 -> {  }
            in 3..5 -> {  }
            in 6..8 -> {
                if(p != null) {
                    if(!Config.botEnabled){
                        BlockChess.instance.sendMessage("<red>Bots are currently disabled!", p)
                        p.playSound(p.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                        return
                    }
                    CpuGUI().open(p)
                }
            }
        }
    }
}