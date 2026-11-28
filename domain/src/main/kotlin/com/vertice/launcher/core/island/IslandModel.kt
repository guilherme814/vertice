package com.vertice.launcher.core.island

/**
 * Retrato de uma notificação, sem nada de Android. O conteúdo (título/texto) só existe na memória:
 * por isso o toString o esconde, para nunca escapar para um log por acidente.
 */
data class RawNotification(
    val key: String,
    val packageName: String,
    val category: String? = null,
    val isOngoing: Boolean = false,
    val isGroupSummary: Boolean = false,
    val isSilent: Boolean = false,
    val isSecret: Boolean = false,
    /** Início do cronômetro (ms), se a notificação mostra um. */
    val chronometerBase: Long? = null,
    val hasCallActions: Boolean = false,
    /** Rótulos das ações (ex.: "Recusar", "Atender"). Só os nomes: a ação em si fica no Android. */
    val actions: List<String> = emptyList(),
    val title: String? = null,
    val text: String? = null
) {
    override fun toString() = "RawNotification(pacote=$packageName, categoria=$category)"

    companion object {
        const val CATEGORY_CALL = "call"
        const val CATEGORY_ALARM = "alarm"
    }
}

/** Prioridade na ilha: maior número ganha a cápsula. */
enum class IslandPriority { Low, Notification, Media, Timer, Call }

/** Evento já normalizado, pronto para a ilha mostrar. [key] identifica o mesmo evento ao longo do tempo. */
sealed interface IslandEvent {
    val key: String
    val priority: IslandPriority

    /** Fica na tela enquanto a atividade existir (música, chamada, timer). */
    val persistent: Boolean

    data class Notification(
        override val key: String,
        val packageName: String,
        val title: String?,
        val text: String?,
        val contentHidden: Boolean,
        override val persistent: Boolean
    ) : IslandEvent {
        override val priority get() = IslandPriority.Notification
        override fun toString() = "Notification(pacote=$packageName, oculto=$contentHidden)"
    }

    data class Call(
        override val key: String,
        val packageName: String,
        val caller: String?,
        /** Já em andamento (cronômetro) em vez de tocando. */
        val inProgress: Boolean,
        val chronometerBase: Long?,
        val actions: List<String> = emptyList()
    ) : IslandEvent {
        override val priority get() = IslandPriority.Call
        override val persistent get() = true
        override fun toString() = "Call(pacote=$packageName, emAndamento=$inProgress)"
    }

    data class Timer(
        override val key: String,
        val packageName: String,
        val label: String?,
        val chronometerBase: Long?
    ) : IslandEvent {
        override val priority get() = IslandPriority.Timer
        override val persistent get() = true
        override fun toString() = "Timer(pacote=$packageName)"
    }

    data class Media(
        override val key: String,
        val packageName: String,
        val title: String?,
        val artist: String?,
        val playing: Boolean
    ) : IslandEvent {
        override val priority get() = IslandPriority.Media
        override val persistent get() = playing
        override fun toString() = "Media(pacote=$packageName, tocando=$playing)"
    }

    data class Charging(override val key: String = "charging", val percent: Int) : IslandEvent {
        override val priority get() = IslandPriority.Low
        override val persistent get() = false
    }

    data class Bluetooth(override val key: String, val deviceName: String?) : IslandEvent {
        override val priority get() = IslandPriority.Low
        override val persistent get() = false
        override fun toString() = "Bluetooth()"
    }
}

/** O que a ilha mostra agora. */
sealed interface IslandState {
    data object Hidden : IslandState
    data class Compact(val event: IslandEvent) : IslandState
    data class Expanded(val event: IslandEvent) : IslandState

    val currentEvent: IslandEvent?
        get() = when (this) {
            Hidden -> null
            is Compact -> event
            is Expanded -> event
        }
}
