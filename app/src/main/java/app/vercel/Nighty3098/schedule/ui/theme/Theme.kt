package app.vercel.Nighty3098.schedule.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import app.vercel.Nighty3098.schedule.domain.model.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
)

/**
 * AMOLED-схема: принудительно чёрный фон, игнорируем dynamic colors.
 * Тёмная всегда (AMOLED имеет смысл только в dark).
 */
private val AmoledColorScheme = darkColorScheme(
    primary = AmoledPrimary,
    onPrimary = AmoledOnPrimary,
    primaryContainer = AmoledPrimaryContainer,
    onPrimaryContainer = AmoledOnPrimaryContainer,
    secondary = AmoledSecondary,
    tertiary = AmoledTertiary,
    background = AmoledBackground,
    onBackground = AmoledOnBackground,
    surface = AmoledSurface,
    onSurface = AmoledOnSurface,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = AmoledOnSurfaceVariant,
    surfaceContainer = AmoledSurfaceContainer,
    surfaceContainerLow = AmoledBackground,
    surfaceContainerHigh = AmoledSurfaceContainer,
    surfaceContainerLowest = AmoledBackground,
    surfaceContainerHighest = AmoledHighest,
    surfaceDim = AmoledBackground,
    surfaceBright = AmoledBright,
    outline = Color(0xFF49454F),
    outlineVariant = AmoledOutlineVariant,
    inverseSurface = AmoledInverseSurface,
    inverseOnSurface = AmoledInverseOnSurface,
    inversePrimary = AmoledInversePrimary,
)

/**
 * Монохромная схема: чёрный фон и только оттенки серого.
 * Всегда тёмная, dynamic colors игнорируются.
 */
private val MonochromeColorScheme = darkColorScheme(
    primary = MonoPrimary,
    onPrimary = MonoOnPrimary,
    primaryContainer = MonoPrimaryContainer,
    onPrimaryContainer = MonoOnPrimaryContainer,
    secondary = MonoSecondary,
    tertiary = MonoTertiary,
    background = MonoBackground,
    onBackground = MonoOnBackground,
    surface = MonoSurface,
    onSurface = MonoOnSurface,
    surfaceVariant = MonoSurfaceVariant,
    onSurfaceVariant = MonoOnSurfaceVariant,
    surfaceContainer = MonoSurfaceContainer,
    surfaceContainerLow = MonoBackground,
    surfaceContainerHigh = MonoSurfaceContainerHigh,
    surfaceContainerLowest = MonoBackground,
    surfaceContainerHighest = MonoHighest,
    surfaceDim = MonoBackground,
    surfaceBright = MonoBright,
    outline = MonoOutline,
    outlineVariant = MonoOutlineVariant,
    inverseSurface = MonoInverseSurface,
    inverseOnSurface = MonoInverseOnSurface,
    inversePrimary = MonoInversePrimary,
)

/** Solarized Osaka, тёмный вариант палитры. */
private val SolarizedOsakaColorScheme = darkColorScheme(
    primary = SolPrimary,
    onPrimary = SolOnPrimary,
    primaryContainer = SolPrimaryContainer,
    onPrimaryContainer = SolOnPrimaryContainer,
    secondary = SolSecondary,
    tertiary = SolTertiary,
    background = SolBackground,
    onBackground = SolOnBackground,
    surface = SolSurface,
    onSurface = SolOnSurface,
    surfaceVariant = SolSurfaceVariant,
    onSurfaceVariant = SolOnSurfaceVariant,
    surfaceContainer = SolSurfaceContainer,
    surfaceContainerLow = SolBackground,
    surfaceContainerHigh = SolSurfaceContainerHigh,
    surfaceContainerLowest = SolBackground,
    surfaceContainerHighest = SolHighest,
    surfaceDim = SolBackground,
    surfaceBright = SolOnSurface,
    outline = SolOutline,
    outlineVariant = SolSurfaceContainer,
    inverseSurface = SolOnSurface,
    inverseOnSurface = SolBackground,
    inversePrimary = SolBackground,
)

