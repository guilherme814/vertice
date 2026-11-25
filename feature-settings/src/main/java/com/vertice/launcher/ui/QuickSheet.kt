package com.vertice.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vertice.launcher.HomeUi
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.vertice.launcher.HomeActions
import com.vertice.launcher.core.ClockFormat
import com.vertice.launcher.core.Accent
import com.vertice.launcher.core.ClockStyle
import com.vertice.launcher.core.GlassLevel
import com.vertice.launcher.core.GridSpec
import com.vertice.launcher.core.IconShape
import com.vertice.launcher.core.IconSize
import com.vertice.launcher.core.Wallpaper

enum class SheetTab(val label: String) {
    Wallpaper("Papel de parede"),
    Theme("Tema"),
    Grid("Grade e ícones"),
    Clock("Relógio"),
    Backup("Backup"),
    Diagnostics("Diagnóstico")
}

/**
 * Folha de personalização: sobe da parte de baixo e deixa a tela inicial visível acima,
 * então cada mudança aparece ao vivo. Começo da futura tela de ajustes completa.
 */
@Composable
fun QuickSheet(
    vm: HomeActions,
    ui: HomeUi,
    initialTab: SheetTab = SheetTab.Wallpaper,
    onClose: () -> Unit
) {
    var tab by remember { mutableStateOf(initialTab) }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { onClose() } }   // toque fora fecha
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Panel.copy(alpha = 0.97f))
                .pointerInput(Unit) { detectTapGestures { } }          // toque dentro não fecha
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Box(
                Modifier.align(Alignment.CenterHorizontally).padding(bottom = 10.dp)
                    .clip(CircleShape).background(Muted.copy(alpha = 0.4f))
                    .fillMaxWidth(0.12f).height(4.dp)
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Personalizar", color = Snow, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "✕", color = Muted, fontSize = 20.sp,
                    modifier = Modifier.clip(CircleShape).clickable { onClose() }.padding(10.dp)
                )
            }
            Row(
                Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SheetTab.entries.forEach { t -> CategoryChip(t.label, tab == t) { tab = t } }
            }
            Spacer(Modifier.height(14.dp))
            Column(Modifier.verticalScroll(rememberScrollState())) {
                when (tab) {
                    SheetTab.Wallpaper -> WallpaperTab(vm, ui)
                    SheetTab.Theme -> ThemeTab(vm, ui)
                    SheetTab.Grid -> GridTab(vm, ui)
                    SheetTab.Clock -> ClockTab(vm, ui)
                    SheetTab.Backup -> BackupTab(vm)
                    SheetTab.Diagnostics -> DiagnosticsTab()
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp, top = 4.dp))
}

