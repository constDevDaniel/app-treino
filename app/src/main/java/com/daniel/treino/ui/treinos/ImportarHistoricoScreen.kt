package com.daniel.treino.ui.treinos

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.daniel.treino.TreinoApp
import com.daniel.treino.data.db.Treino
import com.daniel.treino.data.planilha.ImportadorPlanilha
import com.daniel.treino.data.planilha.LeitorPlanilha
import com.daniel.treino.data.planilha.PreviaSessao
import com.daniel.treino.data.repository.TreinoRepository
import com.daniel.treino.ui.components.EmblemaTreino
import com.daniel.treino.ui.components.PtBr
import com.daniel.treino.ui.components.formatarCarga
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DiaSelecionavel(val previa: PreviaSessao) {
    var selecionado by mutableStateOf(!previa.jaExisteNoDia)
    var treinoId by mutableStateOf(previa.treinoId)
}

sealed interface EstadoImportacao {
    data object Inicial : EstadoImportacao
    data object Lendo : EstadoImportacao
    data class Previa(val dias: List<DiaSelecionavel>, val avisos: List<String>) : EstadoImportacao
    data object Importando : EstadoImportacao
    data class Concluido(val quantidade: Int) : EstadoImportacao
    data class Erro(val mensagem: String) : EstadoImportacao
}

class ImportarHistoricoViewModel(
    private val app: TreinoApp,
    private val importador: ImportadorPlanilha,
    treinos: TreinoRepository
) : ViewModel() {
    var estado by mutableStateOf<EstadoImportacao>(EstadoImportacao.Inicial)
        private set

    val treinos: StateFlow<List<Treino>> = treinos.treinos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun lerArquivo(uri: Uri) {
        estado = EstadoImportacao.Lendo
        viewModelScope.launch {
            try {
                val tabela = withContext(Dispatchers.IO) {
                    val stream = app.contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("Não foi possível abrir o arquivo.")
                    stream.use { LeitorPlanilha.lerTabela(it) }
                }
                val resultado = LeitorPlanilha.analisar(tabela)
                if (resultado.sessoes.isEmpty()) {
                    val sufixo = resultado.avisos.take(3).joinToString("") { "\n• $it" }
                    estado = EstadoImportacao.Erro("Nenhum treino encontrado na planilha.$sufixo")
                } else {
                    val previas = importador.gerarPrevias(resultado)
                    estado = EstadoImportacao.Previa(
                        dias = previas.map(::DiaSelecionavel),
                        avisos = resultado.avisos
                    )
                }
            } catch (e: Exception) {
                estado = EstadoImportacao.Erro(e.message ?: "Não foi possível ler a planilha.")
            }
        }
    }

    fun importar() {
        val atual = estado as? EstadoImportacao.Previa ?: return
        val escolhidos = atual.dias.filter { it.selecionado }.map {
            it.previa.sessao to it.treinoId
        }
        if (escolhidos.isEmpty()) return
        estado = EstadoImportacao.Importando
        viewModelScope.launch {
            try {
                val qtd = importador.importar(escolhidos)
                estado = EstadoImportacao.Concluido(qtd)
            } catch (e: Exception) {
                estado = EstadoImportacao.Erro(e.message ?: "Falha ao importar.")
            }
        }
    }

    fun reiniciar() {
        estado = EstadoImportacao.Inicial
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TreinoApp
                ImportarHistoricoViewModel(app, app.importadorHistorico, app.treinoRepository)
            }
        }
    }
}

private val FormatoDataCompleta: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, dd/MM/yyyy", PtBr)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportarHistoricoScreen(voltar: () -> Unit) {
    val vm: ImportarHistoricoViewModel = viewModel(factory = ImportarHistoricoViewModel.Factory)
    val treinos by vm.treinos.collectAsStateWithLifecycle()

    val seletorArquivo = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            vm.lerArquivo(uri)
        }
    }

    val abrirSeletor = {
        seletorArquivo.launch(
            arrayOf(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/octet-stream"
            )
        )
    }

    val estado = vm.estado

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Importar do Excel", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = voltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (estado is EstadoImportacao.Previa) {
                        TextButton(onClick = abrirSeletor) {
                            Text("Trocar arquivo")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (estado is EstadoImportacao.Previa) {
                val selecionados = estado.dias.count { it.selecionado }
                Surface(tonalElevation = 3.dp) {
                    Column(modifier = Modifier.navigationBarsPadding()) {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "$selecionados de ${estado.dias.size} ${plural(estado.dias.size, "dia", "dias")}",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = vm::importar,
                                enabled = selecionados > 0
                            ) {
                                Text(
                                    "Importar $selecionados ${plural(selecionados, "treino", "treinos")}"
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (estado) {
                EstadoImportacao.Inicial -> InstrucoesImportacao(aoEscolher = abrirSeletor)
                EstadoImportacao.Lendo -> EstadoCarregando("Lendo planilha...")
                EstadoImportacao.Importando -> EstadoCarregando("Importando treinos...")
                is EstadoImportacao.Previa -> ListaPrevia(estado = estado, treinos = treinos)
                is EstadoImportacao.Concluido -> EstadoFinal(
                    icone = Icons.Outlined.CheckCircle,
                    corIcone = MaterialTheme.colorScheme.primary,
                    titulo = "${estado.quantidade} ${plural(estado.quantidade, "treino importado", "treinos importados")}",
                    mensagem = "Os dados já aparecem no histórico, nos gráficos de evolução e nos recordes pessoais.",
                    rotuloBotao = "Concluir",
                    aoClicar = voltar
                )
                is EstadoImportacao.Erro -> EstadoFinal(
                    icone = Icons.Outlined.ErrorOutline,
                    corIcone = MaterialTheme.colorScheme.error,
                    titulo = "Não deu para importar",
                    mensagem = estado.mensagem,
                    rotuloBotao = "Escolher outro arquivo",
                    aoClicar = {
                        vm.reiniciar()
                        abrirSeletor()
                    }
                )
            }
        }
    }
}

@Composable
private fun InstrucoesImportacao(aoEscolher: () -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Traga seus treinos anotados em planilha (.xlsx) para o histórico do app.",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        item {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Formato esperado", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Uma linha por exercício feito no dia, com 4 colunas na primeira aba:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    ExemploTabela()
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "• Data: dd/mm/aaaa ou célula de data do Excel\n" +
                            "• Carga: número em kg (deixe vazio se não usou peso)\n" +
                            "• Repetições: número por série ou formato 3x12\n" +
                            "• Linhas da mesma data viram um único treino",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Button(
                onClick = aoEscolher,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.UploadFile, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Selecionar arquivo .xlsx")
            }
        }
    }
}

