package net.zemoa.gutenprint

import java.io.File
import kotlinx.coroutines.test.runTest
import net.zemoa.gutenprint.domains.printing.CopyResult
import net.zemoa.gutenprint.domains.printing.FileRepository
import net.zemoa.gutenprint.domains.printing.FileSelectionError
import net.zemoa.gutenprint.domains.printing.PreviewRepository
import net.zemoa.gutenprint.domains.printing.PreviewResult
import net.zemoa.gutenprint.domains.printing.PrintingStore
import net.zemoa.gutenprint.domains.printing.StoredFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrintingStoreTest {
    @Test
    fun rejectedReplacementKeepsThePreviousFile() = runTest {
        val first = stored("first.pdf")
        val repository = FakeFiles(CopyResult.Success(first))
        val store = PrintingStore(repository, FakePreview())
        store.select("first")
        repository.result = CopyResult.Failure(FileSelectionError.UnsupportedFormat)

        store.select("second")

        assertEquals(first, store.state.value.selectedFile)
        assertEquals(FileSelectionError.UnsupportedFormat, store.state.value.error)
        assertEquals(emptyList<File>(), repository.deleted)
    }

    @Test
    fun successfulReplacementDeletesTheOldFileAfterCopy() = runTest {
        val first = stored("first.pdf")
        val second = stored("second.png")
        val repository = FakeFiles(CopyResult.Success(first))
        val store = PrintingStore(repository, FakePreview())
        store.select("first")
        repository.result = CopyResult.Success(second)

        store.select("second")

        assertEquals(second, store.state.value.selectedFile)
        assertEquals(listOf(first.file), repository.deleted)
    }

    @Test
    fun unavailablePreviewDoesNotRejectTheFile() = runTest {
        val file = stored("document.docx")
        val store = PrintingStore(FakeFiles(CopyResult.Success(file)), FakePreview(PreviewResult.Unavailable))

        store.select("document")

        assertEquals(file, store.state.value.selectedFile)
        assertEquals(PreviewResult.Unavailable, store.state.value.preview)
        assertNull(store.state.value.error)
    }

    private fun stored(name: String) = StoredFile(File(name), name, checkNotNull(net.zemoa.gutenprint.domains.printing.FileFormat.from(name, null)))

    private class FakeFiles(var result: CopyResult) : FileRepository {
        val deleted = mutableListOf<File>()
        override suspend fun copy(source: String) = result
        override suspend fun delete(file: File) { deleted += file }
    }

    private class FakePreview(private val result: PreviewResult = PreviewResult.Available()) : PreviewRepository {
        override suspend fun prepare(file: StoredFile) = result
    }
}
