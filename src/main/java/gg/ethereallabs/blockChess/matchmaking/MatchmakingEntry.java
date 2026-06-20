package gg.ethereallabs.blockChess.matchmaking;

import java.util.UUID;

public class MatchmakingEntry {
    private final UUID playerId;
    private final int playerElo;
    private int searchRange;
    private final long joinedAt;

    public MatchmakingEntry(UUID playerId, int playerElo, int searchRange, long joinedAt) {
        this.playerId = playerId;
        this.playerElo = playerElo;
        this.searchRange = searchRange;
        this.joinedAt = joinedAt;
    }

    public UUID playerId() { return playerId; }
    public int playerElo() { return playerElo; }
    public int searchRange() { return searchRange; }
    public long joinedAt() { return joinedAt; }

    public void incrementSearchRange(int amount) { this.searchRange += amount; }
}
