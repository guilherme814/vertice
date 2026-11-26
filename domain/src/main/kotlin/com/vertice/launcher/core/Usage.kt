package com.vertice.launcher.core

import java.net.URLDecoder
import java.net.URLEncoder

/** Quantas vezes o app foi aberto pela launcher e quando foi a última. Fica só no aparelho. */
data class Usage(val count: Int, val lastMs: Long)

/** Uso local dos apps (mais usados, recentes). Funções puras. */
object UsageOps {
    /** Guarda no máximo isto: os de uso mais antigo saem primeiro. */
    const val MAX = 150

    fun record(map: Map<String, Usage>, key: String, now: Long): Map<String, Usage> {
        val next = map + (key to Usage((map[key]?.count ?: 0) + 1, now))
        if (next.size <= MAX) return next
        return next.entries.sortedByDescending { it.value.lastMs }.take(MAX).associate { it.key to it.value }
    }

    fun mostUsed(map: Map<String, Usage>, allowed: Set<String>, n: Int): List<String> =
        map.filterKeys { it in allowed }.entries
            .sortedWith(compareByDescending<Map.Entry<String, Usage>> { it.value.count }.thenByDescending { it.value.lastMs }.thenBy { it.key })
            .take(n).map { it.key }
}

object UsageCodec {
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun dec(s: String) = URLDecoder.decode(s, "UTF-8")

    fun encode(key: String, u: Usage) = "${enc(key)}|${u.count}|${u.lastMs}"

    fun decode(line: String): Pair<String, Usage>? {
        val p = line.split('|')
        if (p.size != 3) return null
        val count = p[1].toIntOrNull()?.takeIf { it > 0 } ?: return null
        val last = p[2].toLongOrNull() ?: return null
        return try { dec(p[0]) to Usage(count, last) } catch (_: IllegalArgumentException) { null }
    }

    fun encodeAll(map: Map<String, Usage>): Set<String> = map.map { (k, v) -> encode(k, v) }.toSet()
    fun decodeAll(lines: Set<String>): Map<String, Usage> = lines.mapNotNull(::decode).toMap()
}
