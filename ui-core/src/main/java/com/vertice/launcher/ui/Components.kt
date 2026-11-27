package com.vertice.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import com.vertice.launcher.core.IconStyle
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vertice.launcher.data.AppEntry
import com.vertice.launcher.data.IconCache
import kotlin.math.roundToInt

/** Ícone assíncrono: usa o cache e só decodifica se faltar. */
@Composable
fun AppIcon(
    app: AppEntry,
    icons: IconCache,
    size: Dp,
    modifier: Modifier = Modifier,
    style: IconStyle = IconStyle()
) {
    val px = with(LocalDensity.current) { size.toPx() }.roundToInt()
    val full = style.shape.needsFullBleed
    var bitmap: ImageBitmap? by remember(app.key, px, full) { mutableStateOf(icons.peek(app, px, full)) }
    LaunchedEffect(app.key, px, full) {
        if (bitmap == null) bitmap = icons.load(app, px, full)
    }
    val shape = remember(style.shape) { style.shape.percent?.let { RoundedCornerShape(it) } ?: RectangleShape }
    Box(modifier.size(size)) {
        bitmap?.let {
            Image(
                bitmap = it, contentDescription = app.label,
                modifier = Modifier.size(size)
                    .then(if (full && style.shadow) Modifier.shadow(3.dp, shape) else Modifier)
                    .then(if (full) Modifier.clip(shape) else Modifier)
            )
        }
    }
}

@Composable
fun CategoryChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) Mint else PanelSoft,
        shape = RoundedCornerShape(50),
        modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick)
    ) {
        Text(
            text,
            color = if (selected) Ink else Snow,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
