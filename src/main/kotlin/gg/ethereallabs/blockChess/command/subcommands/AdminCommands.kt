package gg.ethereallabs.blockChess.command.subcommands

import gg.ethereallabs.blockChess.BlockChess
import gg.ethereallabs.blockChess.command.abstract.BaseCommand
import gg.ethereallabs.blockChess.data.LocalStorage
import gg.ethereallabs.blockChess.elo.EloManager
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class AdminCommands : BaseCommand("admin") {
    override fun execute(
        sender: CommandSender,
        args: Array<out String>
    ): Boolean {
        if (!sender.hasPermission("blockchess.admin")) {
            BlockChess.instance.sendMessage("<red>You don't have permission to use this command!", sender)
            return true
        }

        if(args.isEmpty()){
            return true
        }

        when(args[0]){
            "elo" -> {
                if (args.size < 2) {
                    BlockChess.instance.sendMessage("<red>Usage: /chess admin elo <set|add|remove> <player> <amount>", sender)
                    return true
                }
                when(args[1]){
                    "set" -> {
                        if (!sender.hasPermission("blockchess.admin.elo.set")) {
                            BlockChess.instance.sendMessage("<red>You don't have permission to use this command!", sender)
                            return true
                        }
                        handleSetElo(sender, args)
                    }
                    "add" -> {
                        if (!sender.hasPermission("blockchess.admin.elo.add")) {
                            BlockChess.instance.sendMessage("<red>You don't have permission to use this command!", sender)
                            return true
                        }
                        handleAddElo(sender, args)
                    }
                    "remove" -> {
                        if (!sender.hasPermission("blockchess.admin.elo.remove")) {
                            BlockChess.instance.sendMessage("<red>You don't have permission to use this command!", sender)
                            return true
                        }
                        handleRemoveElo(sender, args)
                    }
                    else -> BlockChess.instance.sendMessage("<red>Unknown subcommand! Use: set, add, or remove", sender)
                }
            }
        }

        return true
    }

    fun handleSetElo(sender : CommandSender, args: Array<out String>){
        if(args.size < 3){
            return
        }
        val target = sender.server.getPlayerExact(args[2])
        if (target == null) {
            BlockChess.instance.sendMessage("<red>This player is not online: ${args[3]}", sender)
            return
        }

        val amount = args[3].toInt()

        if(EloManager.players[target.uniqueId] == null){
            LocalStorage.loadPlayerData(sender as Player)
        }

        EloManager.players[target.uniqueId]?.rating = amount

        LocalStorage.savePlayerData(target)

        BlockChess.instance.sendMessage("<yellow>You've set <gray>${target.name}'s</gray> ELO to <gray>$amount</gray>", sender)
        BlockChess.instance.sendMessage("<yellow>An admin has set your ELO to <gray>$amount</gray>", sender)
    }

    fun handleAddElo(sender : CommandSender, args: Array<out String>){
        if(args.size < 3){
            return
        }
        val target = sender.server.getPlayerExact(args[2])
        if (target == null) {
            BlockChess.instance.sendMessage("<red>This player is not online: ${args[3]}", sender)
            return
        }

        val amount = args[3].toInt()

        if(EloManager.players[target.uniqueId] == null){
            LocalStorage.loadPlayerData(sender as Player)
        }

        EloManager.players[target.uniqueId]?.rating += amount

        BlockChess.instance.sendMessage("<yellow>You've added <gray>$amount</gray> ELO to <gray>$target.name</gray>", sender)
        BlockChess.instance.sendMessage("<yellow>An admin has increased your ELO of <gray>$amount</gray>", sender)
    }

    fun handleRemoveElo(sender : CommandSender, args: Array<out String>){
        if(args.size < 3){
            return
        }
        val target = sender.server.getPlayerExact(args[2])
        if (target == null) {
            BlockChess.instance.sendMessage("<red>This player is not online: ${args[3]}", sender)
            return
        }

        val amount = args[3].toInt()

        if(EloManager.players[target.uniqueId] == null){
            LocalStorage.loadPlayerData(sender as Player)
        }

        EloManager.players[target.uniqueId]?.rating += amount

        BlockChess.instance.sendMessage("<yellow>You've removed <gray>$amount</gray> ELO to <gray>$target.name</gray>", sender)
        BlockChess.instance.sendMessage("<yellow>An admin has decreased your ELO of <gray>$amount</gray>", sender)
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if (args.isEmpty()) {
            return listOf("elo")
        }

        when (args.size) {
            1 -> {
                return listOf("elo").filter { it.startsWith(args[0], ignoreCase = true) }
            }
            2 -> {
                if (args[0].equals("elo", ignoreCase = true)) {
                    return listOf("set", "add", "remove").filter { it.startsWith(args[1], ignoreCase = true) }
                }
            }
            3 -> {
                if (args[0].equals("elo", ignoreCase = true)) {
                    val partial = args[2]
                    return Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(partial, ignoreCase = true) }
                }
            }
            4 -> {
                if (args[0].equals("elo", ignoreCase = true)) {
                    return listOf("<amount>")
                }
            }
        }
        return emptyList()
    }
}