package com.navajasuiza.history.gallery

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.FileProvider
import com.navajasuiza.data.repository.SnippetRepository
import java.io.File
import kotlin.math.abs

class SnippetViewer @JvmOverloads constructor(
    context: Context,
    private val files: List<File>,
    initialIndex: Int = 0,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onClose: (() -> Unit)? = null
    var onDelete: (() -> Unit)? = null
    var onShare: (() -> Unit)? = null

    private val repository = SnippetRepository(context)
    private var currentIndex = initialIndex
    private val imageView: ImageView
    private val statusTextView: TextView
    
    // ... existing Gesture Detector ... 

    // ... init ...

    // ... existing showMessage, onTouchEvent, showImage, etc ...
    
    private fun shareImage() {
        try {
            val file = files.getOrNull(currentIndex) ?: return
            
            val uri = FileProvider.getUriForFile(
                context,
                "com.jhosue.gravilens.provider",
                file
            )
            
            // Launch the Trampoline Activity
            val intent = Intent()
            intent.component = android.content.ComponentName(context.packageName, "com.navajasuiza.app.ui.ShareActivity")
            intent.putExtra("extra_uri", uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Required because we are in Overlay context
            
            // Grant permissions to the Trampoline Activity so it can pass them on
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) 
            intent.clipData = android.content.ClipData.newRawUri("Snippet", uri)
            
            context.startActivity(intent)
            
            onShare?.invoke()
            
        } catch (e: Exception) {
             e.printStackTrace()
             showMessage("Posiblemente necesites reinstalar la app para registrar la nueva actividad.")
        }
    }
    
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true
        
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (e1 == null) return false
            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y
            
            if (abs(diffX) > abs(diffY) && abs(diffX) > 100 && abs(velocityX) > 100) {
                if (diffX > 0) {
                    showPreviousImage()
                } else {
                    showNextImage()
                }
                return true
            }
            return false
        }
    })

    init {
        setBackgroundColor(Color.BLACK)
        
        // 1. Image View
        imageView = ImageView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        addView(imageView)
        
        // 2. Status Message (Overlay for visible feedback)
        // 2. Status Message (Overlay for visible feedback)
        statusTextView = TextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                setMargins(32, 32, 32, 180) // Higher margin to avoid nav bar intersection
            }
            
            // Capsule Background
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 100f
                setColor(Color.parseColor("#CC202020"))
                setStroke(2, Color.parseColor("#555555"))
            }
            
            setTextColor(Color.WHITE)
            setPadding(60, 30, 60, 30)
            textSize = 14f
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            elevation = 20f
            visibility = View.GONE
        }
        addView(statusTextView)
        
        // 3. Toolbar (Top)
        val toolbar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(16, 16, 16, 16)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.TOP
            }
            background = android.graphics.drawable.GradientDrawable().apply {
                colors = intArrayOf(Color.parseColor("#80000000"), Color.TRANSPARENT)
                orientation = android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM
            }
        }
        
        // 3.1 Share Button
        val btnShare = ImageButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(120, 120) 
            setImageResource(android.R.drawable.ic_menu_share)
            background = null
            setColorFilter(Color.WHITE)
            setOnClickListener { shareImage() }
        }
        
        // 3.2 Delete Button
        val btnDelete = ImageButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(120, 120)
            setImageResource(android.R.drawable.ic_menu_delete)
            background = null
            setColorFilter(Color.WHITE)
            setOnClickListener { deleteImage() }
        }
        
        // 3.3 Close Button
        val btnClose = ImageButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(120, 120)
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            background = null
            setColorFilter(Color.WHITE)
            setOnClickListener { onClose?.invoke() }
        }
        
        toolbar.addView(btnShare)
        toolbar.addView(btnDelete)
        toolbar.addView(btnClose)
        
        addView(toolbar)
        
        showImage(currentIndex)
        
        // Input Handling: Capture Back Button & System Events
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()
    }
    
    override fun dispatchKeyEvent(event: android.view.KeyEvent?): Boolean {
        if (event?.keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
            onClose?.invoke()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!hasWindowFocus) {
            // Close on Home/Recents
            onClose?.invoke()
        }
    }
    
    private val hideStatusRunnable = Runnable {
        statusTextView.visibility = View.GONE
        statusTextView.animate().alpha(0f).duration = 300
    }

    private fun showMessage(text: String) {
        statusTextView.text = text
        statusTextView.alpha = 1f
        statusTextView.visibility = View.VISIBLE
        statusTextView.removeCallbacks(hideStatusRunnable)
        statusTextView.postDelayed(hideStatusRunnable, 2000)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }
    
    private fun showImage(index: Int) {
        if (files.isEmpty()) return
        val validIndex = index.coerceIn(0, files.size - 1)
        currentIndex = validIndex
        
        val bitmap = BitmapFactory.decodeFile(files[validIndex].absolutePath)
        imageView.setImageBitmap(bitmap)
    }
    
    private fun showPreviousImage() {
        if (currentIndex > 0) {
            showImage(currentIndex - 1)
        } else {
            showMessage("Primera imagen")
        }
    }
    
    private fun showNextImage() {
        if (currentIndex < files.size - 1) {
            showImage(currentIndex + 1)
        } else {
            showMessage("Última imagen")
        }
    }
    

    
    private fun deleteImage() {
        val file = files.getOrNull(currentIndex) ?: return
        if (repository.deleteSnippet(file)) {
            showMessage("Eliminado")
            postDelayed({
                onDelete?.invoke()
            }, 600)
        } else {
            showMessage("Error al eliminar")
        }
    }
}
