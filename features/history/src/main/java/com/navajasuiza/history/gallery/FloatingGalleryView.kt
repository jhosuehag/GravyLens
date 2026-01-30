package com.navajasuiza.history.gallery

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.navajasuiza.data.repository.SnippetRepository
import com.navajasuiza.features.history.R
import kotlinx.coroutines.*
import java.io.File

class FloatingGalleryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onCloseClick: (() -> Unit)? = null
    var onSnippetClick: ((File) -> Unit)? = null

    private val repository = SnippetRepository(context)
    private val scope = CoroutineScope(Dispatchers.Main + Job()) // UI Scope
    private val adapter: SnippetAdapter

    init {
        LayoutInflater.from(context).inflate(R.layout.view_floating_gallery, this, true)

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_gallery)
        val emptyText = findViewById<View>(R.id.text_empty_gallery)
        val closeBtn = findViewById<View>(R.id.btn_close_gallery)

        closeBtn.setOnClickListener { onCloseClick?.invoke() }
        
        // Input Handling: Capture Back Button & System Events
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()

        // Grid Layout (3 columns)
        recyclerView.layoutManager = GridLayoutManager(context, 3)

        adapter = SnippetAdapter(
            repository = repository,
            scope = scope,
            onItemClick = { file ->
                onSnippetClick?.invoke(file)
            },
            onItemLongClick = { file ->
                showActionDialog(file)
            }
        )
        recyclerView.adapter = adapter

        // Load data
        loadData(emptyText)
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

    fun reload() {
         loadData(findViewById(R.id.text_empty_gallery))
    }

    private fun loadData(emptyView: View) {
        scope.launch(Dispatchers.IO) {
            val list = repository.getSnippets()
            withContext(Dispatchers.Main) {
                adapter.submitList(list)
                emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
    
    private fun showActionDialog(file: File) {
        // Simple Context Menu logic
        scope.launch(Dispatchers.IO) {
            val deleted = repository.deleteSnippet(file)
            withContext(Dispatchers.Main) {
                if (deleted) {
                    android.widget.Toast.makeText(context, "Eliminado", android.widget.Toast.LENGTH_SHORT).show()
                    reload()
                } else {
                    android.widget.Toast.makeText(context, "Error al eliminar", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }
}
