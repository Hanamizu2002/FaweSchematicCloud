package dev.themeinerlp.faweschematiccloud

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.command.SchematicCommands
import dev.themeinerlp.faweschematiccloud.commands.*
import dev.themeinerlp.faweschematiccloud.util.SchematicUploader
import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabExecutor
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
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
        registerCommand("/schemdownload", DownloadCommand(this))
        registerCommand("/schemload" ,LoadCommand(this))
        registerCommand("/schemloadall", LoadAllCommand(this))
        registerCommand("/schemclear", ClearCommand(this))
        registerCommand("/schemunload", UnloadCommand(this))
        registerCommand("/schemmove",MoveCommand(this))
        registerCommand("/schemsave",SaveCommand(this))
        registerCommand("/schemformats", FormatsCommand(this))
        registerCommand("/schemlist", ListCommand(this))
        registerCommand("/schemdel", DeleteCommand(this))
    }

    private fun registerCommand(command: String, executor: CommandExecutor) {
        Objects.requireNonNull(getCommand(command))?.setExecutor(executor)
    }

}
