package dev.themeinerlp.faweschematiccloud

import dev.themeinerlp.faweschematiccloud.commands.SchemCloudCommand
import dev.themeinerlp.faweschematiccloud.util.SchematicUploader
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.Executors

class FAWESchematicCloud : JavaPlugin() {
    val ioExecutor = Executors.newFixedThreadPool(2)
    lateinit var schematicUploader: SchematicUploader
        private set

    override fun onEnable() {
        saveDefaultConfig()
        schematicUploader = SchematicUploader(this)
        val handler = SchemCloudCommand(this)
        requireNotNull(getCommand("schemcloud")).apply {
            setExecutor(handler)
            tabCompleter = handler
        }
    }

    fun onMainThread(action: () -> Unit) {
        if (isEnabled) server.scheduler.runTask(this, Runnable { if (isEnabled) action() })
    }

    override fun onDisable() {
        ioExecutor.shutdownNow()
    }
}
