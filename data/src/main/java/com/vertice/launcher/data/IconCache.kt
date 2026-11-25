package com.vertice.launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.vertice.launcher.core.PerformanceProfile
import com.vertice.launcher.diag.VLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong

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

    private val hits = AtomicLong()
    private val misses = AtomicLong()
    private val decodes = AtomicLong()
    private val decodeNs = AtomicLong()
    private val slowestMs = AtomicLong()
    private val failures = AtomicLong()

    /** Resumo para o log: taxa de acerto, custo de decodificação e uso de memória. */
    fun stats(): String {
        val h = hits.get(); val m = misses.get(); val d = decodes.get()
        val avg = if (d == 0L) 0 else decodeNs.get() / d / 1_000_000
        return "ícones: acertos=$h faltas=$m decodificados=$d média=${avg}ms mais-lento=${slowestMs.get()}ms " +
            "falhas=${failures.get()} cache=${cache.size() / 1024}KB/${cache.maxSize() / 1024}KB"
    }

    private fun keyOf(app: AppEntry, px: Int) = "${app.key}@$px"

    /** Tamanhos agrupados de 8 em 8 px: tamanhos quase iguais (grades diferentes) reaproveitam o mesmo bitmap. */
    private fun target(px: Int) = (((px.coerceIn(16, maxPx)) + 7) / 8 * 8).coerceAtMost(maxPx)

    /** Resposta imediata se já estiver pronto (evita piscar ao rolar a gaveta). */
    fun peek(app: AppEntry, px: Int): ImageBitmap? {
        val b = cache.get(keyOf(app, target(px)))
        if (b != null) hits.incrementAndGet() else misses.incrementAndGet()
        return b?.asImageBitmap()
    }

    suspend fun load(app: AppEntry, px: Int): ImageBitmap = withContext(Dispatchers.Default) {
        val size = target(px)
        val key = keyOf(app, size)
        cache.get(key)?.let { return@withContext it.asImageBitmap() }
        val t0 = System.nanoTime()
        val bitmap = try {
            val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val drawable = app.info.getBadgedIcon(densityDpi)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(Canvas(b))
            b
        } catch (e: Exception) {
            // Ícone quebrado de um app não pode derrubar a launcher: mostra espaço vazio e registra.
            failures.incrementAndGet()
            VLog.w("Ícones", "Falha ao decodificar ícone de ${app.packageName}", e)
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        } catch (e: OutOfMemoryError) {
            failures.incrementAndGet()
            cache.evictAll()
            VLog.e("Ícones", "Sem memória ao decodificar ${app.packageName}; cache esvaziado", e)
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        }
        val ns = System.nanoTime() - t0
        decodes.incrementAndGet(); decodeNs.addAndGet(ns)
        val ms = ns / 1_000_000
        if (ms > slowestMs.get()) slowestMs.set(ms)
        if (ms >= 40) VLog.d("Ícones", "Ícone lento: ${app.packageName} ${ms}ms (${size}px)")
        cache.put(key, bitmap)
        bitmap.asImageBitmap()
    }

    fun clear() = cache.evictAll()
}
