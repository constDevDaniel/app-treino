package com.daniel.treino.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daniel.treino.R

// Reconstructed from Y1/b.java

private val EsquemaEscuro = darkColorScheme(
    primary = CorTerracota,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF3A2018),
    onPrimaryContainer = Color(0xFFFFB59E),
    secondary = CorAmbar,
    onSecondary = Color(0xFF231A00),
    secondaryContainer = Color(0xFF332610),
    onSecondaryContainer = Color(0xFFFFDEA3),
    tertiary = CorVerde,
    onTertiary = Color(0xFF002110),
    tertiaryContainer = Color(0xFF143022),
    onTertiaryContainer = Color(0xFFA6F2C2),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF4E1A16),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF12161A),
    onBackground = Color(0xFFF2EDE4),
    surface = Color(0xFF181D22),
    onSurface = Color(0xFFF2EDE4),
    surfaceVariant = Color(0xFF262D34),
    onSurfaceVariant = Color(0xFF9EA6AE),
    outline = Color(0xFF3A424B),
    outlineVariant = Color(0xFF262D34),
    surfaceContainerLowest = Color(0xFF0E1216),
    surfaceContainerLow = Color(0xFF161B20),
    surfaceContainer = Color(0xFF1B2127),
    surfaceContainerHigh = Color(0xFF20272E),
    surfaceContainerHighest = Color(0xFF262D34),
)

private val EsquemaClaro = lightColorScheme(
    primary = Color(0xFFC8431E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF380D00),
    secondary = Color(0xFF855400),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDDB5),
    onSecondaryContainer = Color(0xFF2A1800),
    tertiary = Color(0xFF1A7F49),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFA6F2C2),
    onTertiaryContainer = Color(0xFF002110),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF6F2EA),
    onBackground = Color(0xFF171A1E),
    surface = Color(0xFFFDF9F2),
    onSurface = Color(0xFF171A1E),
    surfaceVariant = Color(0xFFE7E1D6),
    onSurfaceVariant = Color(0xFF525860),
    outline = Color(0xFFD0C9BD),
    outlineVariant = Color(0xFFE2DDD2),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F4EC),
    surfaceContainer = Color(0xFFF0ECE2),
    surfaceContainerHigh = Color(0xFFE9E4DA),
    surfaceContainerHighest = Color(0xFFE2DDD2),
)

private val FonteDisplay = FontFamily(
    Font(R.font.barlowcondensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlowcondensed_bold, FontWeight.Bold),
)

private val FonteCorpo = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
)

private fun estiloDisplay(
    tamanhoSp: Int,
    alturaLinhaSp: Int,
    peso: FontWeight = FontWeight.Bold,
): TextStyle = TextStyle(
    fontFamily = FonteDisplay,
    fontWeight = peso,
    fontSize = tamanhoSp.sp,
    lineHeight = alturaLinhaSp.sp,
    letterSpacing = (-0.2).sp,
)

private fun estiloCorpo(
    tamanhoSp: Int,
    alturaLinhaSp: Int,
    peso: FontWeight = FontWeight.Normal,
    espacamentoSp: Double = 0.0,
): TextStyle = TextStyle(
    fontFamily = FonteCorpo,
    fontWeight = peso,
    fontSize = tamanhoSp.sp,
    lineHeight = alturaLinhaSp.sp,
    letterSpacing = espacamentoSp.sp,
)

val TipografiaTreino = Typography(
    displayLarge = estiloDisplay(56, 58, FontWeight.Bold),
    displayMedium = estiloDisplay(44, 46, FontWeight.Bold),
    displaySmall = estiloDisplay(36, 38, FontWeight.Bold),
    headlineLarge = estiloDisplay(32, 36, FontWeight.Bold),
    headlineMedium = estiloDisplay(26, 30, FontWeight.Bold),
    headlineSmall = estiloDisplay(22, 26, FontWeight.SemiBold),
    titleLarge = estiloDisplay(21, 25, FontWeight.SemiBold),
    titleMedium = estiloCorpo(16, 22, FontWeight.SemiBold),
    titleSmall = estiloCorpo(14, 20, FontWeight.SemiBold),
    bodyLarge = estiloCorpo(16, 22, FontWeight.Normal),
    bodyMedium = estiloCorpo(14, 20, FontWeight.Normal),
    bodySmall = estiloCorpo(12, 16, FontWeight.Normal),
    labelLarge = estiloCorpo(14, 18, FontWeight.SemiBold, 0.2),
    labelMedium = estiloCorpo(12, 16, FontWeight.SemiBold, 0.4),
    labelSmall = estiloCorpo(11, 14, FontWeight.SemiBold, 0.5),
)

val FormasTreino = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun TreinoTheme(
    temaEscuro: Boolean = isSystemInDarkTheme(),
    conteudo: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (temaEscuro) EsquemaEscuro else EsquemaClaro,
        typography = TipografiaTreino,
        shapes = FormasTreino,
        content = conteudo,
    )
}
