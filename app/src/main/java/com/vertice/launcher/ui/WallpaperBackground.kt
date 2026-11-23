package com.vertice.launcher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.vertice.launcher.core.Wallpaper

/**
 * Desenha o papel de parede. Os pincéis são montados uma vez por tamanho (drawWithCache);
 * como não há animação, a tela inicial não força nenhum redesenho aqui.
 */
@Composable
fun WallpaperBackground(wallpaper: Wallpaper, modifier: Modifier = Modifier) {
    Box(
        modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val longest = maxOf(w, h)
            val base = Color(wallpaper.base)
            val brushes = wallpaper.blobs.map { b ->
                val c = Color(b.color)
                Brush.radialGradient(
                    colors = listOf(c.copy(alpha = b.alpha), c.copy(alpha = 0f)),
                    center = Offset(b.x * w, b.y * h),
                    radius = b.radius * longest
                )
            }
            // Escurece a base da tela: o dock e os ícones ficam legíveis em qualquer cor.
            val shade = Brush.verticalGradient(
                0.55f to Color.Transparent,
                1f to Color(0x66000000)
            )
            onDrawBehind {
                drawRect(base)
                brushes.forEach { drawRect(it) }
                drawRect(shade)
            }
        }
    )
}
