package gg.ethereallabs.blockChess.events;

import gg.ethereallabs.blockChess.BlockChess;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ChessPieceListener implements Listener {

    private boolean isChessPiece(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(BlockChess.CHESS_PIECE_KEY, PersistentDataType.BOOLEAN);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (isChessPiece(e.getItemInHand())) e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent e) {
        if (isChessPiece(e.getItemDrop().getItemStack())) e.setCancelled(true);
    }

    @EventHandler
    public void onInventoryMove(InventoryMoveItemEvent e) {
        if (isChessPiece(e.getItem())) e.setCancelled(true);
    }
}
