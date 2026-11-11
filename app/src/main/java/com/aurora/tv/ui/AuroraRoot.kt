package com.aurora.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.tv.AuroraViewModel
import com.aurora.tv.Screen

@Composable
fun AuroraRoot(vm: AuroraViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val screen by vm.screen.collectAsStateWithLifecycle()
    val homeSignal by vm.homeSignal.collectAsStateWithLifecycle()

    val s = ui.settings
    val palette = remember(s.dark, s.accent, s.glass, s.wallpaper) { buildPalette(s) }
    val motion = when (s.anim) { 0 -> 0f; 1 -> 0.5f; else -> 1f }

    val tint = remember { mutableStateOf(Color.Transparent) }
    val parallax = remember { mutableFloatStateOf(0f) }
    var menu by remember { mutableStateOf<MenuRequest?>(null) }
    var showAbout by remember { mutableStateOf(false) }
    var focusPending by remember { mutableStateOf(true) }

    CompositionLocalProvider(LocalPalette provides palette, LocalMotion provides motion) {
        MaterialTheme(
            colorScheme = if (palette.dark) darkColorScheme(primary = palette.accent)
            else lightColorScheme(primary = palette.accent)
        ) {
            // The launcher never finishes: Back walks up one level, and on Home it just returns to the top.
            BackHandler { if (screen == Screen.HOME) vm.goHome() else vm.goBack() }

            val open = vm::launch
            val openMenu: (com.aurora.tv.data.AppEntry, MenuContext) -> Unit = { app, ctx -> menu = MenuRequest(app, ctx) }

            Box(Modifier.fillMaxSize().background(palette.bg)) {
                Wallpaper(index = s.wallpaper, tint = tint, parallax = { parallax.floatValue })

                Column(Modifier.fillMaxSize()) {
                    TopBar(current = screen, clock24 = s.clock24, onSelect = vm::go)

                    val d = (320 * motion).toInt()
                    AnimatedContent(
                        targetState = screen,
                        modifier = Modifier.fillMaxSize(),
                        transitionSpec = {
                            (fadeIn(tween(d)) + slideInVertically(tween(d)) { it / 30 }) togetherWith fadeOut(tween(d / 2))
                        },
                        label = "screen",
                    ) { target ->
                        when (target) {
                            Screen.HOME -> HomeScreen(
                                ui = ui,
                                cache = vm.icons,
                                tint = tint,
                                parallax = parallax,
                                homeSignal = homeSignal,
                                requestFocusOnLoad = focusPending,
                                onFocusRequested = { focusPending = false },
                                onOpen = open,
                                onInfo = vm::showInfo,
                                onMenu = openMenu,
                                onOpenSettings = { vm.go(Screen.SETTINGS) },
                            )
                            Screen.MOVIES -> AppGridScreen(
                                "Movies", "Streaming and video apps", ui.video,
                                "No video apps found yet. Install a streaming app and it will appear here.",
                                vm.icons, open, openMenu,
                            )
                            Screen.SHOWS -> {
                                val live = ui.tv.isNotEmpty()
                                AppGridScreen(
                                    "TV Shows",
                                    if (live) "Live TV and channel apps" else "Streaming and video apps",
                                    if (live) ui.tv else ui.video,
                                    "No TV apps found yet. Install a TV or streaming app and it will appear here.",
                                    vm.icons, open, openMenu,
                                )
                            }
                            Screen.APPS -> AppGridScreen(
                                "Apps", "${ui.visible.size} applications \u00B7 hold OK for options", ui.visible,
                                "No launchable apps were found.",
                                vm.icons, open, openMenu,
                            )
                            Screen.GAMES -> AppGridScreen(
                                "Games", null, ui.games,
                                "No games found yet. Install a game and it will appear here.",
                                vm.icons, open, openMenu,
                            )
                            Screen.SETTINGS -> SettingsScreen(
                                ui = ui,
                                vm = vm,
                                onManageApps = { vm.go(Screen.MANAGE_APPS) },
                                onAbout = { showAbout = true },
                            )
                            Screen.MANAGE_APPS -> ManageAppsScreen(
                                ui = ui,
                                cache = vm.icons,
                                onToggleFavourite = vm::toggleFavourite,
                                onSetHidden = vm::setHidden,
                            )
                        }
                    }
                }

                menu?.let { req ->
                    val fav = req.app.packageName in s.favourites
                    AppMenuDialog(
                        request = req,
                        isFavourite = fav,
                        cache = vm.icons,
                        onDismiss = { menu = null },
                        onOpen = { vm.launch(req.app) },
                        onToggleFavourite = { vm.toggleFavourite(req.app) },
                        onMove = { delta ->
                            if (req.context == MenuContext.FAVOURITES) vm.moveFavourite(req.app, delta)
                            else vm.move(req.app, delta)
                        },
                        onHide = { vm.hide(req.app) },
                        onInfo = { vm.showInfo(req.app) },
                        onUninstall = { vm.uninstall(req.app) },
                    )
                }
                if (showAbout) AboutDialog { showAbout = false }
            }
        }
    }
}
