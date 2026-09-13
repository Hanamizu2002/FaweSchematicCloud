package dev.themeinerlp.faweschematiccloud.commands

import com.fastasyncworldedit.core.configuration.Caption
import com.fastasyncworldedit.core.extent.clipboard.MultiClipboardHolder
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat
import com.sk89q.worldedit.util.formatting.text.event.ClickEvent
import dev.themeinerlp.faweschematiccloud.FAWESchematicCloud
import dev.themeinerlp.faweschematiccloud.util.SchematicHolder
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class DownloadCommand(
    private val faweSchematicCloud: FAWESchematicCloud
) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val actor = BukkitAdapter.adapt(sender)
        if (!sender.hasPermission("worldedit.clipboard.download")) {
            actor.print(Caption.of("worldedit.command.permissions"))
            return false
        }
        if (args.isNotEmpty()) {
            actor.print(Caption.of("Usage: /$label download"))
            return false
        }
        val format: BuiltInClipboardFormat = BuiltInClipboardFormat.FAST_V3

        val sessionManager = WorldEdit.getInstance().sessionManager
        val session = sessionManager[actor]
        val clipboard = try { session.clipboard } catch (_: com.sk89q.worldedit.EmptyClipboardException) { null }
        if (clipboard !is MultiClipboardHolder && clipboard != null) {
            actor.print(Caption.of("fawe.web.generating.link", format))
            val schematicHolder = SchematicHolder(clipboard, format)
            faweSchematicCloud.schematicUploader.upload(schematicHolder).whenComplete { result, throwable ->
                faweSchematicCloud.onMainThread {
                    if (throwable != null || result == null || !result.success) {
                        actor.print(Caption.of("fawe.web.generating.link.failed"))
                        faweSchematicCloud.logger.warning("Schematic upload failed: ${throwable?.cause?.message ?: throwable?.message ?: "empty response"}")
                    } else {
                        val download = requireNotNull(result.downloadUrl)
                        actor.print(Caption.of("fawe.web.download.link", download).clickEvent(ClickEvent.openUrl(download)))
                    }
                }
            }
        } else {
            actor.print(Caption.of("fawe.error.no-clipboard"))
            return false
        }
        return true
    }
}
