package com.vertice.launcher.diag

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class LogLevel(val tag: Char) { Debug('D'), Info('I'), Warn('W'), Error('E') }

/** Formato do arquivo de log. Toda linha começa com data e hora, então dá para filtrar com grep/sort. */
object LogFormat {
    private val lineFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    private val fileFmt = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

    fun line(
        timeMs: Long, level: LogLevel, tag: String, thread: String, message: String,
        zone: ZoneId = ZoneId.systemDefault()
    ): String {
        val t = lineFmt.format(Instant.ofEpochMilli(timeMs).atZone(zone))
        // Mensagem de várias linhas: continuação indentada, para cada linha "nova" começar com a data.
        return "$t ${level.tag}/$tag [$thread] ${message.replace("\n", "\n    ")}"
    }

    fun fileStamp(timeMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        fileFmt.format(Instant.ofEpochMilli(timeMs).atZone(zone))

    /** Pilha enxuta: no máximo [max] quadros, avisando quantos ficaram de fora. */
    fun stack(frames: Array<StackTraceElement>, max: Int = 30): String {
        val shown = frames.take(max).joinToString("\n") { "  at $it" }
        val rest = frames.size - max
        return if (rest > 0) "$shown\n  … +$rest quadros" else shown
    }
}

/** Últimas linhas em memória (para a tela de diagnóstico e para anexar a um crash). Seguro entre threads. */
class LineRing(private val capacity: Int) {
    private val items = ArrayDeque<String>()

    @Synchronized fun add(line: String) {
        if (items.size == capacity) items.removeFirst()
        items.addLast(line)
    }

    @Synchronized fun last(n: Int = capacity): List<String> = items.toList().takeLast(n)

    @Synchronized fun clear() = items.clear()
}

/**
 * Resume a fluidez a partir do tempo total de cada quadro.
 * Limites: lento > 32 ms (perdeu ao menos um quadro a 60 Hz), travado > 100 ms, congelado > 700 ms.
 * As faixas são cumulativas: um quadro congelado também conta como travado e como lento.
 */
class FrameStats {
    data class Summary(
        val frames: Int, val slow: Int, val janky: Int, val frozen: Int, val worstMs: Long, val avgMs: Long
    ) {
        fun describe(): String =
            "quadros=$frames lentos(>${SLOW_MS}ms)=$slow travados(>${JANKY_MS}ms)=$janky " +
                "congelados(>${FROZEN_MS}ms)=$frozen pior=${worstMs}ms média=${avgMs}ms"
    }

    private var frames = 0
    private var slow = 0
    private var janky = 0
    private var frozen = 0
    private var worst = 0L
    private var sum = 0L

    fun add(totalMs: Long) {
        frames++
        sum += totalMs
        if (totalMs > worst) worst = totalMs
        if (totalMs > SLOW_MS) slow++
        if (totalMs > JANKY_MS) janky++
        if (totalMs > FROZEN_MS) frozen++
    }

    fun summary() = Summary(frames, slow, janky, frozen, worst, if (frames == 0) 0 else sum / frames)

    fun reset() {
        frames = 0; slow = 0; janky = 0; frozen = 0; worst = 0; sum = 0
    }

    companion object {
        const val SLOW_MS = 32L
        const val JANKY_MS = 100L
        const val FROZEN_MS = 700L
    }
}
