package com.vertice.launcher.island

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import com.vertice.launcher.core.island.IslandEvent

/**
 * Música: observa as sessões de mídia ativas (precisa do acesso a notificações, que já é exigido pela ilha).
 * Um único "espaço" de mídia: a sessão tocando agora ganha; se nenhuma toca, a mais recente com título.
 */
class IslandMedia(
    private val context: Context,
    private val component: ComponentName,
    private val onChange: (IslandEvent.Media?) -> Unit
) {
    private val main = Handler(Looper.getMainLooper())
    private val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val callbacks = HashMap<MediaController, MediaController.Callback>()
    private var current: MediaController? = null
    private var started = false

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list -> track(list.orEmpty()) }

    fun start() {
        if (started) return
        try {
            manager.addOnActiveSessionsChangedListener(sessionsListener, component, main)
            started = true
            track(manager.getActiveSessions(component))
        } catch (_: SecurityException) {
            // Acesso a notificações ainda não concedido: tenta de novo quando o serviço reconectar.
        }
    }

    fun stop() {
        if (!started) return
        started = false
        try { manager.removeOnActiveSessionsChangedListener(sessionsListener) } catch (_: Throwable) {}
        callbacks.forEach { (c, cb) -> try { c.unregisterCallback(cb) } catch (_: Throwable) {} }
        callbacks.clear()
        current = null
    }

    fun previous() { current?.transportControls?.skipToPrevious() }
    fun next() { current?.transportControls?.skipToNext() }
    fun playPause() {
        val c = current ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play()
    }

    private fun track(list: List<MediaController>) {
        callbacks.keys.filter { it !in list }.forEach { c ->
            callbacks.remove(c)?.let { try { c.unregisterCallback(it) } catch (_: Throwable) {} }
        }
        list.forEach { c ->
            if (c !in callbacks) {
                val cb = object : MediaController.Callback() {
                    override fun onPlaybackStateChanged(state: PlaybackState?) = emit()
                    override fun onMetadataChanged(metadata: MediaMetadata?) = emit()
                    override fun onSessionDestroyed() = emit()
                }
                callbacks[c] = cb
                try { c.registerCallback(cb, main) } catch (_: Throwable) {}
            }
        }
        emit()
    }

    private fun emit() {
        val all = callbacks.keys.toList()
        val best = all.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: all.firstOrNull { it.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) != null }
        current = best
        val title = best?.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        if (best == null || title == null) { onChange(null); return }
        val artist = best.metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
        onChange(
            IslandEvent.Media(
                key = KEY, packageName = best.packageName, title = title.take(120), artist = artist?.take(120),
                playing = best.playbackState?.state == PlaybackState.STATE_PLAYING
            )
        )
    }

    companion object { const val KEY = "media" }
}
