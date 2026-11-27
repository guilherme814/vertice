package com.vertice.launcher.data

import android.app.ActivityManager
import android.content.Context
import com.vertice.launcher.core.PerformanceProfile

/** Lê do aparelho o que o domínio não pode saber sozinho. */
object DevicePerformance {
    fun isLowRam(context: Context): Boolean =
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).isLowRamDevice

    fun recommended(context: Context): PerformanceProfile = PerformanceProfile.recommended(isLowRam(context))
}
