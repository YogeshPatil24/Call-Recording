package com.androids.callrecording

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallRecordingService : Service() {
    private val channelId = "CallRecordingChannel"
    private val notificationId = 1
    private val binder = LocalBinder()
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var startTime: Long = 0L
    private var handler = Handler(Looper.getMainLooper())
    private lateinit var notificationManager: NotificationManager
    private var phoneNumber: String? = null
    private var callType: String? = null

    inner class LocalBinder() : Binder() {
        fun getService(): CallRecordingService = this@CallRecordingService
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(channelId, "Call Recording", NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)
    }

    private fun startForegroundNotification() {
        val notification = buildNotification("00:00:00", true)
        startForeground(notificationId, notification)
    }

    private fun buildNotification(elapsedTime: String, isRecording: Boolean): Notification {
        val stopIntent = Intent(this, CallRecordingService::class.java).apply {
            action = "STOP_RECORDING"
        }

        val resumeIntent = Intent(this, CallRecordingService::class.java).apply {
            action = "START_RECORDING"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(resumeIntent)
        } else {
            startService(resumeIntent)
        }

        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val resumePendingIntent = PendingIntent.getService(this, 1, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Recording in Progress")
            .setContentText("Elapsed Time: $elapsedTime")
            .setSmallIcon(R.drawable.voice)
            .setOngoing(true)
            .addAction(
                if (isRecording) R.drawable.pause else R.drawable.play,
                if (isRecording) "Pause" else "Resume",
                if (isRecording) stopPendingIntent else resumePendingIntent
            )
            .build()
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
        startTime = System.currentTimeMillis()
        startForegroundNotification()
        updateNotification()
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
            stopForeground(false)
            handler.removeCallbacksAndMessages(null)
        }
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

    private fun updateNotification() {
        handler.postDelayed({
            val elapsedTime = System.currentTimeMillis() - startTime
            val formattedTime = formatElapsedTime(elapsedTime)
            notificationManager.notify(notificationId, buildNotification(formattedTime, isRecording))
            updateNotification()
        }, 1000)
    }

    @SuppressLint("DefaultLocale")
    private fun formatElapsedTime(elapsedTime: Long): String {
        val seconds = (elapsedTime / 1000) % 60
        val minutes = (elapsedTime / 1000 / 60) % 60
        val hours = (elapsedTime / 1000 / 3600)
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return START_NOT_STICKY
        }
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

}