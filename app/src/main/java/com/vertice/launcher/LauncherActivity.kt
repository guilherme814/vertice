package com.vertice.launcher

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vertice.launcher.data.AppEntry
import com.vertice.launcher.ui.AppDrawer
import com.vertice.launcher.ui.HomeScreen
import com.vertice.launcher.ui.VerticeTheme
import kotlin.math.roundToInt

/** A tela inicial do aparelho. `singleTask`: o botão Home volta aqui em vez de empilhar cópias. */
class LauncherActivity : ComponentActivity() {

    private val vm: LauncherViewModel by viewModels()

    /** Incrementa a cada toque no botão Home com a launcher já aberta. */
    private var homeSignal by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val ui by vm.ui.collectAsStateWithLifecycle()
            val view = LocalView.current
            var drawerOpen by remember { mutableStateOf(false) }

            // Botão Home: fecha a gaveta se estiver aberta.
            LaunchedEffect(homeSignal) { if (homeSignal > 0) drawerOpen = false }
            BackHandler(enabled = drawerOpen) { drawerOpen = false }

            val launch: (AppEntry, Rect?) -> Unit = { app, r ->
                val options = if (r != null) {
                    ActivityOptions.makeScaleUpAnimation(
                        view, r.left.roundToInt(), r.top.roundToInt(), r.width.roundToInt(), r.height.roundToInt()
                    ).toBundle()
                } else null
                vm.openApp(app, options)
                drawerOpen = false
            }

            VerticeTheme {
                Box(Modifier.fillMaxSize()) {
                    HomeScreen(vm = vm, ui = ui, onLaunch = launch, onOpenDrawer = { drawerOpen = true })

                    // Perfil Ultra Lite: sem animação nenhuma (sem custo de GPU em aparelho fraco).
                    val animate = ui.profile.animations
                    AnimatedVisibility(
                        visible = drawerOpen,
                        enter = if (animate) slideInVertically { it / 6 } + fadeIn() else EnterTransition.None,
                        exit = if (animate) slideOutVertically { it / 6 } + fadeOut() else ExitTransition.None
                    ) {
                        AppDrawer(vm = vm, ui = ui, onLaunch = launch, onClose = { drawerOpen = false })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) homeSignal++
    }
}