@Composable
private fun ExemploTabela() {
    val linhas = listOf(
        listOf("Data", "Exercício", "Carga", "Reps"),
        listOf("10/03/2026", "Supino reto", "60", "10"),
        listOf("10/03/2026", "Tríceps corda", "25", "12"),
        listOf("12/03/2026", "Agachamento", "80", "8")
    )
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            linhas.forEachIndexed { idx, cols ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    cols.forEachIndexed { cIdx, texto ->
                        Text(
                            texto,
                            modifier = Modifier.weight(if (cIdx == 1) 1.5f else 1f),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListaPrevia(
    estado: EstadoImportacao.Previa,
    treinos: List<Treino>
) {
    val mapaTreinos = remember(treinos) { treinos.associateBy { it.id } }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (estado.avisos.isNotEmpty()) {
            item {
                OutlinedCard(
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Column {
                            Text(
                                "${estado.avisos.size} ${plural(estado.avisos.size, "linha ignorada", "linhas ignoradas")}",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                estado.avisos.take(3).joinToString("\n"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Confira os dias encontrados e a qual treino cada um pertence.",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val todosMarcados = estado.dias.all { it.selecionado }
                TextButton(
                    onClick = {
                        val novo = !todosMarcados
                        estado.dias.forEach { it.selecionado = novo }
                    }
                ) {
                    Text(if (todosMarcados) "Desmarcar" else "Marcar todos")
                }
            }
        }

        items(estado.dias, key = { "${it.previa.sessao.data}-${it.previa.sessao.treinoInformado}" }) { item ->
            CartaoDiaImportacao(
                item = item,
                treinos = treinos,
                treinoAtual = item.treinoId?.let { mapaTreinos[it] }
            )
        }
    }
}

@Composable
private fun CartaoDiaImportacao(
    item: DiaSelecionavel,
    treinos: List<Treino>,
    treinoAtual: Treino?
) {
    val previa = item.previa
    val sessao = previa.sessao
    val dataFormatada = remember(sessao.data) {
        sessao.data.format(FormatoDataCompleta).replaceFirstChar { it.titlecase(PtBr) }
    }
    val nomeExibicao = treinoAtual?.nome ?: sessao.treinoInformado ?: "Treino importado"
    val qtdExercicios = remember(sessao) { sessao.exercicios().size }
    val qtdSeries = sessao.linhas.size

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmblemaTreino(
                    id = treinoAtual?.id,
                    nome = nomeExibicao,
                    icone = treinoAtual?.icone,
                    tamanho = 40.dp
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(dataFormatada, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "$qtdExercicios ${plural(qtdExercicios, "exercício", "exercícios")}, $qtdSeries ${plural(qtdSeries, "série", "séries")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Checkbox(
                    checked = item.selecionado,
                    onCheckedChange = { item.selecionado = it }
                )
            }

            if (previa.treinoNaoEncontrado != null) {
                Text(
                    "Treino \"${previa.treinoNaoEncontrado}\" da planilha não existe no app. Escolha um ou importe sem treino.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (previa.jaExisteNoDia) {
                Text(
                    "Já existe um treino concluído neste dia. Marque só se não for o mesmo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (treinos.isNotEmpty()) {
                SeletorTreinoDia(
                    treinos = treinos,
                    selecionado = treinoAtual,
                    aoEscolher = { item.treinoId = it?.id }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeletorTreinoDia(
    treinos: List<Treino>,
    selecionado: Treino?,
    aoEscolher: (Treino?) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = it }
    ) {
        OutlinedTextField(
            value = selecionado?.nome ?: "Sem ficha (Treino importado)",
            onValueChange = {},
            readOnly = true,
            label = { Text("Associar ao treino") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium
        )
        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            treinos.forEach { t ->
                DropdownMenuItem(
                    text = { Text(t.nome) },
                    onClick = {
                        aoEscolher(t)
                        expandido = false
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Sem ficha (Treino importado)") },
                onClick = {
                    aoEscolher(null)
                    expandido = false
                }
            )
        }
    }
}

@Composable
private fun EstadoCarregando(mensagem: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            mensagem,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EstadoFinal(
    icone: ImageVector,
    corIcone: androidx.compose.ui.graphics.Color,
    titulo: String,
    mensagem: String,
    rotuloBotao: String,
    aoClicar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icone,
            contentDescription = null,
            tint = corIcone,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(titulo, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            mensagem,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = aoClicar) {
            Text(rotuloBotao)
        }
    }
}

private fun plural(n: Int, singular: String, plural: String): String =
    if (n == 1) singular else plural
