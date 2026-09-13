package dev.themeinerlp.faweschematiccloud.commands

import com.fastasyncworldedit.core.configuration.Settings
import com.fastasyncworldedit.core.util.MainUtil
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.session.ClipboardHolder
import dev.themeinerlp.faweschematiccloud.FAWESchematicCloud
import org.bukkit.entity.Player
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.CompletableFuture

class LoadCommand(private val plugin: FAWESchematicCloud) {
    fun execute(player: Player, label: String, args: Array<out String>) {
        if (!player.hasPermission("worldedit.clipboard.load")) {
            player.sendMessage("Missing permission: worldedit.clipboard.load")
            return
        }
        if (args.size !in 1..2) {
            player.sendMessage("Usage: /$label load <filename|url:key> [format]")
            return
        }
        val filename = args[0]
        val remote = filename.startsWith("url:", ignoreCase = true)
        if (remote && !player.hasPermission("worldedit.schematic.load.web")) {
            player.sendMessage("Missing permission: worldedit.schematic.load.web")
            return
        }
        val key = filename.substringAfter(':', "")
        if (remote && !key.matches(DOWNLOAD_KEY)) {
            player.sendMessage("Invalid Arkitektonika download key (expected 32 lowercase hexadecimal characters).")
            return
        }
        val format = ClipboardFormats.findByAlias(args.getOrElse(1) { "fast" })
        if (format == null) {
            player.sendMessage("Unknown schematic format.")
            return
        }
        val actor = BukkitAdapter.adapt(player)
        val worldEdit = WorldEdit.getInstance()
        val session = worldEdit.sessionManager[actor]
        val saveDir = worldEdit.getWorkingDirectoryPath(worldEdit.configuration.saveDir).toFile()
        val perPlayer = Settings.settings().PATHS.PER_PLAYER_SCHEMATICS
        val playerDir = if (perPlayer) File(saveDir, player.uniqueId.toString()) else saveDir
        val allowedDir = if (player.hasPermission("worldedit.schematic.load.other")) saveDir else playerDir
        val downloadUrl = plugin.config.getString("arkitektonika.downloadUrl")
        player.sendMessage("Loading schematic...")
        CompletableFuture.supplyAsync({
            if (remote) {
                readRemote(requireNotNull(downloadUrl) { "Missing arkitektonika.downloadUrl" }.replace("{key}", key), format)
            } else {
                readLocal(playerDir, allowedDir, filename, format)
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
    }

    private fun readRemote(address: String, format: ClipboardFormat): Clipboard {
        val connection = URI(address).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Download failed (HTTP ${connection.responseCode})")
            }
            connection.inputStream.use { input -> format.getReader(input).use { it.read() } }
        } finally {
            connection.disconnect()
        }
    }

    private fun readLocal(directory: File, allowedDirectory: File, filename: String, format: ClipboardFormat): Clipboard {
        val file = MainUtil.resolve(directory, filename, format, false).canonicalFile
        require(file.toPath().startsWith(allowedDirectory.canonicalFile.toPath())) {
            "Schematic is outside the permitted directory"
        }
        if (!file.isFile) throw IOException("Schematic does not exist")
        val detectedFormat = ClipboardFormats.findByFile(file) ?: throw IOException("Unknown schematic format")
        return file.inputStream().use { input -> detectedFormat.getReader(input).use { it.read() } }
    }

    private companion object {
        val DOWNLOAD_KEY = Regex("[0-9a-f]{32}")
    }
}
