package com.navajasuiza.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.navajasuiza.core.OverlayManager
import com.navajasuiza.core.PrefsManager
import kotlinx.coroutines.*

class FloatingService : Service() {

    private lateinit var overlayManager: OverlayManager
    private lateinit var prefsManager: PrefsManager
    
    // ... properties
    private val captureManager by lazy { com.navajasuiza.service.capture.CaptureManager(this) }
    private val textRecognizer by lazy { com.navajasuiza.ocr.TextRecognizerManager(this) }
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())


    companion object {
        private const val CHANNEL_ID = "navaja_suiza_service_channel"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "com.navajasuiza.service.ACTION_STOP"
    }

    private lateinit var repo: com.navajasuiza.data.repository.ClipboardRepository

    override fun onCreate() {
        super.onCreate()
        overlayManager = OverlayManager(this)
        prefsManager = PrefsManager(this)
        setupNotification()
        
        // Initialize Memory (DB + Clipboard)
        val db = com.navajasuiza.data.local.AppDatabase.getDatabase(this)
        repo = com.navajasuiza.data.repository.ClipboardRepository(db.copiedTextDao())
        
        val clipboardWatcher = com.navajasuiza.service.clipboard.ClipboardWatcher(this)
        this.clipboardWatcher = clipboardWatcher
        
        clipboardWatcher.start()
        
        serviceScope.launch {
            clipboardWatcher.clipboardFlow.collect { text ->
                if (prefsManager.isHistoryEnabled) {
                    android.util.Log.d("NavajaMemory", "Clipboard detected: $text")
                    repo.addCopiedText(text)
                }
            }
        }
    }
    
    private var clipboardWatcher: com.navajasuiza.service.clipboard.ClipboardWatcher? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        overlayManager.hideHandle()
        overlayManager.hidePanel()
        captureManager.clear()
        clipboardWatcher?.stop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE" || intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        
        // Check for permission data in intent (either from Start or explicit Permission Result)
        val code = intent?.getIntExtra("extra_result_code", 0) ?: 0
        val data = intent?.getParcelableExtra<Intent>("extra_data")
        
        if (code != 0 && data != null) {
             captureManager.setPermissionResult(code, data)
             // If this was an explicit permission result (not just start), maybe show a toast?
             // For "START_WITH_PERMISSION", we don't need to say "Permission Granted" every time.
             // But valid permission is set.
        }
        
        // Ensure foreground is started/updated
        setupNotification()
        
        showInitialOverlay()

        return START_STICKY
    }

    private fun getCurrentOrientation(): Int {
        return resources.configuration.orientation
    }

    private fun saveCurrentPosition(x: Int, y: Int) {
        if (getCurrentOrientation() == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            prefsManager.handleXLandscape = x
            prefsManager.handleYLandscape = y
        } else {
            prefsManager.handleXPortrait = x
            prefsManager.handleYPortrait = y
        }
    }

    private fun loadPositionForCurrentOrientation(): Pair<Int, Int> {
        return if (getCurrentOrientation() == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            Pair(prefsManager.handleXLandscape, prefsManager.handleYLandscape)
        } else {
            Pair(prefsManager.handleXPortrait, prefsManager.handleYPortrait)
        }
    }

    private fun showInitialOverlay() {
        android.util.Log.d("OVERLAY_DEBUG", "Service: showInitialOverlay called")
        
        val handleView = com.navajasuiza.service.ui.FloatingHandleView(
            this,
            onPositionChanged = { dx, dy ->
                 overlayManager.getLayoutParams()?.let { params ->
                    val newX = params.x + dx
                    val newY = params.y + dy
                    updatePositionWithLimits(newX, newY)
                }
            },
            onSnapFinished = { targetX, targetY ->
                attemptSnapAnimation(targetX)
            },
            onSwipeToOpen = {
                showPanel()
            }
        )
        
        val (x, y) = loadPositionForCurrentOrientation()
        overlayManager.showHandle(handleView, x, y)
    }
    
    private fun getRealDisplayMetrics(): android.util.DisplayMetrics {
        val dm = android.util.DisplayMetrics()
        val wm = getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        wm.defaultDisplay.getRealMetrics(dm)
        return dm
    }

    private fun updatePositionWithLimits(targetX: Int, targetY: Int) {
        val dm = getRealDisplayMetrics()
        val screenHeight = dm.heightPixels
        val viewHeight = overlayManager.getView()?.height ?: 100 // Estimate if null
        
        // Enforce Safe Vertical Limits
        // Avoid Status Bar (Top) and Navigation Bar (Bottom) areas
        // 72dp gives enough buffer for standard status bars and gesture nav hints
        val verticalMargin = android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_DIP, 72f, dm).toInt()
            
        val minY = verticalMargin
        val maxY = screenHeight - verticalMargin - viewHeight
        
        // Strict Clamping
        val clampedY = targetY.coerceIn(minY, maxY)
        
        overlayManager.updatePosition(targetX, clampedY)
    }

    // ... NOTE: updatePositionWithLimits does NOT save to prefs anymore, just moves view.

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        android.util.Log.d("OVERLAY_DEBUG", "Configuration changed")
        
        // Simply load the saved position for the NEW orientation
        val (x, y) = loadPositionForCurrentOrientation()
        
        // We might need to clamp it again just to be safe (e.g. if screen size changed within same orientation?)
        // But mainly to apply the new position.
        updatePositionWithLimits(x, y)
    }

    private fun attemptSnapAnimation(targetX: Int) {
        val startX = overlayManager.getLayoutParams()?.x?.toFloat() ?: 0f
        
        val anim = androidx.dynamicanimation.animation.SpringAnimation(
            androidx.dynamicanimation.animation.FloatValueHolder(startX)
        )
        
        anim.setSpring(androidx.dynamicanimation.animation.SpringForce(targetX.toFloat()).apply {
            stiffness = androidx.dynamicanimation.animation.SpringForce.STIFFNESS_LOW
            dampingRatio = androidx.dynamicanimation.animation.SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
        })
        
        anim.addUpdateListener { _, value, _ ->
            overlayManager.getLayoutParams()?.let { params ->
                updatePositionWithLimits(value.toInt(), params.y)
            }
        }
        
        anim.addEndListener { _, _, _, _ ->
             overlayManager.getLayoutParams()?.let { params ->
                 // SAVE position only after snap finishes
                 saveCurrentPosition(params.x, params.y)
             }
        }
        
        anim.start()
    }
    
    private fun setupNotification() {
        val channelName = "Floating Tool Service"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = createNotification(CHANNEL_ID)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotification(channelId: String): android.app.Notification {
        val notificationBuilder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            Notification.Builder(this)
        }

        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        notificationBuilder
            .setContentTitle("Navaja Suiza")
            .setContentText("Herramienta activa")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingIntent)
            .setOngoing(true)

        val stopIntent = Intent(this, FloatingService::class.java).apply {
            action = "STOP_SERVICE"
        }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        
        notificationBuilder.addAction(
            Notification.Action.Builder(null, "Detener", stopPendingIntent).build()
        )

        return notificationBuilder.build()
    }

    private fun showPanel() {
        val params = overlayManager.getLayoutParams() ?: return
        val screenWidth = getRealDisplayMetrics().widthPixels
        val isLeft = params.x < screenWidth / 2
        
        val themeContext = android.view.ContextThemeWrapper(this, R.style.Theme_NavajaSuiza_Overlay)
        val panel = com.navajasuiza.service.ui.FloatingPanelView(themeContext)
        
        panel.setCallbacks(
            dismissCallback = {
                overlayManager.hidePanel()
            },
            scanCallback = {
                if (captureManager.hasPermission) {
                    performCapture()
                } else {
                    android.widget.Toast.makeText(this, "Falta permiso de captura. Reinicia el servicio.", android.widget.Toast.LENGTH_LONG).show()
                }
            },
            historyCallback = {
                showHistory()
            },
            snipCallback = {
                if (captureManager.hasPermission) {
                    performSnip()
                } else {
                    android.widget.Toast.makeText(this, "Falta permiso de captura", android.widget.Toast.LENGTH_LONG).show()
                }
            },
            galleryCallback = {
                showGallery()
            }
        )
        
        panel.prepareLayout(isLeft, params.y)
        
        overlayManager.showPanel(panel)
        panel.post {
            panel.animateOpen()
        }
    }
    
    private fun showHistory() {
        // We use a ThemeWrapper to ensure styles (like CardView) work correctly if not provided by Application Context
        val themeContext = android.view.ContextThemeWrapper(this, R.style.Theme_NavajaSuiza_Overlay)
        val historyView = com.navajasuiza.features.history.FloatingHistoryView(themeContext)
        
        val controller = com.navajasuiza.history.HistoryController(repo, serviceScope)
        historyView.initialize(controller)
        
        historyView.onCloseClick = {
            overlayManager.hidePanel()
        }
        
        // Full screen panel for history? Or sized?
        // Let's make it match parent (the scrim container in OverlayManager will handle it)
        overlayManager.showPanel(historyView)
    }

    private fun showGallery() {
        val themeContext = android.view.ContextThemeWrapper(this, R.style.Theme_NavajaSuiza_Overlay)
        val galleryView = com.navajasuiza.history.gallery.FloatingGalleryView(themeContext)
        
        galleryView.onCloseClick = {
            overlayManager.hidePanel()
        }
        
        galleryView.onSnippetClick = { file ->
            showSnippetViewer(file)
        }
        
        overlayManager.showPanel(galleryView)
    }

    private fun showSnippetViewer(file: java.io.File) {
        val themeContext = android.view.ContextThemeWrapper(this, R.style.Theme_NavajaSuiza_Overlay)
        
        // Fetch fresh list to support swiping
        val snippetRepo = com.navajasuiza.data.repository.SnippetRepository(this)
        val snippets = snippetRepo.getSnippets()
        val index = snippets.indexOfFirst { it.absolutePath == file.absolutePath }.coerceAtLeast(0)
        
        val viewer = com.navajasuiza.history.gallery.SnippetViewer(themeContext, snippets, index)
        
        viewer.onClose = {
            // Back to gallery when closing viewer
            showGallery()
        }
        
        viewer.onDelete = {
            // If deleted, go back to gallery (which will reload)
            showGallery()
        }
        
        viewer.onShare = {
            // Delay hiding the panel to ensure the Share Intent has time to launch the target app
            // and the system transition begins. Hiding too fast might cancel the context/interaction.
            serviceScope.launch {
                delay(1500)
                overlayManager.hidePanel()
            }
        }
        
        overlayManager.showPanel(viewer)
    }

    private fun performSnip() {
        overlayManager.hidePanel()
        serviceScope.launch {
            delay(450) // Increased from 150 to ensure panel is fully gone
            try {
                val bitmapFull = captureManager.captureOnce()
                if (bitmapFull != null) {
                    // Fix: Do NOT crop status bar. Pass full screen for accurate 1:1 mapping.
                    showSnippingOverlay(bitmapFull)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showSnippingOverlay(bitmap: android.graphics.Bitmap) {
         val themeContext = android.view.ContextThemeWrapper(this, R.style.Theme_NavajaSuiza_Overlay)
         val overlay = com.navajasuiza.service.ui.SnippingOverlayView(themeContext, bitmap)
         overlay.onClose = {
             overlayManager.hidePanel()
         }
         overlay.onSnipConfirmed = { croppedBitmap ->
             // Don't hide immediately. Show loading/success state on the overlay itself.
             
             serviceScope.launch(Dispatchers.IO) {
                 val savedFile = com.navajasuiza.service.capture.ImageSaver.saveBitmap(this@FloatingService, croppedBitmap)
                 withContext(Dispatchers.Main) {
                     if (savedFile != null) {
                         // Show visual feedback on the view (we will add this method next)
                         overlay.showSavedMessage()
                         
                         // Wait for user to see it
                         delay(1000)
                         overlayManager.hidePanel()
                     } else {
                         android.widget.Toast.makeText(applicationContext, "Error al guardar imagen", android.widget.Toast.LENGTH_SHORT).show()
                         overlayManager.hidePanel()
                     }
                 }
             }
         }
         overlayManager.showPanel(overlay)
    }
    
    private fun performCapture() {
        // Hide the menu panel immediately to clear screen for capture
        overlayManager.hidePanel()

        serviceScope.launch {
            // Wait for panel to disappear
            delay(150)
            
            try {
                val bitmapFull = captureManager.captureOnce()
                if (bitmapFull != null) {
                    // Alignment Fix: Crop Status Bar
                    // The Standard Overlay Window starts BELOW the status bar.
                    // We must crop the screenshot so the image (and OCR coords) start there too.
                    
                    val resources = resources
                    val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
                    val statusBarHeight = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
                    
                    val cropHeight = bitmapFull.height - statusBarHeight
                    
                    val bitmapCropped = if (cropHeight > 0 && statusBarHeight > 0) {
                        try {
                            android.graphics.Bitmap.createBitmap(bitmapFull, 0, statusBarHeight, bitmapFull.width, cropHeight)
                        } catch (e: Exception) {
                            bitmapFull
                        }
                    } else {
                        bitmapFull
                    }
                    
                    val ocrResult = textRecognizer.processBitmap(bitmapCropped)
                    
                    if (ocrResult.blocks.isEmpty()) {
                         android.widget.Toast.makeText(this@FloatingService, "No se detectó texto", android.widget.Toast.LENGTH_SHORT).show()
                         return@launch
                    }
                    
                    showOcrOverlay(bitmapCropped, ocrResult.blocks)
                } else {
                    android.widget.Toast.makeText(this@FloatingService, "Error: Bitmap nulo", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(this@FloatingService, "Error: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }
    
    private fun showOcrOverlay(bitmap: android.graphics.Bitmap, blocks: List<com.navajasuiza.ocr.OcrBlock>) {
         val themeContext = android.view.ContextThemeWrapper(this, R.style.Theme_NavajaSuiza_Overlay)
         val container = FrameLayout(themeContext)
         container.fitsSystemWindows = false
         
         val overlay = com.navajasuiza.service.ui.HighlightOverlayView(themeContext)
         
         overlay.setData(
             bitmap = bitmap,
             blocks = blocks,
             onBlockClick = { text ->
                 // Explicitly save to history on click (User Requirement)
                 if (prefsManager.isHistoryEnabled) {
                     serviceScope.launch(Dispatchers.IO) {
                         repo.addCopiedText(text, "ocr")
                     }
                 }

                 // Copy logic
                 val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                 val clip = android.content.ClipData.newPlainText("OCR Result", text)
                 clipboard.setPrimaryClip(clip)
                 
                 // Feedback logic (Snackbar - Reverted as requested)
                 com.google.android.material.snackbar.Snackbar.make(container, "Copiado: $text", com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                     .setBackgroundTint(android.graphics.Color.DKGRAY)
                     .setTextColor(android.graphics.Color.WHITE)
                     .show()
             },
             onClose = {
                 overlayManager.hidePanel()
             }
         )
         
         container.addView(overlay, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
         overlayManager.showPanel(container)
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
