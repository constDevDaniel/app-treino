package com.daniel.treino.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.daniel.treino.TreinoApp

class DescansoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext as? TreinoApp ?: return
        val timer = app.descanso

        when (intent?.action) {
            ACAO_FIM -> {
                val pendingResult = goAsync()
                timer.concluir()
                pendingResult.finish()
            }
            ACAO_MAIS_30 -> {
                timer.adicionarSegundos(30)
            }
            ACAO_PULAR -> {
                timer.cancelar()
            }
        }
    }

    companion object {
        const val ACAO_FIM = "com.daniel.treino.DESCANSO_FIM"
        const val ACAO_MAIS_30 = "com.daniel.treino.DESCANSO_MAIS_30"
        const val ACAO_PULAR = "com.daniel.treino.DESCANSO_PULAR"
    }
}
