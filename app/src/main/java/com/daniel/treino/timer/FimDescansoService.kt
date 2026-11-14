package com.daniel.treino.timer

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.daniel.treino.R

class FimDescansoService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificacao: Notification = NotificationCompat.Builder(this, DescansoTimer.CANAL_CRONOMETRO)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setContentTitle("Descanso concluído")
            .setContentText("Hora da próxima série")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notificacao,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notificacao)
        }

        AlertaDescanso.tocar(this) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    companion object {
        private const val NOTIFICATION_ID = 2
    }
}
