package com.daniel.treino.ui.treinos

import android.os.Parcelable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
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
import com.daniel.treino.data.db.Exercicio
import com.daniel.treino.data.repository.LinhaModelo
import com.daniel.treino.data.repository.TreinoRepository
import com.daniel.treino.ui.components.IconeTreino
import com.daniel.treino.ui.components.SeletorIconeTreino
import com.daniel.treino.ui.components.corDoTreino
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
data class LinhaRascunho(
    val chave: Long,
    val exercicioId: Long?,
    val nome: String,
    val series: String,
    val repeticoes: String
) : Parcelable

@Parcelize
data class Rascunho(
    val nome: String,
    val linhas: List<LinhaRascunho>,
    val icone: String? = null
) : Parcelable

class EditorTreinoViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repo: TreinoRepository
) : ViewModel() {
    private val idArg: Long = savedStateHandle["id"] ?: -1L
    val treinoId: Long? = idArg.takeIf { it > 0L }
    val ehNovo: Boolean = treinoId == null

    var rascunho by mutableStateOf(savedStateHandle.get<Rascunho>("rascunho"))
        private set

    val catalogo: StateFlow<List<Exercicio>> = repo.exercicios
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var chaveSeq = System.nanoTime()

    init {
        if (rascunho == null) {
            if (treinoId == null) {
                atualizar(Rascunho("", listOf(novaLinha())))
            } else {
                viewModelScope.launch {
                    val treino = repo.obterTreino(treinoId)
                    val itens = repo.fichaDoTreino(treinoId).first()
                    val linhas = itens.map {
                        LinhaRascunho(
                            chave = chaveSeq++,
                            exercicioId = it.exercicioId,
                            nome = it.nomeExercicio,
                            series = it.seriesPlanejadas.toString(),
                            repeticoes = it.repeticoesAlvo.toString()
                        )
                    }.ifEmpty { listOf(novaLinha()) }
                    atualizar(Rascunho(treino?.nome ?: "", linhas, treino?.icone))
                }
            }
        }
    }

    private fun novaLinha() = LinhaRascunho(chaveSeq++, null, "", "3", "10")

    private fun atualizar(novo: Rascunho) {
        rascunho = novo
        savedStateHandle["rascunho"] = novo
    }

    fun renomear(nome: String) {
        val atual = rascunho ?: return
        atualizar(atual.copy(nome = nome))
    }

    fun escolherIcone(icone: IconeTreino) {
        val atual = rascunho ?: return
        val novoValor = if (atual.icone == icone.chave) null else icone.chave
        atualizar(atual.copy(icone = novoValor))
    }

    fun adicionarLinha() {
        val atual = rascunho ?: return
        atualizar(atual.copy(linhas = atual.linhas + novaLinha()))
    }

    fun alterarLinha(linha: LinhaRascunho) {
        val atual = rascunho ?: return
        atualizar(atual.copy(linhas = atual.linhas.map { if (it.chave == linha.chave) linha else it }))
    }

    fun removerLinha(chave: Long) {
        val atual = rascunho ?: return
        atualizar(atual.copy(linhas = atual.linhas.filterNot { it.chave == chave }))
    }

    fun mover(indice: Int, direcao: Int) {
        val atual = rascunho ?: return
        val destino = indice + direcao
        if (destino !in atual.linhas.indices) return
        val mut = atual.linhas.toMutableList()
        val item = mut.removeAt(indice)
        mut.add(destino, item)
        atualizar(atual.copy(linhas = mut))
    }

    suspend fun salvar() {
        val atual = rascunho ?: return
        val itens = atual.linhas.map {
            LinhaModelo(
                nome = it.nome,
                series = it.series.toIntOrNull()?.coerceIn(1, 20) ?: 3,
                repeticoes = it.repeticoes.toIntOrNull()?.coerceIn(1, 200) ?: 10
            )
        }
        repo.salvarTreino(treinoId, atual.nome, atual.icone, itens)
    }

    suspend fun apagar() {
        val id = treinoId ?: return
        repo.apagarTreino(id)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TreinoApp
                EditorTreinoViewModel(createSavedStateHandle(), app.treinoRepository)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTreinoScreen(voltar: () -> Unit) {
    val vm: EditorTreinoViewModel = viewModel(factory = EditorTreinoViewModel.Factory)
    val rascunho = vm.rascunho ?: return
    val catalogo by vm.catalogo.collectAsStateWithLifecycle()
    val escopo = rememberCoroutineScope()
    var confirmarExclusao by rememberSaveable { mutableStateOf(false) }

    val podeSalvar = rascunho.nome.isNotBlank() && rascunho.linhas.any { it.nome.isNotBlank() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vm.ehNovo) "Novo treino" else "Editar treino") },
                navigationIcon = {
                    IconButton(onClick = voltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (!vm.ehNovo) {
                        IconButton(onClick = { confirmarExclusao = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Apagar treino")
                        }
                    }
                    TextButton(
                        onClick = {
                            escopo.launch {
                                vm.salvar()
                                voltar()
                            }
                        },
                        enabled = podeSalvar
                    ) {
                        Text("Salvar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = rascunho.nome,
                    onValueChange = vm::renomear,
                    label = { Text("Nome do treino") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                SeletorIconeTreino(
                    selecionado = IconeTreino.porChave(rascunho.icone) ?: IconeTreino.sugerir(rascunho.nome),
                    cor = corDoTreino(vm.treinoId),
                    aoEscolher = vm::escolherIcone
                )
            }

            itemsIndexed(rascunho.linhas, key = { _, l -> l.chave }) { index, linha ->
                LinhaExercicioEditor(
                    linha = linha,
                    catalogo = catalogo,
                    podeSubir = index > 0,
                    podeDescer = index < rascunho.linhas.lastIndex,
                    aoAlterar = vm::alterarLinha,
                    aoMover = { vm.mover(index, it) },
                    aoRemover = { vm.removerLinha(linha.chave) }
                )
            }

            item {
                OutlinedButton(
                    onClick = vm::adicionarLinha,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Adicionar exercício")
                }
            }
        }
    }

    if (confirmarExclusao) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            title = { Text("Apagar treino?") },
            text = { Text("A ficha será removida. O histórico de sessões anteriores permanece.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarExclusao = false
                        escopo.launch {
                            vm.apagar()
                            voltar()
                        }
                    }
                ) {
                    Text("Apagar")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarExclusao = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinhaExercicioEditor(
    linha: LinhaRascunho,
    catalogo: List<Exercicio>,
    podeSubir: Boolean,
    podeDescer: Boolean,
    aoAlterar: (LinhaRascunho) -> Unit,
    aoMover: (Int) -> Unit,
    aoRemover: () -> Unit
) {
    var focado by remember { mutableStateOf(false) }
    val sugestoes = remember(linha.nome, catalogo) {
        if (linha.nome.isBlank()) {
            catalogo.take(6)
        } else {
            catalogo.filter {
                it.nome.contains(linha.nome, ignoreCase = true) &&
                    !it.nome.equals(linha.nome, ignoreCase = true)
            }.take(5)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExposedDropdownMenuBox(
                    expanded = focado && sugestoes.isNotEmpty(),
                    onExpandedChange = {},
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = linha.nome,
                        onValueChange = { texto ->
                            val existente = catalogo.firstOrNull {
                                it.nome.equals(texto.trim(), ignoreCase = true)
                            }
                            aoAlterar(linha.copy(nome = texto, exercicioId = existente?.id))
                        },
                        label = { Text("Exercício") },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .fillMaxWidth()
                            .onFocusChanged { focado = it.isFocused },
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = focado && sugestoes.isNotEmpty(),
                        onDismissRequest = { focado = false },
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        sugestoes.forEach { ex ->
                            DropdownMenuItem(
                                text = { Text(ex.nome) },
                                onClick = {
                                    aoAlterar(linha.copy(nome = ex.nome, exercicioId = ex.id))
                                    focado = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = aoRemover) {
                    Icon(Icons.Filled.Close, contentDescription = "Remover")
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = linha.series,
                    onValueChange = { texto ->
                        if (texto.all { it.isDigit() } && texto.length <= 2) {
                            aoAlterar(linha.copy(series = texto))
                        }
                    },
                    label = { Text("Séries") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = linha.repeticoes,
                    onValueChange = { texto ->
                        if (texto.all { it.isDigit() } && texto.length <= 3) {
                            aoAlterar(linha.copy(repeticoes = texto))
                        }
                    },
                    label = { Text("Reps") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.weight(0.6f))
                IconButton(onClick = { aoMover(-1) }, enabled = podeSubir) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Subir")
                }
                IconButton(onClick = { aoMover(1) }, enabled = podeDescer) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Descer")
                }
            }
        }
    }
}
