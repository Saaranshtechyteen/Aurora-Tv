package com.aurora.tv.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Discovers and launches installed apps. Only uses the intents declared in <queries>. */
class AppRepository(context: Context) {
    private val appContext = context.applicationContext
    private val pm = appContext.packageManager

    @Suppress("DEPRECATION")
    suspend fun loadApps(): List<AppEntry> = withContext(Dispatchers.Default) {
        val tvIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val stdIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val out = LinkedHashMap<String, AppEntry>()

        fun collect(list: List<ResolveInfo>, leanback: Boolean) {
            for (ri in list) {
                val ai = ri.activityInfo ?: continue
                val pkg = ai.packageName
                if (pkg == appContext.packageName || out.containsKey(pkg)) continue
                val info = ai.applicationInfo
                val category = when {
                    info == null -> AppCategory.OTHER
                    info.category == ApplicationInfo.CATEGORY_VIDEO -> AppCategory.VIDEO
                    info.category == ApplicationInfo.CATEGORY_AUDIO -> AppCategory.AUDIO
                    info.category == ApplicationInfo.CATEGORY_GAME ||
                        (info.flags and ApplicationInfo.FLAG_IS_GAME) != 0 -> AppCategory.GAME
                    else -> AppCategory.OTHER
                }
                val label = try { ri.loadLabel(pm).toString() } catch (e: Exception) { pkg }
                out[pkg] = AppEntry(pkg, ai.name, label, category, leanback)
            }
        }

        // TV (leanback) apps win; plain launcher apps are added for packages without a TV entry point.
        collect(pm.queryIntentActivities(tvIntent, 0), leanback = true)
        collect(pm.queryIntentActivities(stdIntent, 0), leanback = false)
        out.values.sortedBy { it.label.lowercase() }
    }

    /** Packages that provide a Live TV input (used for the "TV Shows" tab). */
    @Suppress("DEPRECATION")
    suspend fun loadTvInputPackages(): Set<String> = withContext(Dispatchers.Default) {
        try {
            pm.queryIntentServices(Intent("android.media.tv.TvInputService"), 0)
                .mapNotNull { it.serviceInfo?.packageName }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun launch(app: AppEntry): Boolean = startSafely(
        Intent(Intent.ACTION_MAIN).apply {
            addCategory(if (app.isLeanback) Intent.CATEGORY_LEANBACK_LAUNCHER else Intent.CATEGORY_LAUNCHER)
            component = ComponentName(app.packageName, app.className)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
    )

    fun showInfo(app: AppEntry): Boolean = startSafely(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )

    fun uninstall(app: AppEntry): Boolean = startSafely(
        Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )

    fun openSystemSettings(): Boolean = startSafely(
        Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )

    private fun startSafely(intent: Intent): Boolean = try {
        appContext.startActivity(intent)
        true
    } catch (e: Exception) {   // ActivityNotFoundException, SecurityException, ...
        false
    }
}
