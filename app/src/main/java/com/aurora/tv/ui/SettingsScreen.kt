package com.aurora.tv.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurora.tv.AuroraViewModel
import com.aurora.tv.UiState

private fun cycle(current: Int, delta: Int, count: Int) = (current + delta + count) % count

@Composable
fun SettingsScreen(
    ui: UiState,
    vm: AuroraViewModel,
    onManageApps: () -> Unit,
    onAbout: () -> Unit,
) {
    val p = LocalPalette.current
    val s = ui.settings
    val wallpapers = Wallpapers.list
    val wIndex = s.wallpaper.coerceIn(0, wallpapers.lastIndex)
    val animNames = listOf("Off", "Reduced", "Full")

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Settings", "Use \u25C0 \u25B6 to change a value, OK to select")
        Row(Modifier.fillMaxSize().padding(horizontal = 48.dp)) {
            LazyColumn(
                modifier = Modifier.weight(1.25f).fillMaxHeight(),
                contentPadding = PaddingValues(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { GroupLabel("Appearance") }
                item {
                    SettingRow(
                        "Wallpaper", wallpapers[wIndex].name,
                        onClick = { vm.setWallpaper(cycle(wIndex, 1, wallpapers.size)) },
                        onLeft = { vm.setWallpaper(cycle(wIndex, -1, wallpapers.size)) },
                        onRight = { vm.setWallpaper(cycle(wIndex, 1, wallpapers.size)) },
                    )
                }
                item {
                    val n = AccentThemes.names.size
                    SettingRow(
                        "Theme", AccentThemes.names[s.accent.coerceIn(0, n - 1)],
                        onClick = { vm.setAccent(cycle(s.accent, 1, n)) },
                        onLeft = { vm.setAccent(cycle(s.accent, -1, n)) },
                        onRight = { vm.setAccent(cycle(s.accent, 1, n)) },
                    )
                }
                item {
                    SettingRow(
                        "Appearance", if (s.dark) "Dark" else "Light",
                        onClick = { vm.setDark(!s.dark) },
                        onLeft = { vm.setDark(!s.dark) },
                        onRight = { vm.setDark(!s.dark) },
                    )
                }
                item {
                    SettingRow(
                        "Glass intensity", "${s.glass + 1} / 5",
                        onClick = { vm.setGlass(cycle(s.glass, 1, 5)) },
                        onLeft = { vm.setGlass((s.glass - 1).coerceAtLeast(0)) },
                        onRight = { vm.setGlass((s.glass + 1).coerceAtMost(4)) },
                    )
                }
                item {
                    SettingRow(
                        "Animation intensity", animNames[s.anim.coerceIn(0, 2)],
                        onClick = { vm.setAnim(cycle(s.anim, 1, 3)) },
                        onLeft = { vm.setAnim(cycle(s.anim, -1, 3)) },
                        onRight = { vm.setAnim(cycle(s.anim, 1, 3)) },
                    )
                }

                item { GroupLabel("Launcher") }
                item {
                    SettingRow(
                        "Clock format", if (s.clock24) "24-hour" else "12-hour",
                        onClick = { vm.setClock24(!s.clock24) },
                        onLeft = { vm.setClock24(!s.clock24) },
                        onRight = { vm.setClock24(!s.clock24) },
                    )
                }
                item {
                    SettingRow(
                        "Show non-TV apps", if (s.showNonTv) "On" else "Off",
                        onClick = { vm.setShowNonTv(!s.showNonTv) },
                        onLeft = { vm.setShowNonTv(!s.showNonTv) },
                        onRight = { vm.setShowNonTv(!s.showNonTv) },
                    )
                }
                item { SettingRow("Clear recently opened", null, onClick = { vm.clearRecents() }) }

                item { GroupLabel("Apps") }
                item { SettingRow("Manage apps and favourites", "${s.favourites.size} favourites \u00B7 ${s.hidden.size} hidden", onClick = onManageApps) }
                item { SettingRow("Android system settings", null, onClick = { vm.openSystemSettings() }) }

                item { GroupLabel("About") }
                item { SettingRow("About Aurora TV", null, onClick = onAbout) }
            }

            Column(Modifier.weight(1f).padding(start = 36.dp, top = 14.dp)) {
                WallpaperPreview(wIndex)
                Spacer(Modifier.height(14.dp))
                Text(wallpapers[wIndex].name, color = p.text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Text("Changes apply instantly and are saved automatically.", color = p.muted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun WallpaperPreview(index: Int) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(24.dp)
    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(shape)) {
        Crossfade(targetState = index, animationSpec = tween(400), label = "preview") { i ->
            WallpaperLayer(Wallpapers.list[i], Modifier.fillMaxSize())
        }
        // Miniature glass card shows the current glass intensity and accent colour
        Box(
            Modifier.align(Alignment.BottomStart).padding(14.dp).width(130.dp).height(46.dp)
                .glass(RoundedCornerShape(14.dp))
        ) {
            Box(
                Modifier.align(Alignment.CenterStart).padding(start = 12.dp).size(20.dp)
                    .clip(CircleShape).drawBehind { drawRect(p.accent) }
            )
        }
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text.uppercase(),
        color = LocalPalette.current.muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(start = 6.dp, top = 10.dp),
    )
}

@Composable
private fun SettingRow(
    title: String,
    value: String?,
    onClick: () -> Unit,
    onLeft: (() -> Unit)? = null,
    onRight: (() -> Unit)? = null,
) {
    val p = LocalPalette.current
    TvFocusable(
        onClick = onClick,
        onLeft = onLeft,
        onRight = onRight,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(20.dp),
        focusedScale = 1.03f,
    ) { _ ->
        Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            if (value != null) {
                val shown = if (onLeft != null) "\u2039  $value  \u203A" else value
                Text(shown, color = p.accent, fontSize = 15.sp, maxLines = 1)
            }
        }
    }
}
