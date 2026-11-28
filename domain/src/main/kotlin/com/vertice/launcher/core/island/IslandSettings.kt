package com.vertice.launcher.core.island

/** Fundo da cápsula: preto (HyperOS), grafite ou vidro (preto levemente translúcido). */
enum class IslandBackground(val label: String, val argb: Long, val alpha: Float) {
    Black("Preto", 0xFF000000, 1f),
    Graphite("Grafite", 0xFF1C1F26, 1f),
    Glass("Vidro", 0xFF000000, 0.82f);

    companion object {
        fun fromName(name: String?) = entries.firstOrNull { it.name == name } ?: Black
    }
}

enum class IslandWidth(val label: String, val dp: Int) {
    Compact("Compacta", 150),
    Medium("Média", 200),
    Wide("Larga", 260);

    companion object {
        fun fromName(name: String?) = entries.firstOrNull { it.name == name } ?: Medium
    }
}

/** Visual da ilha. Só o HyperOS existe na v1; o campo já está no modelo para o Vértice Studio. */
enum class IslandTheme(val label: String) {
    HyperOS("HyperOS");

    companion object {
        fun fromName(name: String?) = entries.firstOrNull { it.name == name } ?: HyperOS
    }
}

data class IslandSettings(
    val enabled: Boolean = false,
    val media: Boolean = true,
    val calls: Boolean = true,
    val timers: Boolean = true,
    val charging: Boolean = true,
    /** Aviso de Bluetooth conectado (exige a permissão BLUETOOTH_CONNECT para mostrar o nome). */
    val bluetooth: Boolean = false,
    /** Transição de tamanho da cápsula. Desligar poupa GPU em aparelho fraco. */
    val animate: Boolean = true,
    /** Quanto tempo uma notificação temporária fica na tela. */
    val tempSeconds: Int = 5,
    /** Distância do topo da tela (dp). */
    val offsetDp: Int = 8,
    val width: IslandWidth = IslandWidth.Medium,
    val theme: IslandTheme = IslandTheme.HyperOS,
    val background: IslandBackground = IslandBackground.Black,
    /** Mensagem nova abre o painel sozinha. */
    val autoExpand: Boolean = true,
    /** Quanto tempo o painel fica aberto sozinho. */
    val expandSeconds: Int = 4
) {
    fun sanitized() = copy(
        tempSeconds = tempSeconds.coerceIn(TEMP_MIN, TEMP_MAX),
        offsetDp = offsetDp.coerceIn(OFFSET_MIN, OFFSET_MAX),
        expandSeconds = expandSeconds.coerceIn(EXPAND_MIN, EXPAND_MAX)
    )

    val expandMs: Long get() = sanitized().expandSeconds * 1000L

    val tempMs: Long get() = sanitized().tempSeconds * 1000L

    /** O interruptor do tipo de evento está ligado? (Notificações dependem do Gerenciador, não daqui.) */
    fun allows(event: IslandEvent): Boolean = enabled && when (event) {
        is IslandEvent.Media -> media
        is IslandEvent.Call -> calls
        is IslandEvent.Timer -> timers
        is IslandEvent.Charging -> charging
        is IslandEvent.Bluetooth -> bluetooth
        is IslandEvent.Notification -> true
    }

    companion object {
        const val TEMP_MIN = 2
        const val TEMP_MAX = 10
        const val OFFSET_MIN = 0
        const val OFFSET_MAX = 60
        const val EXPAND_MIN = 2
        const val EXPAND_MAX = 8
    }
}

enum class IslandMode(val label: String) {
    Temporary("Temporária"),
    Persistent("Persistente");

    companion object {
        fun fromName(name: String?) = entries.firstOrNull { it.name == name } ?: Temporary
    }
}

/** Configuração de um app no Gerenciador da Ilha. App ausente da lista = desligado. */
data class IslandAppConfig(
    val packageName: String,
    val enabled: Boolean = true,
    val mode: IslandMode = IslandMode.Temporary,
    val showContent: Boolean = true
)

/** Apps sugeridos no Gerenciador (nunca ligados sozinhos). */
object IslandApps {
    val SUGGESTED: List<Pair<String, String>> = listOf(
        "com.whatsapp" to "WhatsApp",
        "com.whatsapp.w4b" to "WhatsApp Business",
        "com.instagram.android" to "Instagram",
        "org.telegram.messenger" to "Telegram"
    )
}

/** Texto simples, uma linha por app: `pacote|ligado|modo|conteudo`. Linhas inválidas são ignoradas. */
object IslandAppsCodec {
    fun encode(apps: Collection<IslandAppConfig>): String =
        apps.joinToString("\n") { "${it.packageName}|${it.enabled}|${it.mode.name}|${it.showContent}" }

    fun decode(text: String?): Map<String, IslandAppConfig> {
        if (text.isNullOrBlank()) return emptyMap()
        val out = LinkedHashMap<String, IslandAppConfig>()
        text.lineSequence().forEach { line ->
            val p = line.split('|')
            if (p.size != 4 || p[0].isBlank() || p[0].length > 200) return@forEach
            out[p[0]] = IslandAppConfig(p[0], p[1] == "true", IslandMode.fromName(p[2]), p[3] == "true")
        }
        return out
    }
}
