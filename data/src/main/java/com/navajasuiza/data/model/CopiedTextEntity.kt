package com.navajasuiza.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "copied_text")
data class CopiedTextEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val source: String? = null, // e.g. "clipboard", "ocr"
    val timestamp: Long = System.currentTimeMillis()
)
