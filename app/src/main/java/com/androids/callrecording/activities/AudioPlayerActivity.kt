package com.androids.callrecording.activities

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.androids.callrecording.R
import com.androids.callrecording.databinding.ActivityAudioPlayerBinding

class AudioPlayerActivity : AppCompatActivity() {
    lateinit var mBinder: ActivityAudioPlayerBinding

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var currentPos = 0
    private lateinit var handler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinder = DataBindingUtil.setContentView(this, R.layout.activity_audio_player)
        initUI()
        setContentView(mBinder.root)
    }

    @SuppressLint("SetTextI18n")
    private fun initUI() {
        val filePath = intent.getStringExtra("filePath") ?: return
        val fileName = intent.getStringExtra("fileName") ?: return
        mediaPlayer = MediaPlayer()
        mediaPlayer!!.setDataSource(filePath)
        mediaPlayer!!.prepare()


        mBinder.apply {
            tvRecordingTitle.text = fileName
            tvTotalDuration.text = formatDuration(mediaPlayer!!.duration)
            seekBar.max = mediaPlayer!!.duration
            handler = Handler(mainLooper)
            btnPlayPause.setOnClickListener {
                if (isPlaying) pauseAudio() else playAudio()
            }
        }
        mediaPlayer!!.setOnCompletionListener {
            isPlaying = false
            mBinder.btnPlayPause.setImageResource(R.drawable.play_icon)
        }

        mBinder.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekbar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer!!.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekbar: SeekBar?) {
                updateSeekBar()
            }

            override fun onStopTrackingTouch(seekbar: SeekBar?) {
                updateSeekBar()
            }

        })
        updateSeekBar()
    }

    @SuppressLint("SetTextI18n")
    private fun playAudio() {
        mediaPlayer!!.start()
        isPlaying = true
        mBinder.btnPlayPause.setImageResource(R.drawable.pause_icon)
        updateSeekBar()
    }

    @SuppressLint("SetTextI18n")
    private fun pauseAudio() {
        mediaPlayer!!.pause()
        isPlaying = false
        mBinder.btnPlayPause.setImageResource(R.drawable.play_icon)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        handler.removeCallbacksAndMessages(null)
    }

    private fun updateSeekBar() {
        if (isPlaying) {
            mBinder.seekBar.progress = mediaPlayer!!.currentPosition
            mBinder.tvCurrentPosition.text = formatDuration(mediaPlayer!!.currentPosition)
            handler.postDelayed({ updateSeekBar() }, 100)
        }
    }

    private fun formatDuration(duration: Int): String {
        val minutes = duration / 1000 / 60
        val seconds = (duration / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}