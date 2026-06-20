package gg.ethereallabs.blockChess.utils;

import gg.ethereallabs.blockChess.BlockChess;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.logging.Logger;

public class InventorySerializer {
    private InventorySerializer() {}

    private static final Logger logger = Bukkit.getLogger();

    public static byte[] itemStackArrayToBytes(ItemStack[] items) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
        dataOutput.writeInt(items.length);

        for (int index = 0; index < items.length; index++) {
            ItemStack item = items[index];
            dataOutput.writeObject(item);
            logger.fine("Serializing item[" + index + "]: " + (item != null ? item.getType() : "null") + " x" + (item != null ? item.getAmount() : 0));
        }

        dataOutput.close();
        return outputStream.toByteArray();
    }

    public static ItemStack[] bytesToItemStackArray(byte[] bytes) throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
        int size = dataInput.readInt();
        ItemStack[] items = new ItemStack[size];

        logger.fine("Deserializing inventory with " + size + " slots...");

        for (int i = 0; i < size; i++) {
            try {
                Object obj = dataInput.readObject();
                items[i] = (obj instanceof ItemStack) ? (ItemStack) obj : null;
                logger.fine("Deserialized item[" + i + "]: " + (items[i] != null ? items[i].getType() : "null") + " x" + (items[i] != null ? items[i].getAmount() : 0));
            } catch (Exception ex) {
                logger.warning("Failed to deserialize item at index " + i + ": " + ex.getMessage());
                ex.printStackTrace();
            }
        }

        dataInput.close();
        return items;
    }

    public static void saveInventoryToPDC(Player player) {
        if (player == null) {
            logger.warning("saveInventoryToPDC called with null player!");
            return;
        }

        NamespacedKey key = new NamespacedKey(BlockChess.instance, "saved_inventory");
        logger.info("[BlockChess] Saving inventory for " + player.getName() + "...");

        ItemStack[] contents = player.getInventory().getContents();
        int nonNullItems = 0;
        for (ItemStack item : contents) if (item != null) nonNullItems++;

        logger.info("[BlockChess] Found " + nonNullItems + " items to save.");

        try {
            byte[] invBytes = itemStackArrayToBytes(contents);
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
                ItemStack[] items = bytesToItemStackArray(invBytes);
                player.getInventory().setContents(items);
                player.getPersistentDataContainer().remove(key);
            } catch (Exception e) {
                logger.warning("Failed to restore inventory for " + player.getName() + ": " + e.getMessage());
            }
        }, 2L);
    }
}
