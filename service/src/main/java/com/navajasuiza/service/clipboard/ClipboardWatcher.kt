package com.navajasuiza.service.clipboard

import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ClipboardWatcher(context: Context) {

    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val _clipboardFlow = MutableSharedFlow<String>(replay = 0)
    val clipboardFlow: SharedFlow<String> = _clipboardFlow.asSharedFlow()

    private var lastCopiedText: String? = null

    private val listener = ClipboardManager.OnPrimaryClipChangedListener {
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val rawText = clip.getItemAt(0).text?.toString()
            if (!rawText.isNullOrBlank()) {
                val text = rawText.trim()
                if (text.isNotEmpty() && text != lastCopiedText) {
                    lastCopiedText = text
                    _clipboardFlow.tryEmit(text)
                }
            }
        }
    }

    fun start() {
        try {
            clipboard.addPrimaryClipChangedListener(listener)
        } catch (e: Exception) {
            // Monitor might fail on some background restrictions
            e.printStackTrace()
        }
    }

    fun stop() {
        try {
            clipboard.removePrimaryClipChangedListener(listener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
