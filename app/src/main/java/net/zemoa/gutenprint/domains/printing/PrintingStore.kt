package net.zemoa.gutenprint.domains.printing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.zemoa.gutenprint.domains.interfaces.Logger
import net.zemoa.gutenprint.domains.interfaces.NoOpLogger

/**
 * State of the current print journey. A null selected file means no journey is active.
 * [isProcessing] covers validation, copying and preview preparation.
 */
data class PrintingState(
    val selectedFile: StoredFile? = null,
    val preview: PreviewResult? = null,
    val error: FileSelectionError? = null,
    val isProcessing: Boolean = false,
)

class PrintingStore(
    private val files: FileRepository,
    private val previews: PreviewRepository,
    private val logger: Logger = NoOpLogger,
) {
    private companion object {
        const val TAG = "PrintingStore"
    }

    private val mutableState = MutableStateFlow(PrintingState())
    private val mutableEvents = MutableSharedFlow<FileSelectionError>(extraBufferCapacity = 1)
    private val mutex = Mutex()
    val state: StateFlow<PrintingState> = mutableState.asStateFlow()
    val events: SharedFlow<FileSelectionError> = mutableEvents.asSharedFlow()

    suspend fun select(source: String) = mutex.withLock {
        mutableState.value = mutableState.value.copy(error = null, isProcessing = true)
        logger.info(TAG, "Selecting source file")
        logger.debug(TAG, "Source URI: $source")
        var copiedFile: StoredFile? = null
        try {
            when (val copied = files.copy(source)) {
                is CopyResult.Failure -> {
                    logger.warning(TAG, "File selection rejected: ${copied.error}")
                    mutableState.value = mutableState.value.copy(error = copied.error, isProcessing = false)
                    mutableEvents.tryEmit(copied.error)
                }
                is CopyResult.Success -> {
                    copiedFile = copied.file
                    val previousState = mutableState.value
                    val preview = previews.prepare(copied.file)
                    if (preview is PreviewResult.Unavailable) {
                        logger.warning(TAG, "Preview unavailable for ${copied.file.displayName}")
                        mutableEvents.tryEmit(FileSelectionError.PreviewUnavailable)
                    }
                    previousState.selectedFile?.let { files.delete(it.file) }
                    (previousState.preview as? PreviewResult.Available)?.image?.let { files.delete(it) }
                    mutableState.value = PrintingState(copied.file, preview, null, false)
                    logger.info(TAG, "File selected: ${copied.file.displayName}")
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            logger.error(TAG, "Unexpected error while selecting a file", exception)
            copiedFile?.let { files.delete(it.file) }
            mutableState.value = mutableState.value.copy(
                error = FileSelectionError.CopyFailed,
                isProcessing = false,
            )
            mutableEvents.tryEmit(FileSelectionError.CopyFailed)
        }
    }

    fun rejectMultipleFiles() {
        logger.warning(TAG, "Multiple files rejected")
        mutableState.value = mutableState.value.copy(error = FileSelectionError.MultipleFiles)
        mutableEvents.tryEmit(FileSelectionError.MultipleFiles)
    }

    fun rejectUnreadableFile() {
        logger.warning(TAG, "Unreadable shared file rejected")
        mutableState.value = mutableState.value.copy(error = FileSelectionError.CannotRead)
        mutableEvents.tryEmit(FileSelectionError.CannotRead)
    }

    suspend fun endJourney() = mutex.withLock {
        logger.info(TAG, "Ending print journey")
        val currentState = mutableState.value
        currentState.selectedFile?.let { files.delete(it.file) }
        (currentState.preview as? PreviewResult.Available)?.image?.let { files.delete(it) }
        mutableState.value = PrintingState()
    }
}
