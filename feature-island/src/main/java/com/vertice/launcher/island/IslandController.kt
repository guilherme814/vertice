package com.vertice.launcher.island

import android.content.Context
import android.os.SystemClock
import com.vertice.launcher.core.island.IslandAppConfig
import com.vertice.launcher.core.island.IslandArbiter
import com.vertice.launcher.core.island.IslandClassifier
import com.vertice.launcher.core.island.IslandEvent
import com.vertice.launcher.core.island.IslandSettings
import com.vertice.launcher.core.island.IslandState
import com.vertice.launcher.core.island.IslandStore
import com.vertice.launcher.core.island.RawNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Cola tudo: recebe notificações, passa pelo classificador, deixa o árbitro decidir e manda a janela mostrar ou sumir.
 * Tudo roda na thread principal (a janela exige); o trabalho é minúsculo.
 */
class IslandController(private val context: Context, private val store: IslandStore) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val arbiter = IslandArbiter()
    private val overlay = IslandOverlay(context, onTap = { toggle() }, onDismiss = { dismiss() })

    private val _settings = MutableStateFlow(IslandSettings())
    val settings: StateFlow<IslandSettings> = _settings
    private var apps: Map<String, IslandAppConfig> = emptyMap()
    private var wake: Job? = null
    private var started = false

    private fun now() = SystemClock.elapsedRealtime()

    /** Chamado quando o serviço conecta. Idempotente. */
    fun start() {
        if (started) return
        started = true
        scope.launch { store.settings.collect { s -> _settings.value = s; if (!s.enabled) clear() else overlay.updateLayout(s) } }
        scope.launch { store.apps.collect { apps = it } }
    }

    fun onNotification(raw: RawNotification) {
        val s = _settings.value
        val event = IslandClassifier.classify(raw, s, apps) ?: return
        push(event)
    }

    fun onNotificationRemoved(key: String) = apply(arbiter.onRemoved(key, now()))

    /** Eventos que não vêm de notificação (música, carregamento, Bluetooth nas próximas etapas). */
    fun push(event: IslandEvent) {
        val s = _settings.value
        if (!s.enabled) return
        apply(arbiter.onEvent(event, now(), s.tempMs))
    }

    /** Botão "Testar ilha" das configurações: funciona mesmo com a ilha desligada. */
    fun test() {
        val s = _settings.value.copy(enabled = true)
        apply(arbiter.onEvent(
            IslandEvent.Notification("teste", context.packageName, "Vértice Island", "Está funcionando", false, false),
            now(), s.tempMs
        ), s)
    }

    private fun toggle() = apply(arbiter.toggleExpand(now(), _settings.value.tempMs))
    private fun dismiss() = apply(arbiter.dismiss(now()))

    private fun clear() {
        arbiter.clear()
        apply(IslandState.Hidden)
    }

    private fun apply(state: IslandState, s: IslandSettings = _settings.value) {
        overlay.render(state, s)
        wake?.cancel()
        val at = arbiter.nextWakeAt() ?: return
        wake = scope.launch {
            delay((at - now()).coerceAtLeast(0) + 20)
            apply(arbiter.tick(now()))
        }
    }

    /** O serviço caiu: some a janela para não deixar uma cápsula "morta" na tela. */
    fun stop() {
        started = false
        wake?.cancel()
        arbiter.clear()
        overlay.hide()
    }
}
