package com.vertice.launcher.diag

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/**
 * Vigia da thread principal. Enquanto a launcher está visível, pergunta a cada 250 ms "você está viva?".
 * Se a thread principal demora mais de 1 s para responder, registra ONDE ela está parada (pilha) —
 * é isso que mostra quem causa o travamento — e depois quanto tempo durou.
 * Só roda com a tela visível (onResume..onPause), para não gastar bateria em segundo plano.
 */
object Watchdog {
    private const val TICK_MS = 250L
    private const val REPORT_AFTER_MS = 1000L
    private const val RESAMPLE_MS = 2000L
    private const val MAX_SAMPLES = 4

    private val main = Handler(Looper.getMainLooper())
    @Volatile private var thread: Thread? = null

    @Synchronized
    fun start() {
        if (thread?.isAlive == true) return
        thread = Thread({ loop() }, "vertice-watchdog").apply { isDaemon = true; start() }
    }

    @Synchronized
    fun stop() {
        thread?.interrupt()
        thread = null
    }

    @Volatile private var ticked = true

    private fun loop() {
        try {
            while (!Thread.currentThread().isInterrupted) {
                ticked = false
                val sent = SystemClock.uptimeMillis()
                main.post { ticked = true }
                Thread.sleep(TICK_MS)
                if (ticked) continue

                // A thread principal não respondeu: acompanha até ela voltar.
                var samples = 0
                var nextSampleAt = REPORT_AFTER_MS
                var reported = false
                while (!ticked) {
                    val blocked = SystemClock.uptimeMillis() - sent
                    if (blocked >= nextSampleAt && samples < MAX_SAMPLES) {
                        val stack = Looper.getMainLooper().thread.stackTrace
                        VLog.w("Travamento", "Thread principal parada há ${blocked}ms. Pilha:\n${LogFormat.stack(stack, 28)}")
                        samples++
                        reported = true
                        nextSampleAt = blocked + RESAMPLE_MS
                    }
                    Thread.sleep(100)
                }
                if (reported) {
                    VLog.w("Travamento", "Thread principal liberada após ~${SystemClock.uptimeMillis() - sent}ms")
                }
            }
        } catch (_: InterruptedException) {
            // parou junto com onPause
        } catch (t: Throwable) {
            VLog.w("Travamento", "Vigia encerrado por erro", t)
        }
    }
}
