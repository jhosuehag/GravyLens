package com.navajasuiza.data.local

import androidx.room.*
import com.navajasuiza.data.model.CopiedTextEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CopiedTextDao {

    @Query("SELECT * FROM copied_text ORDER BY timestamp DESC LIMIT :limit")
    fun getAll(limit: Int = 50): Flow<List<CopiedTextEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CopiedTextEntity): Long

    @Query("DELETE FROM copied_text WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM copied_text")
    suspend fun clearAll()

    @Query("SELECT * FROM copied_text WHERE text LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    suspend fun search(query: String): List<CopiedTextEntity>

    // Pruning logic: Keep only top N newest
    @Query("DELETE FROM copied_text WHERE id NOT IN (SELECT id FROM copied_text ORDER BY timestamp DESC LIMIT :keepCount)")
    suspend fun prune(keepCount: Int = 50)

    @Transaction
    suspend fun insertAndPrune(entity: CopiedTextEntity, limit: Int = 50) {
        insert(entity)
        prune(limit)
    }
}
