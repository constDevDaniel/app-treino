package com.daniel.treino.ui.agenda

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun AgendaScreen() {
    val app = LocalContext.current.applicationContext as TreinoApp
    val vm: AgendaViewModel = viewModel(
        factory = AgendaViewModel.factory(app.treinoRepository),
    )
    val ui by vm.ui.collectAsState()
    val hoje = remember { LocalDate.now().dayOfWeek.value }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column {
                    Text(
                        text = "Agenda",
                        style = MaterialTheme.typography.displayMedium,
                    )
                    Text(
                        text = "Escolha o treino de cada dia. No dia, é só tocar em Iniciar na aba Hoje.",
                        modifier = Modifier.padding(bottom = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(
                items = (1..7).toList(),
                key = { it },
            ) { diaSemana ->
                LinhaDia(
                    diaSemana = diaSemana,
                    ehHoje = diaSemana == hoje,
                    treinoAtual = ui.semana[diaSemana],
                    todosTreinos = ui.treinos,
                    aoEscolher = { treino ->
                        vm.definirDia(diaSemana, treino?.id)
                    },
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun LinhaDia(
    diaSemana: Int,
    ehHoje: Boolean,
    treinoAtual: Treino?,
    todosTreinos: List<Treino>,
    aoEscolher: (Treino?) -> Unit,
) {
    var menuAberto by remember { mutableStateOf(false) }
    val nomeDia = remember(diaSemana) {
        Formatadores.nomeDiaSemana(diaSemana, TextStyle.FULL)
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = { menuAberto = true },
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = if (ehHoje) {
                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (treinoAtual != null) {
                    EmblemaTreino(
                        treinoId = treinoAtual.id,
                        nome = treinoAtual.nome,
                        tamanho = 40.dp,
                    )
                } else {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outlineVariant),
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = if (ehHoje) "$nomeDia · hoje" else nomeDia,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = treinoAtual?.nome ?: "Descanso",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (treinoAtual == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Selecionar treino",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        DropdownMenu(
            expanded = menuAberto,
            onDismissRequest = { menuAberto = false },
        ) {
            todosTreinos.forEach { treino ->
                DropdownMenuItem(
                    text = { Text(treino.nome) },
                    leadingIcon = {
                        EmblemaTreino(
                            treinoId = treino.id,
                            nome = treino.nome,
                            tamanho = 28.dp,
                        )
                    },
                    trailingIcon = {
                        if (treinoAtual?.id == treino.id) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    },
                    onClick = {
                        menuAberto = false
                        aoEscolher(treino)
                    },
                )
            }
            if (todosTreinos.isNotEmpty()) {
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = { Text("Descanso (sem treino)") },
                trailingIcon = {
                    if (treinoAtual == null) {
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                },
                onClick = {
                    menuAberto = false
                    aoEscolher(null)
                },
            )
        }
    }
}
