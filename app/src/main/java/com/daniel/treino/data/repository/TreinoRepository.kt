package com.daniel.treino.data.repository

import androidx.room.withTransaction
import com.daniel.treino.data.db.AgendaDao
import com.daniel.treino.data.db.AgendaDia
import com.daniel.treino.data.db.DiaAgendado
import com.daniel.treino.data.db.Exercicio
import com.daniel.treino.data.db.ExercicioDao
import com.daniel.treino.data.db.ExercicioDoTreino
import com.daniel.treino.data.db.ExercicioModelo
import com.daniel.treino.data.db.ResumoModelo
import com.daniel.treino.data.db.Treino
import com.daniel.treino.data.db.TreinoDao
import com.daniel.treino.data.db.TreinoDatabase
import kotlinx.coroutines.flow.Flow

data class LinhaModelo(
    val nome: String,
    val series: Int,
    val repeticoes: Int
)

typealias ItemFichaInput = LinhaModelo

class TreinoRepository(val db: TreinoDatabase) {
    val treinoDao: TreinoDao = db.treinoDao()
    val exercicioDao: ExercicioDao = db.exercicioDao()
    val agendaDao: AgendaDao = db.agendaDao()

    val treinos: Flow<List<Treino>> = treinoDao.observarTodos()
    val exercicios: Flow<List<Exercicio>> = exercicioDao.observarTodos()
    val agenda: Flow<List<DiaAgendado>> = agendaDao.observarAgenda()
    val resumosModelo: Flow<List<ResumoModelo>> = treinoDao.observarResumosModelo()
    val resumos: Flow<List<ResumoModelo>> get() = resumosModelo

    fun observarModelo(treinoId: Long): Flow<List<ExercicioDoTreino>> =
        treinoDao.observarModelo(treinoId)

    fun fichaDoTreino(treinoId: Long): Flow<List<ExercicioDoTreino>> =
        observarModelo(treinoId)

    suspend fun buscarTreino(id: Long): Treino? =
        treinoDao.buscarPorId(id)

    suspend fun obterTreino(id: Long): Treino? =
        buscarTreino(id)

    suspend fun excluirTreino(id: Long) {
        treinoDao.excluir(id)
    }

    suspend fun apagarTreino(id: Long) = excluirTreino(id)

    suspend fun obterOuCriarExercicio(nome: String): Long {
        val limpo = nome.trim()
        val existente = exercicioDao.buscarPorNome(limpo)
        if (existente != null) return existente.id

        val id = exercicioDao.inserir(Exercicio(nome = limpo))
        if (id != -1L) return id

        return exercicioDao.buscarPorNome(limpo)?.id
            ?: throw IllegalStateException("Exercício não encontrado: $limpo")
    }

    suspend fun renomearExercicio(id: Long, novoNome: String): Boolean = db.withTransaction {
        val limpo = novoNome.trim()
        val existente = exercicioDao.buscarPorNome(limpo)
        if (existente == null || existente.id == id) {
            exercicioDao.atualizarNome(id, limpo)
            false
        } else {
            exercicioDao.reatribuirModelos(id, existente.id)
            exercicioDao.reatribuirSeries(id, existente.id)
            exercicioDao.excluir(id)
            true
        }
    }

    suspend fun salvarTreino(
        id: Long?,
        nome: String,
        icone: String?,
        linhas: List<LinhaModelo>
    ): Long = db.withTransaction {
        val nomeLimpo = nome.trim()
        val treinoId = if (id == null) {
            treinoDao.inserir(Treino(id = 0, nome = nomeLimpo, icone = icone))
        } else {
            treinoDao.atualizar(Treino(id = id, nome = nomeLimpo, icone = icone))
            id
        }

        val filtradas = linhas.filter { it.nome.isNotBlank() }
        val modelos = ArrayList<ExercicioModelo>(filtradas.size)
        for ((index, linha) in filtradas.withIndex()) {
            val exercicioId = obterOuCriarExercicio(linha.nome)
            modelos.add(
                ExercicioModelo(
                    id = 0,
                    treinoId = treinoId,
                    exercicioId = exercicioId,
                    ordem = index,
                    series = linha.series.coerceAtLeast(1),
                    repeticoes = linha.repeticoes.coerceAtLeast(1)
                )
            )
        }

        treinoDao.substituirModelo(treinoId, modelos)
        treinoId
    }

    suspend fun agendarDia(diaSemana: Int, treinoId: Long?) {
        if (treinoId == null) {
            agendaDao.remover(diaSemana)
        } else {
            agendaDao.salvar(AgendaDia(diaSemana = diaSemana, treinoId = treinoId))
        }
    }

    suspend fun definirDia(diaSemana: Int, treinoId: Long?) = agendarDia(diaSemana, treinoId)
}
