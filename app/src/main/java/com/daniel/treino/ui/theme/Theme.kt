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
    primary = CorAmbar,
    onPrimary = Color(0xFF1D1700),
    primaryContainer = Color(0xFF3A3110),
    onPrimaryContainer = Color(0xFFFFE89A),
    secondary = Color(0xFF9A9DA3),
    onSecondary = Color(0xFF17181A),
    secondaryContainer = Color(0xFF2C2E32),
    onSecondaryContainer = Color(0xFFEDECE8),
    tertiary = CorTerracota,
    onTertiary = Color.White,
    background = Color(0xFF17181A),
    onBackground = Color(0xFFEDECE8),
    surface = Color(0xFF17181A),
    onSurface = Color(0xFFEDECE8),
    surfaceVariant = Color(0xFF2C2E32),
    onSurfaceVariant = Color(0xFF9A9DA3),
    surfaceTint = CorAmbar,
    error = Color(0xFFFF6B5E),
    outline = Color(0xFF4A4D52),
    outlineVariant = Color(0xFF33363A),
    surfaceContainer = Color(0xFF222326),
    surfaceContainerHigh = Color(0xFF27292C),
    surfaceContainerHighest = Color(0xFF2C2E32),
    surfaceContainerLow = Color(0xFF1C1D20),
    surfaceContainerLowest = Color(0xFF111214),
)

private val EsquemaClaro = lightColorScheme(
    primary = Color(0xFF8A6A00),
    onPrimary = Color.White,
    primaryContainer = CorAmbar,
    onPrimaryContainer = Color(0xFF1D1700),
    secondary = Color(0xFF5B5F66),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCDFE3),
    onSecondaryContainer = Color(0xFF17181A),
    tertiary = CorTerracota,
    onTertiary = Color.White,
    background = Color(0xFFECEEF0),
    onBackground = Color(0xFF17181A),
    surface = Color(0xFFECEEF0),
    onSurface = Color(0xFF17181A),
    surfaceVariant = Color(0xFFDCDFE3),
    onSurfaceVariant = Color(0xFF5B5F66),
    outline = Color(0xFF9A9DA3),
    outlineVariant = Color(0xFFC9CCD1),
    surfaceContainer = Color(0xFFF2F3F5),
    surfaceContainerHigh = Color(0xFFE6E8EB),
    surfaceContainerHighest = Color(0xFFDCDFE3),
    surfaceContainerLow = Color(0xFFF6F7F8),
    surfaceContainerLowest = Color.White,
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
)

private fun estiloCorpo(
    tamanhoSp: Int,
    alturaLinhaSp: Int,
    peso: FontWeight = FontWeight.Normal,
): TextStyle = TextStyle(
    fontFamily = FonteCorpo,
    fontWeight = peso,
    fontSize = tamanhoSp.sp,
    lineHeight = alturaLinhaSp.sp,
    letterSpacing = 0.0.sp,
)

val TipografiaTreino = Typography(
    displayLarge = estiloDisplay(72, 72, FontWeight.Bold),
    displayMedium = estiloDisplay(56, 58, FontWeight.Bold),
    displaySmall = estiloDisplay(44, 48, FontWeight.Bold),
    headlineLarge = estiloDisplay(36, 40, FontWeight.Bold),
    headlineMedium = estiloDisplay(30, 34, FontWeight.Bold),
    headlineSmall = estiloDisplay(26, 30, FontWeight.SemiBold),
    titleLarge = estiloDisplay(24, 28, FontWeight.SemiBold),
    titleMedium = estiloCorpo(17, 24, FontWeight.SemiBold),
    titleSmall = estiloCorpo(15, 20, FontWeight.SemiBold),
    bodyLarge = estiloCorpo(16, 24, FontWeight.Normal),
    bodyMedium = estiloCorpo(14, 20, FontWeight.Normal),
    bodySmall = estiloCorpo(13, 18, FontWeight.Normal),
    labelLarge = estiloCorpo(15, 20, FontWeight.SemiBold),
    labelMedium = estiloCorpo(13, 16, FontWeight.Medium),
    labelSmall = estiloCorpo(12, 16, FontWeight.Medium),
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
