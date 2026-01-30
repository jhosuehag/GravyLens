package com.navajasuiza.features.history

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.navajasuiza.features.history.databinding.ViewFloatingHistoryBinding
import com.navajasuiza.history.HistoryAdapter
import com.navajasuiza.history.HistoryController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FloatingHistoryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ViewFloatingHistoryBinding
    private var controller: HistoryController? = null
    private var adapter: HistoryAdapter? = null
    private val viewScope = CoroutineScope(Dispatchers.Main + Job())
    
    var onCloseClick: (() -> Unit)? = null

    init {
        binding = ViewFloatingHistoryBinding.inflate(android.view.LayoutInflater.from(context), this, true)
        setupUI()
        
        // Input Handling: Capture Back Button & System Events
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()
    }
    
    override fun dispatchKeyEvent(event: android.view.KeyEvent?): Boolean {
        if (event?.keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
            onCloseClick?.invoke()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!hasWindowFocus) {
           onCloseClick?.invoke()
        }
    }
    
    fun initialize(controller: HistoryController) {
        this.controller = controller
        setupAdapter()
        observeData()
    }

    private fun setupUI() {
        binding.btnClose.setOnClickListener { onCloseClick?.invoke() }
        
        binding.etSearch.addTextChangedListener { text ->
            controller?.updateSearch(text?.toString() ?: "")
        }
        
        binding.btnClearHistory.setOnClickListener {
            val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Borrar Todo")
                .setMessage("¿Estás seguro de vaciar el historial?")
                .setPositiveButton("Sí") { _, _ -> controller?.clearHistory() }
                .setNegativeButton("No", null)
                .create()
                
            // Necessary for showing dialog from a Service/Overlay context
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                dialog.window?.setType(android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            } else {
                dialog.window?.setType(android.view.WindowManager.LayoutParams.TYPE_PHONE)
            }
            
            dialog.show()
        }
        
        binding.rvHistory.layoutManager = LinearLayoutManager(context)
    }

    private fun setupAdapter() {
        adapter = HistoryAdapter(
            onItemClick = { item ->
                // Copy and maybe close?
                copyToClipboard(item.text)
            },
            onCopyClick = { item ->
                copyToClipboard(item.text)
            },
            onDeleteClick = { item ->
                controller?.delete(item)
                showFeedback("Elemento eliminado")
            }
        )
        binding.rvHistory.adapter = adapter
    }

    private fun observeData() {
        viewScope.launch {
            controller?.filteredHistory?.collectLatest { list ->
                adapter?.submitList(list)
                binding.tvEmptyState.visibility = if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
    }
    
    private fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("History", text)
        clipboard.setPrimaryClip(clip)
        
        showFeedback("Copiado al portapapeles")
    }

    private fun showFeedback(message: String) {
        // Use TextView with GradientDrawable to avoid CardView theming issues in Service context
        val textView = android.widget.TextView(context).apply {
            text = message
            setTextColor(android.graphics.Color.WHITE)
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            
            // Programmatic rounded background
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                setColor(android.graphics.Color.rgb(50, 50, 50))
                cornerRadius = 100f // Fully rounded
            }
            
            // Padding
            val padHorizontal = (24 * resources.displayMetrics.density).toInt()
            val padVertical = (12 * resources.displayMetrics.density).toInt()
            setPadding(padHorizontal, padVertical, padHorizontal, padVertical)
            
            // Elevation
            elevation = 100f
        }
        
        val params = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
            bottomMargin = (80 * resources.displayMetrics.density).toInt() // Lift it up a bit more
        }
        
        // Ensure running on UI thread (just in case)
        post {
            try {
                addView(textView, params)
                
                // Animate
                textView.alpha = 0f
                textView.translationY = 50f
                textView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(200)
                    .start()
                
                // Remove
                textView.postDelayed({
                    textView.animate()
                        .alpha(0f)
                        .translationY(50f)
                        .setDuration(200)
                        .withEndAction {
                            try { removeView(textView) } catch (e: Exception) {}
                        }.start()
                }, 2000)
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to Toast if everything explodes
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewScope.cancel()
    }
}
