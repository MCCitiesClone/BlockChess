package gg.ethereallabs.blockChess.matchmaking

import gg.ethereallabs.blockChess.BlockChess
import gg.ethereallabs.blockChess.elo.EloManager
import gg.ethereallabs.blockChess.game.GameManager
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Represents a player in the matchmaking queue
 */
data class MatchmakingEntry(
    val playerId: UUID,
    val playerElo: Int,
    var searchRange: Int,
    val joinedAt: Long
)

/**
 * Manages the online matchmaking system with dynamic elo range expansion
 */
object MatchmakingManager {

    private val queue: MutableMap<UUID, MatchmakingEntry> = ConcurrentHashMap()
    private var task: BukkitTask? = null

    private const val INITIAL_RANGE = 100
    private const val RANGE_INCREMENT = 200
    private const val EXPANSION_INTERVAL = 120L

    /**
     * Adds a player to the matchmaking queue
     */
    fun joinQueue(player: Player) {
        // Check if player is already in a game
        if (GameManager.getGame(player) != null) {
            BlockChess.instance.sendMessage("<red>You are already in a match!", player)
            return
        }

        // Check if player is already in queue
        if (queue.containsKey(player.uniqueId)) {
            BlockChess.instance.sendMessage("<yellow>You are already in the matchmaking queue!", player)
            return
        }

        // Get player's elo
        val playerData = EloManager.players[player.uniqueId]
        val elo = playerData?.rating ?: EloManager.defaultElo

        // Add to queue
        val entry = MatchmakingEntry(
            playerId = player.uniqueId,
            playerElo = elo,
            searchRange = INITIAL_RANGE,
            joinedAt = System.currentTimeMillis()
        )
        queue[player.uniqueId] = entry

        BlockChess.instance.sendMessage("<green>✓ Joined matchmaking queue! <yellow>Type /chess leavequeue to leave the matchmaking.", player)
        BlockChess.instance.sendMessage("<gray>Your rating: <yellow>$elo", player)
        BlockChess.instance.sendMessage("<gray>Searching for opponents...", player)
        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f)

        // Try to find a match immediately
        findMatches()

        // Start the periodic task if not already running
        if (task == null || task?.isCancelled == true) {
            startMatchmakingTask()
        }
    }

    /**
     * Removes a player from the matchmaking queue
     */
    fun leaveQueue(playerId: UUID): Boolean {
        val removed = queue.remove(playerId)
        if (removed != null) {
            val player = Bukkit.getPlayer(playerId)
            if (player != null && player.isOnline) {
                BlockChess.instance.sendMessage("<yellow>Left matchmaking queue.", player)
                player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.8f)
            }

            // Stop task if queue is empty
            if (queue.isEmpty()) {
                stopMatchmakingTask()
            }
            return true
        }
        return false
    }

    /**
     * Checks if a player is in the matchmaking queue
     */
    fun isInQueue(playerId: UUID): Boolean {
        return queue.containsKey(playerId)
    }

    /**
     * Finds and creates matches between players in the queue
     * Prioritizes players with closest elo ratings
     */
    private fun findMatches() {
        if (queue.size < 2) return

        // Get all entries sorted by elo for efficient matching
        val entries = queue.values.toList()
        val matched = mutableSetOf<UUID>()

        // Find all possible pairs and their elo differences
        val possibleMatches = mutableListOf<Triple<MatchmakingEntry, MatchmakingEntry, Int>>()

        for (i in entries.indices) {
            if (matched.contains(entries[i].playerId)) continue

            for (j in i + 1 until entries.size) {
                if (matched.contains(entries[j].playerId)) continue

                val entry1 = entries[i]
                val entry2 = entries[j]
                val eloDiff = abs(entry1.playerElo - entry2.playerElo)

                // Check if both players are in each other's search range
                if (eloDiff <= entry1.searchRange && eloDiff <= entry2.searchRange) {
                    possibleMatches.add(Triple(entry1, entry2, eloDiff))
                }
            }
        }

        // Sort by elo difference (closest elo first)
        possibleMatches.sortBy { it.third }

        // Create matches starting from closest elo pairs
        for ((entry1, entry2, eloDiff) in possibleMatches) {
            if (matched.contains(entry1.playerId) || matched.contains(entry2.playerId)) {
                continue
            }

            val player1 = Bukkit.getPlayer(entry1.playerId)
            val player2 = Bukkit.getPlayer(entry2.playerId)

            if (player1 != null && player1.isOnline && player2 != null && player2.isOnline) {
                // Mark as matched
                matched.add(entry1.playerId)
                matched.add(entry2.playerId)

                // Remove from queue
                queue.remove(entry1.playerId)
                queue.remove(entry2.playerId)

                // Create the match
                createMatch(player1, player2, eloDiff)
            }
        }

        // Stop task if queue is empty
        if (queue.isEmpty()) {
            stopMatchmakingTask()
        }
    }

    /**
     * Creates a match between two players
     */
    private fun createMatch(player1: Player, player2: Player, eloDiff: Int) {
        // Randomly decide who plays white
        val (white, black) = if (Random().nextBoolean()) {
            Pair(player1, player2)
        } else {
            Pair(player2, player1)
        }

        val whiteName = EloManager.getChessistName(white)
        val blackName = EloManager.getChessistName(black)

        // Notify both players
        BlockChess.instance.sendMessage("<green>✓ Match found! (Elo difference: $eloDiff)", player1, player2)
        player1.playSound(player1.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f)
        player2.playSound(player2.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f)

        // Start the game
        Bukkit.getScheduler().runTaskLater(BlockChess.instance, Runnable {
            val success = GameManager.startMatchmakingGame(white, black)
            if (success) {
                BlockChess.instance.sendMessage("<yellow>Match started! White: <white>$whiteName</white>", white)
                BlockChess.instance.sendMessage("<yellow>Match started! Black: <dark_gray>$blackName</dark_gray>", black)
            } else {
                BlockChess.instance.sendMessage("<red>Failed to start match. One of the players might be in another game.", white, black)
            }
        }, 20L) // Start after 1 second
    }

    /**
     * Starts the periodic matchmaking task
     */
    private fun startMatchmakingTask() {
        task = Bukkit.getScheduler().runTaskTimer(BlockChess.instance, Runnable {
            // Expand search range for all players in queue
            for (entry in queue.values) {
                entry.searchRange += RANGE_INCREMENT

                val player = Bukkit.getPlayer(entry.playerId)
                if (player != null && player.isOnline) {
                    val timeInQueue = (System.currentTimeMillis() - entry.joinedAt) / 1000
                    BlockChess.instance.sendMessage(
                        "<gray>Still searching... range: ±${entry.searchRange})",
                        player
                    )
                }
            }

            // Try to find matches with expanded ranges
            findMatches()

            // Clean up offline players
            queue.entries.removeIf { (_, entry) ->
                val player = Bukkit.getPlayer(entry.playerId)
                player == null || !player.isOnline
            }

            // Stop if queue is empty
            if (queue.isEmpty()) {
                stopMatchmakingTask()
            }
        }, EXPANSION_INTERVAL, EXPANSION_INTERVAL)
    }

    /**
     * Stops the periodic matchmaking task
     */
    private fun stopMatchmakingTask() {
        task?.cancel()
        task = null
    }

    /**
     * Clears the entire matchmaking queue
     */
    fun clearQueue() {
        queue.clear()
        stopMatchmakingTask()
    }

    /**
     * Gets the current queue size
     */
    fun getQueueSize(): Int = queue.size
}
