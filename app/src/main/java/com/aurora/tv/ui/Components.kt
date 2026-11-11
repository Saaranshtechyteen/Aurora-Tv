package com.aurora.tv.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurora.tv.Screen
import com.aurora.tv.data.AppEntry
import com.aurora.tv.data.IconCache
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MenuContext { APPS, FAVOURITES }

// ------------------------------------------------------------------------------------------------
// Icons and cards
// ------------------------------------------------------------------------------------------------

@Composable
fun AppIcon(app: AppEntry, cache: IconCache, size: Dp, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val art by produceState(initialValue = cache.peekIcon(app), key1 = app.packageName) {
        if (value == null) value = cache.icon(app)
    }
    val loaded = art
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (loaded != null) {
            Image(bitmap = loaded.bitmap, contentDescription = app.label, modifier = Modifier.fillMaxSize())
        } else {
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(size * 0.22f)).background(p.text.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(app.label.take(1).uppercase(), color = p.muted, fontSize = (size.value * 0.4f).sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun AppTile(
    app: AppEntry,
    cache: IconCache,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    TvFocusable(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.size(width = 132.dp, height = 124.dp),
        shape = RoundedCornerShape(22.dp),
        focusedScale = 1.09f,
    ) { progress ->
        Column(
            Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AppIcon(
                app, cache, 64.dp,
                Modifier.graphicsLayer {
                    // icon floats slightly above the glass while focused (cheap parallax)
                    val s = 1f + 0.10f * progress()
                    scaleX = s
                    scaleY = s
                    translationY = -4.dp.toPx() * progress()
                },
            )
            Spacer(Modifier.size(8.dp))
            Text(
                app.label,
                color = p.text,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun WideCard(app: AppEntry, cache: IconCache, onClick: () -> Unit, onLongClick: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(22.dp)
    val banner by produceState<ImageBitmap?>(initialValue = cache.peekBanner(app), key1 = app.packageName) {
        if (value == null) value = cache.banner(app)
    }
    val art by produceState(initialValue = cache.peekIcon(app), key1 = app.packageName) {
        if (value == null) value = cache.icon(app)
    }
    TvFocusable(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = Modifier.size(width = 224.dp, height = 126.dp),
        shape = shape,
        focusedScale = 1.07f,
    ) { _ ->
        Box(Modifier.fillMaxSize().clip(shape)) {
            val b = banner
            if (b != null) {
                Image(bitmap = b, contentDescription = app.label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.linearGradient(listOf((art?.avg ?: p.accent).copy(alpha = 0.55f), Color.Transparent))
                    )
                )
                AppIcon(app, cache, 56.dp, Modifier.align(Alignment.Center))
            }
            Box(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().height(46.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xB3000000))))
            )
            Text(
                app.label,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
            )
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Rows
// ------------------------------------------------------------------------------------------------

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        color = LocalPalette.current.text,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 48.dp, top = 18.dp),
    )
}

private val RowPadding = PaddingValues(horizontal = 48.dp, vertical = 14.dp)

@Composable
fun AppRow(
    title: String,
    apps: List<AppEntry>,
    cache: IconCache,
    menuContext: MenuContext,
    onOpen: (AppEntry) -> Unit,
    onMenu: (AppEntry, MenuContext) -> Unit,
) {
    Column {
        SectionTitle(title)
        LazyRow(contentPadding = RowPadding, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(apps, key = { it.packageName }) { app ->
                AppTile(app, cache, onClick = { onOpen(app) }, onLongClick = { onMenu(app, menuContext) })
            }
        }
    }
}

@Composable
fun WideRow(
    title: String,
    apps: List<AppEntry>,
    cache: IconCache,
    onOpen: (AppEntry) -> Unit,
    onMenu: (AppEntry, MenuContext) -> Unit,
) {
    Column {
        SectionTitle(title)
        LazyRow(contentPadding = RowPadding, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(apps, key = { it.packageName }) { app ->
                WideCard(app, cache, onClick = { onOpen(app) }, onLongClick = { onMenu(app, MenuContext.APPS) })
            }
        }
    }
}

@Composable
fun HintRow(title: String, hint: String) {
    val p = LocalPalette.current
    Column {
        SectionTitle(title)
        GlassPanel(
            Modifier.padding(horizontal = 48.dp, vertical = 14.dp).fillMaxWidth().height(64.dp),
            RoundedCornerShape(22.dp),
        ) {
            Text(hint, color = p.muted, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 24.dp))
        }
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String? = null) {
    val p = LocalPalette.current
    Column(Modifier.padding(start = 48.dp, top = 10.dp, bottom = 4.dp)) {
        Text(title, color = p.text, fontSize = 32.sp, fontWeight = FontWeight.Light)
        if (subtitle != null) Text(subtitle, color = p.muted, fontSize = 14.sp)
    }
}

@Composable
fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        GlassPanel(Modifier.padding(32.dp), RoundedCornerShape(26.dp)) {
            Text(message, color = LocalPalette.current.muted, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 36.dp, vertical = 28.dp))
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Top bar
// ------------------------------------------------------------------------------------------------

private enum class Tab(val label: String, val screen: Screen) {
    HOME("Home", Screen.HOME),
    MOVIES("Movies", Screen.MOVIES),
    SHOWS("TV Shows", Screen.SHOWS),
    APPS("Apps", Screen.APPS),
    GAMES("Games", Screen.GAMES),
    SETTINGS("Settings", Screen.SETTINGS),
}

@Composable
fun TopBar(current: Screen, clock24: Boolean, onSelect: (Screen) -> Unit) {
    val selected = if (current == Screen.MANAGE_APPS) Screen.SETTINGS else current
    Row(
        Modifier.fillMaxWidth().padding(start = 48.dp, end = 48.dp, top = 20.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AuroraLogo()
        Spacer(Modifier.width(28.dp))
        Row(
            Modifier.glass(RoundedCornerShape(26.dp)).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Tab.entries.forEach { tab ->
                TabItem(tab.label, selected = tab.screen == selected) { onSelect(tab.screen) }
            }
        }
        Spacer(Modifier.weight(1f))
        NetworkBadge()
        Spacer(Modifier.width(22.dp))
        ClockBlock(clock24)
    }
}

@Composable
private fun TabItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val pill = RoundedCornerShape(22.dp)
    TvFocusable(onClick = onClick, shape = pill, focusedScale = 1.06f, drawGlass = false) { _ ->
        Box(
            Modifier.then(if (selected) Modifier.background(p.text.copy(alpha = 0.10f), pill) else Modifier)
                .padding(horizontal = 18.dp, vertical = 9.dp)
        ) {
            Text(
                label,
                color = if (selected) p.accent else p.text,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun AuroraLogo() {
    val p = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(30.dp)) {
            drawCircle(Brush.linearGradient(listOf(p.accent, p.accent2)), radius = size.minDimension / 2f)
            drawArc(
                color = Color.White.copy(alpha = 0.9f),
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(size.width * 0.2f, size.height * 0.30f),
                size = Size(size.width * 0.6f, size.height * 0.6f),
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text("Aurora", color = p.text, fontSize = 20.sp, fontWeight = FontWeight.Light, letterSpacing = 1.sp)
    }
}

@Composable
private fun ClockBlock(clock24: Boolean) {
    val p = LocalPalette.current
    val text by produceState(initialValue = formatNow(clock24), key1 = clock24) {
        while (true) {
            value = formatNow(clock24)
            delay(60_000L - System.currentTimeMillis() % 60_000L + 20L)
        }
    }
    val (time, date) = text
    Column(horizontalAlignment = Alignment.End) {
        Text(time, color = p.text, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        Text(date, color = p.muted, fontSize = 12.sp)
    }
}

private fun formatNow(clock24: Boolean): Pair<String, String> {
    val now = Date()
    val locale = Locale.getDefault()
    val time = SimpleDateFormat(if (clock24) "HH:mm" else "h:mm a", locale).format(now)
    val date = SimpleDateFormat("EEE, d MMM", locale).format(now)
    return time to date
}

private enum class Net { WIFI, ETHERNET, OTHER, OFFLINE }

@Composable
private fun rememberNetwork(): State<Net> {
    val ctx = LocalContext.current
    val state = remember { mutableStateOf(Net.OFFLINE) }
    DisposableEffect(ctx) {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        fun compute() {
            val caps = try { cm?.getNetworkCapabilities(cm.activeNetwork) } catch (e: SecurityException) { null }
            state.value = when {
                caps == null -> Net.OFFLINE
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Net.WIFI
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Net.ETHERNET
                else -> Net.OTHER
            }
        }
        compute()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = compute()
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = compute()
            override fun onLost(network: Network) { state.value = Net.OFFLINE }
        }
        val registered = try { cm?.registerDefaultNetworkCallback(callback); cm != null } catch (e: Exception) { false }
        onDispose { if (registered) try { cm?.unregisterNetworkCallback(callback) } catch (e: Exception) { } }
    }
    return state
}

@Composable
private fun NetworkBadge() {
    val p = LocalPalette.current
    val net by rememberNetwork()
    val label = when (net) {
        Net.WIFI -> "Wi-Fi"
        Net.ETHERNET -> "Ethernet"
        Net.OTHER -> "Online"
        Net.OFFLINE -> "Offline"
    }
    val tint = if (net == Net.OFFLINE) p.muted.copy(alpha = 0.6f) else p.text
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(22.dp)) {
            val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            val cx = size.width / 2f
            val bottom = size.height * 0.88f
            for (i in 1..3) {
                val r = size.width * 0.17f * i
                drawArc(
                    color = tint,
                    startAngle = 225f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(cx - r, bottom - r),
                    size = Size(r * 2f, r * 2f),
                    style = stroke,
                )
            }
            drawCircle(tint, radius = 2.2.dp.toPx(), center = Offset(cx, bottom))
        }
        Spacer(Modifier.width(6.dp))
        Text(label, color = tint, fontSize = 13.sp)
    }
}
