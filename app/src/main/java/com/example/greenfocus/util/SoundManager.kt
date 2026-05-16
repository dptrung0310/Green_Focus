package com.example.greenfocus.util

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.util.Log
import com.example.greenfocus.R

object Sound {
    // All sound taken from Mixkit without any copyright infringement
    val WIN_BELL = R.raw.mixkit_achievement_bell_600
    val WIN_APPLAUSE = R.raw.mixkit_small_group_moderate_applause_505
    val LOSE = R.raw.mixkit_cartoon_sad_party_horn_527
    val CLICK = R.raw.mixkit_modern_technology_select_3124
}

class SoundManager(
    private var context: Context
) {
    private var stoppablePlayer: MediaPlayer? = null
    /**
     * Plays a custom sound from the res/raw folder.
     * Example usage: soundManager.playSound(R.raw.timer_chime)
     * NOTE: This requires a Context, so if you want to run it, pass it from Composable or a separate Service
     */
    fun playSound(soundResId: Int) {
        try {
            // Create a local variable, not a class-level one
            val localPlayer = MediaPlayer.create(context, soundResId)
            localPlayer.setOnCompletionListener { player ->
                player.release()
            }
            localPlayer.start()
        } catch (e: Exception) {
            Log.e("SoundManager", "Failed to play custom sound", e)
        }
    }

    /**
     * Plays a custom sound from the res/raw folder. This time it stops any previous sound playing
     * Example usage: soundManager.playSound(R.raw.timer_chime)
     * NOTE: This requires a Context, so if you want to run it, pass it from Composable or a separate Service
     */
    fun playStoppableSound(soundResId: Int) {
        stopSound()
        try {
            // Create a local variable, not a class-level one
            stoppablePlayer = MediaPlayer.create(context, soundResId)
            stoppablePlayer?.setOnCompletionListener { player ->
                player.release()
                if (stoppablePlayer === player) {
                    stoppablePlayer = null
                }
            }
            stoppablePlayer?.start()
        } catch (e: Exception) {
            Log.e("SoundManager", "Failed to play custom sound", e)
        }
    }

    fun stopSound() {
        stoppablePlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            // Always release the player when you are done with it!
            player.release()
        }
        stoppablePlayer = null
    }

    /**
     * Plays the user's default system notification sound.
     * Great fallback if you don't want to bundle an mp3.
     */
    fun playSystemDefaultNotification() {
        try {
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, defaultSoundUri)
            ringtone.play()
        } catch (e: Exception) {
            Log.e("SoundManager", "Failed to play system sound", e)
        }
    }

}