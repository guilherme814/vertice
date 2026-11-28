package com.vertice.launcher.island

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.vertice.launcher.core.island.IslandSettings
import com.vertice.launcher.core.island.IslandState
import com.vertice.launcher.island.ui.IslandHost

/**
 * Dona da janela flutuante. Regra de ouro: com a ilha oculta NÃO existe janela (custo zero em repouso).
 * A janela tem o tamanho da cápsula, nunca a tela inteira, e não rouba o foco do app que está por baixo.
 */
class IslandOverlay(
    private val context: Context,
    private val callbacks: IslandCallbacks
) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val state = mutableStateOf<IslandState>(IslandState.Hidden)
    private val settings = mutableStateOf(IslandSettings())

    private var view: ComposeView? = null
    private var owner: Owner? = null

    /** Dono de ciclo de vida mínimo: o Compose precisa de um fora de uma Activity. Um novo a cada exibição. */
    private class Owner : LifecycleOwner, SavedStateRegistryOwner {
        private val registry = LifecycleRegistry(this)
        private val saved = SavedStateRegistryController.create(this)
        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = saved.savedStateRegistry
        fun create() { saved.performAttach(); saved.performRestore(null); registry.currentState = Lifecycle.State.RESUMED }
        fun destroy() { registry.currentState = Lifecycle.State.DESTROYED }
    }

    fun canDraw(): Boolean = Settings.canDrawOverlays(context)

    fun render(newState: IslandState, s: IslandSettings) {
        settings.value = s
        state.value = newState
        if (newState is IslandState.Hidden) { hide(); return }
        if (view == null) show(s)
    }

    fun updateLayout(s: IslandSettings) {
        settings.value = s
        val v = view ?: return
        try { wm.updateViewLayout(v, params(s)) } catch (_: Throwable) {}
    }

    private fun params(s: IslandSettings): WindowManager.LayoutParams {
        val density = context.resources.displayMetrics.density
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (s.sanitized().offsetDp * density).toInt()
            title = "Vértice Island"
        }
    }

    private fun show(s: IslandSettings) {
        if (!canDraw()) return   // sem a permissão de sobreposição não há o que desenhar
        val o = Owner().also { it.create() }
        val v = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(o)
            setViewTreeSavedStateRegistryOwner(o)
            setContent { IslandHost(state.value, settings.value, callbacks) }
        }
        try {
            wm.addView(v, params(s))
            view = v
            owner = o
        } catch (_: Throwable) {
            o.destroy()
        }
    }

    fun hide() {
        val v = view ?: return
        view = null
        try { wm.removeView(v) } catch (_: Throwable) {}
        owner?.destroy()
        owner = null
    }
}
