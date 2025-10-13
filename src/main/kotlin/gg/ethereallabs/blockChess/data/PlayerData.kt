package gg.ethereallabs.blockChess.data

import java.time.Instant

data class PlayerData(
    var rating: Int = 800,
    var gamesPlayed: Int = 0,
    var wins: Int = 0,
    var losses: Int = 0,
    var draws: Int = 0,
    var lastPlayed : Instant? = null
)