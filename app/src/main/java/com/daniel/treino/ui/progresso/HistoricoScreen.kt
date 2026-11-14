package com.daniel.treino.ui.progresso

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.daniel.treino.TreinoApp
import com.daniel.treino.data.db.SessaoHistorico
import com.daniel.treino.data.repository.SessaoRepository
import com.daniel.treino.ui.components.EmblemaTreino
import com.daniel.treino.ui.components.EstadoVazio
import com.daniel.treino.ui.components.Formatadores
import com.daniel.treino.ui.components.PtBr
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoricoViewModel(private val repo: SessaoRepository) : ViewModel() {
    val sessoes: StateFlow<List<SessaoHistorico>?> = repo.historico
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun excluir(id: Long) {
        viewModelScope.launch { repo.excluir(id) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TreinoApp
                HistoricoViewModel(app.sessaoRepository)
            }
        }
    }
}

private val FormatoData: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, dd/MM/yyyy", PtBr)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricoScreen(voltar: () -> Unit) {
    val vm: HistoricoViewModel = viewModel(factory = HistoricoViewModel.Factory)
    val sessoes by vm.sessoes.collectAsStateWithLifecycle()
    var confirmandoId by rememberSaveable { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = voltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        val lista = sessoes ?: return@Scaffold
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (lista.isEmpty()) {
                item {
                    EstadoVazio(
                        icone = Icons.Outlined.History,
                        titulo = "Nenhum treino concluído",
                        mensagem = "Quando você finalizar um treino, ele aparecerá aqui."
                    )
                }
            }
            items(lista, key = { it.id }) { sessao ->
                CartaoSessaoHistorico(
                    sessao = sessao,
                    aoExcluir = { confirmandoId = sessao.id },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }

    val alvo = confirmandoId?.let { id -> sessoes?.firstOrNull { it.id == id } }
    if (alvo != null) {
        AlertDialog(
            onDismissRequest = { confirmandoId = null },
            icon = { EmblemaTreino(treinoId = alvo.treinoId, nome = alvo.nomeTreino, tamanho = 48.dp) },
            title = { Text("Excluir ${alvo.nomeTreino}?") },
            text = {
                Text(
                    "O treino de ${formatarData(alvo.inicio)} sai do histórico, dos relatórios e dos recordes. Não dá para desfazer."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.excluir(alvo.id)
                        confirmandoId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Excluir treino")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoId = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun CartaoSessaoHistorico(
    sessao: SessaoHistorico,
    aoExcluir: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 6.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EmblemaTreino(treinoId = sessao.treinoId, nome = sessao.nomeTreino, tamanho = 44.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(sessao.nomeTreino, style = MaterialTheme.typography.titleLarge)
                Text(
                    formatarData(sessao.inicio),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val rotuloSeries = if (sessao.series == 1) "série feita" else "séries feitas"
                Text(
                    "${sessao.series} $rotuloSeries, ${Formatadores.formatarVolume(sessao.volume)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = aoExcluir) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Excluir ${sessao.nomeTreino} de ${formatarData(sessao.inicio)}"
                )
            }
        }
    }
}

private fun formatarData(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(FormatoData)
        .replaceFirstChar { it.titlecase(PtBr) }
