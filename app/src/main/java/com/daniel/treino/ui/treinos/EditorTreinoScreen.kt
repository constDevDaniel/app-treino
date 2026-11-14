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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.text.style.TextAlign
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
import com.daniel.treino.ui.components.EmblemaTreino
import com.daniel.treino.ui.components.IconesTreino
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

    fun escolherIcone(icone: IconesTreino) {
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
                            Icon(Icons.Filled.Delete, contentDescription = "Apagar treino")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EmblemaTreino(
                        id = vm.treinoId ?: 0L,
                        nome = rascunho.nome,
                        icone = rascunho.icone,
                        tamanho = 52.dp
                    )
                    OutlinedTextField(
                        value = rascunho.nome,
                        onValueChange = vm::renomear,
                        label = { Text("Nome do treino (ex: Treino A – Peito)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            item {
                SeletorIcone(
                    idTreino = vm.treinoId ?: 0L,
                    nomeTreino = rascunho.nome,
                    selecionado = rascunho.icone,
                    aoEscolher = vm::escolherIcone
                )
            }

            item {
                Text("Exercícios", style = MaterialTheme.typography.titleMedium)
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
                    modifier = Modifier.fillMaxWidth()
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

@Composable
private fun SeletorIcone(
    idTreino: Long,
    nomeTreino: String,
    selecionado: String?,
    aoEscolher: (IconesTreino) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Ícone",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(IconesTreino.entries, key = { it.chave }) { opcao ->
                val ativo = selecionado == opcao.chave
                Surface(
                    onClick = { aoEscolher(opcao) },
                    shape = MaterialTheme.shapes.medium,
                    color = if (ativo) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    border = if (ativo) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        null
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .width(76.dp)
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        EmblemaTreino(
                            id = idTreino,
                            nome = nomeTreino,
                            icone = opcao.chave,
                            tamanho = 36.dp
                        )
                        Text(
                            opcao.rotulo,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
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

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                IconButton(onClick = { aoMover(-1) }, enabled = podeSubir) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Subir")
                }
                IconButton(onClick = { aoMover(1) }, enabled = podeDescer) {
                    Icon(Icons.Filled.ArrowDownward, contentDescription = "Descer")
                }
                IconButton(onClick = aoRemover) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remover")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoNumero(
                    rotulo = "Séries",
                    valor = linha.series,
                    minimo = 1,
                    maximo = 20,
                    aoMudar = { aoAlterar(linha.copy(series = it)) },
                    modifier = Modifier.weight(1f)
                )
                CampoNumero(
                    rotulo = "Repetições",
                    valor = linha.repeticoes,
                    minimo = 1,
                    maximo = 200,
                    aoMudar = { aoAlterar(linha.copy(repeticoes = it)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CampoNumero(
    rotulo: String,
    valor: String,
    minimo: Int,
    maximo: Int,
    aoMudar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val numero = valor.toIntOrNull() ?: minimo
    Column(modifier = modifier) {
        Text(
            rotulo,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilledTonalIconButton(
                onClick = { aoMudar((numero - 1).coerceAtLeast(minimo).toString()) },
                enabled = numero > minimo,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.Remove,
                    contentDescription = "Diminuir $rotulo",
                    modifier = Modifier.size(18.dp)
                )
            }
            OutlinedTextField(
                value = valor,
                onValueChange = { texto ->
                    if (texto.all { it.isDigit() } && texto.length <= 3) {
                        aoMudar(texto)
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            FilledTonalIconButton(
                onClick = { aoMudar((numero + 1).coerceAtMost(maximo).toString()) },
                enabled = numero < maximo,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Aumentar $rotulo",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
