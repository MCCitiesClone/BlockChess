package gg.ethereallabs.blockChess.utils;

import gg.ethereallabs.blockChess.BlockChess;
import org.bukkit.Bukkit;

public class SyncHelper {
    private SyncHelper() {}

    public static void runSync(Runnable block) {
        Bukkit.getScheduler().runTask(BlockChess.instance, block);
    }
}
