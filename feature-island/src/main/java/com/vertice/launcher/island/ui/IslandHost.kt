package com.vertice.launcher.island.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vertice.launcher.core.island.IslandEvent
import com.vertice.launcher.core.island.IslandSettings
import com.vertice.launcher.core.island.IslandState
import com.vertice.launcher.island.IslandCallbacks

private val Soft = Color(0xFFB8BEC8)
private val Hairline = Color.White.copy(alpha = 0.12f)

/**
 * Cápsula da Vértice Island (tema HyperOS): preta, cantos totalmente arredondados, ícone do app à esquerda.
 * Sem blur nem animação contínua: só o tamanho muda, uma vez, quando abre ou fecha.
 * Arrastar para cima dispensa; tocar abre/fecha o painel.
 */
@Composable
fun IslandHost(state: IslandState, settings: IslandSettings, cb: IslandCallbacks) {
    val event = state.currentEvent ?: return
    val expanded = state is IslandState.Expanded
    val shape = RoundedCornerShape(if (expanded) 30.dp else 50.dp)
    Column(
        Modifier
            .widthIn(min = 110.dp, max = settings.width.dp.dp)
            .animateContentSize()
            .clip(shape)
            .background(Color.Black)
            .border(0.5.dp, Hairline, shape)
            .pointerInput(Unit) { detectVerticalDragGestures { _, dy -> if (dy < -8f) cb.onDismiss() } }
            .clickable { cb.onTap() }
            .padding(horizontal = if (expanded) 16.dp else 10.dp, vertical = if (expanded) 14.dp else 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EventIcon(event, if (expanded) 36.dp else 24.dp)
            Column(Modifier.weight(1f, fill = false)) {
                Text(
                    headline(event), color = Color.White, fontSize = if (expanded) 15.sp else 13.sp,
                    fontWeight = if (expanded) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (expanded) {
                    val d = detail(event)
                    if (d.isNotEmpty()) Text(d, color = Soft, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
            if (!expanded) Box(Modifier.size(6.dp).clip(CircleShape).background(accent(event)))
        }
        if (expanded) Controls(event, cb)
    }
}

@Composable
private fun EventIcon(e: IslandEvent, size: Dp) {
    val pkg = when (e) {
        is IslandEvent.Notification -> e.packageName
        is IslandEvent.Call -> e.packageName
        is IslandEvent.Timer -> e.packageName
        is IslandEvent.Media -> e.packageName
        else -> null
    }
    val icon = rememberAppIcon(pkg)
    val shape = RoundedCornerShape(size * 0.28f)
    if (icon != null) {
        Image(icon, null, Modifier.size(size).clip(shape))
    } else {
        Box(Modifier.size(size).clip(shape).background(accent(e).copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
            Text(glyph(e), color = accent(e), fontSize = (size.value * 0.5f).sp)
        }
    }
}

private fun accent(e: IslandEvent): Color = when (e) {
    is IslandEvent.Media -> Color(0xFFFF5C8A)
    is IslandEvent.Notification -> Color(0xFF4DD0A1)
    is IslandEvent.Call -> Color(0xFF34C759)
    is IslandEvent.Timer -> Color(0xFFFF9F0A)
    is IslandEvent.Charging -> Color(0xFFB8F28B)
    is IslandEvent.Bluetooth -> Color(0xFF4DA3FF)
}

private fun glyph(e: IslandEvent) = when (e) {
    is IslandEvent.Media -> "♪"
    is IslandEvent.Notification -> "✉"
    is IslandEvent.Call -> "☎"
    is IslandEvent.Timer -> "⏱"
    is IslandEvent.Charging -> "⚡"
    is IslandEvent.Bluetooth -> "ᛒ"
}

private fun headline(e: IslandEvent): String = when (e) {
    is IslandEvent.Media -> e.title ?: "Tocando"
    is IslandEvent.Notification -> if (e.contentHidden) "Nova notificação" else (e.title ?: "Notificação")
    is IslandEvent.Call -> e.caller ?: if (e.inProgress) "Em chamada" else "Chamada"
    is IslandEvent.Timer -> e.label ?: "Cronômetro"
    is IslandEvent.Charging -> "Carregando ${e.percent}%"
    is IslandEvent.Bluetooth -> e.deviceName ?: "Bluetooth conectado"
}

private fun detail(e: IslandEvent): String = when (e) {
    is IslandEvent.Media -> e.artist.orEmpty()
    is IslandEvent.Notification -> e.text.orEmpty()
    is IslandEvent.Call -> if (e.inProgress) "Em andamento" else "Chamada recebida"
    is IslandEvent.Timer -> "Em contagem"
    is IslandEvent.Charging -> ""
    is IslandEvent.Bluetooth -> ""
}

/** Botões do painel expandido: música (anterior/tocar/próxima), chamada (as ações do próprio app) e "Abrir". */
@Composable
private fun Controls(e: IslandEvent, cb: IslandCallbacks) {
    Row(
        Modifier.padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        when (e) {
            is IslandEvent.Media -> {
                Pill("⏮") { cb.onPrevious() }
                Pill(if (e.playing) "⏸" else "▶") { cb.onPlayPause() }
                Pill("⏭") { cb.onNext() }
            }
            is IslandEvent.Call -> e.actions.forEachIndexed { i, title ->
                if (title.isNotBlank()) Pill(title) { cb.onAction(i) }
            }
            is IslandEvent.Notification -> Pill("Abrir") { cb.onOpen() }
            else -> {}
        }
    }
}

@Composable
private fun Pill(text: String, onClick: () -> Unit) {
    Text(
        text, color = Color.White, fontSize = 13.sp, maxLines = 1,
        modifier = Modifier.clip(CircleShape).background(Color(0xFF2A2D33)).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp)
    )
}
