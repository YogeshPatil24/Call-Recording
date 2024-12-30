package com.androids.callrecording

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.P)
class CallStateReceiver : BroadcastReceiver() {
    private var isOutgoing = false
    private var isRecording = false

    @SuppressLint("ServiceCast", "UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent!!.action
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        when (action) {
            Intent.ACTION_NEW_OUTGOING_CALL -> {
                isOutgoing = true
                val outgoingNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER) ?: "Unknown"
                startRecordingService(context!!, outgoingNumber, "Outgoing")
            }

            "android.intent.action.PHONE_STATE" -> {
                when (state) {
                    TelephonyManager.EXTRA_STATE_RINGING -> {
                        isOutgoing = false
                        if (incomingNumber != null) {
                            startRecordingService(context!!, incomingNumber, "Incoming")
                        }
                    }

                    TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                        if (!isRecording) {
                            val outgoingNumber =
                                intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER) ?: incomingNumber
                            if (outgoingNumber != null) {
                                startRecordingService(context!!, outgoingNumber, "Outgoing")
                            }
                            isRecording = true
                        }

                    }

                    TelephonyManager.EXTRA_STATE_IDLE -> {
                        if (isRecording) {
                            stopRecordingService(context!!)
                            isRecording = false
                        }
                    }
                }
            }
        }
        /*        val fileName = intent?.getStringExtra("fileName") ?: return
                val filePath = intent.getStringExtra("filePath") ?: return
                val timestamp = intent.getLongExtra("timestamp", 0L)
                val duration = intent.getLongExtra("duration", 0L)
        //        val phonenumber = intent.getLongExtra("")

                val recording =
                    CallRecording(
                        fileName = fileName,
                        filePath = filePath,
                        timestamp = timestamp,
                        duration = duration,
                        phoneNumber = phoneNumber!!
                    )
                MainActivity().addRecording(recording)*/
    }

    private fun startRecordingService(context: Context, phoneNumber: String, callType: String) {
        val serviceIntent = Intent(context, CallRecordingService::class.java).apply {
            action = "START_RECORDING"
            putExtra("phoneNumber", phoneNumber)
            putExtra("callType", callType)
        }
        context.startService(serviceIntent)
    }

    private fun stopRecordingService(context: Context) {
        val serviceIntent = Intent(context, CallRecordingService::class.java).apply {
            action = "STOP_RECORDING"
        }
        context.startService(serviceIntent)
    }

}