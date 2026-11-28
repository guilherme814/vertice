package com.vertice.launcher.core.island

/**
 * Máquina de estados da ilha: recebe eventos e o relógio (sempre passado de fora, para testar) e diz o que mostrar.
 * Regras: maior prioridade ganha; empate = o mais recente; temporários expiram; persistentes ficam até serem removidos;
 * fila limitada; o mesmo [IslandEvent.key] atualiza em vez de duplicar.
 */
class IslandArbiter(private val maxEntries: Int = 6) {

    private class Entry(var event: IslandEvent, var expiresAt: Long?, var seq: Long, var dismissed: Boolean)

    private val entries = LinkedHashMap<String, Entry>()
    private var seq = 0L
    private var expandedKey: String? = null

    fun onEvent(event: IslandEvent, now: Long, tempMs: Long): IslandState {
        expire(now)
        val old = entries[event.key]
        val expiry = if (event.persistent) null else now + tempMs
        if (old == null) {
            entries[event.key] = Entry(event, expiry, ++seq, false)
            trim()
        } else {
            // Conteúdo novo traz a ilha de volta mesmo que tenha sido dispensada; conteúdo igual, não.
            if (old.event != event) old.dismissed = false
            if (!event.persistent) old.dismissed = false
            old.event = event
            old.expiresAt = expiry
            old.seq = ++seq
        }
        return state(now)
    }

    fun onRemoved(key: String, now: Long): IslandState {
        entries.remove(key)
        if (expandedKey == key) expandedKey = null
        return state(now)
    }

    fun tick(now: Long): IslandState = state(now)

    /** Toque na cápsula: abre o painel; de novo, fecha. */
    fun toggleExpand(now: Long, tempMs: Long): IslandState {
        expire(now)
        val top = top() ?: return IslandState.Hidden
        val k = top.event.key
        if (expandedKey == k) expandedKey = null
        else {
            expandedKey = k
            // Aberto, um temporário não some debaixo do dedo.
            if (top.expiresAt != null) top.expiresAt = maxOf(top.expiresAt!!, now + tempMs * 2)
        }
        return state(now)
    }

    fun collapse(now: Long): IslandState { expandedKey = null; return state(now) }

    /** Dispensar: some a cápsula desse evento sem cancelar a atividade (a música continua tocando). */
    fun dismiss(now: Long): IslandState {
        expire(now)
        top()?.let { it.dismissed = true; if (expandedKey == it.event.key) expandedKey = null }
        return state(now)
    }

    /** Quando chamar [tick] de novo (ms), ou null se nada vai expirar sozinho. */
    fun nextWakeAt(): Long? = entries.values.filter { !it.dismissed }.mapNotNull { it.expiresAt }.minOrNull()

    fun size(): Int = entries.size

    fun clear() { entries.clear(); expandedKey = null }

    private fun expire(now: Long) {
        val it = entries.values.iterator()
        while (it.hasNext()) {
            val e = it.next()
            if (e.expiresAt != null && e.expiresAt!! <= now) {
                if (expandedKey == e.event.key) expandedKey = null
                it.remove()
            }
        }
    }

    private fun trim() {
        while (entries.size > maxEntries) {
            // Sai o de menor prioridade; entre iguais, o mais antigo.
            val victim = entries.values.minWith(compareBy<Entry>({ it.event.priority }, { it.seq }))
            entries.remove(victim.event.key)
            if (expandedKey == victim.event.key) expandedKey = null
        }
    }

    private fun top(): Entry? =
        entries.values.filter { !it.dismissed }.maxWithOrNull(compareBy<Entry>({ it.event.priority }, { it.seq }))

    private fun state(now: Long): IslandState {
        expire(now)
        val t = top() ?: return IslandState.Hidden
        return if (expandedKey == t.event.key) IslandState.Expanded(t.event) else IslandState.Compact(t.event)
    }
}
