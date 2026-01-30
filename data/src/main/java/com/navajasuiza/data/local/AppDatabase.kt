package com.navajasuiza.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.navajasuiza.data.model.CopiedTextEntity

@Database(entities = [CopiedTextEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun copiedTextDao(): CopiedTextDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "navaja_suiza_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
