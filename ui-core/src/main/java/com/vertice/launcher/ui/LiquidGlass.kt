package com.vertice.launcher.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawOutline
import androidx.compose.ui.unit.dp

/**
 * "Liquid glass" sem desfoque em tempo real (que não cabe em aparelhos fracos): vidro leitoso com brilho no topo,
 * borda de luz (clara no canto superior esquerdo, some no meio, volta no inferior direito) e, no modo rico,
 * um reflexo suave. Tudo é desenhado numa única passada, sem camadas fora da tela.
 * [alpha] é a opacidade do corpo do vidro; o modo rico vem do perfil de desempenho (Glass.rich).
 */
fun Modifier.liquidGlass(shape: Shape, alpha: Float = Glass.panel, rich: Boolean = Glass.rich): Modifier =
    drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val body = PanelSoft.copy(alpha = alpha)
        val sheen = Brush.verticalGradient(
            0f to Color.White.copy(alpha = if (rich) 0.16f else 0.08f),
            0.55f to Color.Transparent,
            endY = size.height
        )
        val rim = Brush.linearGradient(
            0f to Color.White.copy(alpha = if (rich) 0.55f else 0.35f),
            0.45f to Color.White.copy(alpha = 0.05f),
            1f to Color.White.copy(alpha = if (rich) 0.28f else 0.18f),
            start = Offset.Zero, end = Offset(size.width, size.height)
        )
        val glow = if (rich) Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
            center = Offset(size.width * 0.18f, 0f), radius = size.width * 0.55f
        ) else null
        val stroke = Stroke(width = 1.dp.toPx())
        onDrawBehind {
            drawOutline(outline, body)
            drawOutline(outline, sheen)
            glow?.let { drawOutline(outline, it) }
            drawOutline(outline, rim, style = stroke)
        }
    }
