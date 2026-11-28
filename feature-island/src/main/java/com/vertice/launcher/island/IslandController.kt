package com.vertice.launcher.island

import android.app.PendingIntent
import android.content.ComponentName
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
import com.vertice.launcher.ui.IslandSignal
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
    private val overlay = IslandOverlay(
        context,
        IslandCallbacks(
            onTap = { toggle() }, onDismiss = { dismiss() },
            onPrevious = { media.previous() }, onPlayPause = { media.playPause() }, onNext = { media.next() },
            onOpen = { openContent() }, onAction = { runAction(it) }
        )
    )
    private val media = IslandMedia(
        context, ComponentName(context, IslandNotificationListener::class.java)
    ) { e -> if (e == null) onNotificationRemoved(IslandMedia.KEY) else push(e) }
    private val system = IslandSystemEvents(context) { push(it) }

    /** PendingIntents das notificações vivas (conteúdo e ações). Limitado: nada de acumular. */
    private class Handles(val content: PendingIntent?, val actions: List<PendingIntent>)
    private val handles = object : LinkedHashMap<String, Handles>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Handles>?) = size > 20
    }

    private val _settings = MutableStateFlow(IslandSettings())
    val settings: StateFlow<IslandSettings> = _settings
    private var apps: Map<String, IslandAppConfig> = emptyMap()
    private var wake: Job? = null
    private val pending = HashMap<String, Job>()
    private var started = false

    /** O serviço de notificações está conectado e a ilha pronta? */
    val isRunning: Boolean get() = started

    private fun now() = SystemClock.elapsedRealtime()

    /** Chamado quando o serviço conecta. Idempotente. */
    fun start() {
        if (started) return
        started = true
        scope.launch {
            store.settings.collect { s ->
                _settings.value = s
                if (!s.enabled) { clear(); media.stop(); system.stop() }
                else {
                    overlay.updateLayout(s)
                    media.start(); system.start()
                    if (!s.media) onNotificationRemoved(IslandMedia.KEY)
                }
            }
        }
        scope.launch { store.apps.collect { apps = it } }
    }

    fun onNotification(raw: RawNotification, content: PendingIntent? = null, actions: List<PendingIntent> = emptyList()) {
        val s = _settings.value
        val event = IslandClassifier.classify(raw, s, apps) ?: return
        syncArbiter(s)
        handles[raw.key] = Handles(content, actions)
        if (event is IslandEvent.Notification) {
            // Rajada de mensagens da mesma conversa: espera um pouco e mostra só a última, numa animação só.
            pending.remove(event.key)?.cancel()
            pending[event.key] = scope.launch {
                delay(DEBOUNCE_MS)
                pending.remove(event.key)
                push(event)
            }
        } else push(event)
    }

    fun onNotificationRemoved(key: String) {
        pending.remove(key)?.cancel()
        handles.remove(key)
        apply(arbiter.onRemoved(key, now()))
    }

    /** Eventos que não vêm de notificação (música, carregamento, Bluetooth nas próximas etapas). */
    fun push(event: IslandEvent) {
        val s = _settings.value
        if (!s.allows(event)) return
        syncArbiter(s)
        apply(arbiter.onEvent(event, now(), s.tempMs))
    }

    private fun current(): IslandEvent? = overlayState?.currentEvent

    private fun openContent() {
        val key = current()?.key ?: return
        send(handles[key]?.content)
        dismiss()
    }

    private fun runAction(index: Int) {
        val key = current()?.key ?: return
        send(handles[key]?.actions?.getOrNull(index))
    }

    private fun send(pi: PendingIntent?) {
        try { pi?.send() } catch (_: PendingIntent.CanceledException) {}
    }

    /** Botão "Testar ilha" das configurações: funciona mesmo com a ilha desligada. */
    fun test() {
        val s = _settings.value.copy(enabled = true)
        syncArbiter(s)
        apply(arbiter.onEvent(
            IslandEvent.Notification("teste", context.packageName, "Vértice Island", "Está funcionando", false, false),
            now(), s.tempMs
        ), s)
    }

    /** As configurações de abertura automática valem já para o próximo evento. */
    private fun syncArbiter(s: IslandSettings) {
        arbiter.autoExpandEnabled = s.autoExpand
        arbiter.autoExpandMs = s.expandMs
    }

    private fun toggle() = apply(arbiter.toggleExpand(now(), _settings.value.tempMs))
    private fun dismiss() = apply(arbiter.dismiss(now()))

    private fun clear() {
        pending.values.forEach { it.cancel() }
        pending.clear()
        arbiter.clear()
        apply(IslandState.Hidden)
    }

    private var overlayState: IslandState? = null

    private fun apply(state: IslandState, s: IslandSettings = _settings.value) {
        overlayState = state
        IslandSignal.expanded = state is IslandState.Expanded
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
        media.stop(); system.stop()
        pending.values.forEach { it.cancel() }
        pending.clear()
        handles.clear()
        wake?.cancel()
        arbiter.clear()
        overlay.hide()
        IslandSignal.expanded = false
    }

    private companion object { const val DEBOUNCE_MS = 300L }
}
