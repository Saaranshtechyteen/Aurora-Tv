package com.aurora.tv

import android.app.Application
import android.os.SystemClock
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.tv.data.AppEntry
import com.aurora.tv.data.AppRepository
import com.aurora.tv.data.IconCache
import com.aurora.tv.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuroraViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SettingsStore(application)
    private val repo = AppRepository(application)
    val icons = IconCache(application.packageManager)

    private val installed = MutableStateFlow<List<AppEntry>?>(null)
    private val tvPackages = MutableStateFlow<Set<String>>(emptySet())

    private val _screen = MutableStateFlow(Screen.HOME)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    /** Incremented on every HOME press so the home screen can scroll to top and refocus. */
    private val _homeSignal = MutableStateFlow(0)
    val homeSignal: StateFlow<Int> = _homeSignal.asStateFlow()

    private var lastRefresh = 0L

    val ui: StateFlow<UiState> = combine(installed, store.settings, tvPackages) { apps, s, tv ->
        if (apps == null) UiState(settings = s) else buildUiState(apps, s, tv)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    init { refresh(force = true) }

    // ---- navigation ----
    fun go(screen: Screen) { _screen.value = screen }
    fun goHome() { _screen.value = Screen.HOME; _homeSignal.update { it + 1 } }
    fun goBack() { _screen.value = if (_screen.value == Screen.MANAGE_APPS) Screen.SETTINGS else Screen.HOME }

    // ---- app list ----
    fun refresh(force: Boolean = false, clearIcons: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        if (!force && installed.value != null && now - lastRefresh < 1500) return
        lastRefresh = now
        if (clearIcons) icons.clear()
        viewModelScope.launch {
            installed.value = repo.loadApps()
            tvPackages.value = repo.loadTvInputPackages()
        }
    }

    // ---- actions ----
    fun launch(app: AppEntry) {
        if (repo.launch(app)) {
            viewModelScope.launch { store.recordLaunch(app.packageName) }
        } else {
            toast("Couldn't open ${app.label}")
        }
    }

    fun showInfo(app: AppEntry) { if (!repo.showInfo(app)) toast("App info isn't available") }
    fun uninstall(app: AppEntry) { if (!repo.uninstall(app)) toast("This app can't be uninstalled") }
    fun openSystemSettings() { if (!repo.openSystemSettings()) toast("System settings aren't available") }

    fun toggleFavourite(app: AppEntry) { viewModelScope.launch { store.toggleFavourite(app.packageName) } }
    fun setHidden(app: AppEntry, hidden: Boolean) { viewModelScope.launch { store.setHidden(app.packageName, hidden) } }

    fun hide(app: AppEntry) {
        setHidden(app, true)
        toast("${app.label} hidden. Restore it in Settings > Manage apps")
    }

    fun moveFavourite(app: AppEntry, delta: Int) {
        viewModelScope.launch { store.moveFavourite(app.packageName, delta) }
    }

    /** Moves an app one step in the global launcher order (the order of the Apps tab and "Installed" row). */
    fun move(app: AppEntry, delta: Int) {
        viewModelScope.launch {
            val current = ui.value
            val list = current.visible.map { it.packageName }.toMutableList()
            val i = list.indexOf(app.packageName)
            val j = i + delta
            if (i < 0 || j !in list.indices) return@launch
            java.util.Collections.swap(list, i, j)
            // keep saved positions for hidden apps so they come back where they were
            val rest = current.settings.order.filter { it !in list }
            store.setOrder(list + rest)
        }
    }

    fun clearRecents() { viewModelScope.launch { store.clearRecents() } }
    fun setWallpaper(v: Int) { viewModelScope.launch { store.setWallpaper(v) } }
    fun setAccent(v: Int) { viewModelScope.launch { store.setAccent(v) } }
    fun setDark(v: Boolean) { viewModelScope.launch { store.setDark(v) } }
    fun setGlass(v: Int) { viewModelScope.launch { store.setGlass(v) } }
    fun setAnim(v: Int) { viewModelScope.launch { store.setAnim(v) } }
    fun setClock24(v: Boolean) { viewModelScope.launch { store.setClock24(v) } }
    fun setShowNonTv(v: Boolean) { viewModelScope.launch { store.setShowNonTv(v) } }

    private fun toast(msg: String) {
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
    }
}
