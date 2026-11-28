package com.vertice.launcher.island.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vertice.launcher.core.island.IslandEvent
import com.vertice.launcher.core.island.IslandSettings
import com.vertice.launcher.core.island.IslandState

/**
 * Cápsula da Vértice Island (tema HyperOS: preta, cantos totalmente arredondados).
 * I1: só texto e um glifo; ícones reais, capa de álbum e controles chegam nas etapas I2/I3.
 * Sem blur nem animação contínua: só o tamanho muda, uma vez, quando abre ou fecha.
 */
@Composable
fun IslandHost(state: IslandState, settings: IslandSettings, onTap: () -> Unit, onDismiss: () -> Unit) {
    val event = state.currentEvent ?: return
    val expanded = state is IslandState.Expanded
    val shape = RoundedCornerShape(if (expanded) 28.dp else 50.dp)
    Column(
        Modifier
            .widthIn(min = 96.dp, max = settings.width.dp.dp)
            .animateContentSize()
            .clip(shape)
            .background(Color.Black)
            .pointerInput(Unit) { detectVerticalDragGestures { _, dy -> if (dy < -8f) onDismiss() } }
            .clickable { onTap() }
            .padding(horizontal = 16.dp, vertical = if (expanded) 14.dp else 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(glyph(event), color = Color.White, fontSize = 15.sp)
            Text(
                headline(event), color = Color.White, fontSize = 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        if (expanded) {
            Spacer(Modifier.width(1.dp))
            Text(
                detail(event), color = Color(0xFFB8BEC8), fontSize = 12.sp,
                maxLines = 3, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
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
