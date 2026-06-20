package gg.ethereallabs.blockChess.events;

import gg.ethereallabs.blockChess.BlockChess;
import gg.ethereallabs.blockChess.data.LocalStorage;
import gg.ethereallabs.blockChess.elo.EloManager;
import gg.ethereallabs.blockChess.game.Game;
import gg.ethereallabs.blockChess.game.GameManager;
import gg.ethereallabs.blockChess.matchmaking.MatchmakingManager;
import gg.ethereallabs.blockChess.utils.InventorySerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        EloManager.cancelRemoval(player.getUniqueId());
        InventorySerializer.restoreInventoryFromPDC(player);

        if (LocalStorage.playerHasData(player)) {
            LocalStorage.loadPlayerData(player);
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        EloManager.scheduleRemoval(player.getUniqueId(), BlockChess.instance);
        MatchmakingManager.leaveQueue(player.getUniqueId());

        Game game = GameManager.getGame(player);
        if (game != null && !game.isEnded()) {
            Game.ResultType forfeitResult = game.white == player
                    ? Game.ResultType.WHITE_FORFEIT
                    : Game.ResultType.BLACK_FORFEIT;
            game.finalizeGame(forfeitResult);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Game game = GameManager.getGame(player);
        if (game == null || game.isEnded()) return;
        // Chess pieces in the player's inventory slots aren't real items — don't drop them
        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Game game = GameManager.getGame(player);
        if (game != null && !game.isEnded()) {
            // Game still active — reopen the chess GUI after respawn
            Bukkit.getScheduler().runTaskLater(BlockChess.instance, () -> {
                var gui = game.getPlayerGUI(player);
                if (gui != null) gui.open(player);
            }, 5L);
        } else {
            // Game ended while the player was dead — restore their real inventory now
            InventorySerializer.restoreInventoryFromPDC(player);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        if (GameManager.playersAwaitingInvRestore.containsKey(player.getUniqueId())) {
            System.out.println("Restoring player inventory after closing inventory");
            InventorySerializer.restoreInventoryFromPDC(player);
            GameManager.playersAwaitingInvRestore.remove(player.getUniqueId());
            return;
        }

        Game game = GameManager.getGame(player);
        if (game == null) return;

        var promoGUI = GameManager.playersPromoting.get(player.getUniqueId());
        var surrendGUI = GameManager.playersSurrending.get(player.getUniqueId());
        var requestDrawGUI = GameManager.playersRequestingDraw.get(player.getUniqueId());
        var acceptingDrawGUI = GameManager.playersAcceptingDraw.get(player.getUniqueId());

        var gui = game.getPlayerGUI(player);

        if (game.isEnded()) {
            GameManager.playersPromoting.remove(player.getUniqueId());
            GameManager.playersSurrending.remove(player.getUniqueId());
            GameManager.playersRequestingDraw.remove(player.getUniqueId());
            GameManager.playersAcceptingDraw.remove(player.getUniqueId());
            return;
        }

        if (promoGUI != null && event.getInventory() == promoGUI.getInventory()) {
            GameManager.playersPromoting.remove(player.getUniqueId());
            BlockChess.instance.sendMessage("<red>You need to choose a piece!", player);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            Bukkit.getScheduler().runTaskLater(BlockChess.instance, () -> promoGUI.open(player), 3L);
            return;
        }
        if (surrendGUI != null && event.getInventory() == surrendGUI.getInventory()) {
            GameManager.playersSurrending.remove(player.getUniqueId());
            Bukkit.getScheduler().runTaskLater(BlockChess.instance, () -> { if (gui != null) gui.open(player); }, 3L);
            return;
        }
        if (requestDrawGUI != null && event.getInventory() == requestDrawGUI.getInventory()) {
            GameManager.playersRequestingDraw.remove(player.getUniqueId());
            Bukkit.getScheduler().runTaskLater(BlockChess.instance, () -> { if (gui != null) gui.open(player); }, 3L);
            return;
        }
        if (acceptingDrawGUI != null && event.getInventory() == acceptingDrawGUI.getInventory()) {
            GameManager.playersAcceptingDraw.remove(player.getUniqueId());
            Bukkit.getScheduler().runTaskLater(BlockChess.instance, () -> { if (gui != null) gui.open(player); }, 3L);
            return;
        }

        if (gui != null && gui.getInventory() == event.getInventory()
                && !GameManager.playersPromoting.containsKey(player.getUniqueId())
                && !GameManager.playersSurrending.containsKey(player.getUniqueId())
                && !GameManager.playersRequestingDraw.containsKey(player.getUniqueId())
                && !GameManager.playersAcceptingDraw.containsKey(player.getUniqueId())
                && event.getReason() != InventoryCloseEvent.Reason.DEATH) {
            player.getInventory().clear();
            Bukkit.getScheduler().runTaskLater(BlockChess.instance, () -> gui.open(player), 3L);
            BlockChess.instance.sendMessage("<red>You can't close the GUI while in a game!", player);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
}
