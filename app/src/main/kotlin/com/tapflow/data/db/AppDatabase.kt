package com.tapflow.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tapflow.data.db.dao.ActionDao
import com.tapflow.data.db.dao.AutomationDao
import com.tapflow.data.db.dao.ExecutionLogDao
import com.tapflow.data.db.dao.RegisteredTagDao
import com.tapflow.data.db.entity.ActionEntity
import com.tapflow.data.db.entity.AutomationEntity
import com.tapflow.data.db.entity.ExecutionLogEntity
import com.tapflow.data.db.entity.RegisteredTagEntity

/**
 * Main Room Database for TapFlow local persistence.
 */
@Database(
    entities = [
        RegisteredTagEntity::class,
        AutomationEntity::class,
        ActionEntity::class,
        ExecutionLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun registeredTagDao(): RegisteredTagDao
    abstract fun automationDao(): AutomationDao
    abstract fun actionDao(): ActionDao
    abstract fun executionLogDao(): ExecutionLogDao

    companion object {
        private const val DATABASE_NAME = "tapflow.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun create(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).build().also { instance = it }
            }
        }
    }
}
