package com.navajasuiza.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.navajasuiza.app.ui.OverlayPermissionActivity
import com.navajasuiza.service.FloatingService

class MainActivity : AppCompatActivity() {
    
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (checkOverlayPermission()) {
            startServiceFlow()
        } else {
            Toast.makeText(this, "Permission required to start service", Toast.LENGTH_SHORT).show()
        }
    }

    private val mediaProjectionManager by lazy {
        getSystemService(android.content.Context.MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
    }

    private val captureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            startFloatingService(result.resultCode, result.data!!)
        } else {
            Toast.makeText(this, "Permission denied. Service requires screen capture.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startFloatingService(resultCode: Int, data: Intent) {
        val intent = Intent(this, FloatingService::class.java).apply {
             putExtra("extra_result_code", resultCode)
             putExtra("extra_data", data)
             action = "com.navajasuiza.service.action.START_WITH_PERMISSION"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "Service Started", Toast.LENGTH_SHORT).show()
    }

    private fun startServiceFlow() {
        // Step 2: Request Screen Capture Permission
        captureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btn_start_service).setOnClickListener {
            if (checkOverlayPermission()) {
                startServiceFlow()
            } else {
                requestOverlayPermission()
            }
        }
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(this, OverlayPermissionActivity::class.java)
        overlayPermissionLauncher.launch(intent)
    }
}
