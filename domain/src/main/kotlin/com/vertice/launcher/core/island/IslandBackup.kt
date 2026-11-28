package com.vertice.launcher.core.island

import java.net.URLDecoder
import java.net.URLEncoder

/** Tudo da ilha que vai no backup. Nunca contém conteúdo de notificações (a ilha não guarda isso). */
data class IslandSnapshot(val settings: IslandSettings, val apps: List<IslandAppConfig>)

/** Linhas `island.*` dentro do backup geral. Valores inválidos viram o padrão (nunca derrubam a restauração). */
object IslandBackup {
    const val PREFIX = "island."

    fun encode(s: IslandSnapshot): List<String> {
        val v = s.settings.sanitized()
        return buildList {
            add("${PREFIX}enabled=${v.enabled}")
            add("${PREFIX}media=${v.media}")
            add("${PREFIX}calls=${v.calls}")
            add("${PREFIX}timers=${v.timers}")
            add("${PREFIX}charging=${v.charging}")
            add("${PREFIX}bluetooth=${v.bluetooth}")
            add("${PREFIX}animate=${v.animate}")
            add("${PREFIX}tempSeconds=${v.tempSeconds}")
            add("${PREFIX}offsetDp=${v.offsetDp}")
            add("${PREFIX}width=${v.width.name}")
            add("${PREFIX}theme=${v.theme.name}")
            add("${PREFIX}background=${v.background.name}")
            add("${PREFIX}autoExpand=${v.autoExpand}")
            add("${PREFIX}expandSeconds=${v.expandSeconds}")
            s.apps.sortedBy { it.packageName }.forEach {
                add("${PREFIX}app=" + URLEncoder.encode(it.packageName, "UTF-8") + "|${it.enabled}|${it.mode.name}|${it.showContent}")
            }
        }
    }

    /** [kv] = pares `chave sem prefixo -> valor`; [appLines] = valores das linhas `island.app`. */
    fun decode(kv: Map<String, String>, appLines: List<String>): IslandSnapshot {
        val d = IslandSettings()
        fun bool(k: String, def: Boolean) = kv[k]?.let { it == "true" } ?: def
        val settings = IslandSettings(
            enabled = bool("enabled", d.enabled),
            media = bool("media", d.media),
            calls = bool("calls", d.calls),
            timers = bool("timers", d.timers),
            charging = bool("charging", d.charging),
            bluetooth = bool("bluetooth", d.bluetooth),
            animate = bool("animate", d.animate),
            tempSeconds = kv["tempSeconds"]?.toIntOrNull() ?: d.tempSeconds,
            offsetDp = kv["offsetDp"]?.toIntOrNull() ?: d.offsetDp,
            width = IslandWidth.fromName(kv["width"]),
            theme = IslandTheme.fromName(kv["theme"]),
            background = IslandBackground.fromName(kv["background"]),
            autoExpand = bool("autoExpand", d.autoExpand),
            expandSeconds = kv["expandSeconds"]?.toIntOrNull() ?: d.expandSeconds
        ).sanitized()
        val apps = appLines.mapNotNull { line ->
            val p = line.split('|')
            if (p.size != 4) return@mapNotNull null
            val pkg = try { URLDecoder.decode(p[0], "UTF-8") } catch (_: IllegalArgumentException) { return@mapNotNull null }
            if (pkg.isBlank() || pkg.length > 200) null
            else IslandAppConfig(pkg, p[1] == "true", IslandMode.fromName(p[2]), p[3] == "true")
        }.distinctBy { it.packageName }.take(300)
        return IslandSnapshot(settings, apps)
    }
}
