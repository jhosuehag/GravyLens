package com.navajasuiza.service.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.max

class SnippingOverlayView @JvmOverloads constructor(
    context: Context,
    private val screenshot: Bitmap? = null,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onClose: (() -> Unit)? = null
    var onSnipConfirmed: ((Bitmap) -> Unit)? = null

    /* UI Components */
    private val backgroundImageView = ImageView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        scaleType = ImageView.ScaleType.FIT_XY
        screenshot?.let { setImageBitmap(it) }
    }
    
    private val closeButton = android.widget.ImageButton(context).apply {
        val size = (48 * resources.displayMetrics.density).toInt()
        val margin = (24 * resources.displayMetrics.density).toInt()
        layoutParams = LayoutParams(size, size).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = margin + (32 * resources.displayMetrics.density).toInt()
            rightMargin = margin
        }
        background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(Color.RED)
            setStroke((2 * resources.displayMetrics.density).toInt(), Color.WHITE)
        }
        setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        setColorFilter(Color.WHITE)
        elevation = 20f
        setOnClickListener { onClose?.invoke() }
    }

    private val statusView = android.widget.TextView(context).apply {
       layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
           gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
           setMargins(0, 0, 0, 80)
       }
       background = android.graphics.drawable.GradientDrawable().apply {
           shape = android.graphics.drawable.GradientDrawable.RECTANGLE
           cornerRadius = 100f
           setColor(Color.parseColor("#CC202020"))
           setStroke(2, Color.parseColor("#555555"))
       }
       setTextColor(Color.WHITE)
       textSize = 16f
       setPadding(60, 30, 60, 30)
       text = "Guardado ✅"
       visibility = View.GONE
       elevation = 30f
       alpha = 0f
    }

    private val actionsContainer = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        visibility = View.GONE
        elevation = 20f
        
        background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            setColor(Color.WHITE)
            cornerRadius = 100f
        }
        setPadding(20, 10, 20, 10)
        
        // Cancel Button
        val btnCancel = android.widget.ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams((40 * resources.displayMetrics.density).toInt(), (40 * resources.displayMetrics.density).toInt()).apply { marginEnd = 30 }
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.RED)
            setPadding(10, 10, 10, 10)
            isClickable = true
            isFocusable = true
            background = android.util.TypedValue().let { tv ->
                context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, tv, true)
                resources.getDrawable(tv.resourceId, context.theme)
            }
            setOnClickListener { 
                resetSelection()
            }
        }
        
        // Confirm Button
        val btnConfirm = android.widget.ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams((40 * resources.displayMetrics.density).toInt(), (40 * resources.displayMetrics.density).toInt())
            setImageResource(android.R.drawable.ic_menu_save)
            setColorFilter(Color.parseColor("#4CAF50")) // Green
            setPadding(10, 10, 10, 10)
            isClickable = true
            isFocusable = true
            background = android.util.TypedValue().let { tv ->
                context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, tv, true)
                resources.getDrawable(tv.resourceId, context.theme)
            }
            setOnClickListener {
                confirmSelection()
            }
        }
        
        addView(btnCancel)
        addView(btnConfirm)
    }

    private val selectionLayer = SelectionLayer(context)

    init {
        addView(backgroundImageView)
        addView(selectionLayer)
        addView(closeButton)
        addView(statusView)
        
        val params = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        addView(actionsContainer, params)
        
        // Input Handling: Capture Back Button for native experience
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()
        
        alpha = 0f
        animate().alpha(1f).setDuration(300).start()
    }
    
    override fun dispatchKeyEvent(event: android.view.KeyEvent?): Boolean {
        if (event?.keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
            onClose?.invoke()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Aggressive memory cleanup for large bitmaps to prevent OOM
        if (screenshot != null && !screenshot.isRecycled) {
            screenshot.recycle()
        }
    }
    
    fun showSavedMessage() {
        post {
            actionsContainer.visibility = View.GONE
            selectionLayer.visibility = View.GONE // Hide selection box
            closeButton.visibility = View.GONE // Ensure user waits
            
            statusView.visibility = View.VISIBLE
            statusView.animate()
                .alpha(1f)
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(300)
                .withEndAction {
                    statusView.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                }
                .start()
        }
    }
    
    private fun confirmSelection() {
        val rect = selectionLayer.getSelectionRect() ?: return
        if (screenshot != null) {
            // Calculate scale factor in case View size != Bitmap size (e.g. FIT_XY with Status Bar insets)
            // This fixes the "shifted up" issue where the view is slightly squashed vertically compared to the full screenshot
            val viewWidth = backgroundImageView.width.toFloat()
            val viewHeight = backgroundImageView.height.toFloat()
            
            val scaleX = if (viewWidth > 0) screenshot.width / viewWidth else 1f
            val scaleY = if (viewHeight > 0) screenshot.height / viewHeight else 1f

            val x = (rect.left * scaleX).toInt().coerceAtLeast(0)
            val y = (rect.top * scaleY).toInt().coerceAtLeast(0)
            val w = (rect.width() * scaleX).toInt().coerceAtMost(screenshot.width - x)
            val h = (rect.height() * scaleY).toInt().coerceAtMost(screenshot.height - y)
            
            if (w > 0 && h > 0) {
                try {
                    val cropped = Bitmap.createBitmap(screenshot, x, y, w, h)
                    onSnipConfirmed?.invoke(cropped)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    private fun resetSelection() {
        selectionLayer.clear()
        actionsContainer.visibility = View.GONE
    }

    private inner class SelectionLayer(context: Context) : View(context) {
        
        private val scrimPaint = Paint().apply { color = Color.parseColor("#99000000") }
        private val eraserPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
        private val borderPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 3f }
        private val handlePaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL; setShadowLayer(4f, 0f, 2f, Color.DKGRAY) }

        private var currentRect: RectF? = null
        
        // Interaction State
        private var mode = 0 // 0=NONE, 1=CREATE, 2=RESIZE_TL, 3=RESIZE_TR, 4=RESIZE_BL, 5=RESIZE_BR
        private var activeHandlePoint: PointF? = null // The fixed opposite point during resize
        
        // Touch tracking
        private var downX = 0f
        private var downY = 0f
        
        private val handleRadius = 20f
        private val touchRadius = 60f // Larger hit area

        fun getSelectionRect() = currentRect
        fun clear() { currentRect = null; invalidate() }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val x = event.x
            val y = event.y
            
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    actionsContainer.visibility = View.GONE
                    downX = x
                    downY = y
                    
                    val rect = currentRect
                    if (rect != null) {
                        // Check Handles
                        when {
                            hitTest(x, y, rect.left, rect.top) -> { mode = 2; activeHandlePoint = PointF(rect.right, rect.bottom) }
                            hitTest(x, y, rect.right, rect.top) -> { mode = 3; activeHandlePoint = PointF(rect.left, rect.bottom) }
                            hitTest(x, y, rect.left, rect.bottom) -> { mode = 4; activeHandlePoint = PointF(rect.right, rect.top) }
                            hitTest(x, y, rect.right, rect.bottom) -> { mode = 5; activeHandlePoint = PointF(rect.left, rect.top) }
                            else -> {
                                mode = 1 // Create new
                                currentRect = null
                            }
                        }
                    } else {
                        mode = 1 // Create
                    }
                    invalidate()
                    return true
                }
                
                MotionEvent.ACTION_MOVE -> {
                    when (mode) {
                        1 -> { // Create
                            val left = min(downX, x)
                            val right = max(downX, x)
                            val top = min(downY, y)
                            val bottom = max(downY, y)
                            currentRect = RectF(left, top, right, bottom)
                        }
                        2, 3, 4, 5 -> { // Resize
                            activeHandlePoint?.let { fixed ->
                                val left = min(fixed.x, x)
                                val right = max(fixed.x, x)
                                val top = min(fixed.y, y)
                                val bottom = max(fixed.y, y)
                                currentRect = RectF(left, top, right, bottom)
                            }
                        }
                    }
                    invalidate()
                    return true
                }
                
                MotionEvent.ACTION_UP -> {
                    mode = 0
                    currentRect?.let { r ->
                        if (r.width() > 10 && r.height() > 10) {
                            showActions(r)
                        } else {
                            currentRect = null // Too small
                            invalidate()
                        }
                    }
                    return true
                }
            }
            return super.onTouchEvent(event)
        }

        private fun hitTest(tx: Float, ty: Float, hx: Float, hy: Float): Boolean {
            return hypot(tx - hx, ty - hy) <= touchRadius
        }
        
        private fun showActions(rect: RectF) {
            // Force measure to ensure we have correct dimensions before calculating position
            actionsContainer.measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST)
            )
            
            // Use measured dimensions, with safe fallbacks if something goes wrong
            val containerWidth = actionsContainer.measuredWidth.takeIf { it > 0 } ?: (100 * resources.displayMetrics.density).toInt()
            val containerHeight = actionsContainer.measuredHeight.takeIf { it > 0 } ?: (60 * resources.displayMetrics.density).toInt()
            
            val params = actionsContainer.layoutParams as FrameLayout.LayoutParams
            val padding = 24f // Increased margin from edges
            
            // 1. Vertical Positioning logic
            // Default: Try to place BELOW the selection
            var top = rect.bottom + padding
            
            // Check if it fits below (using view height)
            if (top + containerHeight > height - padding) {
                // Doesn't fit below, try ABOVE
                top = rect.top - containerHeight - padding
            }
            
            // Safety Clamp: If selection is HUGE and covers everything, force it inside safely
            // Priority: Keep it on screen
            top = top.coerceAtMost((height - containerHeight - padding)) // Don't go off bottom
            top = top.coerceAtLeast(padding) // Don't go off top

            params.topMargin = top.toInt()
            
            // 2. Horizontal Positioning logic
            // Goal: Center align with the selection box
            val rectCenter = rect.centerX()
            var left = rectCenter - (containerWidth / 2f)
            
            // Strict Horizontal Clamping
            // Ensure we never go past the left edge (padding)
            // Ensure we never go past the right edge (width - containerWidth - padding)
            val maxLeft = width - containerWidth - padding
            
            left = left.coerceAtLeast(padding).coerceAtMost(maxLeft)
            
            params.leftMargin = left.toInt()
            params.gravity = Gravity.TOP or Gravity.START
            
            actionsContainer.layoutParams = params
            actionsContainer.visibility = View.VISIBLE
            actionsContainer.requestLayout()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val count = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)
            currentRect?.let { r ->
                canvas.drawRect(r, eraserPaint)
                canvas.drawRect(r, borderPaint)
                
                // Draw Handles
                canvas.drawCircle(r.left, r.top, handleRadius, handlePaint)
                canvas.drawCircle(r.right, r.top, handleRadius, handlePaint)
                canvas.drawCircle(r.left, r.bottom, handleRadius, handlePaint)
                canvas.drawCircle(r.right, r.bottom, handleRadius, handlePaint)
            }
            canvas.restoreToCount(count)
        }
    }
}
