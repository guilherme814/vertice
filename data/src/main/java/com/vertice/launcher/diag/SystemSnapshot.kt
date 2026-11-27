package com.vertice.launcher.diag

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import java.util.Locale

/** Fotografias do estado do aparelho e do app, para o início de cada sessão e para os relatórios. */
object SystemSnapshot {

    private const val MB = 1024L * 1024L

    fun sinceProcessStartMs(): Long =
        SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime()

    fun header(context: Context): String {
        val c = context.applicationContext
        val pm = c.packageManager
        val info = try { pm.getPackageInfo(c.packageName, 0) } catch (_: Throwable) { null }
        @Suppress("DEPRECATION")
        val code = info?.versionCode
        val debuggable = (c.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val am = c.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am?.getMemoryInfo(it) }
        val dm = c.resources.displayMetrics
        val power = c.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return buildString {
            appendLine("=== Vértice Launcher — início de sessão ===")
            appendLine("App: ${c.packageName} versão=${info?.versionName} código=$code ${if (debuggable) "DEBUG" else "RELEASE"}")
            appendLine("Instalado/atualizado em: ${info?.lastUpdateTime?.let { LogFormat.fileStamp(it) }}")
            appendLine("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) patch=${Build.VERSION.SECURITY_PATCH}")
            appendLine("Aparelho: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE}) hardware=${Build.HARDWARE} abis=${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine("Tela: ${dm.widthPixels}x${dm.heightPixels}px ${dm.densityDpi}dpi fonte=${c.resources.configuration.fontScale}")
            appendLine("RAM total=${mi.totalMem / MB}MB pouca-RAM=${am?.isLowRamDevice} limite-heap=${am?.memoryClass}MB (grande=${am?.largeMemoryClass}MB)")
            appendLine("Economia de bateria=${power?.isPowerSaveMode} idioma=${Locale.getDefault().toLanguageTag()}")
            appendLine("Tela inicial padrão: ${defaultHome(c)}")
            append("Processo: pid=${Process.myPid()} ${sinceProcessStartMs()}ms desde o início")
        }
    }

    fun memoryLine(context: Context): String {
        val rt = Runtime.getRuntime()
        val used = (rt.totalMemory() - rt.freeMemory()) / MB
        val max = rt.maxMemory() / MB
        val native = Debug.getNativeHeapAllocatedSize() / MB
        val am = context.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am?.getMemoryInfo(it) }
        return "heap=${used}/${max}MB nativo=${native}MB | sistema livre=${mi.availMem / MB}MB de ${mi.totalMem / MB}MB " +
            "limiar=${mi.threshold / MB}MB pouca-memória=${mi.lowMemory}"
    }

    private fun defaultHome(c: Context): String = try {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val ri = c.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val pkg = ri?.activityInfo?.packageName
        when {
            pkg == null -> "não identificada"
            pkg == c.packageName -> "Vértice (esta)"
            pkg == "android" -> "nenhuma escolhida (o sistema vai perguntar)"
            else -> pkg
        }
    } catch (t: Throwable) {
        "erro: ${t.javaClass.simpleName}"
    }
}
