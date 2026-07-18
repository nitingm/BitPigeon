package com.codingskillshub.bitpigeon.domain.services

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.codingskillshub.bitpigeon.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var soundPool: SoundPool? = null
    private val loadedSounds = mutableSetOf<Int>()

    private var messageSentId: Int = 0
    private var liveMessageReceivedId: Int = 0
    private var fileUploadedId: Int = 0
    private var fileReceivedId: Int = 0
    private var buttonClickId: Int = 0

    init {
        initialize()
    }

    /**
     * Initializes the SoundPool and preloads audio resources.
     * Can be called safely multiple times (e.g., in onResume if release() was called).
     */
    fun initialize() {
        if (soundPool != null) return

        Log.d("AudioService", "Initializing SoundPool")

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSounds.add(sampleId)
            } else {
                Log.e("AudioService", "Failed to load sound with id: $sampleId, status: $status")
            }
        }

        // Preload sounds
        messageSentId = soundPool?.load(context, R.raw.message_sent, 1) ?: 0
        liveMessageReceivedId = soundPool?.load(context, R.raw.live_message_received, 1) ?: 0
        fileUploadedId = soundPool?.load(context, R.raw.file_sent, 1) ?: 0
        fileReceivedId = soundPool?.load(context, R.raw.file_received, 1) ?: 0
        buttonClickId = soundPool?.load(context, R.raw.button_click, 1) ?: 0
    }

    fun playMessageSentSound() = playSound(messageSentId)
    fun playMessageReceivedSound() = playSound(liveMessageReceivedId)
    fun playFileUploadedSound() = playSound(fileUploadedId)
    fun playFileReceivedSound() = playSound(fileReceivedId)
    fun playButtonClickSound() = playSound(buttonClickId)

    private fun playSound(soundId: Int) {
        val currentSoundPool = soundPool
        if (currentSoundPool != null && soundId != 0 && loadedSounds.contains(soundId)) {
            currentSoundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        } else {
            Log.w("AudioService", "Sound $soundId not loaded yet, failed to load, or SoundPool is null")
        }
    }

    /**
     * Releases SoundPool resources and clears the loaded sounds set.
     */
    fun release() {
        Log.d("AudioService", "Releasing SoundPool")
        soundPool?.release()
        soundPool = null
        loadedSounds.clear()
        messageSentId = 0
        liveMessageReceivedId = 0
        fileUploadedId = 0
        fileReceivedId = 0
        buttonClickId = 0
    }
}
