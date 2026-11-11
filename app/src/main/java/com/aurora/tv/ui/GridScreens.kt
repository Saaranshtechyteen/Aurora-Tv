package com.aurora.tv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurora.tv.UiState
import com.aurora.tv.data.AppEntry
import com.aurora.tv.data.IconCache

/** Used by Apps, Games, Movies and TV Shows. */
@Composable
fun AppGridScreen(
    title: String,
    subtitle: String?,
    apps: List<AppEntry>,
    emptyMessage: String,
    cache: IconCache,
    onOpen: (AppEntry) -> Unit,
    onMenu: (AppEntry, MenuContext) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title, subtitle)
        if (apps.isEmpty()) {
            EmptyState(emptyMessage)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(apps, key = { it.packageName }) { app ->
                    AppTile(app, cache, onClick = { onOpen(app) }, onLongClick = { onMenu(app, MenuContext.APPS) })
                }
            }
        }
    }
}

/** Settings > Manage apps: toggle favourite / visibility for every installed app. */
@Composable
fun ManageAppsScreen(
    ui: UiState,
    cache: IconCache,
    onToggleFavourite: (AppEntry) -> Unit,
    onSetHidden: (AppEntry, Boolean) -> Unit,
) {
    val p = LocalPalette.current
    val favourites = ui.settings.favourites.toSet()
    val hidden = ui.settings.hidden
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Manage apps", "Choose favourites and hide apps you never use. Press Back to return.")
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(ui.all, key = { it.packageName }) { app ->
                val isFav = app.packageName in favourites
                val isHidden = app.packageName in hidden
                GlassPanel(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(app, cache, 40.dp)
                        Spacer(Modifier.width(16.dp))
                        Text(
                            app.label, color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                        )
                        Chip(if (isFav) "\u2605 Favourite" else "\u2606 Favourite", active = isFav) { onToggleFavourite(app) }
                        Spacer(Modifier.width(10.dp))
                        Chip(if (isHidden) "Hidden" else "Visible", active = !isHidden) { onSetHidden(app, !isHidden) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(label: String, active: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    TvFocusable(
        onClick = onClick,
        modifier = Modifier.width(140.dp).height(38.dp),
        shape = RoundedCornerShape(19.dp),
        focusedScale = 1.06f,
    ) { _ ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, color = if (active) p.accent else p.muted, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}
