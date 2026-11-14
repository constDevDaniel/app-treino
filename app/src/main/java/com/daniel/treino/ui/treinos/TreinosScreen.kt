package com.daniel.treino.ui.treinos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.daniel.treino.data.db.Exercicio
import com.daniel.treino.data.db.ResumoTreino
import com.daniel.treino.data.db.Treino
import com.daniel.treino.data.repository.TreinoRepository
import com.daniel.treino.ui.components.EmblemaTreino
import com.daniel.treino.ui.components.EstadoVazio
import com.daniel.treino.ui.components.escalaPressao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TreinosViewModel(private val repo: TreinoRepository) : ViewModel() {
    val treinos: StateFlow<List<Treino>?> = repo.treinos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val exercicios: StateFlow<List<Exercicio>> = repo.exercicios
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val resumos: StateFlow<Map<Long, ResumoTreino>> = repo.resumos
        .map { lista -> lista.associateBy { it.treinoId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    suspend fun renomearExercicio(id: Long, novoNome: String): Boolean =
        repo.renomearExercicio(id, novoNome)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TreinoApp
                TreinosViewModel(app.treinoRepository)
            }
        }
    }
}

@Composable
fun TreinosScreen(
    aoEditar: (Long) -> Unit,
    aoCriar: () -> Unit,
    aoImportar: () -> Unit
) {
    val vm: TreinosViewModel = viewModel(factory = TreinosViewModel.Factory)
    val treinos by vm.treinos.collectAsStateWithLifecycle()
    val exercicios by vm.exercicios.collectAsStateWithLifecycle()
    val resumos by vm.resumos.collectAsStateWithLifecycle()
    val escopo = rememberCoroutineScope()
    var editandoExercicio by remember { mutableStateOf<Exercicio?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = aoCriar,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Novo treino") }
            )
        }
    ) { padding ->
        val lista = treinos ?: return@Scaffold
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 20.dp,
                end = 20.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Treinos",
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            if (lista.isEmpty()) {
                item {
                    EstadoVazio(
                        icone = Icons.Outlined.FitnessCenter,
                        titulo = "Sem treinos por aqui",
                        mensagem = "Monte uma ficha com exercícios, séries e repetições."
                    )
                }
            }

            items(lista, key = { "t-${it.id}" }) { treino ->
                val resumo = resumos[treino.id]
                val qtdExercicios = resumo?.exercicios ?: 0
                val qtdSeries = resumo?.series ?: 0
                val interacao = remember { MutableInteractionSource() }

                Surface(
                    onClick = { aoEditar(treino.id) },
                    interactionSource = interacao,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .escalaPressao(interacao),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EmblemaTreino(
                            id = treino.id,
                            nome = treino.nome,
                            icone = treino.icone,
                            tamanho = 44.dp
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(treino.nome, style = MaterialTheme.typography.titleMedium)
                            val legenda = if (qtdExercicios > 0) {
                                "$qtdExercicios exercícios, $qtdSeries séries"
                            } else {
                                "Toque para adicionar exercícios"
                            }
                            Text(
                                legenda,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = aoImportar,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        Icons.Outlined.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Importar treinos do Excel")
                }
            }

            if (exercicios.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Exercícios",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Renomear aqui vale para todos os treinos e para o histórico.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(exercicios, key = { "e-${it.id}" }) { ex ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            ex.nome,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 4.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        IconButton(onClick = { editandoExercicio = ex }) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "Renomear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    val alvo = editandoExercicio
    if (alvo != null) {
        DialogoRenomearExercicio(
            exercicio = alvo,
            aoFechar = { editandoExercicio = null },
            aoConfirmar = { novoNome, aoErro ->
                escopo.launch {
                    if (vm.renomearExercicio(alvo.id, novoNome)) {
                        editandoExercicio = null
                    } else {
                        aoErro()
                    }
                }
            }
        )
    }
}

@Composable
private fun DialogoRenomearExercicio(
    exercicio: Exercicio,
    aoFechar: () -> Unit,
    aoConfirmar: (String, () -> Unit) -> Unit
) {
    var nome by rememberSaveable(exercicio.id) { mutableStateOf(exercicio.nome) }
    var erro by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Renomear exercício") },
        text = {
            OutlinedTextField(
                value = nome,
                onValueChange = {
                    nome = it
                    erro = false
                },
                singleLine = true,
                isError = erro,
                supportingText = if (erro) {
                    { Text("Já existe um exercício com esse nome.") }
                } else {
                    null
                }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { aoConfirmar(nome) { erro = true } },
                enabled = nome.isNotBlank()
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = aoFechar) {
                Text("Cancelar")
            }
        }
    )
}
