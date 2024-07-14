package work.alsace.faweschematiccloud.commands

import com.fastasyncworldedit.core.configuration.Caption
import com.fastasyncworldedit.core.extent.clipboard.MultiClipboardHolder
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat
import com.sk89q.worldedit.util.formatting.text.event.ClickEvent
import work.alsace.faweschematiccloud.FAWESchematicCloud
import work.alsace.faweschematiccloud.util.SchematicHolder
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class DownloadCommand(
    private val faweSchematicCloud: FAWESchematicCloud
) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        val actor = BukkitAdapter.adapt(sender)
        if (!sender.hasPermission("worldedit.clipboard.download")) {
            actor.print(Caption.of("worldedit.command.permissions"))
            return false
        }
        if (args != null && args.isNotEmpty()) {
            actor.print(Caption.of("usage: //download"))
            return false
        }
        val format: BuiltInClipboardFormat = BuiltInClipboardFormat.FAST

        val sessionManager = WorldEdit.getInstance().sessionManager
        val session = sessionManager[actor]
        val clipboard = session.clipboard
        if (clipboard !is MultiClipboardHolder && clipboard != null) {
            actor.print(Caption.of("fawe.web.generating.link", format))
            val schematicHolder = SchematicHolder(clipboard, format)
            faweSchematicCloud.schematicUploader.upload(schematicHolder).whenComplete { result, throwable ->
                if (throwable != null || !result.success) {
                    actor.print(Caption.of("fawe.web.generating.link.failed"))
                    return@whenComplete
                } else {
                    val download = result.downloadUrl!!
                    val frontEndDownload = result.downloadUrl
                    actor.print(
                        Caption.of("fawe.web.download.link", frontEndDownload).clickEvent(ClickEvent.openUrl(download))
                    )
                }
            }
        } else {
            actor.print(Caption.of("fawe.error.no-clipboard"))
            return false
        }
        return true
    }
}
