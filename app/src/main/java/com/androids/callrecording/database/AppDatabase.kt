package com.androids.callrecording.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.androids.callrecording.interfaces.CallRecordingDao
import com.androids.callrecording.modelclass.CallRecording

@Database(entities = [CallRecording::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun callRecordingDao(): CallRecordingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "call_recording_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}