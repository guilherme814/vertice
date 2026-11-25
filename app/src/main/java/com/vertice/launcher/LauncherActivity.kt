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
import com.vertice.launcher.core.Nav
import com.vertice.launcher.core.NavEvent
import com.vertice.launcher.core.NavState
import com.vertice.launcher.core.Screen
import com.vertice.launcher.data.AppEntry
import com.vertice.launcher.diag.VLog
import com.vertice.launcher.ui.AppDrawer
import com.vertice.launcher.ui.HomeScreen
import com.vertice.launcher.ui.QuickSheet
import com.vertice.launcher.ui.VerticeTheme
import com.vertice.launcher.ui.WallpaperBackground
import kotlin.math.roundToInt

/** A tela inicial do aparelho. `singleTask`: o botão Home volta aqui em vez de empilhar cópias. */
class LauncherActivity : ComponentActivity() {

    private val vm: LauncherViewModel by viewModels {
        LauncherViewModel.factory((application as VerticeApp).container)
    }

    /** Incrementa a cada toque no botão Home com a launcher já aberta. */
    private var homeSignal by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        lastLocales = resources.configuration.locales.toLanguageTags()

        setContent {
            val ui by vm.ui.collectAsStateWithLifecycle()
            val view = LocalView.current
            // Quem decide qual tela está à frente é a máquina de estados do domínio (Nav).
            var nav by remember { mutableStateOf(NavState()) }
            val dispatch: (NavEvent) -> Unit = { nav = Nav.reduce(nav, it) }

            // Botão Home: fecha gaveta/folha e cancela o modo mover.
            LaunchedEffect(homeSignal) { if (homeSignal > 0) dispatch(NavEvent.HomePressed) }
            BackHandler(enabled = Nav.handlesBack(nav)) { dispatch(NavEvent.Back) }

            val launch: (AppEntry, Rect?) -> Unit = { app, r ->
                val options = if (r != null) {
                    ActivityOptions.makeScaleUpAnimation(
                        view, r.left.roundToInt(), r.top.roundToInt(), r.width.roundToInt(), r.height.roundToInt()
                    ).toBundle()
                } else null
                vm.openApp(app, options)
                dispatch(NavEvent.AppLaunched)
            }

            VerticeTheme {
                Box(Modifier.fillMaxSize()) {
                    if (!ui.wallpaper.isSystem) WallpaperBackground(ui.wallpaper, Modifier.fillMaxSize())
                    HomeScreen(
                        vm = vm, ui = ui, onLaunch = launch,
                        moving = ui.apps.firstOrNull { it.key == nav.moving },
                        onMoveStart = { dispatch(NavEvent.StartMove(it.key)) },
                        onMoveEnd = { dispatch(NavEvent.EndMove) },
                        onOpenDrawer = { dispatch(NavEvent.OpenDrawer) }, onOpenSearch = { dispatch(NavEvent.OpenSearch) }, onOpenSheet = { dispatch(NavEvent.OpenSheet) }
                    )

                    // Perfil Ultra Lite: sem animação nenhuma (sem custo de GPU em aparelho fraco).
                    val animate = ui.profile.animations
                    AnimatedVisibility(
                        visible = nav.screen == Screen.Drawer,
                        enter = if (animate) slideInVertically { it / 6 } + fadeIn() else EnterTransition.None,
                        exit = if (animate) slideOutVertically { it / 6 } + fadeOut() else ExitTransition.None
                    ) {
                        AppDrawer(vm = vm, ui = ui, focusSearch = nav.focusSearch, onLaunch = launch, onClose = { dispatch(NavEvent.Back) })
                    }

                    AnimatedVisibility(
                        visible = nav.screen == Screen.Sheet,
                        enter = if (animate) slideInVertically { it / 3 } + fadeIn() else EnterTransition.None,
                        exit = if (animate) slideOutVertically { it / 3 } + fadeOut() else ExitTransition.None
                    ) {
                        QuickSheet(vm = vm, ui = ui, onClose = { dispatch(NavEvent.Back) })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val home = intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)
        VLog.i("Home", "onNewIntent action=${intent.action} home=$home")
        if (home) homeSignal++
    }

    private var lastLocales: String? = null

    /** Idioma trocado com a launcher aberta (a Activity trata `locale` sozinha): os nomes dos apps precisam ser relidos. */
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        val now = newConfig.locales.toLanguageTags()
        if (lastLocales != null && lastLocales != now) {
            VLog.i("Apps", "Idioma mudou ($lastLocales -> $now): recarregando nomes dos apps")
            vm.refreshApps()
        }
        lastLocales = now
    }

    override fun onPause() {
        vm.logIconStats()
        super.onPause()
    }
}
