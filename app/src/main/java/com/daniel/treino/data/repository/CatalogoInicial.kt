package com.daniel.treino.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.daniel.treino.data.db.AgendaDia
import com.daniel.treino.data.db.SerieRealizada
import com.daniel.treino.data.db.Sessao
import com.daniel.treino.data.db.TreinoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

class CatalogoInicial(
    private val db: TreinoDatabase,
    private val treinos: TreinoRepository
) {
    suspend fun garantirCatalogo(pastaExterna: File?) {
        if (db.treinoDao().contar() == 0) {
            criarCatalogoPadrao()
        }

        if (pastaExterna == null) return
        val arquivo = File(pastaExterna, "importar.json")
        if (!arquivo.exists()) return

        try {
            val texto = withContext(Dispatchers.IO) {
                arquivo.readText(Charsets.UTF_8)
            }
            val json = JSONObject(texto)
            importarJson(json)
            arquivo.renameTo(File(pastaExterna, "importado.json"))
            Log.i("Importador", "Importação concluída")
        } catch (e: Exception) {
            Log.e("Importador", "Falha ao importar $arquivo", e)
        }
    }

    suspend fun criarCatalogoPadrao() = db.withTransaction {
        val idPeito = treinos.salvarTreino(
            id = null,
            nome = "Peito e Tríceps",
            icone = "peito",
            linhas = listOf(
                LinhaModelo("Supino reto com barra", 4, 8),
                LinhaModelo("Supino inclinado com halteres", 3, 10),
                LinhaModelo("Crucifixo na máquina", 3, 12),
                LinhaModelo("Desenvolvimento com halteres", 3, 10),
                LinhaModelo("Elevação lateral", 4, 15),
                LinhaModelo("Tríceps corda na polia", 3, 12),
                LinhaModelo("Tríceps francês", 3, 12)
            )
        )

        val idCostas = treinos.salvarTreino(
            id = null,
            nome = "Costas e Bíceps",
            icone = "costas",
            linhas = listOf(
                LinhaModelo("Puxada frontal aberta", 4, 10),
                LinhaModelo("Remada curvada", 4, 8),
                LinhaModelo("Remada baixa no triângulo", 3, 12),
                LinhaModelo("Crucifixo inverso", 3, 15),
                LinhaModelo("Rosca direta com barra", 3, 10),
                LinhaModelo("Rosca alternada", 3, 12)
            )
        )

        val idPernas = treinos.salvarTreino(
            id = null,
            nome = "Pernas",
            icone = "pernas",
            linhas = listOf(
                LinhaModelo("Agachamento livre", 4, 8),
                LinhaModelo("Leg press 45°", 4, 10),
                LinhaModelo("Cadeira extensora", 3, 12),
                LinhaModelo("Mesa flexora", 3, 12),
                LinhaModelo("Stiff", 3, 10),
                LinhaModelo("Panturrilha em pé", 4, 15)
            )
        )

        val agenda = listOf(
            1 to idPeito,
            2 to idCostas,
            3 to idPernas,
            4 to idPeito,
            5 to idCostas,
            6 to idPernas
        )
        for ((dia, treinoId) in agenda) {
            db.agendaDao().salvar(AgendaDia(diaSemana = dia, treinoId = treinoId))
        }

        val extras = listOf(
            "Supino declinado",
            "Crossover na polia",
            "Flexão de braço",
            "Barra fixa",
            "Remada cavalinho",
            "Remada unilateral (serrote)",
            "Pulldown na polia",
            "Encolhimento com halteres",
            "Desenvolvimento militar",
            "Elevação frontal",
            "Rosca martelo",
            "Rosca Scott",
            "Rosca concentrada",
            "Tríceps testa",
            "Mergulho nas paralelas",
            "Agachamento hack",
            "Passada com halteres",
            "Cadeira abdutora",
            "Cadeira adutora",
            "Elevação pélvica",
            "Panturrilha sentado",
            "Prancha abdominal",
            "Abdominal supra",
            "Abdominal infra"
        )
        for (nome in extras) {
            treinos.obterOuCriarExercicio(nome)
        }
    }

    suspend fun importarJson(raiz: JSONObject) = db.withTransaction {
        val treinosPorNome = LinkedHashMap<String, Long>()
        for (t in db.treinoDao().listarTodos()) {
            treinosPorNome[t.nome] = t.id
        }

        val arrayTreinos = raiz.optJSONArray("treinos") ?: JSONArray()
        for (i in 0 until arrayTreinos.length()) {
            val obj = arrayTreinos.getJSONObject(i)
            val nome = obj.getString("nome")
            val itens = obj.getJSONArray("exercicios")
            val linhas = List(itens.length()) { idx ->
                val ex = itens.getJSONObject(idx)
                LinhaModelo(
                    nome = ex.getString("nome"),
                    series = ex.optInt("series", 3),
                    repeticoes = ex.optInt("repeticoes", 10)
                )
            }
            val id = treinos.salvarTreino(
                id = treinosPorNome[nome],
                nome = nome,
                icone = null,
                linhas = linhas
            )
            treinosPorNome[nome] = id
        }

        val objAgenda = raiz.optJSONObject("agenda")
        if (objAgenda != null) {
            for (dia in 1..7) {
                val nomeTreino = objAgenda.optString(dia.toString(), "")
                val treinoId = treinosPorNome[nomeTreino]
                if (treinoId != null) {
                    db.agendaDao().salvar(AgendaDia(diaSemana = dia, treinoId = treinoId))
                }
            }
        }

        val arraySessoes = raiz.optJSONArray("sessoes") ?: JSONArray()
        val zona = ZoneId.systemDefault()
        for (i in 0 until arraySessoes.length()) {
            val obj = arraySessoes.getJSONObject(i)
            val dataStr = obj.getString("data")
            val treinoNome = obj.optString("treino", "Treino")
            val inicio = LocalDate.parse(dataStr)
                .atTime(12, 0)
                .atZone(zona)
                .toInstant()
                .toEpochMilli()

            val sessaoId = db.sessaoDao().inserirSessao(
                Sessao(
                    id = 0,
                    treinoId = treinosPorNome[treinoNome],
                    treinoNome = treinoNome,
                    inicio = inicio,
                    fim = inicio + 3_600_000L,
                    emAndamento = false
                )
            )

            val arraySeries = obj.getJSONArray("series")
            val seriesPorExercicio = LinkedHashMap<String, MutableList<Pair<Int?, Double?>>>()
            for (j in 0 until arraySeries.length()) {
                val sr = arraySeries.getJSONObject(j)
                val exNome = sr.getString("exercicio")
                val reps = if (sr.isNull("repeticoes")) null else sr.getInt("repeticoes")
                val carga = if (sr.isNull("carga")) null else sr.getDouble("carga")
                seriesPorExercicio.getOrPut(exNome) { ArrayList() }.add(reps to carga)
            }

            var ordem = 0
            for ((exNome, lista) in seriesPorExercicio) {
                val exercicioId = treinos.obterOuCriarExercicio(exNome)
                val entidades = lista.mapIndexed { idx, (reps, carga) ->
                    SerieRealizada(
                        id = 0,
                        sessaoId = sessaoId,
                        exercicioId = exercicioId,
                        ordemExercicio = ordem,
                        numeroSerie = idx + 1,
                        repeticoes = reps,
                        carga = carga,
                        concluida = true
                    )
                }
                db.sessaoDao().inserirSeries(entidades)
                ordem++
            }
        }
    }
}
