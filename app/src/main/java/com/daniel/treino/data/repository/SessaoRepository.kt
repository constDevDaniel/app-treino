package com.daniel.treino.data.repository

import androidx.room.withTransaction
import com.daniel.treino.data.db.SerieDaSessao
import com.daniel.treino.data.db.SerieRealizada
import com.daniel.treino.data.db.Sessao
import com.daniel.treino.data.db.SessaoDao
import com.daniel.treino.data.db.SessaoNoHistorico
import com.daniel.treino.data.db.TreinoDao
import com.daniel.treino.data.db.TreinoDatabase
import com.daniel.treino.data.db.UltimaSessao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SessaoRepository(
    val db: TreinoDatabase,
    private val escopo: CoroutineScope,
    private val aoConcluir: () -> Unit = {}
) {
    val sessaoDao: SessaoDao = db.sessaoDao()
    private val treinoDao: TreinoDao = db.treinoDao()

    val emAndamento: Flow<Sessao?> = sessaoDao.observarEmAndamento()
    val ultimasSessoes: Flow<List<UltimaSessao>> = sessaoDao.observarUltimasSessoes()
    val ultimasSessoesPorTreino: Flow<List<UltimaSessao>> get() = ultimasSessoes
    val historico: Flow<List<SessaoNoHistorico>> = sessaoDao.observarHistorico()

    private val filaComandos = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        escopo.launch {
            for (comando in filaComandos) {
                comando()
            }
        }
    }

    fun observarConcluidasDesde(desde: Long): Flow<List<Sessao>> =
        sessaoDao.observarConcluidasDesde(desde)

    fun sessoesNoIntervalo(inicio: Long, fim: Long): Flow<List<Sessao>> =
        observarConcluidasDesde(inicio).map { lista -> lista.filter { it.inicio < fim } }

    fun observarSeries(sessaoId: Long): Flow<List<SerieDaSessao>> =
        sessaoDao.observarSeries(sessaoId)

    fun seriesDaSessao(sessaoId: Long): Flow<List<SerieDaSessao>> =
        observarSeries(sessaoId)

    suspend fun iniciarSessao(treinoId: Long): Long = iniciarOuRetomar(treinoId)

    suspend fun iniciarOuRetomar(treinoId: Long): Long = db.withTransaction {
        val ativa = sessaoDao.EmAndamentoSync()
        if (ativa != null) {
            return@withTransaction ativa.id
        }

        val treino = treinoDao.buscarPorId(treinoId)
            ?: throw IllegalStateException("Treino não encontrado")
        val modelos = treinoDao.listarModelo(treinoId)

        val sessaoId = sessaoDao.inserirSessao(
            Sessao(
                id = 0,
                treinoId = treino.id,
                treinoNome = treino.nome,
                inicio = System.currentTimeMillis(),
                fim = null,
                emAndamento = true
            )
        )

        val series = ArrayList<SerieRealizada>()
        for (modelo in modelos) {
            for (numSerie in 1..modelo.series) {
                val anterior = sessaoDao.buscarUltimaSerie(modelo.exercicioId, numSerie)
                val reps = anterior?.repeticoes ?: modelo.repeticoes
                val carga = anterior?.carga
                series.add(
                    SerieRealizada(
                        id = 0,
                        sessaoId = sessaoId,
                        exercicioId = modelo.exercicioId,
                        ordemExercicio = modelo.ordem,
                        numeroSerie = numSerie,
                        repeticoes = reps,
                        carga = carga,
                        concluida = false
                    )
                )
            }
        }

        sessaoDao.inserirSeries(series)
        sessaoId
    }

    fun atualizarSerie(
        serieId: Long,
        repeticoes: Int?,
        carga: Double?,
        concluida: Boolean
    ) {
        filaComandos.trySend {
            sessaoDao.atualizarSerie(serieId, repeticoes, carga, concluida)
        }
    }

    fun concluirSessao(sessaoId: Long) {
        filaComandos.trySend {
            sessaoDao.concluirSessao(sessaoId, System.currentTimeMillis())
            aoConcluir()
        }
    }

    fun concluir(sessaoId: Long) = concluirSessao(sessaoId)

    fun descartarSessao(sessaoId: Long) {
        filaComandos.trySend {
            sessaoDao.excluirSessao(sessaoId)
        }
    }

    fun descartar(sessaoId: Long) = descartarSessao(sessaoId)

    suspend fun excluirSessao(sessaoId: Long) {
        sessaoDao.excluirSessao(sessaoId)
    }

    suspend fun excluir(sessaoId: Long) = excluirSessao(sessaoId)

    private suspend fun SessaoDao.EmAndamentoSync(): Sessao? {
        return observarEmAndamento().firstOrNull()
    }
}
