package gg.ethereallabs.blockChess.matchmaking;

import gg.ethereallabs.blockChess.BlockChess;
import gg.ethereallabs.blockChess.elo.EloManager;
import gg.ethereallabs.blockChess.game.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MatchmakingManager {
    private MatchmakingManager() {}

    private static final Map<UUID, MatchmakingEntry> queue = new ConcurrentHashMap<>();
    private static BukkitTask task = null;

    private static final int INITIAL_RANGE = 100;
    private static final int RANGE_INCREMENT = 200;
    private static final long EXPANSION_INTERVAL = 120L;

    private record MatchCandidate(MatchmakingEntry a, MatchmakingEntry b, int eloDiff) {}

    public static void joinQueue(Player player) {
        if (GameManager.getGame(player) != null) {
            BlockChess.instance.sendMessage("<red>You are already in a match!", player);
            return;
        }

        if (queue.containsKey(player.getUniqueId())) {
            BlockChess.instance.sendMessage("<yellow>You are already in the matchmaking queue!", player);
            return;
        }

        var playerData = EloManager.players.get(player.getUniqueId());
        int elo = playerData != null ? playerData.rating : EloManager.defaultElo;

        MatchmakingEntry entry = new MatchmakingEntry(player.getUniqueId(), elo, INITIAL_RANGE, System.currentTimeMillis());
        queue.put(player.getUniqueId(), entry);

        BlockChess.instance.sendMessage("<green>✓ Joined matchmaking queue! <yellow>Type /chess leavequeue to leave the matchmaking.", player);
        BlockChess.instance.sendMessage("<gray>Your rating: <yellow>" + elo, player);
        BlockChess.instance.sendMessage("<gray>Searching for opponents...", player);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);

        findMatches();

        if (task == null || task.isCancelled()) {
            startMatchmakingTask();
        }
    }

    public static boolean leaveQueue(UUID playerId) {
        MatchmakingEntry removed = queue.remove(playerId);
        if (removed != null) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                BlockChess.instance.sendMessage("<yellow>Left matchmaking queue.", player);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.8f);
            }
            if (queue.isEmpty()) stopMatchmakingTask();
            return true;
        }
        return false;
    }

    public static boolean isInQueue(UUID playerId) {
        return queue.containsKey(playerId);
    }

    private static void findMatches() {
        if (queue.size() < 2) return;

        List<MatchmakingEntry> entries = new ArrayList<>(queue.values());
        Set<UUID> matched = new HashSet<>();

        List<MatchCandidate> possibleMatches = new ArrayList<>();

        for (int i = 0; i < entries.size(); i++) {
            if (matched.contains(entries.get(i).playerId())) continue;
            for (int j = i + 1; j < entries.size(); j++) {
                if (matched.contains(entries.get(j).playerId())) continue;

                MatchmakingEntry e1 = entries.get(i);
                MatchmakingEntry e2 = entries.get(j);
                int eloDiff = Math.abs(e1.playerElo() - e2.playerElo());

                if (eloDiff <= e1.searchRange() && eloDiff <= e2.searchRange()) {
                    possibleMatches.add(new MatchCandidate(e1, e2, eloDiff));
                }
            }
        }

        possibleMatches.sort(Comparator.comparingInt(MatchCandidate::eloDiff));

        for (MatchCandidate candidate : possibleMatches) {
            if (matched.contains(candidate.a().playerId()) || matched.contains(candidate.b().playerId())) continue;

            Player p1 = Bukkit.getPlayer(candidate.a().playerId());
            Player p2 = Bukkit.getPlayer(candidate.b().playerId());

            if (p1 != null && p1.isOnline() && p2 != null && p2.isOnline()) {
                matched.add(candidate.a().playerId());
                matched.add(candidate.b().playerId());
                queue.remove(candidate.a().playerId());
                queue.remove(candidate.b().playerId());
                createMatch(p1, p2, candidate.eloDiff());
            }
        }

        if (queue.isEmpty()) stopMatchmakingTask();
    }

    private static void createMatch(Player player1, Player player2, int eloDiff) {
        Player white, black;
        if (new Random().nextBoolean()) { white = player1; black = player2; }
        else { white = player2; black = player1; }

        String whiteName = EloManager.getChessistName(white);
        String blackName = EloManager.getChessistName(black);

        BlockChess.instance.sendMessage("<green>✓ Match found! (Elo difference: " + eloDiff + ")", player1, player2);
        player1.playSound(player1.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        player2.playSound(player2.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);

        Player finalWhite = white, finalBlack = black;
        Bukkit.getScheduler().runTaskLater(BlockChess.instance, () -> {
            boolean success = GameManager.startMatchmakingGame(finalWhite, finalBlack);
            if (success) {
                BlockChess.instance.sendMessage("<yellow>Match started! White: <white>" + whiteName + "</white>", finalWhite);
                BlockChess.instance.sendMessage("<yellow>Match started! Black: <dark_gray>" + blackName + "</dark_gray>", finalBlack);
            } else {
                BlockChess.instance.sendMessage("<red>Failed to start match. One of the players might be in another game.", finalWhite, finalBlack);
            }
        }, 20L);
    }

    private static void startMatchmakingTask() {
        task = Bukkit.getScheduler().runTaskTimer(BlockChess.instance, () -> {
            for (MatchmakingEntry entry : queue.values()) {
                entry.incrementSearchRange(RANGE_INCREMENT);

                Player player = Bukkit.getPlayer(entry.playerId());
                if (player != null && player.isOnline()) {
                    BlockChess.instance.sendMessage("<gray>Still searching... range: ±" + entry.searchRange() + ")", player);
                }
            }

            findMatches();

            queue.entrySet().removeIf(e -> {
                Player p = Bukkit.getPlayer(e.getValue().playerId());
                return p == null || !p.isOnline();
            });

            if (queue.isEmpty()) stopMatchmakingTask();
        }, EXPANSION_INTERVAL, EXPANSION_INTERVAL);
    }

    private static void stopMatchmakingTask() {
        if (task != null) task.cancel();
        task = null;
    }

    public static void clearQueue() {
        queue.clear();
        stopMatchmakingTask();
    }

    public static int getQueueSize() {
        return queue.size();
    }
}
