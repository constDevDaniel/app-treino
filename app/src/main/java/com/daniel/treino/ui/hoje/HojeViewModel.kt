package com.daniel.treino.ui.hoje

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.daniel.treino.data.db.DiaAgendado
import com.daniel.treino.data.db.ResumoModelo
import com.daniel.treino.data.db.Sessao
import com.daniel.treino.data.db.Treino
import com.daniel.treino.data.repository.SessaoRepository
import com.daniel.treino.data.repository.TreinoRepository
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Reconstructed from V1/d.java, V1/y.java, V1/B.java, V1/A.java, V1/m.java, V1/t.java, V1/z.java (case 0), T1/p.java (case 1)

data class DiaDaSemana(
    val data: LocalDate,
    val agendado: DiaAgendado?,
    val feito: Sessao?,
)

data class HojeUi(
    val carregado: Boolean = false,
    val semana: List<DiaDaSemana> = emptyList(),
    val agendado: DiaAgendado? = null,
    val emAndamento: Sessao? = null,
    val treinos: List<Treino> = emptyList(),
    val resumos: Map<Long, ResumoModelo> = emptyMap(),
    val ultimas: Map<Long, Long> = emptyMap(),
)

class HojeViewModel(
    treinoRepo: TreinoRepository,
    private val sessaoRepo: SessaoRepository,
) : ViewModel() {

    val hoje: LocalDate = LocalDate.now()
    private val inicioSemana: LocalDate =
        hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    private val zona: ZoneId = ZoneId.systemDefault()

    private val semanaFlow = combine(
        treinoRepo.agenda,
        sessaoRepo.sessoesNoIntervalo(
            inicio = inicioSemana.atStartOfDay(zona).toInstant().toEpochMilli(),
            fim = inicioSemana.plusDays(7).atStartOfDay(zona).toInstant().toEpochMilli(),
        ),
    ) { agenda, sessoes ->
        val agPorDia = agenda.associateBy { it.diaSemana }
        val sessaoPorDia = sessoes.associateBy {
            Instant.ofEpochMilli(it.inicio).atZone(zona).toLocalDate()
        }
        (0L..6L).map { offset ->
            val d = inicioSemana.plusDays(offset)
            DiaDaSemana(
                data = d,
                agendado = agPorDia[d.dayOfWeek.value],
                feito = sessaoPorDia[d],
            )
        }
    }

    private val extrasFlow = combine(
        treinoRepo.resumos,
        sessaoRepo.ultimasSessoesPorTreino,
    ) { resumos, ultimas ->
        val rMap = resumos.associateBy { it.treinoId }
        val uMap = ultimas.mapNotNull { u ->
            u.treinoId?.let { id -> id to u.inicio }
        }.toMap()
        rMap to uMap
    }

    val ui: StateFlow<HojeUi> = combine(
        semanaFlow,
        sessaoRepo.emAndamento,
        treinoRepo.treinos,
        extrasFlow,
    ) { semana, emAndamento, treinos, (resumos, ultimas) ->
        val diaHoje = semana.firstOrNull { it.data == hoje }
        HojeUi(
            carregado = true,
            semana = semana,
            agendado = diaHoje?.agendado,
            emAndamento = emAndamento,
            treinos = treinos,
            resumos = resumos,
            ultimas = ultimas,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = HojeUi(),
    )

    fun iniciarTreino(treinoId: Long, abrirSessao: (Long) -> Unit) {
        viewModelScope.launch {
            val id = sessaoRepo.iniciarSessao(treinoId)
            abrirSessao(id)
        }
    }

    companion object {
        fun factory(
            treinoRepo: TreinoRepository,
            sessaoRepo: SessaoRepository,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    HojeViewModel(treinoRepo, sessaoRepo)
                }
            }
    }
}