/** Gruvbox, тёмный вариант палитры. */
private val GruvboxColorScheme = darkColorScheme(
    primary = GrPrimary,
    onPrimary = GrOnPrimary,
    primaryContainer = GrPrimaryContainer,
    onPrimaryContainer = GrOnPrimaryContainer,
    secondary = GrSecondary,
    tertiary = GrTertiary,
    background = GrBackground,
    onBackground = GrOnBackground,
    surface = GrSurface,
    onSurface = GrOnSurface,
    surfaceVariant = GrSurfaceVariant,
    onSurfaceVariant = GrOnSurfaceVariant,
    surfaceContainer = GrSurfaceContainer,
    surfaceContainerLow = GrBackground,
    surfaceContainerHigh = GrSurfaceContainerHigh,
    surfaceContainerLowest = GrBackground,
    surfaceContainerHighest = GrHighest,
    surfaceDim = GrBackground,
    surfaceBright = GrBright,
    outline = GrOutline,
    outlineVariant = GrOutlineVariant,
    inverseSurface = GrOnSurface,
    inverseOnSurface = GrBackground,
    inversePrimary = GrBackground,
)

/** Tokyo Night. */
private val TokyoNightColorScheme = darkColorScheme(
    primary = TnPrimary,
    onPrimary = TnOnPrimary,
    primaryContainer = TnPrimaryContainer,
    onPrimaryContainer = TnOnPrimaryContainer,
    secondary = TnSecondary,
    tertiary = TnTertiary,
    background = TnBackground,
    onBackground = TnOnBackground,
    surface = TnSurface,
    onSurface = TnOnSurface,
    surfaceVariant = TnSurfaceVariant,
    onSurfaceVariant = TnOnSurfaceVariant,
    surfaceContainer = TnSurfaceContainer,
    surfaceContainerLow = TnBackground,
    surfaceContainerHigh = TnSurfaceContainerHigh,
    surfaceContainerLowest = TnBackground,
    surfaceContainerHighest = TnHighest,
    surfaceDim = TnBackground,
    surfaceBright = TnOutline,
    outline = TnOutline,
    outlineVariant = TnSurfaceVariant,
    inverseSurface = TnOnSurface,
    inverseOnSurface = TnBackground,
    inversePrimary = TnBackground,
)

/** Catppuccin Mocha. */
private val CatppuccinColorScheme = darkColorScheme(
    primary = CpPrimary,
    onPrimary = CpOnPrimary,
    primaryContainer = CpPrimaryContainer,
    onPrimaryContainer = CpOnPrimaryContainer,
    secondary = CpSecondary,
    tertiary = CpTertiary,
    background = CpBackground,
    onBackground = CpOnBackground,
    surface = CpSurface,
    onSurface = CpOnSurface,
    surfaceVariant = CpSurfaceVariant,
    onSurfaceVariant = CpOnSurfaceVariant,
    surfaceContainer = CpSurfaceContainer,
    surfaceContainerLow = CpBackground,
    surfaceContainerHigh = CpSurfaceContainerHigh,
    surfaceContainerLowest = CpDim,
    surfaceContainerHighest = CpHighest,
    surfaceDim = CpDim,
    surfaceBright = CpBright,
    outline = CpOutline,
    outlineVariant = CpSurfaceVariant,
    inverseSurface = CpOnSurface,
    inverseOnSurface = CpBackground,
    inversePrimary = CpBackground,
)

/**
 * @param themeMode SYSTEM — Material You (dynamic colors с API 31+),
 * остальные — фиксированные тёмные схемы.
 */
@Composable
fun ScheduleTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color доступен на Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themeMode) {
        ThemeMode.AMOLED -> AmoledColorScheme
        ThemeMode.MONOCHROME -> MonochromeColorScheme
        ThemeMode.SOLARIZED_OSAKA -> SolarizedOsakaColorScheme
        ThemeMode.GRUVBOX -> GruvboxColorScheme
        ThemeMode.TOKYO_NIGHT -> TokyoNightColorScheme
        ThemeMode.CATPPUCCIN -> CatppuccinColorScheme
        ThemeMode.SYSTEM -> when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
