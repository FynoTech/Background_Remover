import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.example.autobackgroundremover.OnBackgroundChangeListener
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object BackgroundRemover {

    private val segmenter: SubjectSegmenter

    init {
        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .build()
        segmenter = SubjectSegmentation.getClient(options)
    }

    fun process(
        bitmap: Bitmap,
        trimEmptyPart: Boolean = false,
        listener: OnBackgroundChangeListener
    ) {
        val input = InputImage.fromBitmap(bitmap.copy(Bitmap.Config.ARGB_8888, true), 0)

        segmenter.process(input)
            .addOnSuccessListener { result ->
                val fgBitmap = result.foregroundBitmap
                if (fgBitmap == null) {
                    listener.onFailed(Exception("No foreground detected"))
                    return@addOnSuccessListener
                }

                CoroutineScope(Dispatchers.IO).launch {
                    val output = if (trimEmptyPart) trimKeepSize(fgBitmap) else fgBitmap
                    withContext(Dispatchers.Main) {
                        listener.onSuccess(output)
                    }
                }
            }
            .addOnFailureListener { e -> listener.onFailed(e) }
    }

    private suspend fun trimKeepSize(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height

        var firstX = 0
        var firstY = 0
        var lastX = w
        var lastY = h

        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        loop@ for (x in 0 until w) {
            for (y in 0 until h) {
                if (pixels[x + y * w] != Color.TRANSPARENT) {
                    firstX = x
                    break@loop
                }
            }
        }
        loop@ for (y in 0 until h) {
            for (x in firstX until w) {
                if (pixels[x + y * w] != Color.TRANSPARENT) {
                    firstY = y
                    break@loop
                }
            }
        }
        loop@ for (x in w - 1 downTo firstX) {
            for (y in h - 1 downTo firstY) {
                if (pixels[x + y * w] != Color.TRANSPARENT) {
                    lastX = x
                    break@loop
                }
            }
        }
        loop@ for (y in h - 1 downTo firstY) {
            for (x in w - 1 downTo firstX) {
                if (pixels[x + y * w] != Color.TRANSPARENT) {
                    lastY = y
                    break@loop
                }
            }
        }

        // Create cropped content
        val cropped = Bitmap.createBitmap(bitmap, firstX, firstY, lastX - firstX, lastY - firstY)

        // Paste it back into full-size transparent bitmap
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(cropped, firstX.toFloat(), firstY.toFloat(), null)

        return result
    }
}
