package com.androids.callrecording.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.androids.callrecording.R
import com.androids.callrecording.activities.AudioPlayerActivity
import com.androids.callrecording.modelclass.CallRecording

class RecordingAdapter(
    var context: Context,
    var deleteClickListener: DeleteClickListener,
    private val recordings: List<CallRecording>
) : RecyclerView.Adapter<RecordingAdapter.ViewHolder>() {

    var deleteListener: DeleteClickListener? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_audio_file, parent, false)
        return ViewHolder(itemView = view)
    }

    override fun getItemCount(): Int = recordings.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val recording = recordings[position]
        holder.fileNameTextView.text = recording.fileName
        holder.fileDuration.text = formatDuration(recording.duration)
        holder.phoneNumber.text = recording.phoneNumber
        holder.calltype.text = recording.callType
        holder.itemView.setOnClickListener {
            val intent = Intent(context, AudioPlayerActivity::class.java)
            intent.putExtra("filePath", recording.filePath)
            intent.putExtra("fileName", recording.fileName)
            context.startActivity(intent)
        }
        holder.deleteFile.setOnClickListener {
            deleteListener = deleteClickListener
            deleteClickListener.removeItem(position)
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileNameTextView: TextView = itemView.findViewById(R.id.fileName)
        val fileDuration: TextView = itemView.findViewById(R.id.fileDuration)
        val phoneNumber: TextView = itemView.findViewById(R.id.phoneNumber)
        val calltype: TextView = itemView.findViewById(R.id.callType)
        val deleteFile: ImageView = itemView.findViewById(R.id.deleteFile)
    }

    private fun formatDuration(duration: Long): String {
        val seconds = (duration / 1000) % 60
        val minutes = (duration / 1000) / 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    interface DeleteClickListener {
        fun removeItem(position: Int)
    }
}