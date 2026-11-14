package com.daniel.treino.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedCard
import com.daniel.treino.R
import com.daniel.treino.ui.theme.CorAmbar
import com.daniel.treino.ui.theme.CorAzul
import com.daniel.treino.ui.theme.CorRosa
import com.daniel.treino.ui.theme.CorTerracota
import com.daniel.treino.ui.theme.CorVerde
import java.text.Normalizer
import java.util.Locale

// Reconstructed from S1/x.java, S1/w.java, S1/r.java, S1/l.java, S1/o.java, S1/p.java, S1/q.java, s0/c.java (method g), X1/s.java (case 1), K2/m.java (case 29)

enum class IconeTreino(
    val chave: String,
    @DrawableRes val drawableRes: Int,
    val rotulo: String,
) {
    SUPERIORES("superiores", R.drawable.ic_treino_superiores, "Bíceps"),
    INFERIOR("inferior", R.drawable.ic_treino_inferior, "Avanço"),
    MESA_FLEXORA("mesa_flexora", R.drawable.ic_treino_mesa_flexora, "Mesa flexora"),
    EMPURRAR("empurrar", R.drawable.ic_treino_empurrar, "Supino"),
    PUXAR("puxar", R.drawable.ic_treino_puxar, "Barra fixa"),
    PADRAO("padrao", R.drawable.ic_treino_padrao, "Halter");

    companion object {
        fun porChave(chave: String?): IconeTreino? =
            entries.firstOrNull { it.chave == chave }

        fun sugerir(nome: String): IconeTreino {
            val n = Normalizer.normalize(nome.lowercase(Locale.ROOT), Normalizer.Form.NFD)
                .replace(Regex("\\p{M}+"), "")
            return when {
                contemAlgum(n, "posterior", "flexora", "femoral", "isquio", "stiff") -> MESA_FLEXORA
                contemAlgum(
                    n,
                    "perna",
                    "inferior",
                    "agach",
                    "leg",
                    "quadr",
                    "gluteo",
                    "panturr",
                    "avanco",
                    "afundo",
                    "lower",
                ) -> INFERIOR
                contemAlgum(n, "costa", "puxad", "remad", "barra", "pull", "dorsal", "trapez") -> PUXAR
                contemAlgum(
                    n,
                    "peito",
                    "supino",
                    "empurr",
                    "push",
                    "ombro",
                    "ilitar",
                    "desenvolv",
                    "triceps",
                    "crucif",
                ) -> EMPURRAR
                contemAlgum(n, "biceps", "rosca", "braco", "superior", "upper", "core", "abdom") -> SUPERIORES
                else -> PADRAO
            }
        }

        private fun contemAlgum(texto: String, vararg termos: String): Boolean =
            termos.any { texto.contains(it) }
    }
}

typealias IconesTreino = IconeTreino

val CoresTreino: List<Color> = listOf(
    CorTerracota,
    CorVerde,
    CorAmbar,
    CorAzul,
    CorRosa,
)

val LocalOrdemTreinos = compositionLocalOf<Map<Long, Int>> { emptyMap() }
val LocalIconesTreinos = compositionLocalOf<Map<Long, String?>> { emptyMap() }

@Composable
fun corDoTreino(treinoId: Long?): Color {
    if (treinoId == null) return CorRosa
    val ordem = LocalOrdemTreinos.current[treinoId] ?: (treinoId.hashCode() and Int.MAX_VALUE)
    return CoresTreino[ordem % CoresTreino.size]
}

fun corSobre(cor: Color): Color =
    if (cor.luminance() > 0.45f) Color(0xFF12161A) else Color.White

@Composable
fun EmblemaTreino(
    treinoId: Long? = null,
    nome: String,
    modifier: Modifier = Modifier,
    tamanho: Dp = 44.dp,
    preenchido: Boolean = true,
    esmaecido: Boolean = false,
    id: Long? = treinoId,
    icone: String? = null,
) {
    val idEfetivo = id
    val corBase = corDoTreino(idEfetivo)
    val cor = if (esmaecido) corBase.copy(alpha = 0.45f) else corBase
    val fundo = if (preenchido) cor else cor.copy(alpha = 0.16f)
    val conteudo = if (preenchido) corSobre(cor) else cor
    val chaveSalva = icone ?: if (idEfetivo != null) LocalIconesTreinos.current[idEfetivo] else null
    val iconeResolvido = IconeTreino.porChave(chaveSalva) ?: IconeTreino.sugerir(nome)
    val raio = (tamanho.value * 0.28f).coerceIn(6f, 16f).dp

    Box(
        modifier = modifier
            .size(tamanho)
            .clip(RoundedCornerShape(raio))
            .background(fundo),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconeResolvido.drawableRes),
            contentDescription = null,
            tint = conteudo,
            modifier = Modifier.size((tamanho.value * 0.6f).dp),
        )
    }
}

@Composable
fun SeletorIconeTreino(
    selecionado: IconeTreino,
    cor: Color,
    aoEscolher: (IconeTreino) -> Unit,
) {
    Column {
        Text(
            text = "Ícone",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(IconeTreino.entries) { icone ->
                val ativo = icone == selecionado
                val fundo by animateColorAsState(
                    targetValue = if (ativo) cor else MaterialTheme.colorScheme.surfaceContainerHigh,
                    label = "fundoIcone",
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(68.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(fundo)
                            .clickable { aoEscolher(icone) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(icone.drawableRes),
                            contentDescription = icone.rotulo,
                            tint = if (ativo) corSobre(cor) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    Text(
                        text = icone.rotulo,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (ativo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun Modifier.escalaPressao(interacao: MutableInteractionSource): Modifier {
    val pressionado = remember { mutableStateOf(false) }
    LaunchedEffect(interacao) {
        interacao.interactions.collect { i ->
            when (i) {
                is PressInteraction.Press -> pressionado.value = true
                is PressInteraction.Release, is PressInteraction.Cancel -> pressionado.value = false
            }
        }
    }
    val escala by animateFloatAsState(
        targetValue = if (pressionado.value) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "pressao",
    )
    return this.graphicsLayer {
        scaleX = escala
        scaleY = escala
    }
}

@Composable
fun NumeroAnimado(
    texto: String,
    estilo: TextStyle,
    modifier: Modifier = Modifier,
    cor: Color = Color.Unspecified,
) {
    Row(modifier = modifier) {
        texto.forEachIndexed { indice, caractere ->
            AnimatedContent(
                targetState = caractere,
                transitionSpec = {
                    (slideInVertically(tween(180)) { it / 2 } + fadeIn(tween(180)))
                        .togetherWith(slideOutVertically(tween(180)) { -it / 2 } + fadeOut(tween(120)))
                },
                label = "digito$indice",
            ) { c ->
                Text(
                    text = c.toString(),
                    style = estilo,
                    color = cor,
                )
            }
        }
    }
}

@Composable
fun TextoRolante(
    texto: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified,
) {
    NumeroAnimado(
        texto = texto,
        estilo = if (fontWeight != null) style.copy(fontWeight = fontWeight) else style,
        modifier = modifier,
        cor = color,
    )
}

@Composable
fun EstadoVazio(
    icone: ImageVector,
    titulo: String,
    mensagem: String,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = mensagem,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
