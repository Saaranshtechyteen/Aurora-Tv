package com.aurora.tv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.auroraStore: DataStore<Preferences> by preferencesDataStore(name = "aurora_settings")

private object Keys {
    val FAVS = stringPreferencesKey("favourites")
    val HIDDEN = stringPreferencesKey("hidden")
    val RECENTS = stringPreferencesKey("recents")
    val ORDER = stringPreferencesKey("order")
    val WALLPAPER = intPreferencesKey("wallpaper")
    val ACCENT = intPreferencesKey("accent")
    val DARK = booleanPreferencesKey("dark")
    val GLASS = intPreferencesKey("glass")
    val ANIM = intPreferencesKey("anim")
    val CLOCK24 = booleanPreferencesKey("clock24")
    val NON_TV = booleanPreferencesKey("show_non_tv")
}

// Package names never contain commas, so a CSV string is a safe, compact list encoding.
private fun String?.csv(): List<String> =
    if (this.isNullOrEmpty()) emptyList() else split(',').filter { it.isNotEmpty() }

private fun Preferences.toSettings() = AuroraSettings(
    favourites = this[Keys.FAVS].csv(),
    hidden = this[Keys.HIDDEN].csv().toSet(),
    recents = this[Keys.RECENTS].csv(),
    order = this[Keys.ORDER].csv(),
    wallpaper = this[Keys.WALLPAPER] ?: 0,
    accent = this[Keys.ACCENT] ?: 0,
    dark = this[Keys.DARK] ?: true,
    glass = this[Keys.GLASS] ?: 2,
    anim = this[Keys.ANIM] ?: 2,
    clock24 = this[Keys.CLOCK24] ?: false,
    showNonTv = this[Keys.NON_TV] ?: true,
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.auroraStore

    val settings: Flow<AuroraSettings> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toSettings() }
        .distinctUntilChanged()

    suspend fun toggleFavourite(pkg: String) {
        store.edit { p ->
            val list = p[Keys.FAVS].csv().toMutableList()
            if (!list.remove(pkg)) list.add(pkg)
            p[Keys.FAVS] = list.joinToString(",")
        }
    }

    suspend fun moveFavourite(pkg: String, delta: Int) {
        store.edit { p ->
            val list = p[Keys.FAVS].csv().toMutableList()
            val i = list.indexOf(pkg)
            val j = i + delta
            if (i >= 0 && j in list.indices) {
                java.util.Collections.swap(list, i, j)
                p[Keys.FAVS] = list.joinToString(",")
            }
        }
    }

    suspend fun setHidden(pkg: String, hidden: Boolean) {
        store.edit { p ->
            val set = p[Keys.HIDDEN].csv().toMutableSet()
            if (hidden) set.add(pkg) else set.remove(pkg)
            p[Keys.HIDDEN] = set.joinToString(",")
        }
    }

    suspend fun recordLaunch(pkg: String) {
        store.edit { p ->
            val list = p[Keys.RECENTS].csv().toMutableList()
            list.remove(pkg)
            list.add(0, pkg)
            p[Keys.RECENTS] = list.take(MAX_RECENTS).joinToString(",")
        }
    }

    suspend fun clearRecents() { store.edit { it[Keys.RECENTS] = "" } }
    suspend fun setOrder(order: List<String>) { store.edit { it[Keys.ORDER] = order.joinToString(",") } }
    suspend fun setWallpaper(v: Int) { store.edit { it[Keys.WALLPAPER] = v } }
    suspend fun setAccent(v: Int) { store.edit { it[Keys.ACCENT] = v } }
    suspend fun setDark(v: Boolean) { store.edit { it[Keys.DARK] = v } }
    suspend fun setGlass(v: Int) { store.edit { it[Keys.GLASS] = v } }
    suspend fun setAnim(v: Int) { store.edit { it[Keys.ANIM] = v } }
    suspend fun setClock24(v: Boolean) { store.edit { it[Keys.CLOCK24] = v } }
    suspend fun setShowNonTv(v: Boolean) { store.edit { it[Keys.NON_TV] = v } }

    private companion object { const val MAX_RECENTS = 12 }
}
