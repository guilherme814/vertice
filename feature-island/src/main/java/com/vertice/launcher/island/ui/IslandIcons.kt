package com.vertice.launcher.island.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Ícones pequenos dos apps (96 px), com cache curto: a ilha mostra poucos por vez. */
object IslandIcons {
    private const val SIZE = 96
    private val cache = LruCache<String, ImageBitmap>(40)
    private val missing = HashSet<String>()

    fun cached(pkg: String): ImageBitmap? = cache.get(pkg)

    fun load(context: Context, pkg: String): ImageBitmap? {
        cache.get(pkg)?.let { return it }
        if (pkg in missing) return null
        return try {
            val d = context.packageManager.getApplicationIcon(pkg)
            val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            d.setBounds(0, 0, SIZE, SIZE)
            d.draw(c)
            bmp.asImageBitmap().also { cache.put(pkg, it) }
        } catch (_: Throwable) {
            // App fora da visibilidade de pacotes (ou removido): cai no símbolo.
            missing += pkg
            null
        }
    }
}

@Composable
fun rememberAppIcon(pkg: String?): ImageBitmap? {
    if (pkg == null) return null
    val context = LocalContext.current.applicationContext
    val icon by produceState(IslandIcons.cached(pkg), pkg) {
        if (value == null) value = withContext(Dispatchers.Default) { IslandIcons.load(context, pkg) }
    }
    return icon
}
