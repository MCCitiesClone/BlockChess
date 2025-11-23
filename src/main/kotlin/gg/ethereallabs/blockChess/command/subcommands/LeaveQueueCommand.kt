package gg.ethereallabs.blockChess.command.subcommands

import gg.ethereallabs.blockChess.BlockChess
import gg.ethereallabs.blockChess.command.abstract.BaseCommand
import gg.ethereallabs.blockChess.matchmaking.MatchmakingManager
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class LeaveQueueCommand : BaseCommand("leavequeue") {
    override fun execute(
        sender: CommandSender,
        args: Array<out String>
    ): Boolean {
        if(sender !is Player) {
            BlockChess.instance.sendMessage("<red>This command can only be executed by players!", sender)
            return true
        }

        if (!MatchmakingManager.isInQueue(sender.uniqueId)) {
            BlockChess.instance.sendMessage("<red>You are not in the matchmaking queue!", sender)
            return true
        }

        MatchmakingManager.leaveQueue(sender.uniqueId)
        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return emptyList()
    }
}
