package space.aniroom.app.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import space.aniroom.app.R
import space.aniroom.app.data.ThemeMode

@Immutable
data class MangaColors(
    val paper: Color,
    val paper2: Color,
    val paper3: Color,
    val ink: Color,
    val inkSoft: Color,
    val muted: Color,
    val red: Color,
    val redDeep: Color,
    val yellow: Color,
    val onYellow: Color,
    val green: Color,
    val blue: Color,
    val isDark: Boolean,
)

val LightManga = MangaColors(
    paper = Color(0xFFF1ECE1), paper2 = Color(0xFFFAF7F0), paper3 = Color(0xFFE7E0D2),
    ink = Color(0xFF141210), inkSoft = Color(0xFF3A3530), muted = Color(0xFF6E675C),
    red = Color(0xFFE2342A), redDeep = Color(0xFFB8241C), yellow = Color(0xFFFFD23A), onYellow = Color(0xFF141210),
    green = Color(0xFF2E9E5B), blue = Color(0xFF2F6FDF), isDark = false,
)

val SepiaManga = MangaColors(
    paper = Color(0xFFE8D8B8), paper2 = Color(0xFFF3E7CD), paper3 = Color(0xFFDCC8A2),
    ink = Color(0xFF2A1D10), inkSoft = Color(0xFF4A3826), muted = Color(0xFF6B5840),
    red = Color(0xFFC8352A), redDeep = Color(0xFFA12A21), yellow = Color(0xFFF2C230), onYellow = Color(0xFF2A1D10),
    green = Color(0xFF3C8A4F), blue = Color(0xFF3466B8), isDark = false,
)

val DarkManga = MangaColors(
    paper = Color(0xFF0F0E0D), paper2 = Color(0xFF1A1816), paper3 = Color(0xFF26231F),
    ink = Color(0xFFF3EEE4), inkSoft = Color(0xFFCFC8BB), muted = Color(0xFF9A9286),
    red = Color(0xFFFF4A3D), redDeep = Color(0xFFD93024), yellow = Color(0xFFFFD23A), onYellow = Color(0xFF141210),
    green = Color(0xFF4CC27E), blue = Color(0xFF6A9BFF), isDark = true,
)

val LocalManga = staticCompositionLocalOf { LightManga }

val DisplayFont = FontFamily(Font(R.font.rubik_mono_one, FontWeight.Normal))
val BodyFont = FontFamily(
    Font(R.font.plex_regular, FontWeight.Normal),
    Font(R.font.plex_medium, FontWeight.Medium),
    Font(R.font.plex_semibold, FontWeight.SemiBold),
    Font(R.font.plex_bold, FontWeight.Bold),
    Font(R.font.plex_bold, FontWeight.ExtraBold),
    Font(R.font.plex_bold, FontWeight.Black),
)

private val base = Typography()
val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = DisplayFont),
    displayMedium = base.displayMedium.copy(fontFamily = DisplayFont),
    displaySmall = base.displaySmall.copy(fontFamily = DisplayFont, fontSize = 30.sp),
    headlineLarge = base.headlineLarge.copy(fontFamily = DisplayFont, fontSize = 26.sp),
    headlineMedium = base.headlineMedium.copy(fontFamily = DisplayFont, fontSize = 22.sp),
    headlineSmall = base.headlineSmall.copy(fontFamily = DisplayFont, fontSize = 18.sp),
    titleLarge = base.titleLarge.copy(fontFamily = BodyFont, fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontFamily = BodyFont, fontWeight = FontWeight.Bold),
    titleSmall = base.titleSmall.copy(fontFamily = BodyFont, fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(fontFamily = BodyFont),
    bodyMedium = base.bodyMedium.copy(fontFamily = BodyFont),
    bodySmall = base.bodySmall.copy(fontFamily = BodyFont),
    labelLarge = base.labelLarge.copy(fontFamily = BodyFont, fontWeight = FontWeight.Bold),
    labelMedium = base.labelMedium.copy(fontFamily = BodyFont, fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall.copy(fontFamily = BodyFont, fontWeight = FontWeight.SemiBold),
)

val Kicker = TextStyle(fontFamily = DisplayFont, fontSize = 11.sp, letterSpacing = 1.sp)

@Composable
fun resolveManga(mode: ThemeMode): MangaColors = when (mode) {
    ThemeMode.LIGHT -> LightManga
    ThemeMode.SEPIA -> SepiaManga
    ThemeMode.DARK -> DarkManga
    ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) DarkManga else LightManga
}

@Composable
private fun animated(c: Color): Color {
    val value by animateColorAsState(c, tween(450), label = "theme")
    return value
}

@Composable
fun AniRoomTheme(mode: ThemeMode, materialYou: Boolean = false, content: @Composable () -> Unit) {
    val base = resolveManga(mode)
    val context = androidx.compose.ui.platform.LocalContext.current
    val target = if (materialYou && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        val dyn = if (base.isDark) androidx.compose.material3.dynamicDarkColorScheme(context) else androidx.compose.material3.dynamicLightColorScheme(context)
        base.copy(red = dyn.primary, redDeep = dyn.primary, yellow = dyn.tertiaryContainer, onYellow = dyn.onTertiaryContainer)
    } else base
    val m = target.copy(
        paper = animated(target.paper), paper2 = animated(target.paper2), paper3 = animated(target.paper3),
        ink = animated(target.ink), inkSoft = animated(target.inkSoft), muted = animated(target.muted),
        red = animated(target.red),
    )
    val scheme = if (m.isDark) darkColorScheme(
        primary = m.red, onPrimary = Color.White, secondary = m.yellow, onSecondary = m.onYellow,
        background = m.paper, onBackground = m.ink, surface = m.paper2, onSurface = m.ink,
        surfaceVariant = m.paper3, onSurfaceVariant = m.inkSoft, outline = m.ink, error = m.red,
        surfaceContainer = m.paper2, surfaceContainerHigh = m.paper3, surfaceContainerLow = m.paper2,
        surfaceContainerHighest = m.paper3, surfaceContainerLowest = m.paper,
    ) else lightColorScheme(
        primary = m.red, onPrimary = Color.White, secondary = m.yellow, onSecondary = m.onYellow,
        background = m.paper, onBackground = m.ink, surface = m.paper2, onSurface = m.ink,
        surfaceVariant = m.paper3, onSurfaceVariant = m.inkSoft, outline = m.ink, error = m.red,
        surfaceContainer = m.paper2, surfaceContainerHigh = m.paper3, surfaceContainerLow = m.paper2,
        surfaceContainerHighest = m.paper3, surfaceContainerLowest = m.paper,
    )
    CompositionLocalProvider(LocalManga provides m) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}

object Manga {
    val colors: MangaColors
        @Composable get() = LocalManga.current
}

fun parseColor(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return runCatching { Color(android.graphics.Color.parseColor(hex.trim())) }.getOrDefault(fallback)
}
