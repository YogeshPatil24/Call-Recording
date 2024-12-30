package com.androids.callrecording

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.androids.callrecording.database.AppDatabase
import com.androids.callrecording.modelclass.CallRecording
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallRecordingViewModel(application: Application) : AndroidViewModel(application) {
    private val callRecordingDao = AppDatabase.getDatabase(application).callRecordingDao()

    val recordings: LiveData<List<CallRecording>> = callRecordingDao.getAllRecordings()

    fun addRecording(callRecording: CallRecording) {
        CoroutineScope(Dispatchers.IO).launch {
            callRecordingDao.insert(callRecording)
        }
    }

}