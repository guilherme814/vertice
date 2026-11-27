package com.vertice.launcher.diag

import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.FrameMetrics
import android.view.Window

/**
 * Mede cada quadro desenhado (FrameMetrics, Android 7+). Ao contrário de um Choreographer em loop,
 * não força a tela a acordar 60 vezes por segundo: só é chamado quando há quadro de verdade.
 *
 *  - quadro >= 100 ms: linha própria com a divisão por fase (layout, desenho, GPU...), que diz o que é lento;
 *  - a cada ~5 s com algum quadro lento: resumo com contagens;
 *  - primeiro quadro desde o início do processo: tempo de abertura a frio.
 */
object FrameMonitor {
    private const val NS = 1_000_000.0
    private const val SUMMARY_EVERY_MS = 5000L
    private const val MAX_DETAIL_PER_10S = 12

    private var thread: HandlerThread? = null
    private var window: Window? = null
    private var listener: Window.OnFrameMetricsAvailableListener? = null

    private val stats = FrameStats()
    private var lastSummary = 0L
    private var detailWindowStart = 0L
    private var detailCount = 0
    @Volatile private var firstFrameLogged = false

    @Synchronized
    fun attach(w: Window) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        detach()
        val t = HandlerThread("vertice-frames").also { it.start() }
        val l = Window.OnFrameMetricsAvailableListener { _, fm, dropped -> onFrame(fm, dropped) }
        try {
            w.addOnFrameMetricsAvailableListener(l, Handler(t.looper))
            thread = t; window = w; listener = l
            lastSummary = SystemClock.elapsedRealtime()
        } catch (e: Throwable) {
            t.quitSafely()
            VLog.w("Quadros", "Medição de quadros indisponível", e)
        }
    }

    @Synchronized
    fun detach() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        try {
            listener?.let { window?.removeOnFrameMetricsAvailableListener(it) }
        } catch (_: Throwable) {}
        thread?.quitSafely()
        thread = null; window = null; listener = null
        flushSummary()
    }

    private val RESUME_ARTIFACT_MS = 3_000L

    @Synchronized
    private fun flushSummary() {
        val s = stats.summary()
        if (s.slow > 0) VLog.i("Quadros", s.describe())
        stats.reset()
    }

    @Synchronized
    private fun onFrame(fm: FrameMetrics, dropped: Int) {
        val total = fm.getMetric(FrameMetrics.TOTAL_DURATION) / 1_000_000
        // Quadro de vários segundos com todas as fases ~0 = a janela voltou de segundo plano, não é travamento.
        if (total > RESUME_ARTIFACT_MS) {
            VLog.d("Quadros", "Retorno ao primeiro plano: quadro de ${total}ms (ignorado nas estatísticas)")
            return
        }
        stats.add(total)
        val now = SystemClock.elapsedRealtime()

        if (!firstFrameLogged && fm.getMetric(FrameMetrics.FIRST_DRAW_FRAME) == 1L) {
            firstFrameLogged = true
            VLog.i("Abertura", "Primeiro quadro desenhado: ${total}ms de duração, ${SystemSnapshot.sinceProcessStartMs()}ms desde o início do processo")
        }

        if (total >= FrameStats.JANKY_MS) {
            if (now - detailWindowStart > 10_000) { detailWindowStart = now; detailCount = 0 }
            if (detailCount++ < MAX_DETAIL_PER_10S) {
                fun ms(id: Int) = "%.0f".format(fm.getMetric(id) / NS)
                VLog.w(
                    "Quadros",
                    "Quadro lento: total=${total}ms [entrada=${ms(FrameMetrics.INPUT_HANDLING_DURATION)} " +
                        "anim=${ms(FrameMetrics.ANIMATION_DURATION)} layout=${ms(FrameMetrics.LAYOUT_MEASURE_DURATION)} " +
                        "desenho=${ms(FrameMetrics.DRAW_DURATION)} sync=${ms(FrameMetrics.SYNC_DURATION)} " +
                        "comandos-gpu=${ms(FrameMetrics.COMMAND_ISSUE_DURATION)} swap=${ms(FrameMetrics.SWAP_BUFFERS_DURATION)} " +
                        "atraso=${ms(FrameMetrics.UNKNOWN_DELAY_DURATION)}] perdidos=$dropped"
                )
            }
        }

        if (now - lastSummary >= SUMMARY_EVERY_MS) {
            flushSummary()
            lastSummary = now
        }
    }
}
