package com.navajasuiza.app.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle

/**
 * Trampoline Activity to handle Sharing from Background Service.
 * This activity starts, launches the Share Sheet, and then finishes.
 * Being an Activity ensures the Share Sheet is launched with Foreground priority.
 */
class ShareActivity : Activity() {

    private var isFirstResume = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (savedInstanceState == null) {
            val uri = intent.getParcelableExtra<Uri>("extra_uri")
            if (uri != null) {
                shareUri(uri)
            } else {
                finish()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (!isFirstResume) {
            // We returned from the Chooser
            finish()
        }
        isFirstResume = false
    }
    
    private fun shareUri(uri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            // ClipData is still good practice
            clipData = android.content.ClipData.newRawUri("Snippet", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooser = Intent.createChooser(shareIntent, "Compartir recorte")
        
        // Important: Grant read permission to the chooser intent as well to propagate it
        chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        
        try {
            startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }
}
