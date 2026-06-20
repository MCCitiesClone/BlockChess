package gg.ethereallabs.blockChess.elo;

import gg.ethereallabs.blockChess.BlockChess;
import gg.ethereallabs.blockChess.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EloManager {
    private EloManager() {}

    public static final ConcurrentHashMap<UUID, PlayerData> players = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, BukkitRunnable> pendingRemoval = new ConcurrentHashMap<>();
    public static final int defaultElo = 800;

    public static void scheduleRemoval(UUID playerId, Plugin plugin) {
        if (pendingRemoval.containsKey(playerId)) return;

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                players.remove(playerId);
                pendingRemoval.remove(playerId);
            }
        };

        task.runTaskLater(plugin, 20L * 60 * 10);
        pendingRemoval.put(playerId, task);
    }

    public static void cancelRemoval(UUID playerId) {
        BukkitRunnable task = pendingRemoval.remove(playerId);
        if (task != null) task.cancel();
    }

    public static int updateElo(PlayerData player, PlayerData opponent, double result) {
        int ratingA = player.rating;
        int ratingB = opponent.rating;
        int diff = Math.abs(ratingA - ratingB);
        int k = isProvisional(player) ? 20 : 8;
        int baseGain;
        if (diff < 50) baseGain = k;
        else if (diff < 150) baseGain = k + 2;
        else if (diff < 300) baseGain = k + 4;
        else baseGain = k + 6;
        int lowerWinGain = baseGain + (diff / 50);

        int change;
        if (result == 1.0) {
            change = computeWinChange(player, opponent);
        } else if (result == 0.0) {
            change = -computeWinChange(opponent, player);
        } else if (result == 0.5) {
            if (diff < 50) {
                change = 0;
            } else {
                int drawChange = Math.max(0, lowerWinGain - 8);
                change = ratingA < ratingB ? drawChange : -drawChange;
            }
        } else {
            change = 0;
        }

        player.rating += change;
        player.gamesPlayed += 1;
        player.lastPlayed = Instant.now();
        return change;
    }

    private static int computeWinChange(PlayerData player, PlayerData opponent) {
        int ratingA = player.rating;
        int ratingB = opponent.rating;
        int diff = Math.abs(ratingA - ratingB);
        int k = isProvisional(player) ? 20 : 8;
        int baseGain;
        if (diff < 50) baseGain = k;
        else if (diff < 150) baseGain = k + 2;
        else if (diff < 300) baseGain = k + 4;
        else baseGain = k + 6;
        int lowerWinGain = baseGain + (diff / 50);
        if (ratingA < ratingB) return lowerWinGain;
        if (ratingA > ratingB) return Math.max(2, k - diff / 150);
        return k;
    }

    public static boolean isProvisional(PlayerData player) {
        long daysSinceLast;
        if (player.lastPlayed != null) {
            daysSinceLast = Duration.between(player.lastPlayed, Instant.now()).toDays();
        } else {
            daysSinceLast = Long.MAX_VALUE;
        }
        return player.gamesPlayed < 30 || daysSinceLast > 30;
    }

    public static String getChessistName(Player player) {
        PlayerData playerData = players.get(player.getUniqueId());
        if (playerData == null) return "<gray>" + player.getName();

        int rating = playerData.rating;
        if (rating >= 2600) return "<#7d1515><bold>SGM</bold> " + player.getName();
        if (rating >= 2500) return "<red><bold>GM</bold> " + player.getName();
        if (rating >= 2400) return "<gold><bold>IM</bold> " + player.getName();
        if (rating >= 2200) return "<green><bold>NM</bold> " + player.getName();
        if (rating >= 2000) return "<blue><bold>EXPERT</bold> " + player.getName();
        return "<gray>" + player.getName();
    }
}
