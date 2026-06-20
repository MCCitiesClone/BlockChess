package gg.ethereallabs.blockChess.gui.models;

import gg.ethereallabs.blockChess.BlockChess;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public abstract class BaseMenu implements Listener {
    private final Component title;
    public final int size;
    protected Inventory inv = null;
    private final Set<UUID> viewers = new HashSet<>();

    protected BaseMenu(Component title, int size) {
        this.title = title;
        this.size = size;
    }

    protected BaseMenu(String title, int size) {
        this(LegacyComponentSerializer.legacyAmpersand().deserialize(title), size);
    }

    public void open(Player p) {
        if (!p.isOnline()) return;
        HandlerList.unregisterAll(this);
        inv = Bukkit.createInventory(null, size, title.color(NamedTextColor.WHITE));
        Bukkit.getPluginManager().registerEvents(this, BlockChess.instance);
        viewers.add(p.getUniqueId());
        draw(p);
        p.openInventory(inv);
    }

    public abstract void draw(Player p);

    public abstract void handleClick(Player p, int slot, InventoryClickEvent e);

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getView().getTopInventory() == inv) {
            e.setCancelled(true);
            handleClick(player, e.getRawSlot(), e);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (inv == null || e.getInventory() != inv) return;
        if (!(e.getPlayer() instanceof Player player)) return;
        viewers.remove(player.getUniqueId());
        if (viewers.isEmpty()) {
            HandlerList.unregisterAll(this);
        }
    }

    protected ItemStack createItem(Component name, Material material, List<Component> lore, int qty) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        item.setAmount(qty);
        if (meta != null) {
            meta.customName(name != null ? name.decoration(TextDecoration.ITALIC, false) : null);
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public Inventory getInventory() {
        return inv;
    }
}
