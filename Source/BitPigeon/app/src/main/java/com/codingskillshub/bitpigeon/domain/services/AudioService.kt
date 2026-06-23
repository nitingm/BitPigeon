package com.codingskillshub.bitpigeon.domain.services

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.codingskillshub.bitpigeon.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val soundPool: SoundPool

    private var messageSentId: Int = 0
    private var messageReceivedId: Int = 0
    private var liveMessageReceivedId: Int = 0
    private var fileUploadedId: Int = 0
    private var fileReceivedId: Int = 0
    private var buttonClickId: Int = 0

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        // Preload sounds from res/raw. 
        // Note: You must place the corresponding audio files (e.g., .mp3 or .wav) 
        // in the app/src/main/res/raw/ directory for these to resolve.
        messageSentId = soundPool.load(context, R.raw.message_sent, 1)
//        messageReceivedId = soundPool.load(context, R.raw.message_received, 1)
        liveMessageReceivedId = soundPool.load(context, R.raw.live_message_received, 1)
        fileUploadedId = soundPool.load(context, R.raw.file_sent, 1)
        fileReceivedId = soundPool.load(context, R.raw.file_received, 1)
        buttonClickId = soundPool.load(context, R.raw.button_click, 1)
    }

    fun playMessageSentSound() {
        playSound(messageSentId)
    }

    fun playMessageReceivedSound() {
        playSound(liveMessageReceivedId)
    }

    fun playFileUploadedSound() {
        playSound(fileUploadedId)
    }

    fun playFileReceivedSound() {
        playSound(fileReceivedId)
    }

    fun playButtonClickSound() {
        playSound(buttonClickId)
    }

    private fun playSound(soundId: Int) {
        if (soundId != 0) {
            // Play with volume 1.0, priority 1, no loop, and normal rate
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    /**
     * Release SoundPool resources when they are no longer needed.
     */
    fun release() {
        soundPool.release()
    }
}
