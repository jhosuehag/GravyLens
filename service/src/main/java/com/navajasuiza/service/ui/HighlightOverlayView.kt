package com.navajasuiza.service.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import com.navajasuiza.ocr.OcrBlock

class HighlightOverlayView(context: Context) : View(context) {

    private var backgroundBitmap: Bitmap? = null
    private var ocrBlocks: List<OcrBlock> = emptyList()
    private var onBlockClick: ((String) -> Unit)? = null
    private var onCloseClick: (() -> Unit)? = null

    private val boxPaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.STROKE
        strokeWidth = 4f
        alpha = 200
    }

    private val fillPaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.FILL
        alpha = 70 // Semi-transparent
    }
    
    // Close button (X) top-right estimation
    private val closeButtonRect = Rect()
    private val closePaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        alpha = 180
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 60f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    fun setData(bitmap: Bitmap, blocks: List<OcrBlock>, onBlockClick: (String) -> Unit, onClose: () -> Unit) {
        this.backgroundBitmap = bitmap
        this.ocrBlocks = blocks
        this.onBlockClick = onBlockClick
        this.onCloseClick = onClose
        invalidate()
        
        // Ensure we capture input
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

    private val boxPadding = 0

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw Screenshot Background
        backgroundBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }

        // Draw OCR Blocks with padding
        ocrBlocks.forEach { block ->
            val rect = Rect(block.boundingBox)
            rect.inset(-boxPadding, -boxPadding)
            
            canvas.drawRect(rect, fillPaint)
            canvas.drawRect(rect, boxPaint)
        }
        
        // Draw Close Button (Top-Right)
        val padding = 50
        val size = 120
        val screenWidth = width
        closeButtonRect.set(screenWidth - size - padding, padding, screenWidth - padding, size + padding)
        
        canvas.drawRect(closeButtonRect, closePaint)
        canvas.drawText("X", closeButtonRect.centerX().toFloat(), closeButtonRect.centerY().toFloat() + 20, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val x = event.x.toInt()
            val y = event.y.toInt()

            // Check close button
            if (closeButtonRect.contains(x, y)) {
                onCloseClick?.invoke()
                return true
            }

            // Check blocks with padded rect
            val clickedBlock = ocrBlocks.find { 
                val rect = Rect(it.boundingBox)
                rect.inset(-boxPadding, -boxPadding)
                rect.contains(x, y) 
            }
            clickedBlock?.let {
                onBlockClick?.invoke(it.text)
                return true
            }
        }
        return true // Consume all touches to "freeze" the app
    }
    
    // Add logic to recycle bitmap when view detached?
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Do not recycle bitmap here if it's managed by Service/CaptureManager
        // But we should null it out
        backgroundBitmap = null
    }
}
