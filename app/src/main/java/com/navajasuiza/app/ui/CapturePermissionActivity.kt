package com.navajasuiza.app.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle

/**
 * Transparent Activity to request MediaProjection permission.
 * It sends the result back to FloatingService.
 */
class CapturePermissionActivity : Activity() {

    companion object {
        private const val REQUEST_CODE_CAPTURE_PERM = 1001
        // Matches the action in FloatingService
        const val ACTION_PERMISSION_RESULT = "com.navajasuiza.service.action.PERMISSION_RESULT"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_DATA = "extra_data"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No UI, just start permission flow
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_CAPTURE_PERM)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CAPTURE_PERM) {
            // Send result back to Service
            if (resultCode == RESULT_OK && data != null) {
                sendPermissionToService(resultCode, data)
            }
        }
        finish() // Close activity immediately
    }

    private fun sendPermissionToService(resultCode: Int, data: Intent) {
        val serviceIntent = Intent()
        // We use explicit class name or action. Since Service is in another module but we depend on it...
        // Actually, 'app' depends on 'service' (usually not, purely clean arch, but here 'app' builds 'service').
        // However, to be safe and decoupled, we use class name string or Action.
        // Let's use the explicit ComponentName if we know it, or Action.
        // For simplicity:
        serviceIntent.setClassName(this, "com.navajasuiza.service.FloatingService")
        serviceIntent.action = ACTION_PERMISSION_RESULT
        serviceIntent.putExtra(EXTRA_RESULT_CODE, resultCode)
        serviceIntent.putExtra(EXTRA_DATA, data)
        startService(serviceIntent)
    }
}
