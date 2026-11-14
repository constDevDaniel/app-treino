package com.daniel.treino.ui.progresso

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.daniel.treino.data.db.EvolucaoCarga
import com.daniel.treino.data.db.Exercicio
import com.daniel.treino.data.db.RecordePessoal
import com.daniel.treino.data.db.RelatoriosDao
import com.daniel.treino.data.db.ResumoPeriodo
import com.daniel.treino.data.db.VolumeSemanal
import com.daniel.treino.ui.components.TextoRolante
import com.daniel.treino.ui.components.formatarCarga
import com.daniel.treino.ui.components.formatarDataCurta
import com.daniel.treino.ui.components.formatarVolume
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.point
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.shader.verticalGradient
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.core.common.shader.ShaderProvider
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn

enum class Periodo(val rotulo: String, val meses: Long) {
    Mes("1 mês", 1),
    Trimestre("3 meses", 3),
    Semestre("6 meses", 6),
    Ano("1 ano", 12)
}

data class RelatoriosUi(
    val carregado: Boolean = false,
    val periodo: Periodo = Periodo.Mes,
    val resumo: ResumoPeriodo = ResumoPeriodo(0, 0, 0.0),
    val volumeSemanal: List<VolumeSemanal> = emptyList(),
    val exercicios: List<Exercicio> = emptyList(),
    val exercicio: Exercicio? = null,
    val evolucao: List<EvolucaoCarga> = emptyList(),
    val recordes: List<RecordePessoal> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressoViewModel(
    private val savedStateHandle: SavedStateHandle,
    dao: RelatoriosDao
) : ViewModel() {
    private val periodo = savedStateHandle.getStateFlow("periodo", 0)
        .map { Periodo.entries[it] }
    private val exercicioId = savedStateHandle.getStateFlow("exercicio", -1L)

    private val dadosPeriodo = periodo.flatMapLatest { p ->
        val desde = LocalDate.now()
            .minusMonths(p.meses)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        combine(
            dao.resumoPeriodo(desde),
            dao.volumeSemanal(desde),
            dao.exerciciosNoPeriodo(desde)
        ) { resumo, volume, exercicios ->
            Triple(p, resumo, volume) to exercicios
        }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private val dadosExercicio = combine(
        periodo,
        dadosPeriodo,
        exercicioId
    ) { p, (_, exercicios), id ->
        val escolhido = exercicios.firstOrNull { it.id == id } ?: exercicios.firstOrNull()
        Triple(p, escolhido, exercicios)
    }.flatMapLatest { (p, ex, _) ->
        if (ex == null) {
            flowOf(null to emptyList())
        } else {
            val desde = LocalDate.now()
                .minusMonths(p.meses)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            dao.evolucaoCarga(ex.id, desde).map { ex to it }
        }
    }

    val ui: StateFlow<RelatoriosUi> = combine(
        dadosPeriodo,
        dadosExercicio,
        dao.recordesPessoais()
    ) { (triplo, exercicios), (exercicio, evolucao), recordes ->
        val (p, resumo, volume) = triplo
        RelatoriosUi(
            carregado = true,
            periodo = p,
            resumo = resumo,
            volumeSemanal = volume,
            exercicios = exercicios,
            exercicio = exercicio,
            evolucao = evolucao,
            recordes = recordes
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RelatoriosUi())

    fun selecionarPeriodo(p: Periodo) {
        savedStateHandle["periodo"] = p.ordinal
    }

    fun escolherExercicio(id: Long) {
        savedStateHandle["exercicio"] = id
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TreinoApp
                ProgressoViewModel(createSavedStateHandle(), app.database.relatoriosDao())
            }
        }
    }
}

private val ChaveRotulos = ExtraStore.Key<List<String>>()

private val FormatadorRotulos = CartesianValueFormatter { contexto, x, _ ->
    contexto.model.extraStore[ChaveRotulos].getOrNull(x.toInt())?.takeIf { it.isNotEmpty() } ?: " "
}

@Composable
fun ProgressoScreen(abrirHistorico: () -> Unit) {
    val vm: ProgressoViewModel = viewModel(factory = ProgressoViewModel.Factory)
    val ui by vm.ui.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Relatórios",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineMedium
                )
                TextButton(onClick = abrirHistorico) {
                    Icon(Icons.Outlined.History, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Histórico", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                Periodo.entries.forEachIndexed { index, p ->
                    SegmentedButton(
                        selected = ui.periodo == p,
                        onClick = { vm.selecionarPeriodo(p) },
                        shape = SegmentedButtonDefaults.itemShape(index, Periodo.entries.size),
                        icon = {}
                    ) {
                        Text(p.rotulo)
                    }
                }
            }
        }

        if (!ui.carregado) {
            item { GraficoCarregando() }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CartaoNumero("Treinos", ui.resumo.treinos.toString(), Modifier.weight(1f))
                    CartaoNumero("Séries", ui.resumo.series.toString(), Modifier.weight(1f))
                    CartaoNumero("Volume (kg)", formatarVolume(ui.resumo.volume), Modifier.weight(1f))
                }
            }

            item {
                CartaoSecao(
                    titulo = "Evolução de carga",
                    subtitulo = "Maior carga de cada sessão."
                ) {
                    if (ui.exercicios.isEmpty()) {
                        GraficoVazio()
                    } else {
                        SeletorExercicio(
                            exercicios = ui.exercicios,
                            selecionado = ui.exercicio,
                            aoEscolher = { vm.escolherExercicio(it.id) }
                        )
                        Spacer(Modifier.height(8.dp))
                        GraficoEvolucao(ui.evolucao)
                    }
                }
            }

            item {
                CartaoSecao(
                    titulo = "Volume semanal (kg)",
                    subtitulo = "Soma de repetições × carga por semana."
                ) {
                    if (ui.volumeSemanal.isEmpty()) {
                        GraficoVazio()
                    } else {
                        GraficoVolume(ui.volumeSemanal)
                    }
                }
            }

            item {
                Column {
                    Text("Recordes pessoais", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Maior carga de todos os tempos em cada exercício.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (ui.recordes.isEmpty()) {
                item { GraficoVazio() }
            } else {
                items(ui.recordes, key = { it.exercicioId }) { rec ->
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        ListItem(
                            leadingContent = {
                                Icon(
                                    Icons.Outlined.EmojiEvents,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            },
                            headlineContent = { Text(rec.nome) },
                            supportingContent = { Text("Em ${formatarDataCurta(rec.inicio)}") },
                            trailingContent = {
                                Text(
                                    "${formatarCarga(rec.cargaMaxima)} kg",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CartaoSecao(
    titulo: String,
    subtitulo: String,
    conteudo: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitulo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            conteudo()
        }
    }
}

@Composable
private fun CartaoNumero(rotulo: String, valor: String, modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            TextoRolante(
                texto = valor,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                rotulo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeletorExercicio(
    exercicios: List<Exercicio>,
    selecionado: Exercicio?,
    aoEscolher: (Exercicio) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = it }
    ) {
        OutlinedTextField(
            value = selecionado?.nome ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Exercício") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            exercicios.forEach { ex ->
                DropdownMenuItem(
                    text = { Text(ex.nome) },
                    onClick = {
                        aoEscolher(ex)
                        expandido = false
                    }
                )
            }
        }
    }
}

@Composable
private fun GraficoEvolucao(dados: List<EvolucaoCarga>) {
    if (dados.isEmpty()) {
        GraficoVazio()
        return
    }
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(dados) {
        val (xs, ys, rotulos) = if (dados.size == 1) {
            val d = dados.first()
            Triple(
                listOf(0, 1, 2),
                listOf(d.cargaMaxima, d.cargaMaxima, d.cargaMaxima),
                listOf("", formatarDataCurta(d.inicio), "")
            )
        } else {
            Triple(
                dados.indices.toList(),
                dados.map { it.cargaMaxima },
                dados.map { formatarDataCurta(it.inicio) }
            )
        }
        producer.runTransaction {
            lineSeries { series(xs, ys) }
            extras { it[ChaveRotulos] = rotulos }
        }
    }

    val corPrimaria = MaterialTheme.colorScheme.primary
    val ponto = rememberShapeComponent(fill(corPrimaria), CorneredShape.Pill)
    val camada = rememberLineCartesianLayer(
        LineCartesianLayer.LineProvider.series(
            LineCartesianLayer.rememberLine(
                fill = LineCartesianLayer.LineFill.single(fill(corPrimaria)),
                areaFill = LineCartesianLayer.AreaFill.single(
                    fill(
                        ShaderProvider.verticalGradient(
                            arrayOf(
                                corPrimaria.copy(alpha = 0.32f),
                                corPrimaria.copy(alpha = 0.0f)
                            )
                        )
                    )
                ),
                pointProvider = LineCartesianLayer.PointProvider.single(
                    LineCartesianLayer.point(ponto, size = 8.dp)
                )
            )
        )
    )

    CartesianChartHost(
        chart = graficoPadrao(camada),
        modelProducer = producer,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}

@Composable
private fun GraficoVolume(dados: List<VolumeSemanal>) {
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(dados) {
        val rotulos = dados.map { "S" + it.semana.substringAfter('-') }
        producer.runTransaction {
            columnSeries { series(dados.map { it.volume }) }
            extras { it[ChaveRotulos] = rotulos }
        }
    }

    val corPrimaria = MaterialTheme.colorScheme.primary
    val coluna = rememberLineComponent(
        fill = fill(corPrimaria),
        thickness = 18.dp,
        shape = CorneredShape.rounded(topLeftPercent = 30, topRightPercent = 30)
    )
    val camada = rememberColumnCartesianLayer(
        ColumnCartesianLayer.ColumnProvider.series(coluna)
    )

    CartesianChartHost(
        chart = graficoPadrao(camada),
        modelProducer = producer,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}

@Composable
private fun graficoPadrao(camada: com.patrykandpatrick.vico.core.cartesian.layer.CartesianLayer<*>) =
    rememberCartesianChart(
        camada,
        startAxis = VerticalAxis.rememberStart(
            label = eixoBase().first,
            line = eixoBase().second,
            guideline = rememberAxisGuidelineComponent(
                fill = fill(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
        ),
        bottomAxis = HorizontalAxis.rememberBottom(
            label = eixoBase().first,
            line = eixoBase().second,
            guideline = null,
            valueFormatter = FormatadorRotulos
        )
    )

@Composable
private fun eixoBase() =
    rememberAxisLabelComponent(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textSize = 11.sp
    ) to rememberAxisLineComponent(
        fill = fill(MaterialTheme.colorScheme.outlineVariant)
    )

@Composable
private fun GraficoVazio() {
    Text(
        "Sem dados no período selecionado.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun GraficoCarregando() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
