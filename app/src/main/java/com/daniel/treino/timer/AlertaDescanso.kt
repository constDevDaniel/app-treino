package com.daniel.treino.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.daniel.treino.R

object AlertaDescanso {

    fun tocar(context: Context, aoTerminar: () -> Unit = {}) {
        val appContext = context.applicationContext
        vibrar(appContext)

        val audioManager = appContext.getSystemService(AudioManager::class.java)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attrs)
            .build()

        audioManager?.requestAudioFocus(focusRequest)

        val handler = Handler(Looper.getMainLooper())
        var finalizado = false

        try {
            val afd = appContext.resources.openRawResourceFd(R.raw.fim_descanso)
            val player = MediaPlayer()

            val finalizar = {
                if (!finalizado) {
                    finalizado = true
                    runCatching { player.release() }
                    if (audioManager != null) {
                        runCatching { audioManager.abandonAudioFocusRequest(focusRequest) }
                    }
                    aoTerminar()
                }
            }

            try {
                player.setAudioAttributes(attrs)
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            } finally {
                afd.close()
            }

            player.setVolume(1.0f, 1.0f)
            player.setOnCompletionListener { finalizar() }
            player.setOnErrorListener { _, _, _ ->
                finalizar()
                true
            }
            player.prepare()
            player.start()
            handler.postDelayed({ finalizar() }, 4000L)
        } catch (t: Throwable) {
            Log.e("SomDescanso", "Falha ao tocar alerta de descanso", t)
            if (audioManager != null) {
                runCatching { audioManager.abandonAudioFocusRequest(focusRequest) }
            }
            aoTerminar()
        }
    }

    private fun vibrar(context: Context) {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

        if (vibrator == null || !vibrator.hasVibrator()) return

        val effect = VibrationEffect.createWaveform(
            longArrayOf(0L, 180L, 90L, 180L, 90L, 280L),
            intArrayOf(0, 255, 0, 255, 0, 255),
            -1
        )

        if (Build.VERSION.SDK_INT >= 33) {
            vibrator.vibrate(
                effect,
                VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(
                effect,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
    }
}
