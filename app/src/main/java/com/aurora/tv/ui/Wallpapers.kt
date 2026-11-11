package com.aurora.tv.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Immutable
class Blob(val cx: Float, val cy: Float, val r: Float, val color: Color)

@Immutable
class WallpaperSpec(val name: String, val base: Color, val blobs: List<Blob>, val luma: Float)

/** Procedural wallpapers: a few radial gradients. No bitmaps, no network, a few KB of memory. */
object Wallpapers {
    val list = listOf(
        WallpaperSpec("Aurora", Color(0xFF050A12), listOf(
            Blob(0.18f, 0.12f, 0.70f, Color(0x802FD6C0)),
            Blob(0.85f, 0.08f, 0.62f, Color(0x806B4BFF)),
            Blob(0.55f, 1.00f, 0.75f, Color(0x4D2A6BFF)),
        ), 0.25f),
        WallpaperSpec("Nebula", Color(0xFF070510), listOf(
            Blob(0.80f, 0.20f, 0.70f, Color(0x80B04BFF)),
            Blob(0.15f, 0.85f, 0.65f, Color(0x66FF4B9A)),
            Blob(0.50f, 0.40f, 0.50f, Color(0x332B3BFF)),
        ), 0.28f),
        WallpaperSpec("Deep Ocean", Color(0xFF03090F), listOf(
            Blob(0.30f, 0.05f, 0.80f, Color(0x80168FC0)),
            Blob(0.90f, 0.90f, 0.70f, Color(0x661E5AFF)),
            Blob(0.10f, 0.95f, 0.50f, Color(0x4D13C9B5)),
        ), 0.22f),
        WallpaperSpec("Ember", Color(0xFF0C0605), listOf(
            Blob(0.85f, 0.10f, 0.70f, Color(0x80FF6A3D)),
            Blob(0.10f, 0.20f, 0.60f, Color(0x4DC23A5C)),
            Blob(0.50f, 1.05f, 0.80f, Color(0x4DFF9F3D)),
        ), 0.30f),
        WallpaperSpec("Midnight", Color(0xFF050608), listOf(
            Blob(0.75f, 0.10f, 0.65f, Color(0x33688BFF)),
            Blob(0.20f, 0.90f, 0.55f, Color(0x268B7CFF)),
        ), 0.10f),
    )
}

@Composable
fun WallpaperLayer(spec: WallpaperSpec, modifier: Modifier = Modifier) {
    Box(
        modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val big = max(w, h)
            val brushes = spec.blobs.map { b ->
                Brush.radialGradient(
                    colors = listOf(b.color, Color.Transparent),
                    center = Offset(b.cx * w, b.cy * h),
                    radius = b.r * big,
                )
            }
            onDrawBehind {
                drawRect(spec.base)
                brushes.forEach { drawRect(it) }
            }
        }
    )
}

/**
 * Full-screen background. Layers: cross-fading wallpaper (with parallax), a tint taken from the
 * featured app's icon, and a bottom fade that keeps the rows readable.
 */
@Composable
fun Wallpaper(index: Int, tint: State<Color>, parallax: () -> Float) {
    val p = LocalPalette.current
    val motion = LocalMotion.current
    val duration = (1000 * motion).toInt()
    val animTint by animateColorAsState(tint.value, tween((duration * 1.4f).toInt()), label = "tint")

    Box(Modifier.fillMaxSize()) {
        Crossfade(
            targetState = index.coerceIn(0, Wallpapers.list.lastIndex),
            animationSpec = tween(duration),
            label = "wallpaper",
            modifier = Modifier.fillMaxSize().graphicsLayer {
                // 8% overscan so the parallax shift never reveals an edge
                scaleX = 1.08f
                scaleY = 1.08f
                translationY = -(parallax() * 0.08f).coerceAtMost(40.dp.toPx())
            },
        ) { i -> WallpaperLayer(Wallpapers.list[i], Modifier.fillMaxSize()) }

        Box(
            Modifier.fillMaxSize().drawBehind {
                if (!p.dark) drawRect(p.bg.copy(alpha = 0.78f))
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(animTint.copy(alpha = 0.30f), Color.Transparent),
                        center = Offset(size.width * 0.78f, size.height * 0.22f),
                        radius = size.width * 0.55f,
                    )
                )
                drawRect(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, p.bg.copy(alpha = 0.85f)),
                        startY = size.height * 0.35f,
                        endY = size.height,
                    )
                )
            }
        )
    }
}
