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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.vertice.launcher.HomeActions
import com.vertice.launcher.data.AppEntry

@Composable
fun HomeScreen(
    vm: HomeActions,
    ui: HomeUi,
    onLaunch: (AppEntry, androidx.compose.ui.geometry.Rect?) -> Unit,
    /** App em modo "mover" (estado vem da máquina de navegação). */
    moving: AppEntry?,
    onMoveStart: (AppEntry) -> Unit,
    onMoveEnd: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSheet: () -> Unit
) {
    val now by rememberNow()
    val battery by vm.batteryPercent.collectAsState(initial = null)
    val pages = ui.pageCount + if (moving != null) 1 else 0 // página extra vazia só durante o movimento
    val pager = rememberPagerState(pageCount = { pages })

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
                    onDragEnd = { if (total < -120f) onOpenDrawer() else if (total > 120f) onOpenSearch() }
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
                Spacer(Modifier.height(20.dp))
                ClockWidget(
                    now = now, style = ui.clockStyle, format = ui.clockFormat, showDate = ui.showDate,
                    onClockClick = { vm.openClock() }, onDateClick = { vm.openCalendar() }
                )
            }

            Column(Modifier.weight(1f)) {
                if (moving != null) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Toque numa célula para mover ${moving.label}", color = Mint, fontSize = 12.sp)
                        Text(
                            "Cancelar", color = Snow, fontSize = 12.sp,
                            modifier = Modifier.clip(CircleShape).clickable { onMoveEnd() }.padding(8.dp)
                        )
                    }
                }
                HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
                    HomeGridPage(
                        page = page, ui = ui, vm = vm, moving = moving,
                        onMoveStart = onMoveStart,
                        onDrop = { x, y -> moving?.let { vm.moveToCell(it, page, x, y) }; onMoveEnd() },
                        onLaunch = onLaunch
                    )
                }
                if (pages > 1) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.Center) {
                        repeat(pages) { i ->
                            val active = i == pager.currentPage
                            Box(
                                Modifier.padding(horizontal = 3.dp)
                                    .size(width = if (active) 16.dp else 6.dp, height = 6.dp)
                                    .clip(CircleShape)
                                    .background(if (active) Mint else Muted.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
                if (ui.placed.isEmpty() && !ui.loading && moving == null) {
                    Text(
                        "Segure um app na gaveta e escolha “Colocar na tela inicial”.",
                        color = Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.dock))
                        .background(PanelSoft.copy(alpha = 0.55f))
                        .padding(horizontal = Space.s, vertical = Space.xs + 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (ui.dock.isEmpty()) {
                        Text("Segure um app na gaveta para colocá-lo aqui.", color = Muted, fontSize = 12.sp)
                    }
                    ui.dock.forEach { app ->
                        AppTile(
                            app = app, vm = vm, animateLaunch = ui.profile.animations,
                            showLabel = false, onLaunch = onLaunch,
                            iconSize = (52.dp * ui.iconSize.scale).coerceAtMost(60.dp),
                            onMove = { onMoveStart(app) }
                        )
                    }
                }
                Surface(
                    color = PanelSoft.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50))
                        .clickable(onClickLabel = "Abrir todos os aplicativos") { onOpenDrawer() }
                ) {
                    Row(
                        Modifier.padding(horizontal = 22.dp, vertical = 13.dp),
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

/** Uma página da grade: COLUMNS x ROWS células iguais; célula vazia é só espaço (ou alvo, no modo mover). */
@Composable
private fun HomeGridPage(
    page: Int,
    ui: HomeUi,
    vm: HomeActions,
    moving: AppEntry?,
    onMoveStart: (AppEntry) -> Unit,
    onDrop: (x: Int, y: Int) -> Unit,
    onLaunch: (AppEntry, Rect?) -> Unit
) {
    val byCell = remember(ui.placed, page) {
        ui.placed.filter { it.item.page == page }.associateBy { it.item.x to it.item.y }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val grid = ui.grid
    // O ícone nunca passa do que a célula comporta (grades densas ficam menores sozinhas).
    val labelH = if (ui.showLabels) 16.dp else 0.dp
    val iconSize = minOf(
        48.dp * ui.iconSize.scale,
        maxHeight / grid.rows - labelH - 8.dp,
        maxWidth / grid.columns - 12.dp
    ).coerceAtLeast(28.dp)
    Column(Modifier.fillMaxSize()) {
        repeat(grid.rows) { y ->
            Row(Modifier.weight(1f).fillMaxWidth()) {
                repeat(grid.columns) { x ->
                    val placed = byCell[x to y]
                    Box(
                        Modifier.weight(1f).fillMaxSize()
                            .then(
                                if (moving != null) Modifier.clickable { onDrop(x, y) }
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (placed != null) {
                            if (moving != null) {
                                // No modo mover o ícone não abre o app: o toque vai para a célula.
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    AppIcon(placed.app, vm.icons, iconSize)
                                    Text(
                                        placed.app.label, color = Snow, fontSize = 11.sp, maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                AppTile(
                                    app = placed.app, vm = vm, animateLaunch = ui.profile.animations,
                                    showLabel = ui.showLabels, onLaunch = onLaunch,
                                    iconSize = iconSize, labelLines = 1,
                                    onMove = { onMoveStart(placed.app) }
                                )
                            }
                        } else if (moving != null) {
                            Box(
                                Modifier.size(iconSize).clip(RoundedCornerShape(12.dp))
                                    .background(PanelSoft.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }
        }
    }
    }
}
