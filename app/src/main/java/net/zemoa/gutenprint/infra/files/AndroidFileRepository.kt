package net.zemoa.gutenprint.infra.files

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.zemoa.gutenprint.domains.printing.CopyResult
import net.zemoa.gutenprint.domains.printing.FileFormat
import net.zemoa.gutenprint.domains.printing.FileRepository
import net.zemoa.gutenprint.domains.printing.FileSelectionError
import net.zemoa.gutenprint.domains.printing.StoredFile
import net.zemoa.gutenprint.domains.interfaces.Logger
import net.zemoa.gutenprint.domains.interfaces.NoOpLogger
import androidx.core.net.toUri

class AndroidFileRepository(
    private val resolver: ContentResolver,
    private val temporaryDirectory: File,
    private val logger: Logger = NoOpLogger,
) : FileRepository {
    private companion object {
        const val TAG = "AndroidFileRepository"
    }

    override suspend fun copy(source: String): CopyResult = withContext(Dispatchers.IO) {
        logger.debug(TAG, "Copying source URI: $source")
        val uri = source.toUri()
        val name = try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        } ?: uri.lastPathSegment ?: "document"
        val mimeType = try {
            resolver.getType(uri)
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
        val format = FileFormat.from(name, mimeType)
            ?: return@withContext CopyResult.Failure(FileSelectionError.UnsupportedFormat).also {
                logger.warning(TAG, "Unsupported file format: name=$name mimeType=$mimeType")
            }
        val input = try {
            resolver.openInputStream(uri)
        } catch (_: SecurityException) {
            null
        } catch (_: FileNotFoundException) {
            null
        } ?: return@withContext CopyResult.Failure(FileSelectionError.CannotRead).also {
            logger.warning(TAG, "Unable to open source URI: $source")
        }

        var destination: File? = null
        try {
            val created = File.createTempFile("print-", ".${format.extensions.first()}", temporaryDirectory)
            destination = created
            input.use { source -> created.outputStream().use { target -> source.copyTo(target) } }
            if (created.length() == 0L) {
                created.delete()
                CopyResult.Failure(FileSelectionError.CannotRead).also {
                    logger.warning(TAG, "Source file is empty: $source")
                }
            } else {
                CopyResult.Success(StoredFile(created, name, format))
            }
        } catch (exception: IOException) {
            destination?.delete()
            logger.warning(TAG, "Temporary copy failed: ${exception.message}")
            CopyResult.Failure(FileSelectionError.CopyFailed)
        } catch (exception: SecurityException) {
            destination?.delete()
            logger.warning(TAG, "Temporary copy denied: ${exception.message}")
            CopyResult.Failure(FileSelectionError.CopyFailed)
        }
    }

    override suspend fun delete(file: File) {
        withContext(Dispatchers.IO) {
            file.delete()
        }
    }

    fun clearTemporaryFiles(before: Long) {
        val staleFiles = temporaryDirectory.listFiles { file ->
            file.lastModified() < before &&
                (file.name.startsWith("print-") || file.name.startsWith("preview-"))
        } ?: emptyArray()
        logger.info(TAG, "Cleaning ${staleFiles.size} temporary files")
        staleFiles.forEach { file ->
            logger.debug(TAG, "Deleting temporary file: ${file.name}")
            if (!file.delete()) {
                logger.warning(TAG, "Temporary file could not be deleted: ${file.name}")
            }
        }
    }
}
