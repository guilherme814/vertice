package com.vertice.launcher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.IntSize
import com.vertice.launcher.core.CustomWallpaper
import com.vertice.launcher.core.Wallpaper

/**
 * Desenha o papel de parede. Os pincéis são montados uma vez por tamanho (drawWithCache);
 * como não há animação, a tela inicial não força nenhum redesenho aqui.
 */
@Composable
fun WallpaperBackground(
    wallpaper: Wallpaper,
    modifier: Modifier = Modifier,
    customA: Long = CustomWallpaper.DEFAULT_A,
    customB: Long = CustomWallpaper.DEFAULT_B
) {
    Box(
        modifier.drawWithCache {
            // Pintado UMA vez numa imagem de meia resolução (gradientes não perdem nada ao ampliar) e depois
            // desenhado como uma única textura: em GPU fraca, 1 desenho custa bem menos que fundo + vários gradientes + sombra a cada quadro.
            val sw = (size.width * 0.5f).toInt().coerceAtLeast(1)
            val sh = (size.height * 0.5f).toInt().coerceAtLeast(1)
            val w = sw.toFloat()
            val h = sh.toFloat()
            val longest = maxOf(w, h)
            val custom = wallpaper == Wallpaper.Custom
            val base = Color(if (custom) CustomWallpaper.base(customA) else wallpaper.base)
            val blobs = if (custom) CustomWallpaper.blobs(customA, customB) else wallpaper.blobs
            val bitmap = ImageBitmap(sw, sh)
            CanvasDrawScope().draw(this, layoutDirection, Canvas(bitmap), Size(w, h)) {
                drawRect(base)
                blobs.forEach { b ->
                    val c = Color(b.color)
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(c.copy(alpha = b.alpha), c.copy(alpha = 0f)),
                            center = Offset(b.x * w, b.y * h),
                            radius = b.radius * longest
                        )
                    )
                }
                // Escurece a base da tela: o dock e os ícones ficam legíveis em qualquer cor.
                drawRect(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color(0x66000000)))
            }
            val dst = IntSize(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1))
            onDrawBehind { drawImage(bitmap, dstSize = dst, filterQuality = FilterQuality.Low) }
        }
    )
}
