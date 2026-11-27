package com.vertice.launcher.core

/** Qual tela está à frente. A Home está sempre por baixo. */
enum class Screen { Home, Drawer, Sheet }

/** Estado de navegação da launcher: a tela da frente e, se houver, o app que está sendo movido. */
data class NavState(
    val screen: Screen = Screen.Home,
    val moving: String? = null,
    /** A gaveta abriu para buscar: o campo de busca já nasce focado. */
    val focusSearch: Boolean = false
)

sealed interface NavEvent {
    data object OpenDrawer : NavEvent
    data object OpenSheet : NavEvent
    /** Deslizar para baixo na Home: abre a gaveta já com a busca focada. */
    data object OpenSearch : NavEvent
    /** Botão Home com a launcher já aberta. */
    data object HomePressed : NavEvent
    data object Back : NavEvent
    /** Um app foi aberto (a gaveta fecha). */
    data object AppLaunched : NavEvent
    data class StartMove(val key: String) : NavEvent
    /** Soltou no destino ou cancelou. */
    data object EndMove : NavEvent
}

/**
 * Máquina de estados das telas. Uma função pura decide tudo (gaveta, folha, modo mover, botões
 * Home e Voltar), então o comportamento é previsível e testável — a tela só mostra o resultado.
 */
object Nav {

    fun reduce(s: NavState, e: NavEvent): NavState = when (e) {
        NavEvent.OpenDrawer -> if (s.screen == Screen.Home && s.moving == null) s.copy(screen = Screen.Drawer, focusSearch = false) else s
        NavEvent.OpenSearch -> if (s.screen == Screen.Home && s.moving == null) s.copy(screen = Screen.Drawer, focusSearch = true) else s
        NavEvent.OpenSheet -> if (s.screen == Screen.Home && s.moving == null) s.copy(screen = Screen.Sheet) else s
        NavEvent.HomePressed -> NavState()
        NavEvent.Back -> when {
            s.moving != null -> s.copy(moving = null)
            s.screen != Screen.Home -> s.copy(screen = Screen.Home, focusSearch = false)
            else -> s
        }
        NavEvent.AppLaunched -> NavState()
        is NavEvent.StartMove -> if (s.screen == Screen.Home) s.copy(moving = e.key) else s
        NavEvent.EndMove -> s.copy(moving = null)
    }

    /** O botão Voltar só é nosso quando há algo para fechar; na Home pura o sistema decide. */
    fun handlesBack(s: NavState): Boolean = s.moving != null || s.screen != Screen.Home
}
