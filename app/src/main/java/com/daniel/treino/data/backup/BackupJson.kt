package com.daniel.treino.data.backup

import com.daniel.treino.data.db.AgendaDia
import com.daniel.treino.data.db.Exercicio
import com.daniel.treino.data.db.ExercicioModelo
import com.daniel.treino.data.db.SerieRealizada
import com.daniel.treino.data.db.Sessao
import com.daniel.treino.data.db.Treino
import com.daniel.treino.data.db.TreinoDatabase
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant

class BackupInvalidoException(message: String) : IllegalArgumentException(message)

data class ResumoBackup(
    val criadoEm: Instant,
    val treinos: Int,
    val sessoesConcluidas: Int,
    val series: Int
)

class DadosBackup(
    val criadoEm: Instant,
    val exercicios: List<Exercicio>,
    val treinos: List<Treino>,
    val modelos: List<ExercicioModelo>,
    val agenda: List<AgendaDia>,
    val sessoes: List<Sessao>,
    val series: List<SerieRealizada>
) {
    fun resumo(): ResumoBackup = ResumoBackup(
        criadoEm = criadoEm,
        treinos = treinos.size,
        sessoesConcluidas = sessoes.count { !it.emAndamento },
        series = series.count { it.concluida }
    )
}

typealias ConteudoBackup = DadosBackup

class BackupJson(private val db: TreinoDatabase) {

    suspend fun exportar(): String = db.withTransaction {
        val dao = db.backupDao()
        val exercicios = dao.listarExercicios()
        val treinos = dao.listarTreinos()
        val modelos = dao.listarModelos()
        val agenda = dao.listarAgenda()
        val sessoes = dao.listarSessoes()
        val series = dao.listarSeries()

        val raiz = JSONObject()
            .put("app", "treino")
            .put("formato", 1)
            .put("criadoEm", Instant.now().toString())
            .put("exercicios", paraJsonArray(exercicios) {
                JSONObject()
                    .put("id", it.id)
                    .put("nome", it.nome)
            })
            .put("treinos", paraJsonArray(treinos) {
                JSONObject()
                    .put("id", it.id)
                    .put("nome", it.nome)
                    .put("icone", it.icone ?: JSONObject.NULL)
            })
            .put("modelos", paraJsonArray(modelos) {
                JSONObject()
                    .put("id", it.id)
                    .put("treinoId", it.treinoId)
                    .put("exercicioId", it.exercicioId)
                    .put("ordem", it.ordem)
                    .put("series", it.series)
                    .put("repeticoes", it.repeticoes)
            })
            .put("agenda", paraJsonArray(agenda) {
                JSONObject()
                    .put("diaSemana", it.diaSemana)
                    .put("treinoId", it.treinoId)
            })
            .put("sessoes", paraJsonArray(sessoes) {
                JSONObject()
                    .put("id", it.id)
                    .put("treinoId", it.treinoId ?: JSONObject.NULL)
                    .put("treinoNome", it.treinoNome)
                    .put("inicio", it.inicio)
                    .put("fim", it.fim ?: JSONObject.NULL)
                    .put("emAndamento", it.emAndamento)
            })
            .put("series", paraJsonArray(series) {
                JSONObject()
                    .put("id", it.id)
                    .put("sessaoId", it.sessaoId)
                    .put("exercicioId", it.exercicioId)
                    .put("ordemExercicio", it.ordemExercicio)
                    .put("numeroSerie", it.numeroSerie)
                    .put("repeticoes", it.repeticoes ?: JSONObject.NULL)
                    .put("carga", it.carga ?: JSONObject.NULL)
                    .put("concluida", it.concluida)
            })

        raiz.toString(2)
    }

