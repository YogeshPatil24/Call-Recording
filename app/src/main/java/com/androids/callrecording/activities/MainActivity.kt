package com.androids.callrecording.activities

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.androids.callrecording.CallRecordingService
import com.androids.callrecording.CallRecordingViewModel
import com.androids.callrecording.R
import com.androids.callrecording.adapters.RecordingAdapter
import com.androids.callrecording.databinding.ActivityMainBinding
import com.androids.callrecording.modelclass.CallRecording
import java.io.File

@RequiresApi(Build.VERSION_CODES.P)
class MainActivity : AppCompatActivity() {

    private lateinit var mBinder: ActivityMainBinding
    private lateinit var callRecordAdapter: RecordingAdapter
    private lateinit var viewmodel: CallRecordingViewModel
    private var isReceiverRegistered = false
    private val recordings = mutableListOf<CallRecording>()


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val requiredPermissions = mutableListOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.READ_CALL_LOG,
        android.Manifest.permission.PROCESS_OUTGOING_CALLS,
        android.Manifest.permission.READ_PHONE_STATE,
        android.Manifest.permission.READ_PHONE_NUMBERS,
        android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
        android.Manifest.permission.FOREGROUND_SERVICE,
    ).apply {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            add(android.Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }.toTypedArray()

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 123
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinder = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setContentView(mBinder.root)
        initUI()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun initUI() {

        if (!arePermissionsGranted()) {
            requestPermissions()
        }

        viewmodel = ViewModelProvider(this).get(CallRecordingViewModel::class.java)

        mBinder.apply {
            startrecordCall.setOnClickListener {
                startService(Intent(this@MainActivity, CallRecordingService::class.java))
            }
            stoprecordCall.setOnClickListener {
                stopService(Intent(this@MainActivity, CallRecordingService::class.java))
            }
            mainRecyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
            callRecordAdapter = RecordingAdapter(
                this@MainActivity,
                object : RecordingAdapter.DeleteClickListener {
                    override fun removeItem(position: Int) {
                        recordings.removeAt(position)
                        callRecordAdapter.notifyItemRemoved(position)
                        callRecordAdapter.notifyItemRangeChanged(position, recordings.size)
                    }
                },
                recordings
            )
            mainRecyclerView.adapter = callRecordAdapter
        }
        loadRecordings()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadRecordings() {
        val dir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "CallRecordings")
        if (dir.exists()) {
            recordings.clear()

            dir.listFiles()?.forEach { file ->
                val duration = getRecordingDuration(file)
                val timestamp = file.lastModified()
                val metadataFile = File(file.absolutePath + ".meta")
                val metadata = if (metadataFile.exists()) {
                    metadataFile.readLines().joinToString(separator = "\n")
                } else {
                    null
                }
                val phoneNumber =
                    metadata?.substringAfter("PhoneNumber: ")?.substringBefore("\n") ?: "Unknown"
                val callType =
                    metadata?.substringAfter("CallType: ")?.substringBefore("\n") ?: "Unknown"
                recordings.add(
                    CallRecording(
                        fileName = file.name,
                        filePath = file.absolutePath,
                        timestamp = timestamp,
                        duration = duration,
                        phoneNumber = phoneNumber,
                        callType = callType
                    )
                )
            }
            callRecordAdapter.notifyDataSetChanged()
        }
    }

    fun addRecording(recording: CallRecording) {
        mBinder = DataBindingUtil.setContentView(this, R.layout.activity_main)
        recordings.add(0, recording)
        callRecordAdapter =
            RecordingAdapter(this@MainActivity, object : RecordingAdapter.DeleteClickListener {
                override fun removeItem(position: Int) {
                    recordings.removeAt(position)
                    callRecordAdapter.notifyItemRemoved(position)
                    callRecordAdapter.notifyItemRangeChanged(position, recordings.size)
                }

            }, recordings)
        callRecordAdapter.notifyItemInserted(0)
        mBinder.mainRecyclerView.scrollToPosition(0)
        Toast.makeText(
            this,
            "New recording added: ${recording.phoneNumber},Type:  ${recording.callType}",
            Toast.LENGTH_SHORT
        )
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun arePermissionsGranted(): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSIONS_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            val deniedPermissions = permissions.filterIndexed { index, _ ->
                grantResults[index] != PackageManager.PERMISSION_GRANTED
            }

            if (deniedPermissions.isEmpty()) {
                Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "Permissions denied: ${deniedPermissions.joinToString(", ")}. App cannot function properly.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onResume() {
        super.onResume()
        if (!isReceiverRegistered) {
            val filter = IntentFilter(resources.getResourceName(R.string.BroadCast_Name))
            registerReceiver(recordingReceiver, filter, RECEIVER_EXPORTED)
            isReceiverRegistered = true
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            if (isReceiverRegistered) {
                unregisterReceiver(recordingReceiver)
                isReceiverRegistered = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun getRecordingDuration(file: File): Long {
        if (!file.exists() || !file.canRead()) {
            Log.e("MainActivity", "File not found or not readable: ${file.absolutePath}")
            return 0L
        }

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durationStr =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLong() ?: 0L // Duration in milliseconds
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MainActivity", "Failed to retrieve duration: ${e.message}")
            0L
        } finally {
            retriever.release()
        }
    }

    private val recordingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val fileName = intent!!.getStringExtra("fileName") ?: return
            val filePath = intent.getStringExtra("filePath") ?: return
            val timestamp = intent.getLongExtra("timestamp", 0L)
            val duration = intent.getLongExtra("duration", 0L)
            val phoneNumber = intent.getStringExtra("phoneNumber") ?: "Unknown"
            val callType = intent.getStringExtra("callType") ?: "Unknown"

            val recording = CallRecording(
                fileName = fileName,
                filePath = filePath,
                timestamp = timestamp,
                duration = duration,
                phoneNumber = phoneNumber,
                callType = callType
            )
            addRecording(recording)
        }
    }

    private fun readRecordingMetadata(filePath: String): Pair<String, String>? {
        val file = File("$filePath.meta") // Assuming metadata is stored in `.meta` files
        if (file.exists()) {
            val lines = file.readLines()
            val phoneNumber = lines.getOrNull(0)
            val callType = lines.getOrNull(1)
            return Pair(phoneNumber!!, callType!!)
        }
        return null
    }

}

