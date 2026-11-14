package com.daniel.treino.ui.sessao

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.daniel.treino.TreinoApp
import com.daniel.treino.data.db.SerieDaSessao
import com.daniel.treino.data.repository.SessaoRepository
import com.daniel.treino.timer.DescansoTimer
import com.daniel.treino.ui.components.formatarCarga
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GrupoExercicio(
    val nome: String,
    val series: List<SerieDaSessao>
)

data class SessaoUi(
    val carregado: Boolean = false,
    val ativa: Boolean = true,
    val nome: String = "",
    val treinoId: Long? = null,
    val grupos: List<GrupoExercicio> = emptyList(),
    val feitas: Int = 0,
    val total: Int = 0
)

class SessaoViewModel(
    savedStateHandle: SavedStateHandle,
    private val repo: SessaoRepository,
    private val timer: DescansoTimer
) : ViewModel() {
    val sessaoId: Long = checkNotNull(savedStateHandle["id"])

    val ui: StateFlow<SessaoUi> = combine(
        repo.emAndamento,
        repo.seriesDaSessao(sessaoId)
    ) { ativa, series ->
        val grupos = series
            .groupBy { it.exercicioId to it.nomeExercicio }
            .map { (chave, lista) -> GrupoExercicio(chave.second, lista) }
        val corresponde = ativa != null && ativa.id == sessaoId
        SessaoUi(
            carregado = true,
            ativa = corresponde,
            nome = ativa?.nomeTreino ?: "",
            treinoId = ativa?.treinoId,
            grupos = grupos,
            feitas = series.count { it.concluida },
            total = series.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessaoUi())

    val inicioDescanso: StateFlow<Long?> = timer.inicio
    val fimDescanso: StateFlow<Long?> = timer.fim

    fun salvarSerie(serieId: Long, repsTexto: String, cargaTexto: String, concluida: Boolean) {
        viewModelScope.launch {
            val reps = repsTexto.trim().toIntOrNull()?.coerceAtLeast(0)
            val carga = cargaTexto.trim().replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0)
            repo.atualizarSerie(serieId, reps, carga, concluida)
        }
    }

    fun iniciarDescanso(segundos: Int) = timer.iniciar(segundos)

    fun adicionarDescanso(segundos: Int) = timer.adicionar(segundos)

    fun pularDescanso() = timer.parar()

    fun concluir() {
        viewModelScope.launch {
            timer.parar()
            repo.concluir(sessaoId)
        }
    }

    fun descartar() {
        viewModelScope.launch {
            timer.parar()
            repo.descartar(sessaoId)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TreinoApp
                SessaoViewModel(createSavedStateHandle(), app.sessaoRepository, app.descansoTimer)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessaoScreen(voltar: () -> Unit) {
    val vm: SessaoViewModel = viewModel(factory = SessaoViewModel.Factory)
    val ui by vm.ui.collectAsStateWithLifecycle()
    val inicioDescanso by vm.inicioDescanso.collectAsStateWithLifecycle()
    val fimDescanso by vm.fimDescanso.collectAsStateWithLifecycle()

    LaunchedEffect(ui.carregado, ui.ativa) {
        if (ui.carregado && !ui.ativa) {
            voltar()
        }
    }

    var confirmar by rememberSaveable { mutableStateOf<String?>(null) }
    var menuAberto by remember { mutableStateOf(false) }
    var ultimaMarcacao by rememberSaveable { mutableLongStateOf(0L) }
    var realcarDescanso by remember { mutableStateOf(false) }

    LaunchedEffect(ultimaMarcacao, fimDescanso) {
        if (ultimaMarcacao == 0L || fimDescanso != null) {
            realcarDescanso = false
            return@LaunchedEffect
        }
        realcarDescanso = true
        delay(8_000L)
        realcarDescanso = false
    }

    val progressoAlvo = if (ui.total > 0) ui.feitas.toFloat() / ui.total.toFloat() else 0f
    val progresso by animateFloatAsState(
        targetValue = progressoAlvo,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "progressoSessao"
    )

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(ui.nome, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${ui.feitas} de ${ui.total} séries concluídas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = voltar) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    },
                    actions = {
                        IconButton(onClick = { menuAberto = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Mais opções")
                        }
                        DropdownMenu(
                            expanded = menuAberto,
                            onDismissRequest = { menuAberto = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Descartar treino") },
                                onClick = {
                                    menuAberto = false
                                    confirmar = "descartar"
                                }
                            )
                        }
                    }
                )
                LinearProgressIndicator(
                    progress = { progresso },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    BarraDescanso(
                        inicio = inicioDescanso,
                        fim = fimDescanso,
                        realcar = realcarDescanso,
                        aoIniciar = vm::iniciarDescanso,
                        aoAdicionar = vm::adicionarDescanso,
                        aoPular = vm::pularDescanso
                    )
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                if (ui.feitas < ui.total) {
                                    confirmar = "concluir"
                                } else {
                                    vm.concluir()
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Concluir treino")
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(ui.grupos, key = { it.series.first().exercicioId }) { grupo ->
                GrupoExercicioCard(
                    grupo = grupo,
                    aoSalvar = { serieId, reps, carga, concluida ->
                        if (concluida) {
                            ultimaMarcacao = System.currentTimeMillis()
                        }
                        vm.salvarSerie(serieId, reps, carga, concluida)
                    }
                )
            }
        }
    }

    when (confirmar) {
        "concluir" -> AlertDialog(
            onDismissRequest = { confirmar = null },
            title = { Text("Concluir com séries em aberto?") },
            text = { Text("Você marcou ${ui.feitas} de ${ui.total} séries.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmar = null
                        vm.concluir()
                    }
                ) {
                    Text("Concluir mesmo assim")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmar = null }) {
                    Text("Voltar")
                }
            }
        )

        "descartar" -> AlertDialog(
            onDismissRequest = { confirmar = null },
            title = { Text("Descartar treino?") },
            text = { Text("Nada desta sessão será salvo no histórico.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmar = null
                        vm.descartar()
                    }
                ) {
                    Text("Descartar")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun GrupoExercicioCard(
    grupo: GrupoExercicio,
    aoSalvar: (Long, String, String, Boolean) -> Unit
) {
    val todasFeitas = grupo.series.isNotEmpty() && grupo.series.all { it.concluida }
    val corContainer by animateColorAsState(
        targetValue = if (todasFeitas) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "corGrupo"
    )

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = corContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    grupo.nome,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${grupo.series.count { it.concluida }}/${grupo.series.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (todasFeitas) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Spacer(Modifier.height(2.dp))
            grupo.series.forEach { serie ->
                LinhaSerie(serie = serie, aoSalvar = aoSalvar)
            }
        }
    }
}

@Composable
private fun LinhaSerie(
    serie: SerieDaSessao,
    aoSalvar: (Long, String, String, Boolean) -> Unit
) {
    var reps by rememberSaveable(serie.id) {
        mutableStateOf(serie.repeticoes?.toString() ?: "")
    }
    var carga by rememberSaveable(serie.id) {
        mutableStateOf(serie.carga?.let { formatarCarga(it) } ?: "")
    }

    val corBadge by animateColorAsState(
        targetValue = if (serie.concluida) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        label = "corBadgeSerie"
    )
    val corTextoBadge by animateColorAsState(
        targetValue = if (serie.concluida) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "corTextoBadgeSerie"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(corBadge),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${serie.ordem}",
                style = MaterialTheme.typography.labelMedium,
                color = corTextoBadge
            )
        }
        OutlinedTextField(
            value = reps,
            onValueChange = {
                reps = it
                aoSalvar(serie.id, it, carga, serie.concluida)
            },
            label = { Text("Reps") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = carga,
            onValueChange = {
                carga = it
                aoSalvar(serie.id, reps, it, serie.concluida)
            },
            label = { Text("Carga") },
            suffix = { Text("kg") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        Checkbox(
            checked = serie.concluida,
            onCheckedChange = { marcada ->
                aoSalvar(serie.id, reps, carga, marcada)
            }
        )
    }
}
