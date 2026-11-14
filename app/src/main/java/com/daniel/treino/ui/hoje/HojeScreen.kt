package com.daniel.treino.ui.hoje

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

private val formatoDiaMes: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM", Formatadores.ptBr)

private data class ConfirmacaoRepetirTreino(
    val treinoId: Long,
    val treinoNome: String,
)

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
    var confirmacaoRepetir by remember { mutableStateOf<ConfirmacaoRepetirTreino?>(null) }

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

    fun tentarIniciarTreino(treinoId: Long, treinoNome: String) {
        val ultima = ui.ultimas[treinoId]
        val feitoHoje = ultima != null && Instant.ofEpochMilli(ultima)
            .atZone(ZoneId.systemDefault())
            .toLocalDate() == vm.hoje
        if (feitoHoje) {
            confirmacaoRepetir = ConfirmacaoRepetirTreino(treinoId, treinoNome)
        } else {
            vm.iniciarTreino(treinoId, abrirSessao)
        }
    }

    val repetir = confirmacaoRepetir
    if (repetir != null) {
        AlertDialog(
            onDismissRequest = { confirmacaoRepetir = null },
            icon = {
                EmblemaTreino(
                    treinoId = repetir.treinoId,
                    nome = repetir.treinoNome,
                    tamanho = 48.dp,
                )
            },
            title = {
                Text("${repetir.treinoNome} já foi feito hoje")
            },
            text = {
                Text(
                    "Você já concluiu este treino hoje. Quer iniciar outra sessão de ${repetir.treinoNome} mesmo assim?",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmacaoRepetir = null
                        vm.iniciarTreino(repetir.treinoId, abrirSessao)
                    },
                ) {
                    Text("Iniciar mesmo assim")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmacaoRepetir = null }) {
                    Text("Cancelar")
                }
            },
        )
    }

    if (!ui.carregado) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = dataTexto,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Hoje",
                            style = MaterialTheme.typography.displayMedium,
                        )
                    }
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
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }

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
                            etiqueta = "Treino em andamento",
                            titulo = emAndamento.treinoNome,
                            treinoId = emAndamento.treinoId,
                            resumo = emAndamento.treinoId?.let { ui.resumos[it] },
                            ultimaVez = emAndamento.treinoId?.let { id ->
                                ui.ultimas[id]?.let { formatarUltimaVez(it, vm.hoje) }
                            },
                            acao = "Continuar treino",
                            aoClicar = { abrirSessao(emAndamento.id) },
                        )
                    }
                    concluidoHoje != null -> {
                        CartaoDestaque(
                            etiqueta = "Já feito hoje",
                            titulo = concluidoHoje.treinoNome,
                            treinoId = concluidoHoje.treinoId,
                            resumo = concluidoHoje.treinoId?.let { ui.resumos[it] },
                            ultimaVez = "hoje",
                            acao = "Iniciar treino",
                            aoClicar = {
                                val id = concluidoHoje.treinoId
                                if (id != null) {
                                    tentarIniciarTreino(id, concluidoHoje.treinoNome)
                                } else {
                                    abrirSessao(concluidoHoje.id)
                                }
                            },
                        )
                    }
                    agendado != null -> {
                        CartaoDestaque(
                            etiqueta = "Agendado para hoje",
                            titulo = agendado.treinoNome,
                            treinoId = agendado.treinoId,
                            resumo = ui.resumos[agendado.treinoId],
                            ultimaVez = ui.ultimas[agendado.treinoId]?.let {
                                formatarUltimaVez(it, vm.hoje)
                            },
                            acao = "Iniciar treino",
                            aoClicar = {
                                tentarIniciarTreino(agendado.treinoId, agendado.treinoNome)
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

            if (ui.emAndamento == null && outrosTreinos.isNotEmpty()) {
                item {
                    Text(
                        text = if (ui.agendado != null || concluidoHoje != null) {
                            "Outros treinos"
                        } else {
                            "Iniciar um treino"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
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
                            tentarIniciarTreino(treino.id, treino.nome)
                        },
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
            }
        }
}

@Composable
private fun FaixaSemana(
    semana: List<DiaDaSemana>,
    hoje: LocalDate,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            semana.forEach { dia ->
                val ehHoje = dia.data == hoje
                val feito = dia.feito
                val agendado = dia.agendado
                val treinoId = feito?.treinoId ?: agendado?.treinoId
                val treinoNome = feito?.treinoNome ?: agendado?.treinoNome ?: ""
                val letra = dia.data.dayOfWeek
                    .getDisplayName(TextStyle.NARROW, Formatadores.ptBr)
                    .uppercase(Formatadores.ptBr)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = letra,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (ehHoje) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier.size(34.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (treinoId == null || (feito == null && dia.data.isBefore(hoje))) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.outlineVariant),
                            )
                        } else {
                            EmblemaTreino(
                                treinoId = treinoId,
                                nome = treinoNome,
                                tamanho = 32.dp,
                                preenchido = feito != null,
                                esmaecido = feito == null,
                            )
                        }
                        if (feito != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(15.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Concluído",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(11.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 16.dp, height = 3.dp)
                            .clip(CircleShape)
                            .background(
                                if (ehHoje) MaterialTheme.colorScheme.primary else Color.Transparent,
                            ),
                    )
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
    aoClicar: () -> Unit,
) {
    val interacao = remember { MutableInteractionSource() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .escalaPressao(interacao),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                EmblemaTreino(
                    treinoId = treinoId,
                    nome = titulo,
                    tamanho = 64.dp,
                )
                Spacer(Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = etiqueta,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            if (resumo != null || ultimaVez != null) {
                Spacer(Modifier.height(20.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
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
                    .height(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = acao,
                    style = MaterialTheme.typography.labelLarge,
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = if (temTreinos) "Hoje" else "Bem-vindo",
                style = MaterialTheme.typography.labelLarge,
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
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                    style = MaterialTheme.typography.titleMedium,
                )
                val info = buildList {
                    if (resumo != null && resumo.exercicios > 0) {
                        add("${resumo.exercicios} exercícios")
                    }
                    if (ultimaVez != null) {
                        add("feito $ultimaVez")
                    }
                }.joinToString(", ")

                if (info.isNotEmpty()) {
                    Text(
                        text = info,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Iniciar",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
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
        dias < 7L -> "há $dias dias"
        else -> {
            val dia = Formatadores.nomeDiaSemana(data.dayOfWeek.value, TextStyle.SHORT)
                .lowercase(Formatadores.ptBr)
                .trimEnd('.')
            "$dia ${data.format(formatoDiaMes)}"
        }
    }
}
