package gg.ethereallabs.blockChess.utils

import gg.ethereallabs.blockChess.BlockChess
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import org.bukkit.util.io.BukkitObjectInputStream

object InventorySerializer {

    private val logger = Bukkit.getLogger()

    fun itemStackArrayToBytes(items: Array<ItemStack?>): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val dataOutput = BukkitObjectOutputStream(outputStream)
        dataOutput.writeInt(items.size)

        for ((index, item) in items.withIndex()) {
            dataOutput.writeObject(item)
            logger.fine("Serializing item[$index]: ${item?.type ?: "null"} x${item?.amount ?: 0}")
        }

        dataOutput.close()
        return outputStream.toByteArray()
    }

    fun bytesToItemStackArray(bytes: ByteArray): Array<ItemStack?> {
        val inputStream = ByteArrayInputStream(bytes)
        val dataInput = BukkitObjectInputStream(inputStream)
        val size = dataInput.readInt()
        val items = arrayOfNulls<ItemStack>(size)

        logger.fine("Deserializing inventory with $size slots...")

        for (i in 0 until size) {
            try {
                val item = dataInput.readObject() as? ItemStack
                items[i] = item
                logger.fine("Deserialized item[$i]: ${item?.type ?: "null"} x${item?.amount ?: 0}")
            } catch (ex: Exception) {
                logger.warning("Failed to deserialize item at index $i: ${ex.message}")
                ex.printStackTrace()
            }
        }

        dataInput.close()
        return items
    }

    fun saveInventoryToPDC(player: Player?) {
        if (player == null) {
            logger.warning("saveInventoryToPDC called with null player!")
            return
        }

        val key = NamespacedKey(BlockChess.instance, "saved_inventory")
        logger.info("[BlockChess] Saving inventory for ${player.name}...")

        val contents = player.inventory.contents
        val nonNullItems = contents.filterNotNull().size

        logger.info("[BlockChess] Found $nonNullItems items to save.")
        val invBytes = itemStackArrayToBytes(contents)

        player.persistentDataContainer.set(key, PersistentDataType.BYTE_ARRAY, invBytes)
        player.inventory.clear()

        logger.info("[BlockChess] Inventory saved and cleared for ${player.name}.")
    }

    fun restoreInventoryFromPDC(player: Player?) {
        if (player == null) return

        val key = NamespacedKey(BlockChess.instance, "saved_inventory")
        val invBytes = player.persistentDataContainer.get(key, PersistentDataType.BYTE_ARRAY) ?: return
        Bukkit.getScheduler().runTaskLater(BlockChess.instance, Runnable {
            val items = bytesToItemStackArray(invBytes)
            player.inventory.contents = items
            player.persistentDataContainer.remove(key)
        }, 2L)
    }

}
