package com.androids.callrecording.interfaces

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.androids.callrecording.modelclass.CallRecording

@Dao
interface CallRecordingDao {
    @Insert
    suspend fun insert(callRecordingDao: CallRecording)

    @Query("SELECT * FROM CallRecording ORDER BY timestamp DESC")
    fun getAllRecordings(): LiveData<List<CallRecording>>
}