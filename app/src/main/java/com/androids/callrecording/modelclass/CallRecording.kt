package com.androids.callrecording.modelclass

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class CallRecording(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String,
    val filePath: String,
    val timestamp: Long,
    val duration: Long,
    val phoneNumber: String,
    val callType: String
)
