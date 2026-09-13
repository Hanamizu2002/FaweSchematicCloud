package dev.themeinerlp.faweschematiccloud.commands

import com.fastasyncworldedit.core.configuration.Settings
import com.fastasyncworldedit.core.util.MainUtil
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.session.ClipboardHolder
import dev.themeinerlp.faweschematiccloud.FAWESchematicCloud
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.CompletableFuture

class LoadCommand(private val plugin: FAWESchematicCloud) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val player = sender as? Player ?: return true
        if (!player.hasPermission("worldedit.clipboard.load")) {
            player.sendMessage("Missing permission: worldedit.clipboard.load")
            return true
        }
        if (args.size !in 1..2) {
            player.sendMessage("Usage: /$label load <filename|url:key> [format]")
            return true
        }
        val filename = args[0]
        val remote = filename.startsWith("url:", ignoreCase = true)
        if (remote && !player.hasPermission("worldedit.schematic.load.web")) {
            player.sendMessage("Missing permission: worldedit.schematic.load.web")
            return true
        }
        val key = filename.substringAfter(':', "")
        if (remote && !key.matches(Regex("[0-9a-f]{32}"))) {
            player.sendMessage("Invalid Arkitektonika download key (expected 32 lowercase hexadecimal characters).")
            return true
        }
        val format = ClipboardFormats.findByAlias(args.getOrElse(1) { "fast" })
        if (format == null) {
            player.sendMessage("Unknown schematic format.")
            return true
        }
        val actor = BukkitAdapter.adapt(player)
        val worldEdit = WorldEdit.getInstance()
        val session = worldEdit.sessionManager[actor]
        val saveDir = worldEdit.getWorkingDirectoryPath(worldEdit.configuration.saveDir).toFile().canonicalFile
        val perPlayer = Settings.settings().PATHS.PER_PLAYER_SCHEMATICS
        val playerDir = if (perPlayer) File(saveDir, player.uniqueId.toString()).canonicalFile else saveDir
        val allowedDir = if (player.hasPermission("worldedit.schematic.load.other")) saveDir else playerDir
        val downloadUrl = plugin.config.getString("arkitektonika.downloadUrl")
        player.sendMessage("Loading schematic...")
        CompletableFuture.supplyAsync({
            if (remote) {
                val url = URI(requireNotNull(downloadUrl) { "Missing arkitektonika.downloadUrl" }.replace("{key}", key)).toURL()
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                try {
                    if (connection.responseCode != 200) throw IOException("Download failed (HTTP ${connection.responseCode})")
                    connection.inputStream.use { input -> format.getReader(input).use { it.read() } }
                } finally {
                    connection.disconnect()
                }
            } else {
                val file = MainUtil.resolve(playerDir, filename, format, false).canonicalFile
                require(file.toPath().startsWith(allowedDir.toPath())) { "Schematic is outside the permitted directory" }
                if (!file.isFile) throw IOException("Schematic does not exist")
                val detectedFormat = ClipboardFormats.findByFile(file) ?: throw IOException("Unknown schematic format")
                file.inputStream().use { input -> detectedFormat.getReader(input).use { it.read() } }
            }
        }, plugin.ioExecutor).whenComplete { clipboard, error ->
            if (!plugin.isEnabled) {
                clipboard?.close()
            } else plugin.onMainThread {
                if (error != null) {
                    player.sendMessage("Failed to load schematic: ${(error.cause ?: error).message}")
                } else if (player.isOnline) {
                    session.clipboard = ClipboardHolder(clipboard)
                    player.sendMessage("Schematic loaded. Use //paste to paste it.")
                } else clipboard.close()
            }
        }
        return true
    }
}
