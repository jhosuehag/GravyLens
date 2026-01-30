package com.navajasuiza.data.repository

import com.navajasuiza.data.local.CopiedTextDao
import com.navajasuiza.data.model.CopiedTextEntity
import kotlinx.coroutines.flow.Flow

class ClipboardRepository(private val dao: CopiedTextDao) {

    val allHistory: Flow<List<CopiedTextEntity>> = dao.getAll()

    suspend fun addCopiedText(text: String, source: String = "clipboard") {
        val entity = CopiedTextEntity(text = text, source = source)
        dao.insertAndPrune(entity)
    }

    suspend fun deleteItem(id: Long) {
        dao.delete(id)
    }
    
    suspend fun clearHistory() {
        dao.clearAll()
    }
}
