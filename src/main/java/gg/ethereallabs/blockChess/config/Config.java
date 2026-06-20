package gg.ethereallabs.blockChess.config;

import gg.ethereallabs.blockChess.BlockChess;

public class Config {
    private Config() {}

    public static boolean resourcepack = true;
    public static String enginePath = "";
    public static boolean botEnabled = false;

    public static void load(BlockChess plugin) {
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
        resourcepack = cfg.getBoolean("resourcepack.enabled", true);
        enginePath = cfg.getString("engine.path", "plugins/BlockChess/stockfish.exe");
        botEnabled = cfg.getBoolean("bot_gamemode.enabled", true);

        BlockChess.instance.getLogger().info("Loaded Engine Path: " + enginePath);
    }
}
