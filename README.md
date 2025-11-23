# BlockChess

BlockChess is a Minecraft plugin that brings competitive chess to your server. Play against other players, challenge the AI, or join the matchmaking queue for ELO-rated matches—all within an intuitive inventory-based GUI.

## Features

### Game Modes
- **Player vs. Player:** Challenge players directly via invitations.
- **Matchmaking System:** Join the queue for automatic ELO-based pairing with balanced opponents
- **Player vs. Bot:** Face off against Fairy-Stockfish AI with 12 adjustable difficulty levels (200-3000+ ELO)

### ELO Rating System
- **Dynamic Rating:** Earn or lose rating based on match results and opponent strength
- **Player Titles:** Unlock prestigious titles as you improve:
  - **SGM** (Super Grandmaster) - 2600+ ELO
  - **GM** (Grandmaster) - 2500+ ELO
  - **IM** (International Master) - 2400+ ELO
  - **NM** (National Master) - 2200+ ELO
  - **EXPERT** - 2000+ ELO
- **Statistics Tracking:** View detailed stats including games played, wins, losses, draws, and current rating

### Gameplay Features
- **Intuitive GUI:** Click-based interface with visual chessboard in inventory
- **Real-Time Updates:** Instant move synchronization between players
- **Full Chess Rules:** Complete implementation including castling, en passant, and pawn promotion
- **Draw Offers:** Request and accept/decline draws during matches
- **Surrender Option:** Resign from matches when needed
- **Auto-Save Inventory:** Player inventories are saved and restored after matches

### Matchmaking
- **Smart Pairing:** Automatically matches players with similar ELO ratings
- **Dynamic Range:** Search range expands over time to ensure matches are found

## Installation

1. **Download:** Get the latest version of BlockChess from the project's releases page.
2. **Install:** Place the downloaded `.jar` file into your server's `plugins` directory.
3. **Stockfish:** Download the appropriate Fairy-Stockfish executable for your server's operating system from the [Fairy-Stockfish releases page](https://github.com/fairy-stockfish/Fairy-Stockfish/releases).
4. **Configure:** Place the Stockfish executable in the `plugins/BlockChess` directory and ensure the `engine.path` in the `config.yml` file is set correctly.
5. **Restart:** Restart your server to enable the plugin.

## Usage

### Commands

**General Commands:**
- `/chess` - Open the main chess menu GUI
- `/chess info <player>` - View a player's chess statistics and rating

**Player vs Player:**
- `/chess invite <player>` - Invite a player to a chess match
- `/chess accept <player>` - Accept a chess match invitation
- `/chess decline <player>` - Decline a chess match invitation

**Matchmaking:**
- `/chess leavequeue` - Leave the matchmaking queue

**Player vs Bot:**
- `/chess bot <1-12>` - Start a match against the AI (difficulty 1-12)

**Admin Commands:**
- `/chess admin elo set <player> <amount>` - Set a player's ELO rating
- `/chess admin elo add <player> <amount>` - Add ELO to a player's rating
- `/chess admin elo remove <player> <amount>` - Remove ELO from a player's rating

## API Reference

BlockChess does not currently expose a public API for other plugins to use.

## Configuration

The `config.yml` file allows you to configure the following options:

- `resourcepack.enabled` - (Default: `true`) Enable or disable the custom resource pack for chess pieces
- `engine.path` - (Default: `plugins/BlockChess/stockfish.exe`) Path to the Fairy-Stockfish executable
- `bot_gamemode.enabled` - (Default: `true`) Enable or disable the bot gamemode feature

## Permissions

BlockChess includes a comprehensive permission system:

**Player Permissions (default: true):**
- `blockchess.use` - Open the main chess GUI
- `blockchess.info` - View player statistics
- `blockchess.invite` - Invite players to matches
- `blockchess.accept` - Accept match invitations
- `blockchess.decline` - Decline match invitations
- `blockchess.bot` - Play against the AI
- `blockchess.leavequeue` - Leave matchmaking queue

**Admin Permissions (default: op):**
- `blockchess.admin` - Access to all admin commands
- `blockchess.admin.elo.set` - Set player ELO ratings
- `blockchess.admin.elo.add` - Add ELO to players
- `blockchess.admin.elo.remove` - Remove ELO from players
- `blockchess.*` - All BlockChess permissions

## Engine Communication

BlockChess communicates with the chess engine using the Universal Chess Interface (UCI) protocol. This allows the plugin to be compatible with any UCI-compliant chess engine, including the recommended Fairy-Stockfish.

### Engine Strength

The bot's strength can be set from level 1 to 12. The simulation of strength is achieved through a combination of UCI parameters, including ELO rating and move time limits.

For levels 1 through 4, the engine's thinking time is artificially limited to simulate a weaker opponent:

- **Level 1:** 50ms per move
- **Level 2:** 100ms per move
- **Level 3:** 150ms per move
- **Level 4:** 200ms per move

For levels 5 and above, the engine uses the standard game timers (`wtime` and `btime`) to manage its thinking time, resulting in stronger play.

Additionally, for levels 1 through 11, the engine's strength is limited by setting the `UCI_LimitStrength` option to `true` and providing a specific ELO value. For level 12, `UCI_LimitStrength` is set to `false`, allowing the engine to play at its maximum potential.

Here is a breakdown of the ELO rating for each level:

| Level | ELO Rating |
|-------|------------|
| 1     | 200        |
| 2     | 500        |
| 3     | 820        |
| 4     | 1060       |
| 5     | 1350       |
| 6     | 1600       |
| 7     | 1900       |
| 8     | 2200       |
| 9     | 2500       |
| 10    | 2800       |
| 11    | 3050       |
| 12    | Max        |

## Contribution Guidelines

We welcome contributions to BlockChess! If you'd like to contribute, please follow these steps:

1. Fork the repository.
2. Create a new branch for your feature or bug fix.
3. Make your changes and ensure the code is well-formatted and documented.
4. Submit a pull request with a clear description of your changes.

## Attribution

BlockChess utilizes the following open-source projects:

- **Fairy-Stockfish:** A powerful chess engine that provides the bot's intelligence.
  - **GitHub Repository:** [https://github.com/fairy-stockfish/Fairy-Stockfish](https://github.com/fairy-stockfish/Fairy-Stockfish)
  - **License:** GNU General Public License v3.0

- **chesslib:** A Java chess library used for move generation, validation, and game state management.
  - **GitHub Repository:** [https://github.com/bhlangonijr/chesslib](https://github.com/bhlangonijr/chesslib)
  - **License:** Apache License 2.0