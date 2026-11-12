package com.aurora.tv.ui

import android.view.KeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass surface, drawn in a single draw pass (no RenderEffect / real-time blur):
 *  1. soft accent glow beneath a focused element
 *  2. translucent charcoal body (vertical gradient)
 *  3. top specular reflection
 *  4. thin low-opacity highlight border, plus an accent focus ring while focused
 * [highlight] is read in the draw phase only, so animating focus never recomposes.
 */
@Composable
fun Modifier.glass(
    shape: Shape,
    glow: Boolean = false,
    highlight: () -> Float = { 0f },
): Modifier {
    val p = LocalPalette.current
    return drawBehind {
        val h = highlight()
        val outline = shape.createOutline(size, layoutDirection, this)

        if (glow && h > 0.01f) {
            val c = Offset(size.width / 2f, size.height)
            val r = size.width * 0.75f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(p.accent.copy(alpha = 0.35f * h), Color.Transparent),
                    center = c,
                    radius = r,
                ),
                radius = r,
                center = c,
            )
        }
        drawOutline(outline, Brush.verticalGradient(listOf(p.glassTop, p.glassBottom)))
        drawOutline(
            outline,
            Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.10f + 0.12f * h), Color.Transparent),
                endY = size.height * 0.55f,
            ),
        )
        drawOutline(
            outline,
            Brush.linearGradient(
                listOf(
                    p.border.copy(alpha = 0.34f),
                    p.border.copy(alpha = 0.06f),
                    p.border.copy(alpha = 0.16f),
                )
            ),
            style = Stroke(width = 1.5.dp.toPx()),
        )
        if (h > 0.01f) {
            drawOutline(outline, SolidColor(p.accent.copy(alpha = 0.9f * h)), style = Stroke(width = 2.dp.toPx()))
        }
    }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.glass(shape), content = content)
}

private class KeyState {
    var down = false
    var longFired = false
}

/**
 * The single focus primitive for the whole launcher: D-pad focusable, spring-scales when focused,
 * OK/Enter = click, holding OK/Enter = long-click (context menu), optional LEFT/RIGHT handlers.
 * Never needs touch input.
 */
@Composable
fun TvFocusable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    focusedScale: Float = 1.08f,
    drawGlass: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onLeft: (() -> Unit)? = null,
    onRight: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    content: @Composable BoxScope.(progress: () -> Float) -> Unit,
) {
    val motion = LocalMotion.current
    val p = LocalPalette.current
    var focused by remember { mutableStateOf(false) }
    val progress: State<Float> = animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = when {
            motion == 0f -> snap()
            motion < 1f -> spring(dampingRatio = 1f, stiffness = 800f)
            else -> spring(dampingRatio = 0.65f, stiffness = 320f)
        },
        label = "focus",
    )
    val keys = remember { KeyState() }

    var m = modifier.graphicsLayer {
        val s = 1f + (focusedScale - 1f) * progress.value
        scaleX = s
        scaleY = s
    }
    m = if (drawGlass) {
        m.glass(shape, glow = true) { progress.value }
    } else {
        m.drawBehind {
            val h = progress.value
            if (h > 0.01f) {
                drawOutline(
                    shape.createOutline(size, layoutDirection, this),
                    SolidColor(p.text.copy(alpha = 0.16f * h)),
                )
            }
        }
    }
    if (focusRequester != null) m = m.focusRequester(focusRequester)

    Box(
        modifier = m
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { ev ->
                when (ev.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> when (ev.type) {
                        KeyEventType.KeyDown -> {
                            val repeat = ev.nativeKeyEvent.repeatCount
                            if (repeat == 0) {
                                keys.down = true
                                keys.longFired = false
                            } else if (keys.down && !keys.longFired && onLongClick != null) {
                                keys.longFired = true
                                onLongClick()
                            }
                            true
                        }
                        KeyEventType.KeyUp -> {
                            // Ignore a stray KeyUp that belongs to a press that started in another window
                            // (e.g. the key-up arriving after a long-press opened the context menu).
                            val wasDown = keys.down
                            val long = keys.longFired
                            keys.down = false
                            keys.longFired = false
                            if (wasDown && !long) onClick()
                            wasDown
                        }
                        else -> false
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT ->
                        if (onLeft != null && ev.type == KeyEventType.KeyDown) { onLeft(); true } else false
                    KeyEvent.KEYCODE_DPAD_RIGHT ->
                        if (onRight != null && ev.type == KeyEventType.KeyDown) { onRight(); true } else false
                    else -> false
                }
            }
            .focusable(),
    ) {
        content { progress.value }
    }
}
