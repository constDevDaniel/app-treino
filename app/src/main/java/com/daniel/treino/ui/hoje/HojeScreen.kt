package com.daniel.treino.ui.hoje

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.daniel.treino.TreinoApp
import com.daniel.treino.data.db.ResumoModelo
import com.daniel.treino.data.db.Treino
import com.daniel.treino.ui.components.EmblemaTreino
import com.daniel.treino.ui.components.Formatadores
import com.daniel.treino.ui.components.corDoTreino
import com.daniel.treino.ui.components.corSobre
import com.daniel.treino.ui.components.escalaPressao
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit

// Reconstructed from V1/x.java, V1/a..w.java, U1/f.java (case 1), U1/j.java (case 2), S1/g.java (cases 3-4)

private val formatoCabecalho: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Formatadores.ptBr)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojeScreen(
    abrirSessao: (Long) -> Unit,
    abrirBackup: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as TreinoApp
    val vm: HojeViewModel = viewModel(
        factory = HojeViewModel.factory(app.treinoRepository, app.sessaoRepository),
    )
    val ui by vm.ui.collectAsState()
    val estadoBackup by app.backupArquivo.estado.collectAsState()

    val dataTexto = remember(vm.hoje) {
        vm.hoje.format(formatoCabecalho).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Formatadores.ptBr) else it.toString()
        }
    }

    val diaHoje = ui.semana.firstOrNull { it.data == vm.hoje }
    val concluidoHoje = diaHoje?.feito
    val idExcluido = ui.emAndamento?.treinoId ?: concluidoHoje?.treinoId ?: ui.agendado?.treinoId
    val outrosTreinos = remember(ui.treinos, idExcluido) {
        ui.treinos.filter { it.id != idExcluido }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = dataTexto,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Hoje",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = abrirBackup) {
                        val configurado = estadoBackup.destino != null
                        Icon(
                            imageVector = if (configurado) {
                                Icons.Outlined.CloudDone
                            } else {
                                Icons.Outlined.CloudUpload
                            },
                            contentDescription = "Backup",
                            tint = if (configurado) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (!ui.carregado) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                FaixaSemana(
                    semana = ui.semana,
                    hoje = vm.hoje,
                )
            }

            item {
                val emAndamento = ui.emAndamento
                val agendado = ui.agendado
                when {
                    emAndamento != null -> {
                        CartaoDestaque(
                            etiqueta = "EM ANDAMENTO",
                            titulo = emAndamento.treinoNome,
                            treinoId = emAndamento.treinoId,
                            resumo = emAndamento.treinoId?.let { ui.resumos[it] },
                            ultimaVez = null,
                            acao = "Continuar treino",
                            destaqueAtivo = true,
                            aoClicar = { abrirSessao(emAndamento.id) },
                        )
                    }
                    concluidoHoje != null -> {
                        CartaoDestaque(
                            etiqueta = "CONCLUÍDO HOJE",
                            titulo = concluidoHoje.treinoNome,
                            treinoId = concluidoHoje.treinoId,
                            resumo = concluidoHoje.treinoId?.let { ui.resumos[it] },
                            ultimaVez = null,
                            acao = "Ver treino",
                            destaqueAtivo = false,
                            aoClicar = { abrirSessao(concluidoHoje.id) },
                        )
                    }
                    agendado != null -> {
                        CartaoDestaque(
                            etiqueta = "TREINO DO DIA",
                            titulo = agendado.treinoNome,
                            treinoId = agendado.treinoId,
                            resumo = ui.resumos[agendado.treinoId],
                            ultimaVez = ui.ultimas[agendado.treinoId]?.let {
                                formatarUltimaVez(it, vm.hoje)
                            },
                            acao = "Iniciar treino",
                            destaqueAtivo = false,
                            aoClicar = {
                                vm.iniciarTreino(agendado.treinoId, abrirSessao)
                            },
                        )
                    }
                    else -> {
                        CartaoDescanso(
                            temTreinos = ui.treinos.isNotEmpty(),
                        )
                    }
                }
            }

            if (outrosTreinos.isNotEmpty()) {
                item {
                    Text(
                        text = if (ui.agendado != null || concluidoHoje != null) {
                            "Treinar outro"
                        } else {
                            "Iniciar um treino"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                items(
                    items = outrosTreinos,
                    key = { it.id },
                ) { treino ->
                    CartaoOutroTreino(
                        treino = treino,
                        resumo = ui.resumos[treino.id],
                        ultimaVez = ui.ultimas[treino.id]?.let {
                            formatarUltimaVez(it, vm.hoje)
                        },
                        aoIniciar = {
                            vm.iniciarTreino(treino.id, abrirSessao)
                        },
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun FaixaSemana(
    semana: List<DiaDaSemana>,
    hoje: LocalDate,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            semana.forEach { dia ->
                val ehHoje = dia.data == hoje
                val concluido = dia.feito != null
                val agendado = dia.agendado
                val letra = dia.data.dayOfWeek
                    .getDisplayName(TextStyle.NARROW, Formatadores.ptBr)
                    .uppercase(Formatadores.ptBr)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = letra,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (ehHoje) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    concluido -> MaterialTheme.colorScheme.tertiary
                                    ehHoje -> MaterialTheme.colorScheme.primaryContainer
                                    else -> Color.Transparent
                                },
                            )
                            .then(
                                if (ehHoje && !concluido) {
                                    Modifier.border(
                                        width = 1.5.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape,
                                    )
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (concluido) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Concluído",
                                tint = MaterialTheme.colorScheme.onTertiary,
                                modifier = Modifier.size(18.dp),
                            )
                        } else {
                            Text(
                                text = dia.data.dayOfMonth.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                color = if (ehHoje) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    if (agendado != null) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(corDoTreino(agendado.treinoId)),
                        )
                    } else {
                        Spacer(Modifier.size(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CartaoDestaque(
    etiqueta: String,
    titulo: String,
    treinoId: Long?,
    resumo: ResumoModelo?,
    ultimaVez: String?,
    acao: String,
    destaqueAtivo: Boolean,
    aoClicar: () -> Unit,
) {
    val corAcento = if (destaqueAtivo) {
        MaterialTheme.colorScheme.secondary
    } else {
        corDoTreino(treinoId)
    }
    val interacao = remember { MutableInteractionSource() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .escalaPressao(interacao),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = BorderStroke(
            width = 1.5.dp,
            color = corAcento.copy(alpha = if (destaqueAtivo) 0.85f else 0.45f),
        ),
    ) {
        Column(
            modifier = Modifier
                .clickable(
                    interactionSource = interacao,
                    indication = null,
                    onClick = aoClicar,
                )
                .padding(24.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(corAcento),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = etiqueta,
                            style = MaterialTheme.typography.labelMedium,
                            color = corAcento,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.width(12.dp))
                EmblemaTreino(
                    treinoId = treinoId,
                    nome = titulo,
                    tamanho = 56.dp,
                )
            }

            if (resumo != null || ultimaVez != null) {
                Spacer(Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    if (resumo != null) {
                        StatDestaque(
                            valor = resumo.exercicios.toString(),
                            rotulo = if (resumo.exercicios == 1) "exercício" else "exercícios",
                        )
                        StatDestaque(
                            valor = resumo.series.toString(),
                            rotulo = if (resumo.series == 1) "série" else "séries",
                        )
                    }
                    if (ultimaVez != null) {
                        StatDestaque(
                            valor = ultimaVez,
                            rotulo = "última vez",
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = aoClicar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = corAcento,
                    contentColor = corSobre(corAcento),
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = acao,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun StatDestaque(
    valor: String,
    rotulo: String,
) {
    Column {
        Text(
            text = valor,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = rotulo,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CartaoDescanso(
    temTreinos: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = if (temTreinos) "HOJE" else "BEM-VINDO",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (temTreinos) "Dia de descanso" else "Monte seu primeiro treino",
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (temTreinos) {
                    "Nenhum treino agendado para hoje. Aproveite para descansar ou escolha um treino abaixo."
                } else {
                    "Vá até a aba Treinos para criar seus treinos ou importar uma planilha, e depois defina os dias na aba Agenda."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CartaoOutroTreino(
    treino: Treino,
    resumo: ResumoModelo?,
    ultimaVez: String?,
    aoIniciar: () -> Unit,
) {
    val interacao = remember { MutableInteractionSource() }

    Surface(
        onClick = aoIniciar,
        interactionSource = interacao,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .escalaPressao(interacao),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EmblemaTreino(
                treinoId = treino.id,
                nome = treino.nome,
                tamanho = 44.dp,
            )
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = treino.nome,
                    style = MaterialTheme.typography.titleLarge,
                )
                val info = buildList {
                    if (resumo != null && resumo.exercicios > 0) {
                        add("${resumo.exercicios} ex · ${resumo.series} séries")
                    }
                    if (ultimaVez != null) {
                        add("Último: $ultimaVez")
                    }
                }.joinToString(" • ")

                if (info.isNotEmpty()) {
                    Text(
                        text = info,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Iniciar",
                tint = corDoTreino(treino.id),
            )
        }
    }
}

private fun formatarUltimaVez(epochMillis: Long, hoje: LocalDate): String {
    val data = Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val dias = ChronoUnit.DAYS.between(data, hoje)
    return when {
        dias <= 0L -> "hoje"
        dias == 1L -> "ontem"
        dias < 7L -> {
            val dia = Formatadores.nomeDiaSemana(data.dayOfWeek.value, TextStyle.SHORT)
                .lowercase(Formatadores.ptBr)
            "$dia (${dias}d)"
        }
        else -> "há ${dias}d"
    }
}
