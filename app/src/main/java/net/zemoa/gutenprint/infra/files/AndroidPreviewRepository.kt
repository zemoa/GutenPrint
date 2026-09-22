package net.zemoa.gutenprint.infra.files

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.zemoa.gutenprint.domains.printing.FileFormat
import net.zemoa.gutenprint.domains.printing.PreviewRepository
import net.zemoa.gutenprint.domains.printing.PreviewResult
import net.zemoa.gutenprint.domains.printing.StoredFile
import net.zemoa.gutenprint.domains.interfaces.Logger
import net.zemoa.gutenprint.domains.interfaces.NoOpLogger

class AndroidPreviewRepository(
    private val temporaryDirectory: File,
    private val logger: Logger = NoOpLogger,
) : PreviewRepository {
    private companion object {
        const val TAG = "AndroidPreviewRepository"
        const val MAX_PREVIEW_DIMENSION = 2048
    }

    override suspend fun prepare(file: StoredFile): PreviewResult = withContext(Dispatchers.IO) {
        logger.debug(TAG, "Preparing preview: file=${file.file.name} format=${file.format}")
        when (file.format) {
            FileFormat.JPG,
            FileFormat.PNG -> renderImage(file.file)
            FileFormat.PDF -> renderPdf(file.file)
            FileFormat.DOCX -> PreviewResult.Unavailable.also {
                logger.warning(TAG, "Preview not available for DOCX: ${file.file.name}")
            }
        }
    }

    private fun renderImage(file: File): PreviewResult {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            logger.warning(TAG, "Image dimensions could not be read: ${file.name}")
            return PreviewResult.Unavailable
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        var bitmap: Bitmap? = null
        var output: File? = null
        return try {
            val decoded = BitmapFactory.decodeFile(file.path, options) ?: return PreviewResult.Unavailable
            bitmap = decoded
            val previewFile = File.createTempFile("preview-", ".png", temporaryDirectory)
            output = previewFile
            val compressed = previewFile.outputStream().use {
                decoded.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            if (!compressed) {
                output.delete()
                PreviewResult.Unavailable
            } else {
                PreviewResult.Available(previewFile)
            }
        } catch (exception: Exception) {
            output?.delete()
            logger.warning(TAG, "Image preview failed: ${exception.message}")
            PreviewResult.Unavailable
        } catch (error: OutOfMemoryError) {
            output?.delete()
            logger.error(TAG, "Out of memory while rendering image preview", error)
            PreviewResult.Unavailable
        } finally {
            bitmap?.recycle()
        }
    }

    private fun renderPdf(file: File): PreviewResult {
        var output: File? = null
        var bitmap: Bitmap? = null
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    if (renderer.pageCount == 0) return PreviewResult.Unavailable
                    renderer.openPage(0).use { page ->
                        if (page.width <= 0 || page.height <= 0) return PreviewResult.Unavailable
                        val scale = minOf(
                            1f,
                            MAX_PREVIEW_DIMENSION.toFloat() / page.width,
                            MAX_PREVIEW_DIMENSION.toFloat() / page.height,
                        )
                        val width = (page.width * scale).toInt().coerceAtLeast(1)
                        val height = (page.height * scale).toInt().coerceAtLeast(1)
                        val renderedBitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap = renderedBitmap
                        renderedBitmap.eraseColor(Color.WHITE)
                        page.render(renderedBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val previewFile = File.createTempFile("preview-", ".png", temporaryDirectory)
                        output = previewFile
                        previewFile.outputStream().use {
                            if (!renderedBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) {
                                previewFile.delete()
                                return PreviewResult.Unavailable
                            }
                        }
                        PreviewResult.Available(previewFile)
                    }
                }
            }
        } catch (exception: Exception) {
            output?.delete()
            logger.warning(TAG, "PDF preview failed: ${exception.message}")
            PreviewResult.Unavailable
        } catch (error: OutOfMemoryError) {
            output?.delete()
            logger.error(TAG, "Out of memory while rendering PDF preview", error)
            PreviewResult.Unavailable
        } finally {
            bitmap?.recycle()
        }
    }

    private fun sampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        while (maxOf(width, height) / sampleSize > MAX_PREVIEW_DIMENSION && sampleSize <= Int.MAX_VALUE / 2) {
            sampleSize *= 2
        }
        return sampleSize
    }
}
