package com.aurora.tv.data

import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Byte-bounded LRU caches for app icons and TV banners. Icons are decoded off the main thread, at a
 * fixed small size, only for items that actually scroll into view.
 */
class IconCache(private val pm: PackageManager) {

    @Immutable
    class Art(val bitmap: ImageBitmap, val avg: Color)

    private val maxBytes = (Runtime.getRuntime().maxMemory() / 16).coerceIn(4L shl 20, 12L shl 20).toInt()

    private val icons = object : LruCache<String, Art>(maxBytes) {
        override fun sizeOf(key: String, value: Art): Int = value.bitmap.asAndroidBitmap().byteCount
    }
    private val banners = object : LruCache<String, ImageBitmap>(maxBytes) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.asAndroidBitmap().byteCount
    }
    private val noBanner = ConcurrentHashMap.newKeySet<String>()

    fun peekIcon(app: AppEntry): Art? = icons.get(app.packageName)
    fun peekBanner(app: AppEntry): ImageBitmap? = banners.get(app.packageName)

    suspend fun icon(app: AppEntry): Art? {
        val hit = icons.get(app.packageName)
        if (hit != null) return hit
        return withContext(Dispatchers.IO) {
            safe {
                val d = pm.getActivityIcon(ComponentName(app.packageName, app.className))
                val bmp = d.toBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888)
                Art(bmp.asImageBitmap(), averageColor(bmp)).also { icons.put(app.packageName, it) }
            }
        }
    }

    suspend fun banner(app: AppEntry): ImageBitmap? {
        val hit = banners.get(app.packageName)
        if (hit != null) return hit
        if (app.packageName in noBanner) return null
        return withContext(Dispatchers.IO) {
            val result = safe {
                val d = pm.getActivityBanner(ComponentName(app.packageName, app.className))
                d?.toBitmap(BANNER_W, BANNER_H, Bitmap.Config.ARGB_8888)?.asImageBitmap()
            }
            if (result == null) noBanner.add(app.packageName) else banners.put(app.packageName, result)
            result
        }
    }

    fun clear() {
        icons.evictAll()
        banners.evictAll()
        noBanner.clear()
    }

    fun onTrimMemory(level: Int) {
        when {
            level >= android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> { icons.evictAll(); banners.evictAll() }
            level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                icons.trimToSize(maxBytes / 2); banners.trimToSize(maxBytes / 2)
            }
        }
    }

    private fun averageColor(bmp: Bitmap): Color {
        val one = Bitmap.createScaledBitmap(bmp, 1, 1, true)
        val px = one.getPixel(0, 0)
        if (one !== bmp) one.recycle()
        return Color(
            android.graphics.Color.red(px),
            android.graphics.Color.green(px),
            android.graphics.Color.blue(px),
        )
    }

    private inline fun <T> safe(block: () -> T): T? = try {
        block()
    } catch (c: CancellationException) {
        throw c
    } catch (e: Exception) {
        null
    }

    private companion object {
        const val ICON_PX = 160
        const val BANNER_W = 320
        const val BANNER_H = 180
    }
}
