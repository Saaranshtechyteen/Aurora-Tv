package com.aurora.tv

import androidx.compose.runtime.Immutable
import com.aurora.tv.data.AppCategory
import com.aurora.tv.data.AppEntry
import com.aurora.tv.data.AuroraSettings

enum class Screen { HOME, MOVIES, SHOWS, APPS, GAMES, SETTINGS, MANAGE_APPS }

enum class FeaturedKind { CONTINUE, FAVOURITE, PICK }

@Immutable
data class Featured(val app: AppEntry, val kind: FeaturedKind)

@Immutable
data class UiState(
    val loaded: Boolean = false,
    val settings: AuroraSettings = AuroraSettings(),
    val all: List<AppEntry> = emptyList(),          // everything installed, incl. hidden
    val visible: List<AppEntry> = emptyList(),      // not hidden, in the user's order
    val favourites: List<AppEntry> = emptyList(),
    val recents: List<AppEntry> = emptyList(),
    val continueWatching: List<AppEntry> = emptyList(),
    val recommended: List<AppEntry> = emptyList(),
    val video: List<AppEntry> = emptyList(),
    val tv: List<AppEntry> = emptyList(),
    val games: List<AppEntry> = emptyList(),
    val featured: List<Featured> = emptyList(),
)

/** Pure function: runs on Dispatchers.Default inside the ViewModel's combine(). */
fun buildUiState(all: List<AppEntry>, s: AuroraSettings, tvPackages: Set<String>): UiState {
    val byPkg = all.associateBy { it.packageName }
    val pool = all.filter { it.packageName !in s.hidden && (s.showNonTv || it.isLeanback) }
    val orderIdx = s.order.withIndex().associate { (i, pkg) -> pkg to i }
    val visible = pool.sortedWith(
        compareBy<AppEntry>({ orderIdx[it.packageName] ?: Int.MAX_VALUE }, { it.label.lowercase() })
    )
    val visibleSet = visible.mapTo(HashSet()) { it.packageName }
    fun resolve(names: List<String>) = names.mapNotNull { byPkg[it] }.filter { it.packageName in visibleSet }

    val favourites = resolve(s.favourites)
    val recents = resolve(s.recents)
    val cont = recents.filter { it.category == AppCategory.VIDEO || it.category == AppCategory.AUDIO }.take(8)
    val used = (favourites + recents).mapTo(HashSet()) { it.packageName }
    val recommended = visible
        .filter { it.packageName !in used && (it.category == AppCategory.VIDEO || it.category == AppCategory.GAME) }
        .take(14)

    val feat = ArrayList<Featured>()
    cont.take(2).forEach { feat += Featured(it, FeaturedKind.CONTINUE) }
    favourites.take(2).forEach { feat += Featured(it, FeaturedKind.FAVOURITE) }
    recommended.take(2).forEach { feat += Featured(it, FeaturedKind.PICK) }
    if (feat.isEmpty()) visible.take(3).forEach { feat += Featured(it, FeaturedKind.PICK) }

    return UiState(
        loaded = true,
        settings = s,
        all = all,
        visible = visible,
        favourites = favourites,
        recents = recents,
        continueWatching = cont,
        recommended = recommended,
        video = visible.filter { it.category == AppCategory.VIDEO },
        tv = visible.filter { it.packageName in tvPackages },
        games = visible.filter { it.category == AppCategory.GAME },
        featured = feat.distinctBy { it.app.packageName }.take(4),
    )
}
