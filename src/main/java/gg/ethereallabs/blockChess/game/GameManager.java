package gg.ethereallabs.blockChess.game;

import gg.ethereallabs.blockChess.BlockChess;
import gg.ethereallabs.blockChess.elo.EloManager;
import gg.ethereallabs.blockChess.engine.EnemyData;
import gg.ethereallabs.blockChess.gui.GameGUI;
import gg.ethereallabs.blockChess.gui.subgui.AcceptDrawGUI;
import gg.ethereallabs.blockChess.gui.subgui.PromotionGUI;
import gg.ethereallabs.blockChess.gui.subgui.RequestDrawGUI;
import gg.ethereallabs.blockChess.gui.subgui.SurrendGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {
    private GameManager() {}

    private static final Map<UUID, Map<UUID, Invitation>> pendingInvites = new ConcurrentHashMap<>();
    private static final Map<UUID, Game> activeGamesByPlayer = new ConcurrentHashMap<>();

    public static final Map<UUID, GameGUI> playersAwaitingInvRestore = new ConcurrentHashMap<>();

    public static final Map<UUID, PromotionGUI> playersPromoting = new ConcurrentHashMap<>();
    public static final Map<UUID, SurrendGUI> playersSurrending = new ConcurrentHashMap<>();
    public static final Map<UUID, RequestDrawGUI> playersRequestingDraw = new ConcurrentHashMap<>();
    public static final Map<UUID, AcceptDrawGUI> playersAcceptingDraw = new ConcurrentHashMap<>();

    public static Game getGame(Player p) {
        return activeGamesByPlayer.get(p.getUniqueId());
    }

    public static void invite(Player inviter, Player invitee) {
        if (inviter.getUniqueId().equals(invitee.getUniqueId())) {
            BlockChess.instance.sendMessage("<red>You can't invite yourself.", inviter);
            return;
        }

        if (activeGamesByPlayer.containsKey(inviter.getUniqueId()) || activeGamesByPlayer.containsKey(invitee.getUniqueId())) {
            BlockChess.instance.sendMessage("<red>You or the invited player are already playing a match.", inviter);
            return;
        }

        String inviterName = EloManager.getChessistName(inviter);
        String inviteeName = EloManager.getChessistName(invitee);

        Map<UUID, Invitation> inviters = pendingInvites.computeIfAbsent(invitee.getUniqueId(), k -> new ConcurrentHashMap<>());
        if (inviters.containsKey(inviter.getUniqueId())) {
            BlockChess.instance.sendMessage("<red>You already have a pending invite to " + inviteeName + ".", inviter);
            return;
        }
        inviters.put(inviter.getUniqueId(), new Invitation(inviter.getUniqueId(), invitee.getUniqueId()));

        BlockChess.instance.sendMessage("<yellow>Invite sent to " + inviteeName + ".", inviter);
        BlockChess.instance.sendMessage("<yellow>" + inviterName + "</yellow> invited you to play chess (5+0).", invitee);
        BlockChess.instance.sendMessage("<gray>Use <yellow>/chess accept " + inviter.getName() + "</yellow> to accept, <red>/chess decline " + inviter.getName() + "</red> to decline.", invitee);

        Bukkit.getScheduler().runTaskLater(BlockChess.instance, () -> {
            Map<UUID, Invitation> map = pendingInvites.get(invitee.getUniqueId());
            if (map != null && map.remove(inviter.getUniqueId()) != null) {
                if (map.isEmpty()) pendingInvites.remove(invitee.getUniqueId());
                BlockChess.instance.sendMessage("<yellow>The invite to " + inviteeName + " <yellow>has expired.", inviter);
                BlockChess.instance.sendMessage("<yellow>The invite from " + inviterName + " <yellow>has expired.", invitee);
            }
        }, 20L * 60);
    }

    public static boolean accept(Player invitee, Player inviter) {
        String inviterDisplayName = EloManager.getChessistName(inviter);
        String inviteeDisplayName = EloManager.getChessistName(invitee);

        Map<UUID, Invitation> inviters = pendingInvites.get(invitee.getUniqueId());
        if (inviters == null || !inviters.containsKey(inviter.getUniqueId())) {
            BlockChess.instance.sendMessage("<red>No valid invites from " + inviterDisplayName + ".", invitee);
            return false;
        }

        inviters.remove(inviter.getUniqueId());
        if (inviters.isEmpty()) pendingInvites.remove(invitee.getUniqueId());

        Game game = new Game();
        game.start(inviter, invitee);
        activeGamesByPlayer.put(inviter.getUniqueId(), game);
        activeGamesByPlayer.put(invitee.getUniqueId(), game);

        BlockChess.instance.sendMessage("<yellow>Match started against <yellow>" + inviteeDisplayName + "</yellow>. White: <white>" + inviterDisplayName + "</white>", inviter);
        BlockChess.instance.sendMessage("<yellow>Match started against <yellow>" + inviterDisplayName + "</yellow>. Black: <dark_gray>" + inviteeDisplayName + "</dark_gray>", invitee);
        return true;
    }

    public static boolean decline(Player invitee, Player inviter) {
        Map<UUID, Invitation> inviters = pendingInvites.get(invitee.getUniqueId());
        if (inviters == null || !inviters.containsKey(inviter.getUniqueId())) return false;
        inviters.remove(inviter.getUniqueId());
        if (inviters.isEmpty()) pendingInvites.remove(invitee.getUniqueId());

        String inviterDisplayName = EloManager.getChessistName(inviter);
        String inviteeDisplayName = EloManager.getChessistName(invitee);

        BlockChess.instance.sendMessage(inviteeDisplayName + " <yellow>has declined your invite.", inviter);
        BlockChess.instance.sendMessage("<yellow>You have declined " + inviterDisplayName + "'s invite.", invitee);
        return true;
    }

    public static void end(Game game) {
        Player white = game.white;
        Player black = game.black;

        // Set the restore flag before removing from activeGamesByPlayer so that any
        // inventory close event in this tick will find it and restore correctly.
        Bukkit.getScheduler().runTaskLater(BlockChess.instance, () -> {
            if (white != null && white.isOnline()) {
                playersAwaitingInvRestore.put(white.getUniqueId(), game.guiWhite);
            }
            if (black != null && black.isOnline()) {
                playersAwaitingInvRestore.put(black.getUniqueId(), game.guiBlack);
            }
            if (white != null) activeGamesByPlayer.remove(white.getUniqueId());
            if (black != null) activeGamesByPlayer.remove(black.getUniqueId());
        }, 2L);
    }

    public static void startBot(Player player, int difficulty, EnemyData enemyData) {
        if (activeGamesByPlayer.containsKey(player.getUniqueId())) {
            player.sendMessage(BlockChess.mm.deserialize("<red>You already are in a match."));
            return;
        }
        Game game = new Game();
        game.botData = enemyData;
        String enemyName = enemyData.color() + enemyData.name();
        game.startAgainstBot(player, difficulty, true);
        activeGamesByPlayer.put(player.getUniqueId(), game);
        player.sendMessage(BlockChess.mm.deserialize("<gray>Match against " + enemyName + "<gray> started."));
    }

    public static boolean startMatchmakingGame(Player white, Player black) {
        if (activeGamesByPlayer.containsKey(white.getUniqueId()) || activeGamesByPlayer.containsKey(black.getUniqueId())) {
            return false;
        }
        Game game = new Game();
        game.start(white, black);
        activeGamesByPlayer.put(white.getUniqueId(), game);
        activeGamesByPlayer.put(black.getUniqueId(), game);
        return true;
    }
}
