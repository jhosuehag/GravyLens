package com.navajasuiza.service.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import com.navajasuiza.service.R

class FloatingPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var onDismiss: (() -> Unit)? = null
    private var onActionScan: (() -> Unit)? = null
    private var onActionHistory: (() -> Unit)? = null
    private var onActionSnip: (() -> Unit)? = null
    private var onActionGallery: (() -> Unit)? = null
    
    private val panelCard: CardView
    private val scrim: View

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_floating_panel, this, true)
        
        panelCard = findViewById(R.id.panel_card)
        scrim = findViewById(R.id.panel_container) // The FrameLayout itself handles the background color in XML
        
        // Initial state: hidden to prevent flicker
        alpha = 0f
        
        // Scrim click closes panel
        scrim.setOnClickListener {
            closePanel()
        }
        
        // Prevent clicks on card from closing panel
        panelCard.setOnClickListener { /* Consume click */ }
        
        findViewById<View>(R.id.action_scan).setOnClickListener {
            onActionScan?.invoke()
            onDismiss = null // Prevent dismiss callback from hiding the NEW panel if opened
            closePanel()
        }
        
        findViewById<View>(R.id.action_history).setOnClickListener {
            onActionHistory?.invoke()
            onDismiss = null // Prevent dismiss callback from hiding the NEW panel if opened
            closePanel()
        }

        findViewById<View>(R.id.action_snip).setOnClickListener {
            onActionSnip?.invoke()
            onDismiss = null
            closePanel()
        }
        
        findViewById<View>(R.id.action_gallery).setOnClickListener {
            onActionGallery?.invoke()
            onDismiss = null
            closePanel()
        }
        
        // Input Handling: Capture Back Button & System Events
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()
    }
    
    override fun dispatchKeyEvent(event: android.view.KeyEvent?): Boolean {
        if (event?.keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
            closePanel()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!hasWindowFocus) {
            // Mimic native Popup behavior: Close when user interacts elsewhere (Home, Recents, Notifications)
            closePanel()
        }
    }
    
    fun setCallbacks(dismissCallback: () -> Unit, scanCallback: () -> Unit, historyCallback: () -> Unit, snipCallback: () -> Unit, galleryCallback: () -> Unit) {
        this.onDismiss = dismissCallback
        this.onActionScan = scanCallback
        this.onActionHistory = historyCallback
        this.onActionSnip = snipCallback
        this.onActionGallery = galleryCallback
    }

    private val autoCloseHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val autoCloseRunnable = Runnable { closePanel() }

    fun prepareLayout(fromLeft: Boolean, handleY: Int) {
        // Measure first to get accurate height for dynamic content
        val dm = resources.displayMetrics
        val screenHeight = dm.heightPixels
        
        // Measure the card to know exactly how tall it is with current items
        val widthMeasureSpec = MeasureSpec.makeMeasureSpec(dm.widthPixels, MeasureSpec.AT_MOST)
        val heightMeasureSpec = MeasureSpec.makeMeasureSpec(dm.heightPixels, MeasureSpec.AT_MOST)
        panelCard.measure(widthMeasureSpec, heightMeasureSpec)
        
        val panelHeight = panelCard.measuredHeight
        
        // Configure Layout Params
        val params = panelCard.layoutParams as FrameLayout.LayoutParams
        
        // Vertical Alignment Logic: smart shift
        val screenPadding = 24.dpToPx()
        
        // Calculate the ideal top position (usually aligned with handle)
        var targetTop = handleY
        
        // Constraint 1: Don't overflow bottom
        // If (targetTop + height) > (screenHeight - padding), shift up.
        val maxTop = screenHeight - panelHeight - screenPadding
        
        // Constraint 2: Don't overflow top
        val minTop = screenPadding
        
        // Apply constraints
        targetTop = targetTop.coerceAtMost(maxTop).coerceAtLeast(minTop)
        
        params.topMargin = targetTop
        params.bottomMargin = 0
        
        // Horizontal Alignment & Margins
        if (fromLeft) {
            params.leftMargin = 16.dpToPx()
            params.rightMargin = 0
            params.gravity = Gravity.TOP or Gravity.START
        } else {
            params.leftMargin = 0
            params.rightMargin = 16.dpToPx()
            params.gravity = Gravity.TOP or Gravity.END
        }
        panelCard.layoutParams = params
        
        // Set initial translation off-screen for animation
        val offscreenOffset = 500f 
        panelCard.translationX = if (fromLeft) -offscreenOffset else offscreenOffset
    }

    fun animateOpen() {
        // Start Auto-Close Timer
        autoCloseHandler.removeCallbacks(autoCloseRunnable)
        autoCloseHandler.postDelayed(autoCloseRunnable, 10000)

        // Animate visibility and slide
        animate().alpha(1f).setDuration(250).start()
        panelCard.animate()
            .translationX(0f)
            .setInterpolator(DecelerateInterpolator())
            .setDuration(300)
            .start()
    }
    
    fun closePanel() {
        autoCloseHandler.removeCallbacks(autoCloseRunnable)
        animate().alpha(0f).setDuration(200).withEndAction {
            onDismiss?.invoke()
        }.start()
    }
    
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
