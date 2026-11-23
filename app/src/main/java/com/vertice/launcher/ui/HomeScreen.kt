package com.vertice.launcher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.vertice.launcher.HomeUi
import com.vertice.launcher.LauncherViewModel
import com.vertice.launcher.data.AppEntry

@Composable
fun HomeScreen(
    vm: LauncherViewModel,
    ui: HomeUi,
    onLaunch: (AppEntry, androidx.compose.ui.geometry.Rect?) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSheet: () -> Unit
) {
    val now by rememberNow()
    val battery by rememberBatteryPercent()

    Box(
        Modifier
            .fillMaxSize()
            // Véu leve sobre o papel de parede DO APARELHO (que não controlamos) para o texto
            // continuar legível. Os papéis próprios já são desenhados escuros.
            .then(
                if (ui.wallpaper.isSystem)
                    Modifier.background(Brush.verticalGradient(listOf(Color(0x66090D14), Color(0x99090D14))))
                else Modifier
            )
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onOpenSheet() }) }
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onVerticalDrag = { change, dy -> change.consume(); total += dy },
                    onDragEnd = { if (total < -120f) onOpenDrawer() }
                )
            }
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("VÉRTICE", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = PanelSoft, shape = CircleShape,
                        modifier = Modifier.clip(CircleShape).clickable { onOpenSheet() }
                    ) {
                        Text(
                            "✦  Personalizar", color = Snow, fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                    Surface(color = PanelSoft, shape = CircleShape) {
                        Text(
                            "◉  " + (battery?.let { "$it%" } ?: "—"),
                            color = Snow, fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                    }
                }
                Spacer(Modifier.height(36.dp))
                ClockWidget(
                    now = now, style = ui.clockStyle, format = ui.clockFormat, showDate = ui.showDate,
                    onClockClick = { vm.openClock() }, onDateClick = { vm.openCalendar() }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (ui.dock.isEmpty()) {
                        Text("Segure um app na gaveta para colocá-lo aqui.", color = Muted, fontSize = 12.sp)
                    }
                    ui.dock.forEach { app ->
                        AppTile(
                            app = app, vm = vm, animateLaunch = ui.profile.animations,
                            showLabel = false, onLaunch = onLaunch
                        )
                    }
                }
                Surface(
                    color = PanelSoft.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onOpenDrawer() }
                ) {
                    Row(
                        Modifier.padding(horizontal = 18.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Todos os aplicativos", color = Snow, fontSize = 14.sp)
                        Text("↑", color = Mint, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

/** Bateria por broadcast (o sistema avisa só quando muda; sem polling). */
@Composable
private fun rememberBatteryPercent(): State<Int?> {
    val context = LocalContext.current
    val state = remember { androidx.compose.runtime.mutableStateOf<Int?>(null) }
    DisposableEffect(context) {
        fun read(intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            state.value = if (level >= 0 && scale > 0) level * 100 / scale else null
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) = read(intent)
        }
        // ACTION_BATTERY_CHANGED é "sticky": o registro já devolve o valor atual.
        read(context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))
        onDispose { context.unregisterReceiver(receiver) }
    }
    return state
}
