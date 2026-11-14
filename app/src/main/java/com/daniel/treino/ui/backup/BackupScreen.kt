package com.daniel.treino.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.daniel.treino.TreinoApp
import com.daniel.treino.data.backup.EstadoBackup
import com.daniel.treino.ui.components.Formatadores
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Reconstructed from U1/n.java, U1/a..m.java, U1/t..v.java, Z1/C0471u.java (case 2), S1/g.java (cases 1-2)

private val formatoDataHora: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, dd/MM/yyyy 'às' HH:mm", Formatadores.ptBr)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    voltar: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as TreinoApp
    val vm: BackupViewModel = viewModel(
        factory = BackupViewModel.factory(app.backupArquivo),
    )
    val estado by vm.estado.collectAsState()

    val criarDocumento = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            vm.escolherDestino(uri)
        }
    }

    val abrirDocumento = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            vm.escolherArquivoRestaurar(uri)
        }
    }

    val previa = vm.previaRestauracao
    if (previa != null) {
        val r = previa.resumo()
        AlertDialog(
            onDismissRequest = vm::cancelarRestauracao,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Restore,
                    contentDescription = null,
                )
            },
            title = {
                Text("Restaurar este backup?")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Criado em ${formatarDataHora(r.criadoEm.toEpochMilli())}.")
                    Text("${r.treinos} treinos • ${r.sessoesConcluidas} sessões concluídas • ${r.series} séries")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Todos os treinos, agenda e histórico atuais deste aparelho serão substituídos pelos dados do backup.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                Button(onClick = vm::confirmarRestauracao) {
                    Text("Restaurar")
                }
            },
            dismissButton = {
                TextButton(onClick = vm::cancelarRestauracao) {
                    Text("Cancelar")
                }
            },
        )
    }

    val msg = vm.mensagem
    if (msg != null) {
        AlertDialog(
            onDismissRequest = vm::limparMensagem,
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = vm::limparMensagem) {
                    Text("OK")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Backup",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = voltar) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
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
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = "Guarde uma cópia de todos os seus treinos (modelos, agenda e histórico) no Google Drive. Na tela que abrir, escolha Google Drive, a conta e a pasta.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            item {
                CartaoStatusBackup(estado = estado)
            }

            val erro = estado.erro
            if (erro != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = erro,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }

            item {
                val ocupadoOuSalvando = vm.ocupado || estado.salvando
                val temDestinoPermanente = estado.destino != null && estado.destinoPermanente
                Button(
                    onClick = {
                        if (temDestinoPermanente) {
                            vm.fazerBackupAgora()
                        } else {
                            criarDocumento.launch("treino-backup.json")
                        }
                    },
                    enabled = !ocupadoOuSalvando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    if (ocupadoOuSalvando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Salvando...",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.CloudUpload,
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (temDestinoPermanente) {
                                "Fazer backup agora"
                            } else {
                                "Escolher local do backup"
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }

                if (estado.destino != null) {
                    TextButton(
                        onClick = { criarDocumento.launch("treino-backup.json") },
                        enabled = !ocupadoOuSalvando,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Salvar em outro local")
                    }
                }
            }

            if (estado.destino != null && estado.destinoPermanente) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = "Backup automático",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = "Atualiza o arquivo sempre que você conclui um treino ou importa uma planilha.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Switch(
                                checked = estado.automatico,
                                onCheckedChange = vm::definirAutomatico,
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Restaurar",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Traz de volta os treinos de um arquivo de backup, por exemplo num celular novo. Os dados atuais deste celular são substituídos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            item {
                OutlinedButton(
                    onClick = {
                        abrirDocumento.launch(arrayOf("application/json", "text/*", "*/*"))
                    },
                    enabled = !vm.ocupado,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Restore,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Restaurar de um backup",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun CartaoStatusBackup(
    estado: EstadoBackup,
) {
    val configurado = estado.destino != null
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (configurado) {
                    Icons.Outlined.CloudDone
                } else {
                    Icons.Outlined.CloudUpload
                },
                contentDescription = null,
                tint = if (configurado) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = if (configurado) {
                        estado.nomeArquivo ?: "treino-backup.json"
                    } else {
                        "Nenhum arquivo escolhido"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                val subtitulo = when {
                    !configurado -> "Toque em \"Escolher local do backup\" abaixo."
                    estado.ultimoBackup != null -> {
                        val prefixo = estado.local?.let { "$it • " } ?: ""
                        "${prefixo}Último backup: ${formatarDataHora(estado.ultimoBackup)}"
                    }
                    else -> estado.local ?: "Arquivo pronto para receber o backup"
                }
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

private fun formatarDataHora(epochMillis: Long): String {
    val texto = Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(formatoDataHora)
        .replace(".", "")
    return texto.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Formatadores.ptBr) else it.toString()
    }
}
