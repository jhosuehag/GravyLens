package com.navajasuiza.service.ui

import android.content.Context
import android.graphics.Rect
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.navajasuiza.service.R
import kotlin.math.abs

class FloatingHandleView(
    context: Context,
    private val onPositionChanged: (Int, Int) -> Unit,
    private val onSnapFinished: (Int, Int) -> Unit,
    private val onSwipeToOpen: () -> Unit
) : FrameLayout(context) {

    private val handle: View
    private val gestureDetector: GestureDetector
    private var isDragging = false
    private var isSwiped = false
    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    
    // Screen width for snapping - fetch dynamically to handle rotation
    private fun getDisplayMetrics(): android.util.DisplayMetrics {
        val dm = android.util.DisplayMetrics()
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        wm.defaultDisplay.getRealMetrics(dm)
        return dm
    }

    init {
        // ... (existing init code)
        val visualWidthDp = 6f
        val visualHeightDp = 50f // Reduced to 50dp as requested
        val hitTargetSizeDp = 48f // Min touch target size
        
        val dm = getDisplayMetrics()
        val vw = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, visualWidthDp, dm).toInt()
        val vh = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, visualHeightDp, dm).toInt()
        val minHitSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, hitTargetSizeDp, dm).toInt()

        // Ensure the Container (Window) is at least 48dp wide
        minimumWidth = minHitSize
        
        handle = View(context).apply {
            background = ContextCompat.getDrawable(context, R.drawable.handle_pill)
            layoutParams = LayoutParams(vw, vh, Gravity.CENTER)
            isClickable = false // Let parent handle clicks
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        
        addView(handle)

        // Setup Gesture Detector for Long Press
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                if (isSwiped) return
                android.util.Log.d("HANDLE_DEBUG", "onLongPress detected")
                isDragging = true
                performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                handle.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).start()
            }
            
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }
        })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isSwiped) {
            // If already swiped, ignore subsequent events until UP/CANCEL resets state
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                isSwiped = false
                isDragging = false // cleanup
                handle.animate().scaleX(1f).scaleY(1f).start()
            }
            return true
        }

        gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialX = event.rawX 
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isSwiped = false
                
                // For a press animation
                handle.animate().scaleX(1.05f).scaleY(1.05f).setDuration(120).setInterpolator(DecelerateInterpolator()).start()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    onPositionChanged(dx, dy)
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                } else {
                    // Check for Swipe Logic
                    val dm = getDisplayMetrics()
                    val screenWidth = dm.widthPixels
                    val swipeThreshold = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40f, dm)
                    
                    val isLeftHandle = initialX < screenWidth / 2
                    val dx = event.rawX - initialTouchX
                    
                    val triggered = if (isLeftHandle) {
                        dx > swipeThreshold // Swipe Right (Inward)
                    } else {
                        dx < -swipeThreshold // Swipe Left (Inward)
                    }
                    
                    if (triggered) {
                        android.util.Log.d("HANDLE_DEBUG", "Swipe to Open detected")
                        isSwiped = true
                        performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                        onSwipeToOpen()
                        // Cancel any pending animations or drag states
                        handle.animate().scaleX(1f).scaleY(1f).start()
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handle.animate().scaleX(1f).scaleY(1f).setDuration(120).setInterpolator(DecelerateInterpolator()).start()
                
                if (isDragging) {
                    isDragging = false
                    snapToEdge(event.rawX)
                }
                isSwiped = false
            }
        }
        return true
    }
    
    private fun snapToEdge(currentRawX: Float) {
        val dm = getDisplayMetrics()
        val screenWidth = dm.widthPixels
        
        // Calculate offsets for 3dp margin
        // Container is approx 48dp. Handle is 6dp. Centered.
        // Left Padding visual = (48 - 6) / 2 = 21dp (approx, depends on exact px rounding)
        // We want visual to be 3dp from edge.
        // Window Left X = 3dp - 21dp = -18dp.
        
        // Reuse same param logic for consistency
        val visualWidthDp = 6f
        val hitTargetSizeDp = 48f
        val marginDp = 3f
        // Add extra correction for right side as user reported it being flush/stuck
        val rightCorrectionDp = 2f 
        
        val visualWidthPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, visualWidthDp, dm)
        val containerWidthPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, hitTargetSizeDp, dm)
        val marginPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, marginDp, dm)
        val rightCorrectionPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, rightCorrectionDp, dm)
        
        val internalPadding = (containerWidthPx - visualWidthPx) / 2f
        
        // Target Window X for Left Snap
        val targetXLeft = marginPx - internalPadding
        
        // Target Window X for Right Snap
        // ScreenWidth - Margin - VisualWidth - InternalPadding = ScreenWidth - Margin - (VisualWidth + Padding)
        // Visual Right Edge = ScreenWidth - Margin.
        // Window Right Edge = Visual Right Edge + InternalPadding.
        // Window Left X = Window Right Edge - ContainerWidth
        // = (ScreenWidth - Margin + InternalPadding) - ContainerWidth
        // = ScreenWidth - Margin - (ContainerWidth - InternalPadding)
        // = ScreenWidth - Margin - (InternalPadding + VisualWidth)
        val targetXRight = screenWidth - marginPx - rightCorrectionPx - (internalPadding + visualWidthPx)
        
        val targetX = if (currentRawX < screenWidth / 2) targetXLeft.toInt() else targetXRight.toInt()
        
        onSnapFinished(targetX, -1) // -1 indicates Y doesn't snap/change
    }

    private fun expandTouchArea(view: View, minDp: Int) {
        val parent = view.parent as? View ?: return
        val rect = Rect()
        view.getHitRect(rect)
        val minPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, minDp.toFloat(), getDisplayMetrics()).toInt()
        val dx = maxOf(0, minPx - rect.width()) / 2
        val dy = maxOf(0, minPx - rect.height()) / 2
        rect.top -= dy; rect.bottom += dy; rect.left -= dx; rect.right += dx
        parent.touchDelegate = TouchDelegate(rect, view)
    }
}
