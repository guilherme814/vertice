package com.vertice.launcher.island.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vertice.launcher.core.island.IslandAppConfig
import com.vertice.launcher.core.island.IslandApps
import com.vertice.launcher.core.island.IslandMode
import com.vertice.launcher.core.island.IslandStore
import com.vertice.launcher.ui.CategoryChip
import com.vertice.launcher.ui.Ink
import com.vertice.launcher.ui.Mint
import com.vertice.launcher.ui.Muted
import com.vertice.launcher.ui.PanelSoft
import com.vertice.launcher.ui.Snow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer

private data class InstalledApp(val pkg: String, val label: String)

private fun fold(s: String) =
    Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

private fun launchableApps(context: Context): List<InstalledApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
        .filter { it.pkg != context.packageName }
        .distinctBy { it.pkg }
        .sortedBy { fold(it.label) }
}

/**
 * Gerenciador da Ilha: escolhe de quais apps as notificações aparecem na ilha (todos começam desligados).
 * Sem busca mostra os sugeridos e os já ligados; digitando, procura entre todos os apps instalados.
 */
@Composable
fun IslandAppManager(store: IslandStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configs by store.apps.collectAsState(initial = emptyMap())
    val installed by produceState(emptyList<InstalledApp>()) {
        value = withContext(Dispatchers.Default) { launchableApps(context) }
    }
    var query by remember { mutableStateOf("") }
    var open by remember { mutableStateOf<String?>(null) }

    Text("Escolha de quais apps a ilha mostra notificações.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
    BasicTextField(
        value = query, onValueChange = { query = it.take(40) }, singleLine = true,
        textStyle = TextStyle(color = Snow, fontSize = 14.sp),
        cursorBrush = SolidColor(Mint),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(PanelSoft).padding(horizontal = 14.dp, vertical = 12.dp),
        decorationBox = { inner ->
            Box { if (query.isEmpty()) Text("Buscar app…", color = Muted, fontSize = 14.sp); inner() }
        }
    )
    Spacer(Modifier.height(8.dp))

    val suggested = IslandApps.SUGGESTED.map { it.first }.toSet()
    val q = fold(query.trim())
    val rows = if (q.isEmpty()) installed.filter { it.pkg in suggested || configs[it.pkg]?.enabled == true }
    else installed.filter { fold(it.label).contains(q) }.take(30)

    if (rows.isEmpty()) {
        Text(
            if (q.isEmpty()) "Nenhum app sugerido instalado. Use a busca para escolher." else "Nenhum app encontrado.",
            color = Muted, fontSize = 12.sp
        )
    }
    rows.forEach { app ->
        val cfg = configs[app.pkg]
        val on = cfg?.enabled == true
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = rememberAppIcon(app.pkg)
            if (icon != null) Image(icon, null, Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)))
            else Box(Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(PanelSoft))
            Text(
                app.label, color = Snow, fontSize = 15.sp, maxLines = 1,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            )
            Switch(
                checked = on,
                onCheckedChange = { v ->
                    scope.launch { if (v) store.setApp(cfg?.copy(enabled = true) ?: IslandAppConfig(app.pkg)) else store.removeApp(app.pkg) }
                    open = if (v) app.pkg else null
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Ink, checkedTrackColor = Mint,
                    uncheckedThumbColor = Muted, uncheckedTrackColor = PanelSoft
                )
            )
        }
        if (on && cfg != null && open == app.pkg) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(start = 44.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                IslandMode.entries.forEach { m ->
                    CategoryChip(m.label, cfg.mode == m) { scope.launch { store.setApp(cfg.copy(mode = m)) } }
                }
                CategoryChip(if (cfg.showContent) "Mostra texto" else "Só aviso", cfg.showContent) {
                    scope.launch { store.setApp(cfg.copy(showContent = !cfg.showContent)) }
                }
            }
        } else if (on && cfg != null) {
            Text(
                "${cfg.mode.label} · ${if (cfg.showContent) "com texto" else "sem texto"}  (toque no nome para ajustar)",
                color = Muted, fontSize = 11.sp,
                modifier = Modifier.padding(start = 44.dp, bottom = 4.dp).clip(RoundedCornerShape(8.dp))
                    .clickableNoRipple { open = app.pkg }
            )
        }
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))