@Composable
private fun ThemeTab(vm: HomeActions, ui: HomeUi) {
    SectionLabel("Cor de destaque")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Accent.entries.forEach { a ->
            val selected = ui.accent == a
            Box(
                Modifier.size(40.dp).clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Color(a.argb.toInt()))
                    .then(if (selected) Modifier.border(3.dp, Snow, CircleShape) else Modifier)
                    .clickable(onClickLabel = a.label) { vm.setAccent(a) }
                    .semantics { contentDescription = a.label },
                contentAlignment = Alignment.Center
            ) {
                if (a == Accent.System) Text("✦", color = Ink, fontSize = 16.sp)
            }
        }
    }
    Text(
        ui.accent.label + if (ui.accent == Accent.System) " (usa as cores do papel de parede no Android 12+)" else "",
        color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)
    )
    Spacer(Modifier.height(16.dp))
    SectionLabel("Intensidade do vidro (dock e botões)")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GlassLevel.entries.forEach { g -> CategoryChip(g.label, ui.glass == g) { vm.setGlass(g) } }
    }
    Text(
        "Mais forte = painéis mais opacos e texto mais legível sobre papéis de parede claros.",
        color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun GridTab(vm: HomeActions, ui: HomeUi) {
    SectionLabel("Colunas")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (GridSpec.MIN_COLUMNS..GridSpec.MAX_COLUMNS).forEach { c ->
            CategoryChip("$c", ui.grid.columns == c) { vm.setGrid(c, ui.grid.rows) }
        }
    }
    Spacer(Modifier.height(14.dp))
    SectionLabel("Linhas")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (GridSpec.MIN_ROWS..GridSpec.MAX_ROWS).forEach { r ->
            CategoryChip("$r", ui.grid.rows == r) { vm.setGrid(ui.grid.columns, r) }
        }
    }
    Spacer(Modifier.height(14.dp))
    SectionLabel("Forma dos ícones")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconShape.entries.forEach { s -> CategoryChip(s.label, ui.iconStyle.shape == s) { vm.setIconShape(s) } }
    }
    if (ui.iconStyle.shape != IconShape.Original) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Sombra nos ícones", color = Snow, fontSize = 15.sp)
            Switch(
                checked = ui.iconStyle.shadow,
                onCheckedChange = { vm.setIconShadow(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Ink, checkedTrackColor = Mint,
                    uncheckedThumbColor = Muted, uncheckedTrackColor = PanelSoft
                )
            )
        }
        Text("Na economia máxima (Ultra Lite) a sombra fica desligada.", color = Muted, fontSize = 11.sp)
    }
    Spacer(Modifier.height(14.dp))
    SectionLabel("Tamanho dos ícones")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconSize.entries.forEach { s -> CategoryChip(s.label, ui.iconSize == s) { vm.setIconSize(s) } }
    }
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Mostrar nomes dos apps", color = Snow, fontSize = 15.sp)
        Switch(
            checked = ui.showLabels,
            onCheckedChange = { vm.setShowLabels(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Ink, checkedTrackColor = Mint,
                uncheckedThumbColor = Muted, uncheckedTrackColor = PanelSoft
            )
        )
    }
    Text(
        "Ao diminuir a grade, os apps que não cabem vão para a próxima página; nada é perdido.",
        color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun ClockTab(vm: HomeActions, ui: HomeUi) {
    SectionLabel("Estilo")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ClockStyle.entries.forEach { s ->
            CategoryChip(s.label, ui.clockStyle == s) { vm.setClockStyle(s) }
        }
    }
    Spacer(Modifier.height(14.dp))
    SectionLabel("Formato da hora")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ClockFormat.entries.forEach { f ->
            CategoryChip(f.label, ui.clockFormat == f) { vm.setClockFormat(f) }
        }
    }
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Mostrar a data", color = Snow, fontSize = 15.sp)
        Switch(
            checked = ui.showDate,
            onCheckedChange = { vm.setShowDate(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Ink, checkedTrackColor = Mint,
                uncheckedThumbColor = Muted, uncheckedTrackColor = PanelSoft
            )
        )
    }
    Text(
        "Toque no relógio para abrir os alarmes e na data para abrir a agenda.",
        color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun WallpaperTab(vm: HomeActions, ui: HomeUi) {
    SectionLabel("Escolha um papel de parede")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Wallpaper.entries.forEach { w ->
            val selected = ui.wallpaper == w
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clip(RoundedCornerShape(18.dp)).clickable { vm.setWallpaper(w) }
            ) {
                Box(
                    Modifier
                        .size(width = 88.dp, height = 156.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(
                            if (selected) 2.5.dp else 1.dp,
                            if (selected) Mint else Muted.copy(alpha = 0.3f),
                            RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (w.isSystem) {
                        Box(Modifier.fillMaxSize().background(PanelSoft), contentAlignment = Alignment.Center) {
                            Text("▣", color = Muted, fontSize = 26.sp)
                        }
                    } else {
                        WallpaperBackground(w, Modifier.fillMaxSize())
                    }
                }
                Text(
                    w.label, color = if (selected) Mint else Snow, fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                )
            }
        }
    }
    Text(
        "Os papéis são desenhados pelo app (sem imagens), então não pesam. " +
            "\"Do aparelho\" usa o papel de parede que você já tem no Android.",
        color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp)
    )
}
