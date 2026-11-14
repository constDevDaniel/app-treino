package com.daniel.treino.data.planilha

import androidx.room.withTransaction
import com.daniel.treino.data.db.Exercicio
import com.daniel.treino.data.db.SerieRealizada
import com.daniel.treino.data.db.Sessao
import com.daniel.treino.data.db.Treino
import com.daniel.treino.data.db.TreinoDatabase
import java.time.LocalDate
import java.time.ZoneId

data class PreviaSessao(
    val sessao: SessaoPlanilha,
    val treinoId: Long?,
    val treinoNaoEncontrado: String?,
    val jaExisteNoDia: Boolean
)

class ImportadorPlanilha(
    private val db: TreinoDatabase,
    private val zona: ZoneId = ZoneId.systemDefault()
) {
    suspend fun gerarPrevias(resultado: ResultadoPlanilha): List<PreviaSessao> {
        val treinos = db.treinoDao().listarTodos()
        return resultado.sessoes.map { sessao ->
            val treinoMatch = resolverTreino(sessao.treinoInformado, treinos)
            val existentes = sessoesDoDia(sessao.data)
            val jaExiste = if (treinoMatch != null) {
                existentes.any {
                    it.treinoId == treinoMatch.id ||
                        normalizarTexto(it.treinoNome) == normalizarTexto(treinoMatch.nome)
                }
            } else {
                val nomeEsperado = sessao.treinoInformado ?: "Treino importado"
                existentes.any {
                    normalizarTexto(it.treinoNome) == normalizarTexto(nomeEsperado)
                }
            }
            val naoEncontrado = if (sessao.treinoInformado != null && treinoMatch == null) {
                sessao.treinoInformado
            } else {
                null
            }
            PreviaSessao(
                sessao = sessao,
                treinoId = treinoMatch?.id,
                treinoNaoEncontrado = naoEncontrado,
                jaExisteNoDia = jaExiste
            )
        }
    }

    suspend fun importar(itens: List<Pair<SessaoPlanilha, Long?>>): Int = db.withTransaction {
        val treinosPorId = db.treinoDao().listarTodos().associateBy { it.id }
        val exercicioDao = db.exercicioDao()
        val sessaoDao = db.sessaoDao()
        val cacheExercicios = LinkedHashMap<String, Long>()
        var importadas = 0

        for ((sessao, treinoId) in itens) {
            val treino = treinoId?.let { treinosPorId[it] }
            val nomeSessao = treino?.nome ?: sessao.treinoInformado ?: "Treino importado"

            val existentes = sessoesDoDia(sessao.data)
            for (antiga in existentes) {
                val mesmoTreino = if (treino != null) {
                    antiga.treinoId == treino.id ||
                        normalizarTexto(antiga.treinoNome) == normalizarTexto(treino.nome)
                } else {
                    normalizarTexto(antiga.treinoNome) == normalizarTexto(nomeSessao)
                }
                if (mesmoTreino) {
                    sessaoDao.excluirSessao(antiga.id)
                }
            }

            val inicio = sessao.data.atTime(12, 0).atZone(zona).toInstant().toEpochMilli()
            val sessaoId = sessaoDao.inserirSessao(
                Sessao(
                    id = 0,
                    treinoId = treino?.id,
                    treinoNome = nomeSessao,
                    inicio = inicio,
                    fim = inicio,
                    emAndamento = false
                )
            )

            val ordemPorExercicio = LinkedHashMap<Long, Int>()
            val numSeriePorExercicio = LinkedHashMap<Long, Int>()
            val seriesParaInserir = ArrayList<SerieRealizada>(sessao.linhas.size)

            for (linha in sessao.linhas) {
                val chave = normalizarTexto(linha.exercicio)
                val exercicioId = cacheExercicios[chave] ?: run {
                    val existente = exercicioDao.buscarPorNome(linha.exercicio)
                    val id = if (existente != null) {
                        existente.id
                    } else {
                        val inserido = exercicioDao.inserir(Exercicio(nome = linha.exercicio))
                        if (inserido != -1L) {
                            inserido
                        } else {
                            exercicioDao.buscarPorNome(linha.exercicio)?.id
                                ?: throw IllegalStateException("Exercício não encontrado: ${linha.exercicio}")
                        }
                    }
                    cacheExercicios[chave] = id
                    id
                }

                val ordem = ordemPorExercicio.getOrPut(exercicioId) { ordemPorExercicio.size }
                val numeroSerie = (numSeriePorExercicio[exercicioId] ?: 0) + 1
                numSeriePorExercicio[exercicioId] = numeroSerie

                seriesParaInserir.add(
                    SerieRealizada(
                        id = 0,
                        sessaoId = sessaoId,
                        exercicioId = exercicioId,
                        ordemExercicio = ordem,
                        numeroSerie = numeroSerie,
                        repeticoes = linha.repeticoes,
                        carga = linha.carga,
                        concluida = true
                    )
                )
            }

            sessaoDao.inserirSeries(seriesParaInserir)
            importadas++
        }

        importadas
    }

    private suspend fun sessoesDoDia(data: LocalDate): List<Sessao> {
        val inicio = data.atStartOfDay(zona).toInstant().toEpochMilli()
        val fim = data.plusDays(1).atStartOfDay(zona).toInstant().toEpochMilli()
        return db.sessaoDao().listarConcluidasNoIntervalo(inicio, fim)
    }

    private fun resolverTreino(informado: String?, treinos: List<Treino>): Treino? {
        if (informado == null) return null
        val norm = normalizarTexto(informado)
        if (norm.isEmpty()) return null

        treinos.firstOrNull { normalizarTexto(it.nome) == norm }?.let { return it }

        if (norm.length == 1) {
            val sufixo = " $norm"
            treinos.firstOrNull {
                val tn = normalizarTexto(it.nome)
                tn == "treino $norm" || tn.endsWith(sufixo)
            }?.let { return it }
        }

        return treinos.firstOrNull {
            val tn = normalizarTexto(it.nome)
            tn.contains(norm) || norm.contains(tn)
        }
    }
}
