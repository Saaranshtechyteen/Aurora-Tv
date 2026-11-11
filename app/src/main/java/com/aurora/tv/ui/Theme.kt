package com.aurora.tv.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.aurora.tv.data.AuroraSettings

@Immutable
class AuroraPalette(
    val dark: Boolean,
    val bg: Color,
    val glassTop: Color,
    val glassBottom: Color,
    val border: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val accent2: Color,
    val onAccent: Color,
)

val LocalPalette = staticCompositionLocalOf<AuroraPalette> { error("AuroraPalette not provided") }

/** 0 = animations off, 0.5 = reduced, 1 = full. Multiplies durations / disables springs. */
val LocalMotion = staticCompositionLocalOf { 1f }

object AccentThemes {
    val names = listOf("Icy Blue", "Violet", "Rose", "Emerald")
    val colors = listOf(
        Color(0xFF8FD3FF) to Color(0xFF8B7CFF),
        Color(0xFFB79BFF) to Color(0xFF6FD0FF),
        Color(0xFFFF9AC1) to Color(0xFFFFC38F),
        Color(0xFF7BF0C0) to Color(0xFF7BB7FF),
    )
}

fun buildPalette(s: AuroraSettings): AuroraPalette {
    val wallpaper = Wallpapers.list[s.wallpaper.coerceIn(0, Wallpapers.list.lastIndex)]
    val level = s.glass.coerceIn(0, 4)
    // Adaptive transparency: brighter wallpapers get a slightly denser glass so text stays readable.
    val base = 0.30f + level * 0.10f
    val alpha = (base * (0.88f + 0.5f * wallpaper.luma)).coerceIn(0.2f, 0.9f)
    val (accent, accent2) = AccentThemes.colors[s.accent.coerceIn(0, AccentThemes.colors.lastIndex)]
    val glassTint = if (s.dark) Color(0xFF1B1F27) else Color(0xFFFFFFFF)

    return AuroraPalette(
        dark = s.dark,
        bg = if (s.dark) Color(0xFF050608) else Color(0xFFEEF1F6),
        glassTop = glassTint.copy(alpha = (alpha * 1.1f).coerceAtMost(0.95f)),
        glassBottom = glassTint.copy(alpha = alpha * 0.8f),
        border = if (s.dark) Color.White else Color(0xFF0B0D12),
        text = if (s.dark) Color.White else Color(0xFF0B0D12),
        muted = if (s.dark) Color(0xFFA0A7B4) else Color(0xFF586070),
        accent = accent,
        accent2 = accent2,
        onAccent = Color(0xFF07101C),
    )
}
