package dev.themeinerlp.faweschematiccloud.commands

import dev.themeinerlp.faweschematiccloud.FAWESchematicCloud
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class SchemCloudCommand(plugin: FAWESchematicCloud) : TabExecutor {
    private val download = DownloadCommand(plugin)
    private val load = LoadCommand(plugin)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command can only be used by players.")
            return true
        }
        when (args.firstOrNull()?.lowercase()) {
            "download" -> download.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "load" -> load.onCommand(sender, command, label, args.drop(1).toTypedArray())
            else -> sender.sendMessage("Usage: /$label download | /$label load <filename|url:key> [format]")
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> =
        if (sender is Player && args.size == 1) listOf("download", "load").filter {
            it.startsWith(args[0], ignoreCase = true) && sender.hasPermission("worldedit.clipboard.$it")
        } else emptyList()
}
