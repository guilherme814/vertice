package com.vertice.launcher.core.island

/**
 * Máquina de estados da ilha: recebe eventos e o relógio (sempre passado de fora, para testar) e diz o que mostrar.
 *
 * - Maior prioridade ganha; empate = o mais recente; temporários expiram; persistentes ficam até serem removidos.
 * - O mesmo [IslandEvent.key] atualiza em vez de duplicar (mensagem nova na mesma conversa = atualização no lugar).
 * - Mensagem nova abre o painel sozinha por [autoExpandMs] e depois volta à cápsula.
 * - Trava de foco: uma conversa recém-aberta não é interrompida por outra por [holdMs]; só chamada fura a trava.
 * - Fila limitada.
 */
class IslandArbiter(
    private val maxEntries: Int = 6,
    /** Duração da abertura automática; o controlador ajusta conforme as configurações. */
    var autoExpandMs: Long = AUTO_EXPAND_MS,
    /** Desligado: mensagem nova só mostra a cápsula, sem abrir o painel. */
    var autoExpandEnabled: Boolean = true,
    private val holdMs: Long = HOLD_MS
) {

    private class Entry(
        var event: IslandEvent,
        var expiresAt: Long?,
        var seq: Long,
        var dismissed: Boolean,
        val shownAt: Long,
        var wantsExpand: Boolean
    )

    private val entries = LinkedHashMap<String, Entry>()
    private var seq = 0L
    private var clock = 0L
    private var expandedKey: String? = null
    /** Preenchido só quando o painel abriu sozinho (mensagem nova); aberto por toque fica nulo. */
    private var autoExpandUntil: Long? = null
    private var holdKey: String? = null
    private var holdUntil = 0L

    fun onEvent(event: IslandEvent, now: Long, tempMs: Long): IslandState {
        expire(now)
        clock = now
        val prev = top()
        val isMessage = event is IslandEvent.Notification
        val opens = isMessage && autoExpandEnabled
        val expiry = when {
            event.persistent -> null
            // Mensagem precisa durar mais que a abertura automática, senão some antes de recolher.
            opens -> now + maxOf(tempMs, autoExpandMs + 1200)
            else -> now + tempMs
        }
        val old = entries[event.key]
        if (old == null) {
            entries[event.key] = Entry(event, expiry, ++seq, false, now, opens)
            trim()
        } else {
            // Conteúdo novo traz a ilha de volta mesmo que tenha sido dispensada; conteúdo igual, não.
            if (old.event != event) old.dismissed = false
            if (!event.persistent) old.dismissed = false
            old.event = event
            old.expiresAt = expiry
            old.seq = ++seq
            old.wantsExpand = opens
        }
        if (event.priority == IslandPriority.Call) holdKey = null
        else if (isMessage && prev != null && prev.event is IslandEvent.Notification &&
            prev.event.key != event.key && now < prev.shownAt + holdMs
        ) {
            holdKey = prev.event.key
            holdUntil = prev.shownAt + holdMs
        }
        return state(now)
    }

    fun onRemoved(key: String, now: Long): IslandState {
        entries.remove(key)
        if (expandedKey == key) { expandedKey = null; autoExpandUntil = null }
        if (holdKey == key) holdKey = null
        return state(now)
    }

    fun tick(now: Long): IslandState = state(now)

    /** Toque na cápsula: abre o painel; de novo, fecha. Aberto à mão, fica até fechar (ou o evento acabar). */
    fun toggleExpand(now: Long, tempMs: Long): IslandState {
        expire(now)
        clock = now
        val top = top() ?: return IslandState.Hidden
        val k = top.event.key
        if (expandedKey == k) { expandedKey = null; autoExpandUntil = null }
        else {
            expandedKey = k
            autoExpandUntil = null
            top.wantsExpand = false
            // Aberto, um temporário não some debaixo do dedo.
            top.expiresAt?.let { top.expiresAt = maxOf(it, now + tempMs * 2) }
        }
        return state(now)
    }

    fun collapse(now: Long): IslandState { expandedKey = null; autoExpandUntil = null; return state(now) }

    /** Dispensar: some a cápsula desse evento sem cancelar a atividade (a música continua tocando). */
    fun dismiss(now: Long): IslandState {
        expire(now)
        clock = now
        top()?.let {
            it.dismissed = true
            it.wantsExpand = false
            if (expandedKey == it.event.key) { expandedKey = null; autoExpandUntil = null }
        }
        return state(now)
    }

    /** Quando chamar [tick] de novo (ms), ou null se nada vai mudar sozinho. */
    fun nextWakeAt(): Long? {
        val times = ArrayList<Long>(3)
        entries.values.filter { !it.dismissed }.mapNotNullTo(times) { it.expiresAt }
        autoExpandUntil?.let { times += it }
        if (holdKey != null && holdUntil > clock) times += holdUntil
        return times.minOrNull()
    }

    fun size(): Int = entries.size

    fun clear() {
        entries.clear(); expandedKey = null; autoExpandUntil = null; holdKey = null
    }

    private fun expire(now: Long) {
        val it = entries.values.iterator()
        while (it.hasNext()) {
            val e = it.next()
            if (e.expiresAt != null && e.expiresAt!! <= now) {
                if (expandedKey == e.event.key) { expandedKey = null; autoExpandUntil = null }
                if (holdKey == e.event.key) holdKey = null
                it.remove()
            }
        }
        // Abertura automática terminou: volta à cápsula.
        if (autoExpandUntil != null && autoExpandUntil!! <= now) { expandedKey = null; autoExpandUntil = null }
        if (holdKey != null && now >= holdUntil) holdKey = null
    }

    private fun trim() {
        while (entries.size > maxEntries) {
            // Sai o de menor prioridade; entre iguais, o mais antigo.
            val victim = entries.values.minWith(compareBy<Entry>({ it.event.priority }, { it.seq }))
            entries.remove(victim.event.key)
            if (expandedKey == victim.event.key) { expandedKey = null; autoExpandUntil = null }
            if (holdKey == victim.event.key) holdKey = null
        }
    }

    private fun top(): Entry? {
        val hk = holdKey
        if (hk != null && clock < holdUntil) {
            entries[hk]?.takeIf { !it.dismissed }?.let { return it }
        }
        return entries.values.filter { !it.dismissed }.maxWithOrNull(compareBy<Entry>({ it.event.priority }, { it.seq }))
    }

    private fun state(now: Long): IslandState {
        expire(now)
        clock = now
        val t = top() ?: return IslandState.Hidden
        // Mensagem nova que acabou de virar o topo: abre sozinha (ou só renova, se já está aberta no mesmo evento).
        if (t.wantsExpand && t.event is IslandEvent.Notification) {
            t.wantsExpand = false
            if (expandedKey == t.event.key) {
                if (autoExpandUntil != null) autoExpandUntil = now + autoExpandMs   // atualização no lugar
            } else {
                expandedKey = t.event.key
                autoExpandUntil = now + autoExpandMs
            }
        }
        return if (expandedKey == t.event.key) IslandState.Expanded(t.event) else IslandState.Compact(t.event)
    }

    companion object {
        /** Quanto tempo o painel de mensagem fica aberto sozinho. */
        const val AUTO_EXPAND_MS = 3800L
        /** Janela em que uma conversa recém-aberta não é interrompida por outra. */
        const val HOLD_MS = 1800L
    }
}
