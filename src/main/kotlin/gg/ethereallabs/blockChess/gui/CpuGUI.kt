package gg.ethereallabs.blockChess.gui

import gg.ethereallabs.blockChess.BlockChess
import gg.ethereallabs.blockChess.engine.EngineEnemies
import gg.ethereallabs.blockChess.game.GameManager
import gg.ethereallabs.blockChess.gui.models.BaseMenu
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

class CpuGUI : BaseMenu("<shift:-48>ꀂ", 54){

    fun drawEnemies() {
        EngineEnemies.enemySlotMap.forEach { (slot, item) ->
            inv?.setItem(slot, item)
        }
    }

    fun drawControl(){
        val backButton = createItem(BlockChess.mm.deserialize("<aqua>Go Back").decoration(TextDecoration.ITALIC, false),
            Material.IRON_NUGGET, mutableListOf(BlockChess.mm.deserialize("<gray>Go back to main menu").decoration(TextDecoration.ITALIC, false)), 1)

        val backMeta = backButton.itemMeta
        backMeta.setCustomModelData(449)

        backButton.itemMeta = backMeta

        inv?.setItem(4, backButton)
    }

    override fun draw(p: Player?) {
        drawControl()
        drawEnemies()
    }

    override fun handleClick(
        p: Player?,
        slot: Int,
        e: InventoryClickEvent?
    ) {
        e?.isCancelled = true

        if (slot == 4) {
            if(p != null)
                MainGUI().open(p)
            return
        }

        val index = EngineEnemies.slotOrder.indexOf(slot)
        if (index == -1) return

        val enemyData = EngineEnemies.enemiesData[index]

        val skillLevel = index+1

        if(p != null)
            GameManager.startBot(p, skillLevel, enemyData)
    }
}