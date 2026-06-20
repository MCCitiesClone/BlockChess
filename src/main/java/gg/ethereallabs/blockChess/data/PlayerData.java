package gg.ethereallabs.blockChess.data;

import java.time.Instant;

public class PlayerData {
    public int rating;
    public int gamesPlayed;
    public int wins;
    public int losses;
    public int draws;
    public Instant lastPlayed;

    public PlayerData() {
        this.rating = 800;
        this.gamesPlayed = 0;
        this.wins = 0;
        this.losses = 0;
        this.draws = 0;
        this.lastPlayed = null;
    }

    public PlayerData(int rating, int gamesPlayed) {
        this();
        this.rating = rating;
        this.gamesPlayed = gamesPlayed;
    }
}
