package com.vertice.launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.vertice.launcher.core.PerformanceProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Cache de ícones limitado por BYTES (não por quantidade), então o consumo de memória
 * é previsível e o perfil de desempenho decide o teto. O ícone é desenhado como o app o
 * entrega (formato do sistema); formas e ícones temáticos entram numa fase de design.
 */
class IconCache(context: Context, profile: PerformanceProfile) {

    private val densityDpi = context.applicationContext.resources.displayMetrics.densityDpi
    private var maxPx = profile.iconMaxPx

    private val cache = object : LruCache<String, Bitmap>(profile.iconCacheMb * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    fun configure(profile: PerformanceProfile) {
        maxPx = profile.iconMaxPx
        cache.resize(profile.iconCacheMb * 1024 * 1024)
    }

    private fun keyOf(app: AppEntry, px: Int) = "${app.key}@$px"

    private fun target(px: Int) = px.coerceIn(16, maxPx)

    /** Resposta imediata se já estiver pronto (evita piscar ao rolar a gaveta). */
    fun peek(app: AppEntry, px: Int): ImageBitmap? = cache.get(keyOf(app, target(px)))?.asImageBitmap()

    suspend fun load(app: AppEntry, px: Int): ImageBitmap = withContext(Dispatchers.Default) {
        val size = target(px)
        val key = keyOf(app, size)
        cache.get(key)?.let { return@withContext it.asImageBitmap() }
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val drawable = app.info.getBadgedIcon(densityDpi)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(Canvas(bitmap))
        cache.put(key, bitmap)
        bitmap.asImageBitmap()
    }

    fun clear() = cache.evictAll()
}
