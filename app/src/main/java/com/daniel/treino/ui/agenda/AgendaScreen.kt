package com.daniel.treino.ui.agenda

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.daniel.treino.TreinoApp
import com.daniel.treino.data.db.Treino
import com.daniel.treino.ui.components.EmblemaTreino
import com.daniel.treino.ui.components.Formatadores
import java.time.LocalDate
import java.time.format.TextStyle

// Reconstructed from s0/c.java (methods a and e), T1/a..m.java, T1/r..t.java

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen() {
    val app = LocalContext.current.applicationContext as TreinoApp
    val vm: AgendaViewModel = viewModel(
        factory = AgendaViewModel.factory(app.treinoRepository),
    )
    val ui by vm.ui.collectAsState()
    val hoje = remember { LocalDate.now().dayOfWeek.value }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Agenda",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            text = "Treino fixo de cada dia da semana",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (ui.treinos.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Crie seus treinos na aba Treinos primeiro, depois atribua cada um aos dias da semana.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            items((1..7).toList()) { dia ->
                val nomeDia = remember(dia) {
                    Formatadores.nomeDiaSemana(dia, TextStyle.FULL)
                }
                LinhaDiaAgenda(
                    diaNome = nomeDia,
                    ehHoje = dia == hoje,
                    treinoAtual = ui.semana[dia],
                    treinos = ui.treinos,
                    aoEscolher = { id -> vm.definirDia(dia, id) },
                )
            }

            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun LinhaDiaAgenda(
    diaNome: String,
    ehHoje: Boolean,
    treinoAtual: Treino?,
    treinos: List<Treino>,
    aoEscolher: (Long?) -> Unit,
) {
    var aberto by remember { mutableStateOf(false) }

    Surface(
        onClick = {
            if (treinos.isNotEmpty()) {
                aberto = true
            }
        },
        shape = MaterialTheme.shapes.medium,
        color = if (ehHoje) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = if (ehHoje) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (treinoAtual != null) {
                    EmblemaTreino(
                        treinoId = treinoAtual.id,
                        nome = treinoAtual.nome,
                        modifier = Modifier.padding(end = 12.dp),
                        tamanho = 36.dp,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = diaNome,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (ehHoje) {
                            Text(
                                text = "HOJE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                    Text(
                        text = treinoAtual?.nome ?: "Descanso",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (treinoAtual != null) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Escolher treino",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            DropdownMenu(
                expanded = aberto,
                onDismissRequest = { aberto = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Descanso") },
                    onClick = {
                        aoEscolher(null)
                        aberto = false
                    },
                    trailingIcon = if (treinoAtual == null) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
                treinos.forEach { treino ->
                    DropdownMenuItem(
                        text = { Text(treino.nome) },
                        onClick = {
                            aoEscolher(treino.id)
                            aberto = false
                        },
                        trailingIcon = if (treinoAtual?.id == treino.id) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}
