package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LicenceProfile::class, TripLog::class], version = 1, exportSchema = false)
abstract class LogbookDatabase : RoomDatabase() {
    abstract fun logbookDao(): LogbookDao

    companion object {
        @Volatile
        private var INSTANCE: LogbookDatabase? = null

        fun getDatabase(context: Context): LogbookDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LogbookDatabase::class.java,
                    "logbook_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
