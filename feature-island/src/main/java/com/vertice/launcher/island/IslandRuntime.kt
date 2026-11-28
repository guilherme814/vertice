package com.vertice.launcher.island

import android.content.Context
import com.vertice.launcher.core.island.IslandStore

/**
 * Ponte entre o app e o serviço da ilha. O serviço é criado pelo sistema (não por nós), então ele pega o que precisa daqui.
 * O Application chama [install] logo ao nascer, antes de qualquer serviço do mesmo processo.
 */
object IslandRuntime {
    @Volatile private var controller: IslandController? = null

    fun install(context: Context, store: IslandStore) {
        if (controller == null) controller = IslandController(context.applicationContext, store)
    }

    /** Nulo só se o Application ainda não rodou (não deve acontecer). */
    fun controller(): IslandController? = controller
}
