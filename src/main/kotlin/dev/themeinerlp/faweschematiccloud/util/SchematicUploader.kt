package dev.themeinerlp.faweschematiccloud.util

import com.intellectualsites.arkitektonika.Arkitektonika
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat
import dev.themeinerlp.faweschematiccloud.FAWESchematicCloud
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

class SchematicUploader(
    private val faweSchematicCloud: FAWESchematicCloud
) {

    private val logger: Logger = LogManager.getLogger("FAWESchematicCloud/${SchematicUploader::class.java.simpleName}")
    private val tempDir = faweSchematicCloud.dataFolder.toPath()
    private val arkitektonika: Arkitektonika by lazy {
        val backendUrl =
            faweSchematicCloud.config.getString("arkitektonika.backendUrl") ?: throw NullPointerException("Arkitektonika Backend Url not found")
        Arkitektonika.builder().withUrl(backendUrl).withExecutorService(faweSchematicCloud.ioExecutor).build()
    }

    private val downloadUrl = requireNotNull(faweSchematicCloud.config.getString("web.downloadUrl")) {
        "Missing web.downloadUrl"
    }

    fun upload(clipboard: Clipboard, format: ClipboardFormat): CompletableFuture<String> {
        return CompletableFuture.supplyAsync({ writeToTempFile(clipboard, format) }, faweSchematicCloud.ioExecutor)
            .thenCompose { file ->
                try {
                    arkitektonika.upload(file.toFile()).whenComplete { _, _ -> deleteTempFile(file) }
                } catch (e: Exception) {
                    deleteTempFile(file)
                    throw e
                }
            }
            .thenApply { keys -> downloadUrl.replace("{key}", keys.accessKey) }
    }

    private fun deleteTempFile(file: Path) {
        try {
            Files.deleteIfExists(file)
        } catch (e: IOException) {
            logger.warn("Failed to delete temporary schematic", e)
        }
    }

    private fun writeToTempFile(clipboard: Clipboard, format: ClipboardFormat): Path {
        Files.createDirectories(tempDir)
        val file = Files.createTempFile(tempDir, "schematic-", ".schem")
        try {
            Files.newOutputStream(file).use { output ->
                format.getWriter(output).use { writer -> writer.write(clipboard) }
            }
            return file
        } catch (e: Exception) {
            deleteTempFile(file)
            throw e
        }
    }

}
