package com.daniel.treino.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.daniel.treino.data.db.Treino
import com.daniel.treino.data.repository.TreinoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Reconstructed from T1/n.java, T1/q.java, T1/e.java, T1/o.java, T1/p.java (case 0)

data class AgendaUi(
    val semana: Map<Int, Treino> = emptyMap(),
    val treinos: List<Treino> = emptyList(),
)

class AgendaViewModel(
    private val repo: TreinoRepository,
) : ViewModel() {

    val ui: StateFlow<AgendaUi> = combine(
        repo.agenda,
        repo.treinos,
    ) { agenda, treinos ->
        val porId = treinos.associateBy { it.id }
        val mapa = linkedMapOf<Int, Treino>()
        for (dia in agenda) {
            val treino = porId[dia.treinoId]
            if (treino != null) {
                mapa[dia.diaSemana] = treino
            }
        }
        AgendaUi(
            semana = mapa,
            treinos = treinos,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = AgendaUi(),
    )

    fun definirDia(diaSemana: Int, treinoId: Long?) {
        viewModelScope.launch {
            repo.definirDia(diaSemana, treinoId)
        }
    }

    companion object {
        fun factory(repo: TreinoRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    AgendaViewModel(repo)
                }
            }
    }
}
