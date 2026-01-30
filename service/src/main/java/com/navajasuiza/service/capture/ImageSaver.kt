package com.navajasuiza.service.capture

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageSaver {
    
    private const val DIRECTORY_NAME = "NavajaSnips"
    
    fun saveBitmap(context: Context, bitmap: Bitmap): File? {
        // Use external files dir (app-specific, no storage permissions needed on Android 10+)
        val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val snipDir = File(picturesDir, DIRECTORY_NAME)
        
        if (!snipDir.exists()) {
            if (!snipDir.mkdirs()) {
                return null
            }
        }
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "snip_$timestamp.png"
        val file = File(snipDir, fileName)
        
        return try {
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
