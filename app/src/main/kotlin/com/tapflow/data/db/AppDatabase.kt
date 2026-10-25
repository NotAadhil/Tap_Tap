package com.tapflow.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tapflow.data.db.dao.RegisteredTagDao
import com.tapflow.data.db.entity.RegisteredTagEntity

/**
 * Main Room database for TapFlow local persistence.
 */
@Database(
    entities = [RegisteredTagEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun registeredTagDao(): RegisteredTagDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun create(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tapflow.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
