// Android 版作者：南盺
// UI theme and motion layer extracted from MainActivity to keep runtime wiring smaller.
package com.nanxin.hrtrecorder

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.max

val E2Pink = Color(0xFFE77FB5)
val CpaRose = Color(0xFFD9469A)
val TBlue = Color(0xFF4FC3F0)
val MistBlue = Color(0xFFD8F5FF)
val MistPink = Color(0xFFFFE0F0)
val MistLavender = Color(0xFFF1E6FF)

data class AppPalette(
    val ink: Color,
    val muted: Color,
    val card: Color,
    val nav: Color,
    val chartSurface: Color,
    val subtleSurface: Color,
    val divider: Color,
    val backdrop: Brush,
    val reducedMotion: Boolean,
)

val LightAppPalette = AppPalette(
    ink = Color(0xFF101725),
    muted = Color(0xFF737B8A),
    card = Color.White,
    nav = Color.White,
    chartSurface = Color(0xFFFFFDFF),
    subtleSurface = Color(0xFFEFF9FF),
    divider = Color(0xFFC8E7F7),
    backdrop = Brush.linearGradient(
        listOf(
            Color(0xFFCAF2FF),
            Color.White,
            MistPink,
            Color.White,
        ),
        start = Offset.Zero,
        end = Offset(950f, 1550f),
    ),
    reducedMotion = false,
)

val DarkAppPalette = AppPalette(
    ink = Color(0xFFEFF6FF),
    muted = Color(0xFFA9B4C3),
    card = Color(0xFF182030),
    nav = Color(0xFF182232),
    chartSurface = Color(0xFF111827),
    subtleSurface = Color(0xFF20283A),
    divider = Color(0xFF2F3A4E),
    backdrop = Brush.linearGradient(
        listOf(
            Color(0xFF071724),
            Color(0xFF111B2D),
            Color(0xFF241728),
            Color(0xFF0D111A),
        ),
        start = Offset.Zero,
        end = Offset(950f, 1550f),
    ),
    reducedMotion = false,
)

val LocalAppPalette = staticCompositionLocalOf { LightAppPalette }

val Ink: Color
    @Composable get() = LocalAppPalette.current.ink
val Muted: Color
    @Composable get() = LocalAppPalette.current.muted
val CardWhite: Color
    @Composable get() = LocalAppPalette.current.card
val AppGradient: Brush
    @Composable get() = LocalAppPalette.current.backdrop

val AppSmoothEasing = CubicBezierEasing(0.18f, 0.86f, 0.24f, 1f)
val ColorOsEasing = CubicBezierEasing(0.16f, 0.95f, 0.22f, 1f)
const val MICRO_MOTION_MS = 132
const val COLOROS_SLIDE_MS = 218
const val COLOROS_PRESS_MS = 92

fun AppPalette.motionDuration(baseMs: Int): Int =
    if (reducedMotion) max(54, (baseMs * 0.82f).toInt()) else baseMs

fun AppPalette.fadeMotionDuration(baseMs: Int): Int =
    if (reducedMotion) max(62, (baseMs * 0.86f).toInt()) else baseMs

@Composable
fun appColorAsState(targetValue: Color, label: String): Color {
    val palette = LocalAppPalette.current
    val value by animateColorAsState(
        targetValue = targetValue,
        animationSpec = tween(palette.motionDuration(MICRO_MOTION_MS), easing = AppSmoothEasing),
        label = label,
    )
    return value
}

@Composable
fun colorOsPressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.965f,
    label: String,
): Float {
    val palette = LocalAppPalette.current
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !palette.reducedMotion) pressedScale else 1f,
        animationSpec = tween(palette.motionDuration(COLOROS_PRESS_MS), easing = ColorOsEasing),
        label = label,
    )
    return scale
}

@Composable
fun NativeHrtTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val palette = if (darkTheme) DarkAppPalette else LightAppPalette
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = E2Pink,
            secondary = TBlue,
            tertiary = CpaRose,
            primaryContainer = Color(0xFF3A2330),
            secondaryContainer = Color(0xFF173246),
            tertiaryContainer = Color(0xFF342547),
            surface = Color(0xFF111827),
            surfaceVariant = palette.subtleSurface,
            background = Color(0xFF0D111A),
            onSurface = palette.ink,
        )
    } else {
        lightColorScheme(
            primary = E2Pink,
            secondary = TBlue,
            tertiary = CpaRose,
            primaryContainer = MistPink,
            secondaryContainer = MistBlue,
            tertiaryContainer = MistLavender,
            surface = Color(0xFFFFFBFE),
            surfaceVariant = palette.subtleSurface,
            background = Color(0xFFF7FAFF),
            onSurface = palette.ink,
        )
    }
    CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
