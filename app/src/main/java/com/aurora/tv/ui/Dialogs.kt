package com.aurora.tv.ui

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aurora.tv.data.AppEntry
import com.aurora.tv.data.IconCache

class MenuRequest(val app: AppEntry, val context: MenuContext)

@Composable
private fun ScrimDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
private fun MenuItem(label: String, focusRequester: FocusRequester? = null, destructive: Boolean = false, onClick: () -> Unit) {
    val p = LocalPalette.current
    TvFocusable(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(46.dp),
        shape = RoundedCornerShape(16.dp),
        focusedScale = 1.03f,
        drawGlass = false,
        focusRequester = focusRequester,
    ) { _ ->
        Box(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                label,
                color = if (destructive) Color(0xFFFF6B6B) else p.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/** Long-press (hold OK) context menu. Every entry performs a real action. */
@Composable
fun AppMenuDialog(
    request: MenuRequest,
    isFavourite: Boolean,
    cache: IconCache,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onToggleFavourite: () -> Unit,
    onMove: (Int) -> Unit,
    onHide: () -> Unit,
    onInfo: () -> Unit,
    onUninstall: () -> Unit,
) {
    val p = LocalPalette.current
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { first.requestFocus() }
    }
    val inFavourites = request.context == MenuContext.FAVOURITES

    ScrimDialog(onDismiss) {
        GlassPanel(Modifier.width(360.dp), RoundedCornerShape(28.dp)) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(request.app, cache, 44.dp)
                    Spacer(Modifier.width(14.dp))
                    Text(
                        request.app.label, color = p.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(14.dp))
                MenuItem("Open", first) { onDismiss(); onOpen() }
                MenuItem(if (isFavourite) "Remove from favourites" else "Add to favourites") { onDismiss(); onToggleFavourite() }
                if (!inFavourites || isFavourite) {
                    MenuItem(if (inFavourites) "Move earlier in favourites" else "Move earlier") { onDismiss(); onMove(-1) }
                    MenuItem(if (inFavourites) "Move later in favourites" else "Move later") { onDismiss(); onMove(1) }
                }
                MenuItem("Hide from launcher") { onDismiss(); onHide() }
                MenuItem("App info") { onDismiss(); onInfo() }
                MenuItem("Uninstall", destructive = true) { onDismiss(); onUninstall() }
            }
        }
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val version = remember {
        try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "1.0" }
        catch (e: PackageManager.NameNotFoundException) { "1.0" }
    }
    val ok = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { ok.requestFocus() }
    }
    ScrimDialog(onDismiss) {
        GlassPanel(Modifier.width(420.dp), RoundedCornerShape(28.dp)) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Aurora TV", color = p.text, fontSize = 30.sp, fontWeight = FontWeight.Light)
                Spacer(Modifier.height(6.dp))
                Text("Experience Television, Reimagined.", color = p.accent, fontSize = 15.sp)
                Spacer(Modifier.height(14.dp))
                Text("Version $version", color = p.muted, fontSize = 13.sp)
                Text(
                    "A fast, offline-first launcher with Liquid Glass styling. No accounts, no tracking, no network requests.",
                    color = p.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp, bottom = 18.dp),
                )
                Box(Modifier.fillMaxWidth()) { MenuItem("Close", ok) { onDismiss() } }
            }
        }
    }
}
