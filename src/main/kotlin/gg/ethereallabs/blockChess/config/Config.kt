package gg.ethereallabs.blockChess.config

import gg.ethereallabs.blockChess.BlockChess
import org.bukkit.entity.Player

object Config {
    var resourcepack = true
    var enginePath : String = ""
    var botEnabled = false

    fun load(plugin: BlockChess) {
        val cfg = plugin.config
        resourcepack = cfg.getBoolean("resourcepack.enabled", true)
        enginePath = cfg.getString("engine.path", "plugins/BlockChess/stockfish.exe")!!
        botEnabled = cfg.getBoolean("bot_gamemode.enabled", true)

        BlockChess.instance.logger.info("Loaded Engine Path: $enginePath")
    }
}