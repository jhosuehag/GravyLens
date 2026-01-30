package com.navajasuiza.app.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.navajasuiza.app.MainActivity
import com.navajasuiza.app.R

class OverlayPermissionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_overlay_permission)

        val btnGrant = findViewById<Button>(R.id.btn_grant_overlay)
        val tvStatus = findViewById<TextView>(R.id.tv_permission_status)

        btnGrant.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, REQUEST_CODE_OVERLAY)
            } else {
                finishWithResult(true)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OVERLAY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    finishWithResult(true)
                } else {
                    // User denied permission
                }
            } else {
                 finishWithResult(true)
            }
        }
    }
    
    private fun finishWithResult(success: Boolean) {
        setResult(if (success) Activity.RESULT_OK else Activity.RESULT_CANCELED)
        finish()
    }

    companion object {
        private const val REQUEST_CODE_OVERLAY = 1001
    }
}
