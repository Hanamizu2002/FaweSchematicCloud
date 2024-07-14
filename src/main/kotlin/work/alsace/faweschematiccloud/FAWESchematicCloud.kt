package work.alsace.faweschematiccloud

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.command.SchematicCommands
import work.alsace.faweschematiccloud.util.SchematicUploader
import org.bukkit.command.CommandExecutor
import org.bukkit.plugin.java.JavaPlugin
import work.alsace.faweschematiccloud.commands.DownloadCommand
import work.alsace.faweschematiccloud.commands.LoadCommand
import java.util.*

class FAWESchematicCloud : JavaPlugin() {

    val schematicUploader: SchematicUploader by lazy {
        SchematicUploader(this)
    }

    val schematicCommand: SchematicCommands by lazy {
        SchematicCommands(WorldEdit.getInstance())
    }

    override fun onEnable() {
        saveDefaultConfig()
        registerCommand("/download", DownloadCommand(this))
        registerCommand("/schem" , LoadCommand(this))
    }

    private fun registerCommand(command: String, executor: CommandExecutor) {
        Objects.requireNonNull(getCommand(command))?.setExecutor(executor)
    }

}
