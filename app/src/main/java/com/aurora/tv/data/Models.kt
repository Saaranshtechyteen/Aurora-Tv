package com.aurora.tv.data

import androidx.compose.runtime.Immutable

enum class AppCategory { VIDEO, AUDIO, GAME, OTHER }

@Immutable
data class AppEntry(
    val packageName: String,
    val className: String,
    val label: String,
    val category: AppCategory,
    val isLeanback: Boolean,
)

@Immutable
data class AuroraSettings(
    val favourites: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val recents: List<String> = emptyList(),
    val order: List<String> = emptyList(),
    val wallpaper: Int = 0,
    val accent: Int = 0,
    val dark: Boolean = true,
    val glass: Int = 2,          // 0..4
    val anim: Int = 2,           // 0 = off, 1 = reduced, 2 = full
    val clock24: Boolean = false,
    val showNonTv: Boolean = true,
)
