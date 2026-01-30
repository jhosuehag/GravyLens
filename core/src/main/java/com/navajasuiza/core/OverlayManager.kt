package com.navajasuiza.core

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager

class OverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var handleView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    fun showHandle(view: View, x: Int, y: Int) {
        if (handleView != null) {
            android.util.Log.d("OVERLAY_DEBUG", "Handle already shown, ignoring showHandle()")
            return
        }

        android.util.Log.d("OVERLAY_DEBUG", "Intentando añadir overlay en x=$x, y=$y")
        handleView = view
        
        // Use WRAP_CONTENT for width/height to respect the FloatingHandleView's size
        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }

        try {
            windowManager.addView(handleView, layoutParams)
            android.util.Log.d("OVERLAY_DEBUG", "Overlay añadido correctamente")
        } catch (e: Exception) {
            android.util.Log.e("OVERLAY_DEBUG", "Error añadiendo overlay: ${e.message}")
            e.printStackTrace()
        }
    }

    fun hideHandle() {
        handleView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            handleView = null
        }
    }

    fun updatePosition(x: Int, y: Int) {
        layoutParams?.let { params ->
            params.x = x
            params.y = y
            handleView?.let { view ->
                windowManager.updateViewLayout(view, params)
            }
        }
    }
    
    fun getLayoutParams(): WindowManager.LayoutParams? = layoutParams
    
    fun getView(): View? = handleView

    // Panel Management
    private var panelView: View? = null

    fun showPanel(view: View) {
        if (panelView != null) {
            hidePanel()
        }
        
        panelView = view
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
             gravity = Gravity.CENTER
        }

        try {
            windowManager.addView(panelView, params)
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun hidePanel() {
        panelView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            panelView = null
        }
    }
}
