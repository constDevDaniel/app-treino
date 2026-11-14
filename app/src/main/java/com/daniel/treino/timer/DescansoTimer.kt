package com.daniel.treino.timer

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.Lifecycle
import com.daniel.treino.MainActivity
import com.daniel.treino.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DescansoTimer(
    val context: Context,
    private val escopo: CoroutineScope
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("descanso", Context.MODE_PRIVATE)
    }

    private val alarmManager: AlarmManager? = context.getSystemService(AlarmManager::class.java)
    private val notificationManager: NotificationManager? = context.getSystemService(NotificationManager::class.java)

    private val _fim: MutableStateFlow<Long?>
    val fim: StateFlow<Long?>

    private val _inicio: MutableStateFlow<Long?>
    val inicio: StateFlow<Long?>

    private var jobContagem: Job? = null

    init {
        criarCanalNotificacao()

        val fimSalvo = prefs.getLong("fim", 0L)
        val fimValido = fimSalvo.takeIf { it > System.currentTimeMillis() }
        _fim = MutableStateFlow(fimValido)
        fim = _fim.asStateFlow()

        val inicioSalvo = prefs.getLong("inicio", 0L).takeIf { it > 0L }
        _inicio = MutableStateFlow(if (fimValido != null) inicioSalvo else null)
        inicio = _inicio.asStateFlow()

        val fimInicial = _fim.value
        if (fimInicial != null) {
            iniciarMonitoramento(fimInicial)
        }
    }

    fun iniciar(segundos: Int, proximaSerie: String? = null) {
        val agora = System.currentTimeMillis()
        _inicio.value = agora
        prefs.edit {
            putString("proxima", proximaSerie)
            putLong("inicio", agora)
        }
        atualizarFim(agora + segundos * 1000L)
    }

    fun adicionar(segundos: Int) = adicionarSegundos(segundos)

    fun parar() = cancelar()

    fun adicionarSegundos(segundos: Int) {
        val base = maxOf(_fim.value ?: 0L, System.currentTimeMillis())
        atualizarFim(base + segundos * 1000L)
    }

    fun cancelar() {
        jobContagem?.cancel()
        _fim.value = null
        _inicio.value = null
        prefs.edit {
            remove("fim")
            remove("inicio")
            remove("proxima")
        }
        alarmManager?.cancel(pendingIntentAlarme())
        notificationManager?.cancel(NOTIFICATION_ID)
    }

    fun concluir() {
        if (_fim.value == null && !prefs.contains("fim")) return
        cancelar()

        val emPrimeiroPlano = ProcessLifecycleOwner.get()
            .lifecycle
            .currentState
            .isAtLeast(Lifecycle.State.STARTED)

        if (emPrimeiroPlano) {
            AlertaDescanso.tocar(context)
        } else {
            val intent = Intent(context, FimDescansoService::class.java)
            try {
                context.startForegroundService(intent)
            } catch (t: Throwable) {
                AlertaDescanso.tocar(context)
            }
        }
    }

    fun atualizarFim(novoFim: Long) {
        _fim.value = novoFim
        prefs.edit { putLong("fim", novoFim) }
        agendarAlarme(novoFim)
        mostrarNotificacao(novoFim)
        iniciarMonitoramento(novoFim)
    }

    private fun iniciarMonitoramento(fimAlvo: Long) {
        jobContagem?.cancel()
        jobContagem = escopo.launch {
            val restante = fimAlvo - System.currentTimeMillis()
            if (restante > 0L) {
                delay(restante)
            }
            if (_fim.value == fimAlvo && System.currentTimeMillis() >= fimAlvo) {
                concluir()
            }
        }
    }

    private fun agendarAlarme(fimAlvo: Long) {
        val am = alarmManager ?: return
        val pi = pendingIntentAlarme()
        try {
            if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
                val showIntent = PendingIntent.getActivity(
                    context,
                    1,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                am.setAlarmClock(AlarmManager.AlarmClockInfo(fimAlvo, showIntent), pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fimAlvo, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fimAlvo, pi)
        }
    }

    private fun mostrarNotificacao(fimAlvo: Long) {
        val nm = notificationManager ?: return
        val proxima = prefs.getString("proxima", null)

        val abrirIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mais30Intent = PendingIntent.getBroadcast(
            context,
            2,
            Intent(context, DescansoReceiver::class.java).apply {
                action = DescansoReceiver.ACAO_MAIS_30
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pularIntent = PendingIntent.getBroadcast(
            context,
            3,
            Intent(context, DescansoReceiver::class.java).apply {
                action = DescansoReceiver.ACAO_PULAR
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val baseChronometer = SystemClock.elapsedRealtime() +
            (fimAlvo - System.currentTimeMillis()).coerceAtLeast(0L)

        val textoSecundario = if (proxima != null) {
            "Próxima: $proxima"
        } else {
            val hora = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(fimAlvo))
            "Termina às $hora"
        }

        val remoteViews = RemoteViews(context.packageName, R.layout.nowbar_cronometro).apply {
            setChronometer(R.id.cronometro, baseChronometer, null, true)
            setChronometerCountDown(R.id.cronometro, true)
        }

        val corChip = -16742021 // 0xFF008B7B.toInt()
        val extras = Bundle().apply {
            putBoolean("android.requestPromotedOngoing", true)
            putInt("android.ongoingActivityNoti.style", 1)
            putInt("android.ongoingActivityNoti.actionType", 1)
            putInt("android.ongoingActivityNoti.nowbarChronometerPosition", 1)
            putParcelable("android.ongoingActivityNoti.chronometerRemoteView", remoteViews)
            putString("android.ongoingActivityNoti.chronometerRemoteViewTag", CANAL_CRONOMETRO)
            putInt("android.ongoingActivityNoti.chronometerRemoteViewPosition", 1)
            putString("android.ongoingActivityNoti.primaryInfo", "Descanso")
            putString("android.ongoingActivityNoti.secondaryInfo", textoSecundario)
            putString("android.ongoingActivityNoti.nowbarPrimaryInfo", "Descanso")
            putString("android.ongoingActivityNoti.nowbarSecondaryInfo", textoSecundario)
            putInt("android.ongoingActivityNoti.chipBgColor", corChip)
            putInt("android.ongoingActivityNoti.actionPrimarySet", 1)
            putIntegerArrayList("android.ongoingActivityNoti.actionBgColor", arrayListOf(corChip, -1754827))
        }

        val builder = NotificationCompat.Builder(context, CANAL_CRONOMETRO)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setColor(corChip)
            .setContentTitle("Descanso")
            .setContentText(textoSecundario)
            .setWhen(fimAlvo)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(abrirIntent)
            .addExtras(extras)
            .addAction(R.drawable.ic_mais_30, "+30s", mais30Intent)
            .addAction(R.drawable.ic_cancelar, "Pular", pularIntent)

        nm.notify(NOTIFICATION_ID, builder.build())
    }

    private fun criarCanalNotificacao() {
        val nm = notificationManager ?: return
        val canal = NotificationChannel(
            CANAL_CRONOMETRO,
            "Timer de descanso",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Contagem regressiva entre séries"
            setShowBadge(false)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(canal)
    }

    private fun pendingIntentAlarme(): PendingIntent {
        val intent = Intent(context, DescansoReceiver::class.java).apply {
            action = DescansoReceiver.ACAO_FIM
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CANAL_CRONOMETRO = "cronometro_descanso"
        const val NOTIFICATION_ID = 1
    }
}
