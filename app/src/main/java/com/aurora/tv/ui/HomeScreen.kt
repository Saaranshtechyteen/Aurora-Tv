package com.aurora.tv.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.aurora.tv.Featured
import com.aurora.tv.FeaturedKind
import com.aurora.tv.UiState
import com.aurora.tv.data.AppCategory
import com.aurora.tv.data.AppEntry
import com.aurora.tv.data.IconCache
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    ui: UiState,
    cache: IconCache,
    tint: MutableState<Color>,
    parallax: MutableFloatState,
    homeSignal: Int,
    requestFocusOnLoad: Boolean,
    onFocusRequested: () -> Unit,
    onOpen: (AppEntry) -> Unit,
    onInfo: (AppEntry) -> Unit,
    onMenu: (AppEntry, MenuContext) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val listState = rememberLazyListState()
    val heroFocus = remember { FocusRequester() }

    // Parallax input for the wallpaper. Written from the snapshot system, read in a draw-phase lambda.
    LaunchedEffect(listState) {
        snapshotFlow {
            if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat() else 600f
        }.collect { parallax.floatValue = it }
    }
    DisposableEffect(Unit) { onDispose { parallax.floatValue = 0f } }

    // First launch: put focus on Play so the remote works immediately.
    LaunchedEffect(ui.loaded) {
        if (ui.loaded && requestFocusOnLoad) {
            delay(60)
            runCatching { heroFocus.requestFocus() }
            onFocusRequested()
        }
    }
    // HOME pressed while already home: scroll to top and refocus the hero.
    LaunchedEffect(homeSignal) {
        if (homeSignal > 0) {
            listState.animateScrollToItem(0)
            runCatching { heroFocus.requestFocus() }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 40.dp),
    ) {
        item(key = "hero") {
            Hero(
                featured = ui.featured,
                cache = cache,
                tint = tint,
                focusRequester = heroFocus,
                onPlay = onOpen,
                onInfo = onInfo,
                onOpenSettings = onOpenSettings,
            )
        }
        if (ui.continueWatching.isNotEmpty()) {
            item(key = "continue") { WideRow("Continue Watching", ui.continueWatching, cache, onOpen, onMenu) }
        }
        item(key = "favourites") {
            if (ui.favourites.isEmpty()) {
                HintRow("Favourite Apps", "Hold OK on any app and choose \"Add to favourites\" to pin it here.")
            } else {
                AppRow("Favourite Apps", ui.favourites, cache, MenuContext.FAVOURITES, onOpen, onMenu)
            }
        }
        if (ui.recents.isNotEmpty()) {
            item(key = "recents") { AppRow("Recently Opened", ui.recents, cache, MenuContext.APPS, onOpen, onMenu) }
        }
        if (ui.recommended.isNotEmpty()) {
            item(key = "recommended") { AppRow("Recommended", ui.recommended, cache, MenuContext.APPS, onOpen, onMenu) }
        }
        item(key = "installed") {
            AppRow("Installed Applications", ui.visible, cache, MenuContext.APPS, onOpen, onMenu)
        }
    }
}

@Composable
private fun Hero(
    featured: List<Featured>,
    cache: IconCache,
    tint: MutableState<Color>,
    focusRequester: FocusRequester,
    onPlay: (AppEntry) -> Unit,
    onInfo: (AppEntry) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val motion = LocalMotion.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var index by remember(featured) { mutableIntStateOf(0) }

    // Slow auto-rotation; only runs while the launcher is in the foreground and animations are on.
    LaunchedEffect(featured, motion) {
        if (featured.size > 1 && motion > 0f) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    delay(9_000)
                    index = (index + 1) % featured.size
                }
            }
        }
    }

    val current = featured.getOrNull(index % maxOf(featured.size, 1))

    Row(
        Modifier.fillMaxWidth().height(260.dp).padding(horizontal = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Crossfade(
                targetState = current,
                animationSpec = tween((500 * motion).toInt()),
                label = "heroText",
            ) { f -> HeroText(f) }
            Spacer(Modifier.height(20.dp))
            // Buttons live outside the Crossfade so focus is never lost while the artwork swaps.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroButton(
                    label = if (current == null) "Settings" else "Play",
                    primary = true,
                    focusRequester = focusRequester,
                ) { if (current == null) onOpenSettings() else onPlay(current.app) }
                if (current != null) {
                    HeroButton(label = "More Info", primary = false) { onInfo(current.app) }
                }
            }
        }
        Spacer(Modifier.width(32.dp))
        Crossfade(
            targetState = current,
            animationSpec = tween((700 * motion).toInt()),
            label = "heroArt",
        ) { f -> if (f != null) HeroArt(f.app, cache, tint) }
    }
}

@Composable
private fun HeroText(f: Featured?) {
    val p = LocalPalette.current
    val kicker: String
    val title: String
    val body: String
    if (f == null) {
        kicker = "WELCOME"
        title = "Aurora TV"
        body = "Experience Television, Reimagined. Install some apps, or open Settings to make Aurora yours."
    } else {
        kicker = when (f.kind) {
            FeaturedKind.CONTINUE -> "CONTINUE WATCHING"
            FeaturedKind.FAVOURITE -> "FROM YOUR FAVOURITES"
            FeaturedKind.PICK -> "FEATURED"
        }
        title = f.app.label
        body = when {
            f.kind == FeaturedKind.CONTINUE -> "Pick up right where you left off in ${f.app.label}."
            f.app.category == AppCategory.VIDEO -> "Stream movies and shows in ${f.app.label}."
            f.app.category == AppCategory.GAME -> "Jump into a game with ${f.app.label}."
            f.app.category == AppCategory.AUDIO -> "Put on something good with ${f.app.label}."
            else -> "Open ${f.app.label} on your TV."
        }
    }
    Column {
        Text(kicker, color = p.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
        Spacer(Modifier.height(6.dp))
        Text(title, color = p.text, fontSize = 40.sp, fontWeight = FontWeight.Light, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        Text(body, color = p.muted, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(460.dp))
    }
}

@Composable
private fun HeroButton(
    label: String,
    primary: Boolean,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(28.dp)
    TvFocusable(onClick = onClick, shape = shape, focusedScale = 1.07f, focusRequester = focusRequester) { _ ->
        Box(
            Modifier
                .then(
                    if (primary) Modifier.background(Brush.horizontalGradient(listOf(p.accent, p.accent2)), shape)
                    else Modifier
                )
                .padding(horizontal = 28.dp, vertical = 12.dp)
        ) {
            Text(
                label,
                color = if (primary) p.onAccent else p.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun HeroArt(app: AppEntry, cache: IconCache, tint: MutableState<Color>) {
    val art by produceState(initialValue = cache.peekIcon(app), key1 = app.packageName) {
        if (value == null) value = cache.icon(app)
    }
    val loaded = art
    LaunchedEffect(loaded) { if (loaded != null) tint.value = loaded.avg }

    GlassPanel(Modifier.size(210.dp), RoundedCornerShape(40.dp)) {
        Box(
            Modifier.fillMaxSize().drawBehind {
                val c = (loaded?.avg ?: Color.White).copy(alpha = 0.35f)
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(c, Color.Transparent),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width * 0.6f,
                    )
                )
            },
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(app, cache, 112.dp)
        }
    }
}
