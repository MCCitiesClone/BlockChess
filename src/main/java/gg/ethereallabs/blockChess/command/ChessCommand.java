package gg.ethereallabs.blockChess.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import gg.ethereallabs.blockChess.BlockChess;
import gg.ethereallabs.blockChess.data.LocalStorage;
import gg.ethereallabs.blockChess.data.PlayerData;
import gg.ethereallabs.blockChess.elo.EloManager;
import gg.ethereallabs.blockChess.game.GameManager;
import gg.ethereallabs.blockChess.gui.MainGUI;
import gg.ethereallabs.blockChess.matchmaking.MatchmakingManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class ChessCommand {

    public void register(Commands commands) {
        commands.register(
            Commands.literal("chess")
                .requires(src -> src.getSender().hasPermission("blockchess.use"))
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!(sender instanceof Player player)) {
                        BlockChess.instance.sendMessage("<red>This command can only be executed by players!", sender);
                        return 0;
                    }
                    new MainGUI().open(player);
                    return 1;
                })
                .then(Commands.literal("bot")
                    .requires(src -> src.getSender() instanceof Player && src.getSender().hasPermission("blockchess.bot"))
                    .then(Commands.argument("difficulty", IntegerArgumentType.integer(1, 12))
                        .executes(ctx -> {
                            // int difficulty = IntegerArgumentType.getInteger(ctx, "difficulty");
                            // GameManager.startBot((Player) ctx.getSource().getSender(), difficulty);
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("invite")
                    .requires(src -> src.getSender() instanceof Player && src.getSender().hasPermission("blockchess.invite"))
                    .then(Commands.argument("player", ArgumentTypes.player())
                        .executes(ctx -> {
                            Player sender = (Player) ctx.getSource().getSender();
                            try {
                                List<Player> targets = ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource());
                                if (targets.isEmpty()) {
                                    BlockChess.instance.sendMessage("<red>Player not found.", sender);
                                    return 0;
                                }
                                GameManager.invite(sender, targets.get(0));
                                return 1;
                            } catch (Exception e) {
                                BlockChess.instance.sendMessage("<red>Player not found.", sender);
                                return 0;
                            }
                        })
                    )
                )
                .then(Commands.literal("accept")
                    .requires(src -> src.getSender() instanceof Player && src.getSender().hasPermission("blockchess.accept"))
                    .then(Commands.argument("player", ArgumentTypes.player())
                        .executes(ctx -> {
                            Player sender = (Player) ctx.getSource().getSender();
                            try {
                                List<Player> targets = ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource());
                                if (targets.isEmpty()) {
                                    BlockChess.instance.sendMessage("<red>Player not found.", sender);
                                    return 0;
                                }
                                GameManager.accept(sender, targets.get(0));
                                return 1;
                            } catch (Exception e) {
                                BlockChess.instance.sendMessage("<red>Player not found.", sender);
                                return 0;
                            }
                        })
                    )
                )
                .then(Commands.literal("decline")
                    .requires(src -> src.getSender() instanceof Player && src.getSender().hasPermission("blockchess.decline"))
                    .then(Commands.argument("player", ArgumentTypes.player())
                        .executes(ctx -> {
                            Player sender = (Player) ctx.getSource().getSender();
                            try {
                                List<Player> targets = ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource());
                                if (targets.isEmpty()) {
                                    BlockChess.instance.sendMessage("<red>Player not found.", sender);
                                    return 0;
                                }
                                boolean ok = GameManager.decline(sender, targets.get(0));
                                if (!ok) BlockChess.instance.sendMessage("<red>No valid invite from that player.", sender);
                                return ok ? 1 : 0;
                            } catch (Exception e) {
                                BlockChess.instance.sendMessage("<red>Player not found.", sender);
                                return 0;
                            }
                        })
                    )
                )
                .then(Commands.literal("info")
                    .requires(src -> src.getSender().hasPermission("blockchess.info"))
                    .then(Commands.argument("player", ArgumentTypes.player())
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            try {
                                List<Player> targets = ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource());
                                if (targets.isEmpty()) {
                                    BlockChess.instance.sendMessage("<red>Player not found.", sender);
                                    return 0;
                                }
                                Player target = targets.get(0);
                                PlayerData data = EloManager.players.get(target.getUniqueId());
                                if (data == null) {
                                    BlockChess.instance.sendMessage("<red>Could not retrieve " + target.getName() + "'s information.", sender);
                                    return 0;
                                }
                                BlockChess.instance.sendMessage(EloManager.getChessistName(target) + "<yellow> info:", sender);
                                BlockChess.instance.sendMessage(" <gray>Elo: <yellow>" + data.rating, sender);
                                BlockChess.instance.sendMessage(" <gray>Matches Played: <yellow>" + data.gamesPlayed, sender);
                                BlockChess.instance.sendMessage(" <gray>Wins: <yellow>" + data.wins, sender);
                                BlockChess.instance.sendMessage(" <gray>Losses: <yellow>" + data.losses, sender);
                                BlockChess.instance.sendMessage(" <gray>Draws: <yellow>" + data.draws, sender);
                                return 1;
                            } catch (Exception e) {
                                BlockChess.instance.sendMessage("<red>Player not found.", sender);
                                return 0;
                            }
                        })
                    )
                )
                .then(Commands.literal("leavequeue")
                    .requires(src -> src.getSender() instanceof Player && src.getSender().hasPermission("blockchess.leavequeue"))
                    .executes(ctx -> {
                        Player sender = (Player) ctx.getSource().getSender();
                        if (!MatchmakingManager.isInQueue(sender.getUniqueId())) {
                            BlockChess.instance.sendMessage("<red>You are not in the matchmaking queue!", sender);
                            return 0;
                        }
                        MatchmakingManager.leaveQueue(sender.getUniqueId());
                        return 1;
                    })
                )
                .then(Commands.literal("admin")
                    .requires(src -> src.getSender().hasPermission("blockchess.admin"))
                    .then(Commands.literal("elo")
                        .then(Commands.literal("set")
                            .requires(src -> src.getSender().hasPermission("blockchess.admin.elo.set"))
                            .then(Commands.argument("player", ArgumentTypes.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                    .executes(ctx -> {
                                        CommandSender sender = ctx.getSource().getSender();
                                        try {
                                            List<Player> targets = ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource());
                                            if (targets.isEmpty()) { BlockChess.instance.sendMessage("<red>Player not found.", sender); return 0; }
                                            Player target = targets.get(0);
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            if (!EloManager.players.containsKey(target.getUniqueId())) LocalStorage.loadPlayerData(target);
                                            if (EloManager.players.containsKey(target.getUniqueId())) EloManager.players.get(target.getUniqueId()).rating = amount;
                                            LocalStorage.savePlayerData(target);
                                            BlockChess.instance.sendMessage("<yellow>You've set <gray>" + target.getName() + "'s</gray> ELO to <gray>" + amount + "</gray>", sender);
                                            BlockChess.instance.sendMessage("<yellow>An admin has set your ELO to <gray>" + amount + "</gray>", target);
                                            return 1;
                                        } catch (Exception e) { BlockChess.instance.sendMessage("<red>Player not found.", sender); return 0; }
                                    })
                                )
                            )
                        )
                        .then(Commands.literal("add")
                            .requires(src -> src.getSender().hasPermission("blockchess.admin.elo.add"))
                            .then(Commands.argument("player", ArgumentTypes.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                    .executes(ctx -> {
                                        CommandSender sender = ctx.getSource().getSender();
                                        try {
                                            List<Player> targets = ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource());
                                            if (targets.isEmpty()) { BlockChess.instance.sendMessage("<red>Player not found.", sender); return 0; }
                                            Player target = targets.get(0);
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            if (!EloManager.players.containsKey(target.getUniqueId())) LocalStorage.loadPlayerData(target);
                                            if (EloManager.players.containsKey(target.getUniqueId())) EloManager.players.get(target.getUniqueId()).rating += amount;
                                            BlockChess.instance.sendMessage("<yellow>You've added <gray>" + amount + "</gray> ELO to <gray>" + target.getName() + "</gray>", sender);
                                            BlockChess.instance.sendMessage("<yellow>An admin has increased your ELO by <gray>" + amount + "</gray>", target);
                                            return 1;
                                        } catch (Exception e) { BlockChess.instance.sendMessage("<red>Player not found.", sender); return 0; }
                                    })
                                )
                            )
                        )
                        .then(Commands.literal("remove")
                            .requires(src -> src.getSender().hasPermission("blockchess.admin.elo.remove"))
                            .then(Commands.argument("player", ArgumentTypes.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                    .executes(ctx -> {
                                        CommandSender sender = ctx.getSource().getSender();
                                        try {
                                            List<Player> targets = ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource());
                                            if (targets.isEmpty()) { BlockChess.instance.sendMessage("<red>Player not found.", sender); return 0; }
                                            Player target = targets.get(0);
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            if (!EloManager.players.containsKey(target.getUniqueId())) LocalStorage.loadPlayerData(target);
                                            if (EloManager.players.containsKey(target.getUniqueId())) EloManager.players.get(target.getUniqueId()).rating -= amount;
                                            BlockChess.instance.sendMessage("<yellow>You've removed <gray>" + amount + "</gray> ELO from <gray>" + target.getName() + "</gray>", sender);
                                            BlockChess.instance.sendMessage("<yellow>An admin has decreased your ELO by <gray>" + amount + "</gray>", target);
                                            return 1;
                                        } catch (Exception e) { BlockChess.instance.sendMessage("<red>Player not found.", sender); return 0; }
                                    })
                                )
                            )
                        )
                    )
                )
                .build(),
            "Main command for BlockChess",
            List.of()
        );
    }
}
