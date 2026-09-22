package net.zemoa.gutenprint.domains.printing

import java.io.File

enum class FileFormat(val extensions: Set<String>, val mimeTypes: Set<String>) {
    PDF(setOf("pdf"), setOf("application/pdf")),
    DOCX(setOf("docx"), setOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
    JPG(setOf("jpg", "jpeg"), setOf("image/jpeg")),
    PNG(setOf("png"), setOf("image/png"));

    companion object {
        fun from(name: String?, mimeType: String?): FileFormat? {
            val extension = name?.substringAfterLast('.', "")?.lowercase()
            return entries.firstOrNull { it.extensions.contains(extension) || it.mimeTypes.contains(mimeType) }
        }
    }
}

data class StoredFile(
    val file: File,
    val displayName: String,
    val format: FileFormat,
)

sealed interface FileSelectionError {
    // The source was identified, but its format is outside the first-release contract.
    data object UnsupportedFormat : FileSelectionError
    // The source could not be opened or contained no readable data.
    data object CannotRead : FileSelectionError
    // The source was readable, but temporary storage could not receive it completely.
    data object CopyFailed : FileSelectionError
    // A print journey accepts exactly one source file.
    data object MultipleFiles : FileSelectionError
    // The file is accepted, but its visual preview could not be prepared.
    data object PreviewUnavailable : FileSelectionError
}

sealed interface PreviewResult {
    // The selected file has a preview artifact that can be shown to the user.
    data class Available(val image: File? = null) : PreviewResult
    // The file remains printable; only preparation of its visual preview failed.
    data object Unavailable : PreviewResult
}

interface FileRepository {
    suspend fun copy(source: String): CopyResult
    suspend fun delete(file: File)
}

sealed interface CopyResult {
    data class Success(val file: StoredFile) : CopyResult
    data class Failure(val error: FileSelectionError) : CopyResult
}

interface PreviewRepository {
    suspend fun prepare(file: StoredFile): PreviewResult
}
