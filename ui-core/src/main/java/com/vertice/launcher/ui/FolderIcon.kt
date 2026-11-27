package com.vertice.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vertice.launcher.core.Folder
import com.vertice.launcher.core.FolderCover
import com.vertice.launcher.core.FolderTint
import com.vertice.launcher.core.IconStyle
import com.vertice.launcher.data.AppEntry
import com.vertice.launcher.data.IconCache

/** Cor do vidro da pasta (null = vidro puro). */
fun FolderTint.color(): Color? = argb?.let { Color(it.toInt()) }

/** Pasta fechada: quadrado de liquid glass com miniaturas dos apps (grade 2x2 ou pilha em leque). */
@Composable
fun FolderIcon(
    folder: Folder,
    apps: List<AppEntry>,
    icons: IconCache,
    size: Dp,
    style: IconStyle,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(size * 0.26f)
    Box(modifier.size(size).liquidGlass(shape, alpha = 0.55f, tint = folder.tint.color())) {
        when (folder.cover) {
            FolderCover.Grid -> {
                val mini = size * 0.34f
                Column(
                    Modifier.align(Alignment.Center),
                    verticalArrangement = Arrangement.spacedBy(size * 0.06f)
                ) {
                    apps.take(4).chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(size * 0.06f)) {
                            row.forEach { AppIcon(it, icons, mini, style = style) }
                        }
                    }
                }
            }
            FolderCover.Stack -> {
                val big = size * 0.52f
                // O primeiro app fica por cima: desenha de trás para frente.
                apps.take(3).reversed().forEachIndexed { r, app ->
                    val i = minOf(apps.size, 3) - 1 - r
                    Box(Modifier.align(Alignment.TopStart).offset(size * (0.10f + 0.16f * i), size * (0.10f + 0.16f * i)).padding(0.dp)) {
                        AppIcon(app, icons, big, style = style)
                    }
                }
            }
        }
    }
}
