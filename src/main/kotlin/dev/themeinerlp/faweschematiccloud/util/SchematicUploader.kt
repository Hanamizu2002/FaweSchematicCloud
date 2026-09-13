package dev.themeinerlp.faweschematiccloud.util

import com.intellectualsites.arkitektonika.Arkitektonika
import com.intellectualsites.arkitektonika.SchematicKeys
import dev.themeinerlp.faweschematiccloud.FAWESchematicCloud
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.IOException
import java.io.OutputStream
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

    fun upload(holder: SchematicHolder): CompletableFuture<SchematicUploadResult> {
        return CompletableFuture.supplyAsync({ writeToTempFile(holder) }, faweSchematicCloud.ioExecutor)
            .thenCompose { file ->
                try {
                    arkitektonika.upload(file.toFile()).whenComplete { _, _ -> deleteTempFile(file) }
                } catch (e: Exception) {
                    deleteTempFile(file)
                    throw e
                }
            }
            .thenApply(this::wrapIntoResult)
    }

    private fun wrapIntoResult(schematicKeys: SchematicKeys?): SchematicUploadResult {
        schematicKeys ?: return SchematicUploadResult(false)
        val apiDownload = (faweSchematicCloud.config.getString("arkitektonika.downloadUrl")
            ?: throw NullPointerException("Arkitektonika Download Url not found")).replace("{key}", schematicKeys.accessKey)
        val apiDelete = (faweSchematicCloud.config.getString("arkitektonika.deleteUrl")
            ?: throw NullPointerException("Arkitektonika Delete Url not found")).replace("{key}", schematicKeys.deletionKey)
        val download = (faweSchematicCloud.config.getString("web.downloadUrl")
            ?: throw NullPointerException("Web Download Url not found")).replace("{key}", schematicKeys.accessKey)
        val delete = (faweSchematicCloud.config.getString("web.deleteUrl")
            ?: throw NullPointerException("Web Delete Url not found")).replace("{key}", schematicKeys.deletionKey)
        return SchematicUploadResult(true, apiDownload, apiDelete, download, delete)
    }

    private fun deleteTempFile(file: Path) {
        try {
            Files.deleteIfExists(file)
        } catch (e: IOException) {
            logger.warn("Failed to delete temporary schematic", e)
        }
    }

    private fun writeToTempFile(holder: SchematicHolder): Path {
        Files.createDirectories(tempDir)
        val file = Files.createTempFile(tempDir, "schematic-", ".schem")
        try {
            Files.newOutputStream(file).use { writeSchematic(holder, it) }
            return file
        } catch (e: Exception) {
            deleteTempFile(file)
            throw e
        }
    }

    private fun writeSchematic(holder: SchematicHolder, outputStream: OutputStream) {
        val cb = holder.clipboard.clipboard
        holder.format.write(outputStream, cb)

    }


}
