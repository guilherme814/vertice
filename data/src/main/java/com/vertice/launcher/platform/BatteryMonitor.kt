package com.vertice.launcher.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Bateria por broadcast: o receptor só existe enquanto alguém coleta o fluxo. */
class BatteryMonitor(context: Context) : BatteryStatus {
    private val app = context.applicationContext

    override val percent: Flow<Int?> = callbackFlow {
        fun read(intent: Intent?): Int? {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            return if (level >= 0 && scale > 0) level * 100 / scale else null
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) { trySend(read(intent)) }
        }
        // ACTION_BATTERY_CHANGED é "sticky": o registro já devolve o valor atual.
        trySend(read(app.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))))
        awaitClose { try { app.unregisterReceiver(receiver) } catch (_: IllegalArgumentException) {} }
    }.distinctUntilChanged()
}
