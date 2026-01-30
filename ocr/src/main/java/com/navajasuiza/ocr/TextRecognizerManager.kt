package com.navajasuiza.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class OcrResult(
    val blocks: List<OcrBlock>,
    val text: String,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val scaleFactor: Float
)

data class OcrBlock(
    val text: String,
    val boundingBox: Rect
)

class TextRecognizerManager(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun processBitmap(bitmap: Bitmap): OcrResult = withContext(Dispatchers.Default) {
        // Performance: Scale down if too big
        val maxDimension = 1280
        val width = bitmap.width
        val height = bitmap.height
        var scaleFactor = 1f
        var processedBitmap = bitmap

        if (width > maxDimension || height > maxDimension) {
            scaleFactor = if (width > height) {
                maxDimension.toFloat() / width
            } else {
                maxDimension.toFloat() / height
            }
            
            val matrix = Matrix()
            matrix.postScale(scaleFactor, scaleFactor)
            processedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
        }

        val image = InputImage.fromBitmap(processedBitmap, 0)
        
        try {
            val visionText = recognizer.process(image).await()
            val blocks = visionText.textBlocks.mapNotNull { block ->
                val box = block.boundingBox
                if (box != null && block.text.isNotBlank()) {
                     // Scale box back to original coordinates
                     val originalBox = Rect(
                         (box.left / scaleFactor).toInt(),
                         (box.top / scaleFactor).toInt(),
                         (box.right / scaleFactor).toInt(),
                         (box.bottom / scaleFactor).toInt()
                     )
                     OcrBlock(block.text, originalBox)
                } else {
                    null
                }
            }
            
            // Don't recycle original bitmap here as it might be used for display
            if (processedBitmap != bitmap) {
                processedBitmap.recycle()
            }

            OcrResult(blocks, visionText.text, width, height, scaleFactor)
        } catch (e: Exception) {
            e.printStackTrace()
            OcrResult(emptyList(), "", width, height, scaleFactor)
        }
    }
    
    fun close() {
        recognizer.close()
    }
}
