package dev.themeinerlp.faweschematiccloud.commands

import com.fastasyncworldedit.core.configuration.Caption
import com.fastasyncworldedit.core.configuration.Settings
import com.fastasyncworldedit.core.util.MainUtil
import com.sk89q.worldedit.LocalConfiguration
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.util.formatting.text.TextComponent
import dev.themeinerlp.faweschematiccloud.FAWESchematicCloud
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.util.*

class LoadCommand(
    private val faweSchematicCloud: FAWESchematicCloud
) : CommandExecutor {
    private val uuidRegex = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    private val fileRegex = Regex(".*\\.[\\w].*")

    private val apiDownloadBaseUrl = (faweSchematicCloud.config.getString("arkitektonika.downloadUrl")
        ?: throw NullPointerException("Arkitektonika Download Url not found"))

    private fun load(
        player: Player,
        rawFormatName: String,
        rawFilename: String
    ) {
        val config: LocalConfiguration = WorldEdit.getInstance().configuration
        val actor = BukkitAdapter.adapt(player)
        var format: ClipboardFormat?
        var filename: String = rawFilename
        var formatName: String = rawFormatName
        val uri: URI
        var `in`: InputStream? = null
        try {
            if (rawFormatName.startsWith("url:", true)) {
                filename = rawFormatName
                formatName = rawFilename
            }
            if (filename.startsWith("url:", true)) {
                if (!actor.hasPermission("worldedit.schematic.load.web")) {
                    actor.print(Caption.of("fawe.error.no-perm", "worldedit.schematic.load.web"))
                    return
                }
                val accessKey = filename.substringAfterLast('/')
                format = ClipboardFormats.findByAlias(formatName) ?: return
                val webUrl = URL(apiDownloadBaseUrl.replace("{key}", accessKey))
                val byteChannel: ReadableByteChannel = Channels.newChannel(webUrl.openStream())
                `in` = Channels.newInputStream(byteChannel)
                uri = webUrl.toURI()
            } else {
                val saveDir: File = WorldEdit.getInstance().getWorkingDirectoryPath(config.saveDir).toFile()
                var dir = if (Settings.settings().PATHS.PER_PLAYER_SCHEMATICS) File(
                    saveDir,
                    actor.uniqueId.toString()
                ) else saveDir
                var file: File
                if (filename.startsWith("#")) {
                    format = ClipboardFormats.findByAlias(formatName)!!
                    val extensions = format.fileExtensions?.toTypedArray<String>()
                        ?: ClipboardFormats.getFileExtensionArray()
                    file = actor.openFileOpenDialog(extensions)
                    if (!file.exists()) {
                        actor.print(Caption.of("worldedit.schematic.load.does-not-exist", TextComponent.of(filename)))
                        return
                    }
                } else {
                    if (Settings.settings().PATHS.PER_PLAYER_SCHEMATICS && !actor.hasPermission("worldedit.schematic.load.other") && uuidRegex.containsMatchIn(
                            filename
                        )
                    ) {
                        actor.print(Caption.of("fawe.error.no-perm", "worldedit.schematic.load.other"))
                        return
                    }
                    format = if (filename.matches(fileRegex)) {
                        ClipboardFormats
                            .findByExtension(filename.substring(filename.lastIndexOf('.') + 1))!!
                    } else {
                        ClipboardFormats.findByAlias(formatName)
                    }
                    file = MainUtil.resolve(dir, filename, format, false)
                }
                if (!file.exists()) {
                    if (!filename.contains("../")) {
                        dir = WorldEdit.getInstance().getWorkingDirectoryPath(config.saveDir).toFile()
                        file = MainUtil.resolve(dir, filename, format, false)
                    }
                }
                if (!file.exists() || !MainUtil.isInSubDirectory(saveDir, file)) {
                    actor.printError(
                        TextComponent.of(
                            "Schematic " + filename + " does not exist! (" + (file.exists()) + "|" + file + "|" + (MainUtil.isInSubDirectory(
                                saveDir,
                                file
                            )) + ")"
                        )
                    )
                    return
                }
                format = ClipboardFormats.findByFile(file)
                if (format == null) {
                    actor.print(Caption.of("worldedit.schematic.unknown-format", TextComponent.of(formatName)))
                    return
                }
                `in` = FileInputStream(file)
                uri = file.toURI()
            }
            format.hold(actor, uri, `in`)
            actor.print(Caption.of("fawe.worldedit.schematic.schematic.loaded", filename))
        } catch (e: IllegalArgumentException) {
            actor.print(Caption.of("worldedit.schematic.unknown-filename", TextComponent.of(filename)))
        } catch (e: URISyntaxException) {
            actor.print(Caption.of("worldedit.schematic.file-not-exist", TextComponent.of(Objects.toString(e.message))))
        } catch (e: IOException) {
            actor.print(Caption.of("worldedit.schematic.file-not-exist", TextComponent.of(Objects.toString(e.message))))
            faweSchematicCloud.log4JLogger.warn("Failed to load a saved clipboard", e);
        } finally {
            if (`in` != null) {
                try {
                    `in`.close()
                } catch (ignored: IOException) {
                }
            }
        }

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
        if (!sender.hasPermission("worldedit.clipboard.load")) {
            actor.print(Caption.of("worldedit.command.permissions"))
            return false
        }
        val player: Player = sender as Player
        if (args == null || args.isEmpty()) {
            actor.print(Caption.of("usage: //schemload <filename/url> [schem/schematic]"))
            return false
        }
        if (args.size > 2) {
            actor.print(Caption.of("usage: //schemload <filename/url> [schem/schematic]"))
            return false
        }
        val fileName = args[0]
        val formatName = args[1].ifEmpty {
            "fast"
        }
        load(player, formatName, fileName)
        return true
    }
}
