package com.vertice.launcher.island

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.vertice.launcher.core.island.IslandEvent

/** Carregador e Bluetooth: broadcasts do sistema, registrados só enquanto o serviço da ilha está conectado. */
class IslandSystemEvents(private val context: Context, private val onEvent: (IslandEvent) -> Unit) {
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> onEvent(IslandEvent.Charging(percent = batteryPercent()))
                BluetoothDevice.ACTION_ACL_CONNECTED -> onEvent(IslandEvent.Bluetooth("bt", deviceName(intent)))
            }
        }
    }

    fun start() {
        if (registered) return
        registered = true
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        }
        ContextCompat.registerReceiver(context, receiver, f, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    fun stop() {
        if (!registered) return
        registered = false
        try { context.unregisterReceiver(receiver) } catch (_: IllegalArgumentException) {}
    }

    private fun batteryPercent(): Int {
        val i = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        return if (level >= 0 && scale > 0) level * 100 / scale else 0
    }

    private fun deviceName(intent: Intent): String? {
        // O nome exige BLUETOOTH_CONNECT no Android 12+; sem a permissão, mostramos só "Bluetooth conectado".
        if (Build.VERSION.SDK_INT >= 31 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) return null
        return try {
            val d = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            else @Suppress("DEPRECATION") intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            d?.name?.take(60)
        } catch (_: SecurityException) { null }
    }
}
