package com.vertice.launcher.island.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vertice.launcher.core.island.IslandSettings
import com.vertice.launcher.core.island.IslandStore
import com.vertice.launcher.core.island.IslandWidth
import com.vertice.launcher.island.IslandRuntime
import com.vertice.launcher.ui.CategoryChip
import com.vertice.launcher.ui.Ink
import com.vertice.launcher.ui.Mint
import com.vertice.launcher.ui.Muted
import com.vertice.launcher.ui.PanelSoft
import com.vertice.launcher.ui.Snow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Conteúdo da aba "Ilha": checklist de permissões, interruptor, teste e ajustes básicos. */
@Composable
fun IslandSettingsScreen(store: IslandStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s by store.settings.collectAsState(initial = IslandSettings())

    // Reavalia as permissões ao voltar das telas de configuração do Android.
    var tick by remember { mutableStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val listenerOk = remember(tick) { NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName) }
    val overlayOk = remember(tick) { Settings.canDrawOverlays(context) }

    fun save(next: IslandSettings) { scope.launch { store.setSettings(next) } }

    Label("Permissões")
    Check("Acesso a notificações", listenerOk, "Para saber quando chega algo.") {
        open(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }
    Check("Exibir sobre outros apps", overlayOk, "Para desenhar a cápsula por cima dos apps.") {
        open(context, Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
    }
    Text(
        "Em Xiaomi/HyperOS, deixe também o início automático e a bateria \"sem restrições\" para a Vértice, " +
            "ou o sistema pode desligar o serviço.",
        color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)
    )

    val running = IslandRuntime.controller()?.isRunning == true
    Text(
        "Serviço: " + if (running) "conectado" else if (listenerOk) "aguardando o sistema conectar" else "parado (falta o acesso a notificações)",
        color = if (running) Mint else Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp)
    )
    if (listenerOk && !running) {
        CategoryChip("Reconectar serviço", false) {
            try {
                android.service.notification.NotificationListenerService.requestRebind(
                    android.content.ComponentName(context, com.vertice.launcher.island.IslandNotificationListener::class.java)
                )
            } catch (_: Throwable) {}
            tick++
        }
        Spacer(Modifier.height(8.dp))
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Vértice Island", color = Snow, fontSize = 15.sp)
        Switch(
            checked = s.enabled, onCheckedChange = { save(s.copy(enabled = it)) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Ink, checkedTrackColor = Mint,
                uncheckedThumbColor = Muted, uncheckedTrackColor = PanelSoft
            )
        )
    }
    Spacer(Modifier.height(8.dp))
    CategoryChip("Testar ilha", false) { IslandRuntime.controller()?.apply { start(); test() } }

    Spacer(Modifier.height(14.dp))
    Label("Mostrar na ilha")
    Toggle("Música", s.media) { save(s.copy(media = it)) }
    Toggle("Chamadas", s.calls) { save(s.copy(calls = it)) }
    Toggle("Timer e cronômetro", s.timers) { save(s.copy(timers = it)) }
    Toggle("Carregamento", s.charging) { save(s.copy(charging = it)) }
    Toggle("Animação da cápsula", s.animate) { save(s.copy(animate = it)) }
    val btPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { /* o aviso funciona com ou sem o nome */ }
    Toggle("Bluetooth conectado", s.bluetooth) { on ->
        save(s.copy(bluetooth = on))
        if (on && Build.VERSION.SDK_INT >= 31 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) btPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
    }
    Text("Chamadas e timers vêm das notificações do app de telefone/relógio. Bluetooth sem a permissão mostra só \"Bluetooth conectado\".", color = Muted, fontSize = 11.sp)

    Spacer(Modifier.height(14.dp))
    Label("Apps")
    var manager by remember { mutableStateOf(false) }
    CategoryChip(if (manager) "Fechar Gerenciador da Ilha" else "Abrir Gerenciador da Ilha", manager) { manager = !manager }
    if (manager) {
        Spacer(Modifier.height(10.dp))
        IslandAppManager(store)
    }

    Spacer(Modifier.height(14.dp))
    Label("Largura")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IslandWidth.entries.forEach { w -> CategoryChip(w.label, s.width == w) { save(s.copy(width = w)) } }
    }
    Spacer(Modifier.height(8.dp))
    Slide("Distância do topo · ${s.offsetDp} dp", s.offsetDp, IslandSettings.OFFSET_MIN, IslandSettings.OFFSET_MAX) { save(s.copy(offsetDp = it)) }
    Slide("Tempo na tela · ${s.tempSeconds} s", s.tempSeconds, IslandSettings.TEMP_MIN, IslandSettings.TEMP_MAX) { save(s.copy(tempSeconds = it)) }
}

private fun open(context: Context, intent: Intent) {
    try { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Throwable) {}
}

@Composable
private fun Toggle(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, color = Snow, fontSize = 14.sp)
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Ink, checkedTrackColor = Mint,
                uncheckedThumbColor = Muted, uncheckedTrackColor = PanelSoft
            )
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(text, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp, top = 4.dp))
}

@Composable
private fun Check(title: String, ok: Boolean, why: String, onFix: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onFix).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Snow, fontSize = 15.sp)
            Text(why, color = Muted, fontSize = 11.sp)
        }
        Text(if (ok) "✓ ativo" else "Ativar ›", color = if (ok) Mint else Snow, fontSize = 13.sp)
    }
}

@Composable
private fun Slide(label: String, value: Int, min: Int, max: Int, onDone: (Int) -> Unit) {
    var live by remember(value) { mutableStateOf(value.toFloat()) }
    Label(label)
    Slider(
        value = live, onValueChange = { live = it }, onValueChangeFinished = { onDone(live.roundToInt()) },
        valueRange = min.toFloat()..max.toFloat(),
        colors = SliderDefaults.colors(thumbColor = Mint, activeTrackColor = Mint, inactiveTrackColor = PanelSoft)
    )
}
