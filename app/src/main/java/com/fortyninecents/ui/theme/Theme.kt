package com.fortyninecents.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Paleta inspirada no protótipo: verde "dinheiro", grafite da carteira e vermelho/verde para valores.
val BrandGreen = Color(0xFF8CC63F)
val BrandGreenDark = Color(0xFF5C8F1F)
val Charcoal = Color(0xFF454545)
val Coin = Color(0xFFF5A623)

/** Cores semânticas que o Material não cobre (despesa x receita). */
@Immutable
data class MoneyColors(val expense: Color, val income: Color, val neutral: Color)

private val LightMoney = MoneyColors(expense = Color(0xFFE53935), income = Color(0xFF4C9A0E), neutral = Color(0xFF9E9E9E))
private val DarkMoney = MoneyColors(expense = Color(0xFFFF6B66), income = Color(0xFF9BD45A), neutral = Color(0xFF8A8A8A))

val LocalMoneyColors = staticCompositionLocalOf { LightMoney }

private val LightColors = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2CF),
    onPrimaryContainer = Color(0xFF233A06),
    secondary = Charcoal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E8E4),
    onSecondaryContainer = Charcoal,
    tertiary = Coin,
    background = Color(0xFFFAFAF7),
    onBackground = Color(0xFF2B2B2B),
    surface = Color(0xFFFAFAF7),
    onSurface = Color(0xFF2B2B2B),
    surfaceVariant = Color(0xFFEDEDE9),
    onSurfaceVariant = Color(0xFF6B6B66),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF5F5F1),
    surfaceContainer = Color(0xFFF0F0EC),
    surfaceContainerHigh = Color(0xFFEAEAE6),
    outline = Color(0xFFBDBDB8),
    outlineVariant = Color(0xFFDADAD5),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FD35A),
    onPrimary = Color(0xFF1B2E04),
    primaryContainer = Color(0xFF34500F),
    onPrimaryContainer = Color(0xFFD7EFB8),
    secondary = Color(0xFFC9C9C4),
    onSecondary = Color(0xFF2B2B2B),
    secondaryContainer = Color(0xFF3A3A38),
    onSecondaryContainer = Color(0xFFE6E6E1),
    tertiary = Coin,
    background = Color(0xFF121411),
    onBackground = Color(0xFFE4E4DF),
    surface = Color(0xFF121411),
    onSurface = Color(0xFFE4E4DF),
    surfaceVariant = Color(0xFF2A2C28),
    onSurfaceVariant = Color(0xFFB4B6AE),
    surfaceContainerLowest = Color(0xFF0D0F0C),
    surfaceContainerLow = Color(0xFF1A1C18),
    surfaceContainer = Color(0xFF1E201C),
    surfaceContainerHigh = Color(0xFF282A26),
    outline = Color(0xFF6E706A),
    outlineVariant = Color(0xFF3F413C),
)

private val AppTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 0.5.sp),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        labelSmall = labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
    )
}

/** Estilo do valor grande da tela principal. */
val AmountTextStyle = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
)

@Composable
fun FortyNineCentsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalMoneyColors provides if (darkTheme) DarkMoney else LightMoney,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

object AppTheme {
    val money: MoneyColors
        @Composable get() = LocalMoneyColors.current
}
