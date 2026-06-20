package gg.ethereallabs.blockChess.utils;

import gg.ethereallabs.blockChess.BlockChess;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.logging.Logger;

public class InventorySerializer {
    private InventorySerializer() {}

    private static final Logger logger = Bukkit.getLogger();

    public static byte[] itemStackArrayToBytes(ItemStack[] items) {
        return ItemStack.serializeItemsAsBytes(Arrays.asList(items));
    }

    public static ItemStack[] bytesToItemStackArray(byte[] bytes) {
        return ItemStack.deserializeItemsFromBytes(bytes);
    }

    public static void saveInventoryToPDC(Player player) {
        if (player == null) {
            logger.warning("saveInventoryToPDC called with null player!");
            return;
        }

        NamespacedKey key = new NamespacedKey(BlockChess.instance, "saved_inventory");
        logger.info("[BlockChess] Saving inventory for " + player.getName() + "...");

        try {
            byte[] invBytes = itemStackArrayToBytes(player.getInventory().getContents());
            player.getPersistentDataContainer().set(key, PersistentDataType.BYTE_ARRAY, invBytes);
            player.getInventory().clear();
            logger.info("[BlockChess] Inventory saved and cleared for " + player.getName() + ".");
        } catch (Exception e) {
            logger.warning("Failed to save inventory for " + player.getName() + ": " + e.getMessage());
        }
    }

    public static void restoreInventoryFromPDC(Player player) {
        if (player == null) return;

        NamespacedKey key = new NamespacedKey(BlockChess.instance, "saved_inventory");
        byte[] invBytes = player.getPersistentDataContainer().get(key, PersistentDataType.BYTE_ARRAY);
        if (invBytes == null) return;

        Bukkit.getScheduler().runTaskLater(BlockChess.instance, () -> {
            try {
                player.getInventory().setContents(bytesToItemStackArray(invBytes));
                player.getPersistentDataContainer().remove(key);
            } catch (Exception e) {
                logger.warning("Failed to restore inventory for " + player.getName() + ": " + e.getMessage());
            }
        }, 2L);
    }
}
