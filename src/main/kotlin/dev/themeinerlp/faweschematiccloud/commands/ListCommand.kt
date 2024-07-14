package dev.themeinerlp.faweschematiccloud.commands

import com.fastasyncworldedit.core.configuration.Caption
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import dev.themeinerlp.faweschematiccloud.FAWESchematicCloud
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ListCommand(
    private val faweSchematicCloud: FAWESchematicCloud
) : CommandExecutor {
    private fun list(
        player: Player,
        filter: String?,
        page: Int? = 1,
        oldestDate: Boolean = false,
        newestDate: Boolean = false,
        formatName: String? = "fast"
    ) {
        val sessionManager = WorldEdit.getInstance().sessionManager
        val actor = BukkitAdapter.adapt(player)
        val session = sessionManager[actor]
        faweSchematicCloud.schematicCommand.list(
            actor,
            session,
            page ?: 1,
            oldestDate,
            newestDate,
            formatName,
            filter,
            emptyArray<String>()::toString
        )
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
        if (!sender.hasPermission("worldedit.clipboard.list")) {
            actor.print(Caption.of("worldedit.command.permissions"))
            return false
        }

        val player: Player = sender as? Player ?: run {
            actor.print(Caption.of("worldedit.command.only-player"))
            return false
        }

        var filter: String? = null
        var page: Int? = 1
        var oldestDate = false
        var newestDate = false
        var formatName: String? = "fast"

        args?.let {
            if (it.isNotEmpty()) filter = it.getOrNull(0)
            if (it.size > 1) page = it.getOrNull(1)?.toIntOrNull() ?: 1
            if (it.size > 2) oldestDate = it.getOrNull(2)?.toBoolean() ?: false
            if (it.size > 3) newestDate = it.getOrNull(3)?.toBoolean() ?: false
            if (it.size > 4) formatName = it.getOrNull(4)
        }

        list(player, filter, page, oldestDate, newestDate, formatName)
        return true
    }
}
