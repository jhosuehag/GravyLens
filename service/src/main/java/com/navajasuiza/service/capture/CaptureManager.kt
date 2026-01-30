package com.navajasuiza.service.capture

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.lang.Exception
import kotlin.coroutines.resume

class CaptureManager(private val context: Context) {

    private var mediaProjection: MediaProjection? = null
    private var lastResultCode: Int = 0
    private var lastResultData: Intent? = null

    val hasPermission: Boolean
        get() = mediaProjection != null || (lastResultCode != 0 && lastResultData != null)

    fun setPermissionResult(resultCode: Int, data: Intent) {
        lastResultCode = resultCode
        lastResultData = data
        // Initialize projection immediately or wait until capture?
        // Better to initialize on demand to avoid keeping it open?
        // Actually, MediaProjection instance is needed.
        // If we create it now, we should register callback to know if it stops.
        
        // Let's create it lazily in captureOnce, OR create here.
        // For now, store data.
    }

    suspend fun captureOnce(): Bitmap? = withContext(Dispatchers.IO) {
        if (lastResultData == null) return@withContext null

        val mpManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        
        // If projection died, recreate it.
        if (mediaProjection == null) {
            mediaProjection = mpManager.getMediaProjection(lastResultCode, lastResultData!!)
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    mediaProjection = null
                }
            }, android.os.Handler(android.os.Looper.getMainLooper()))
        }
        
        val proj = mediaProjection ?: return@withContext null

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        
        val virtualDisplay = proj.createVirtualDisplay(
            "NavajaSuizaCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, null
        )

        // Wait for image
        // We typically need to wait a bit for the first frame to render into the surface
        var bitmap: Bitmap? = null
        try {
            // Using a simple delay or suspecting onImageAvailable. 
            // A delay is often robust enough for a "screenshot"
            delay(150) 
            
            val image = imageReader.acquireLatestImage()
            if (image != null) {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * width

                bitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                image.close()
                
                // Crop if necessary (remove padding)
                if (rowPadding > 0) {
                     val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                     bitmap.recycle() // Recycle original
                     bitmap = cropped
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            virtualDisplay?.release()
            imageReader.close()
            // We usually KEEP mediaProjection open for subsequent captures, 
            // unless we want to force re-permission? 
            // For now, keep it open.
        }
        
        return@withContext bitmap
    }
    
    fun clear() {
        mediaProjection?.stop()
        mediaProjection = null
        lastResultCode = 0
        lastResultData = null
    }
}