    fun ler(texto: String): DadosBackup {
        if (texto.isBlank()) {
            throw BackupInvalidoException("O arquivo de backup está vazio.")
        }
        val raiz = try {
            JSONObject(texto.trimStart('\uFEFF'))
        } catch (e: JSONException) {
            throw BackupInvalidoException("Este arquivo não é um backup do Treino.")
        }

        if (raiz.optString("app") != "treino") {
            throw BackupInvalidoException("Este arquivo não é um backup do Treino.")
        }
        val formato = raiz.optInt("formato", -1)
        if (formato < 1) {
            throw BackupInvalidoException("Este arquivo não é um backup do Treino.")
        }
        if (formato > 1) {
            throw BackupInvalidoException(
                "Este backup foi feito por uma versão mais nova do Treino. Atualize o app antes de restaurar."
            )
        }

        return try {
            val criadoEm = runCatching { Instant.parse(raiz.optString("criadoEm")) }
                .getOrElse { Instant.EPOCH }

            val exercicios = lerLista(raiz, "exercicios") {
                Exercicio(
                    id = it.getLong("id"),
                    nome = it.getString("nome")
                )
            }
            val treinos = lerLista(raiz, "treinos") {
                val icone = if (it.isNull("icone")) null else it.optString("icone").takeIf { s -> s.isNotBlank() }
                Treino(
                    id = it.getLong("id"),
                    nome = it.getString("nome"),
                    icone = icone
                )
            }
            val modelos = lerLista(raiz, "modelos") {
                ExercicioModelo(
                    id = it.getLong("id"),
                    treinoId = it.getLong("treinoId"),
                    exercicioId = it.getLong("exercicioId"),
                    ordem = it.getInt("ordem"),
                    series = it.getInt("series"),
                    repeticoes = it.getInt("repeticoes")
                )
            }
            val agenda = lerLista(raiz, "agenda") {
                AgendaDia(
                    diaSemana = it.getInt("diaSemana"),
                    treinoId = it.getLong("treinoId")
                )
            }
            val sessoes = lerLista(raiz, "sessoes") {
                Sessao(
                    id = it.getLong("id"),
                    treinoId = if (it.isNull("treinoId")) null else it.getLong("treinoId"),
                    treinoNome = it.getString("treinoNome"),
                    inicio = it.getLong("inicio"),
                    fim = if (it.isNull("fim")) null else it.getLong("fim"),
                    emAndamento = it.getBoolean("emAndamento")
                )
            }
            val series = lerLista(raiz, "series") {
                SerieRealizada(
                    id = it.getLong("id"),
                    sessaoId = it.getLong("sessaoId"),
                    exercicioId = it.getLong("exercicioId"),
                    ordemExercicio = it.getInt("ordemExercicio"),
                    numeroSerie = it.getInt("numeroSerie"),
                    repeticoes = if (it.isNull("repeticoes")) null else it.getInt("repeticoes"),
                    carga = if (it.isNull("carga")) null else it.getDouble("carga"),
                    concluida = it.getBoolean("concluida")
                )
            }

            DadosBackup(
                criadoEm = criadoEm,
                exercicios = exercicios,
                treinos = treinos,
                modelos = modelos,
                agenda = agenda,
                sessoes = sessoes,
                series = series
            )
        } catch (e: JSONException) {
            throw BackupInvalidoException("O arquivo de backup está incompleto ou corrompido.")
        }
    }

    suspend fun restaurar(dados: DadosBackup) {
        db.backupDao().restaurarTudo(
            exercicios = dados.exercicios,
            treinos = dados.treinos,
            modelos = dados.modelos,
            agenda = dados.agenda,
            sessoes = dados.sessoes,
            series = dados.series
        )
    }

    private fun <T> paraJsonArray(itens: List<T>, transformar: (T) -> JSONObject): JSONArray {
        val array = JSONArray()
        for (item in itens) {
            array.put(transformar(item))
        }
        return array
    }

    private fun <T> lerLista(raiz: JSONObject, chave: String, transformar: (JSONObject) -> T): List<T> {
        val array = raiz.optJSONArray(chave) ?: return emptyList()
        return List(array.length()) { index ->
            transformar(array.getJSONObject(index))
        }
    }
}
