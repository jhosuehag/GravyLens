package com.navajasuiza.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import java.io.File

class SnippetRepository(private val context: Context) {

    private val snippetsDir: File
        get() = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "NavajaSnips")

    fun getSnippets(): List<File> {
        val dir = snippetsDir
        if (!dir.exists()) return emptyList()

        return dir.listFiles { file ->
            file.isFile && (file.name.endsWith(".png") || file.name.endsWith(".jpg"))
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun deleteSnippet(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Efficiently loads a thumbnail for the given file.
     */
    fun getThumbnail(file: File, reqWidth: Int, reqHeight: Int): Bitmap? {
        // First decode with inJustDecodeBounds=true to check dimensions
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(file.absolutePath, options)

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        return try {
            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}
