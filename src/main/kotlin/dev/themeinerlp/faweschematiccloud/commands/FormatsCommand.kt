package dev.themeinerlp.faweschematiccloud.commands

import com.fastasyncworldedit.core.configuration.Caption
import com.sk89q.worldedit.bukkit.BukkitAdapter
import dev.themeinerlp.faweschematiccloud.FAWESchematicCloud
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class FormatsCommand(
    private val faweSchematicCloud: FAWESchematicCloud
) : CommandExecutor {
    private fun formats(player: Player) {
        val actor = BukkitAdapter.adapt(player)
        faweSchematicCloud.schematicCommand.formats(actor)
    }

    /**
     * Executes the given command, returning its success.
     * <br></br>
     * If false is returned, then the "usage" plugin.yml entry for this command
     * (if defined) will be sent to the player.
     *
     * @param sender Source of the command
     * @param command Command which was executed
     * @param label Alias of the command which was used
     * @param args Passed command arguments
     * @return true if a valid command, otherwise false
     */
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        val actor = BukkitAdapter.adapt(sender)
        if (!sender.hasPermission("worldedit.clipboard.formats")) {
            actor.print(Caption.of("worldedit.command.permissions"))
            return false
        }
        val player: Player = sender as Player
        if (args != null) {
            actor.print(Caption.of("usage: //schemformats"))
            return false
        }
        formats(player)
        return true
    }

}
