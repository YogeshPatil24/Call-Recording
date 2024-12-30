package com.androids.callrecording

import android.app.Service
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.os.Environment
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallRecordingService : Service() {
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private var phoneNumber: String? = null
    private var callType: String? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {
            "START_RECORDING" -> {
                phoneNumber = intent.getStringExtra("phoneNumber") ?: "Unknown"
                callType = intent.getStringExtra("callType") ?: "Unknown"
                startRecording()
            }

            "STOP_RECORDING" -> {
                stopRecording()
            }
        }
        return START_STICKY
    }

    private fun startRecording() {
        if (isRecording) return

        Log.d("SERVICEEEE", "startRecording: recording started.")
        val dir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "CallRecordings")
        if (!dir.exists()) dir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filePath = "${dir.absolutePath}/Call_$timestamp.mp3"

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(filePath)
            prepare()
            start()
        }
        isRecording = true
        showToast("Recording started..")
    }

    private fun stopRecording() {
        if (!isRecording) return
        Log.d("SERVICEEEE", "stopRecording: recording stoped.")
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            isRecording = false
        }

        showToast("Recording stopped..")

        // Send the recorded file details
        val dir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "CallRecordings")
        val files = dir.listFiles()
        if (files != null && files.isNotEmpty()) {
            val latestFile = files.maxByOrNull { it.lastModified() } // Get the latest file
            if (latestFile != null) {
                val duration = getRecordingDuration(latestFile)
                val intent = Intent(resources.getResourceName(R.string.BroadCast_Name)).apply {
                    putExtra("fileName", latestFile.name)
                    putExtra("filePath", latestFile.absolutePath)
                    putExtra("timestamp", latestFile.lastModified())
                    putExtra("duration", duration)
                    putExtra("phoneNumber", phoneNumber)
                    putExtra("callType", callType)
                }
                saveRecordingMetadata(
                    latestFile.absolutePath,
                    phoneNumber ?: "Unknown",
                    callType ?: "Unknown"
                )
                sendBroadcast(intent)
            }
        }
    }

    private fun getRecordingDuration(file: File): Long {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(file.absolutePath)
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        retriever.release()
        return durationStr?.toLong() ?: 0L
    }

    override fun onDestroy() {
        Log.d("service", "destroy")
        stopRecording()
        super.onDestroy()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun saveRecordingMetadata(filePath: String, phoneNumber: String, callType: String) {
        val metadataFile = File("$filePath.meta")
        metadataFile.writeText("PhoneNumber: $phoneNumber\nCallType: $callType")
    }
}