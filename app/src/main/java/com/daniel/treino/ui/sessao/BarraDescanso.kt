package com.daniel.treino.ui.sessao

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.daniel.treino.ui.components.NumeroAnimado
import com.daniel.treino.ui.components.PtBr
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val TemposPadrao = listOf(
    120 to "2:00",
    180 to "3:00"
)

@Composable
fun BarraDescanso(
    inicio: Long?,
    fim: Long?,
    realcar: Boolean,
    aoIniciar: (Int) -> Unit,
    aoAdicionar: (Int) -> Unit,
    aoPular: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val permissaoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    val iniciarComPermissao: (Int) -> Unit = { segundos ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissaoLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        aoIniciar(segundos)
    }

    var restanteMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(fim) {
        if (fim == null) {
            restanteMs = 0L
            return@LaunchedEffect
        }
        while (true) {
            val falta = fim - System.currentTimeMillis()
            if (falta <= 0L) {
                restanteMs = 0L
                break
            }
            restanteMs = falta
            delay(200L)
        }
    }

    val ativo = fim != null && restanteMs > 0L

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (ativo) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    ) {
        AnimatedContent(targetState = ativo, label = "descanso") { emContagem ->
            if (emContagem && fim != null) {
                ContagemDescanso(
                    inicio = inicio,
                    fim = fim,
                    aoAdicionar = aoAdicionar,
                    aoPular = aoPular
                )
            } else {
                SeletorDescanso(
                    realcar = realcar,
                    aoIniciar = iniciarComPermissao
                )
            }
        }
    }
}

@Composable
private fun SeletorDescanso(
    realcar: Boolean,
    aoIniciar: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TemposPadrao.forEach { (segundos, rotulo) ->
            Surface(
                onClick = { aoIniciar(segundos) },
                shape = CircleShape,
                color = if (realcar) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (realcar) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = rotulo,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (realcar) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContagemDescanso(
    inicio: Long?,
    fim: Long,
    aoAdicionar: (Int) -> Unit,
    aoPular: () -> Unit
) {
    var progresso by remember(inicio, fim) { mutableFloatStateOf(1f) }
    var segundosRestantes by remember(fim) {
        mutableLongStateOf(((fim - System.currentTimeMillis() + 999) / 1000).coerceAtLeast(0L))
    }

    LaunchedEffect(inicio, fim) {
        val inicioEfetivo = inicio ?: System.currentTimeMillis()
        val duracaoTotal = (fim - inicioEfetivo).coerceAtLeast(1L).toFloat()
        while (isActive) {
            withFrameNanos {
                val falta = (fim - System.currentTimeMillis()).coerceAtLeast(0L)
                progresso = (falta / duracaoTotal).coerceIn(0f, 1f)
                segundosRestantes = (falta + 999) / 1000
            }
            if (segundosRestantes <= 0L) break
        }
    }

    val minutos = segundosRestantes / 60
    val segundos = segundosRestantes % 60
    val textoTempo = String.format(PtBr, "%d:%02d", minutos, segundos)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { progresso },
                modifier = Modifier.size(56.dp),
                strokeWidth = 5.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
            Icon(
                imageVector = Icons.Outlined.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        NumeroAnimado(
            texto = textoTempo,
            estilo = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.weight(1f),
            cor = MaterialTheme.colorScheme.onSurface
        )
        OutlinedButton(
            onClick = { aoAdicionar(30) },
            shape = CircleShape
        ) {
            Text("+30s")
        }
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = aoPular) {
            Text("Pular")
        }
    }
}
