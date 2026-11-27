package com.vertice.launcher.core

import java.net.URLDecoder
import java.net.URLEncoder

/** Versões do formato guardado no aparelho. Cada mudança de formato ganha um número e uma migração testada. */
object SettingsSchema {
    /** 1 = dock em texto (`dock`); 2 = layout com contêiner/página/célula (`layout_v2`). */
    const val CURRENT = 2

    /** Migração 1 -> 2: o dock antigo era "chave;chave;..." e vira itens do dock, em ordem. */
    fun layoutFromLegacyDock(dock: String?): List<LayoutItem> =
        dock?.split(';')?.filter { it.isNotEmpty() }
            ?.mapIndexed { i, key -> LayoutItem(key, Container.Dock, 0, i, 0) }
            ?: emptyList()
}

/**
 * Backup das preferências em texto simples (uma linha por dado, legível e tolerante):
 *
 *     VERTICE-BACKUP 1
 *     profile=Premium
 *     layout=<item codificado>
 *
 * Linhas desconhecidas são ignoradas (backup de versão compatível); versão mais nova que a suportada é recusada.
 */
object BackupCodec {
    const val HEADER = "VERTICE-BACKUP"
    const val VERSION = 1

    sealed interface Result {
        data class Ok(val prefs: Prefs, val ignoredLines: Int) : Result
        data class Fail(val reason: String) : Result
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun dec(s: String) = URLDecoder.decode(s, "UTF-8")

    fun encode(p: Prefs): String = buildString {
        appendLine("$HEADER $VERSION")
        p.profile?.let { appendLine("profile=${it.name}") }
        appendLine("clockStyle=${p.clockStyle.name}")
        appendLine("clockFormat=${p.clockFormat.name}")
        appendLine("showDate=${p.showDate}")
        appendLine("wallpaper=${p.wallpaper.name}")
        appendLine("gridColumns=${p.grid.columns}")
        appendLine("gridRows=${p.grid.rows}")
        appendLine("iconSize=${p.iconSize.name}")
        appendLine("showLabels=${p.showLabels}")
        appendLine("accent=${p.accent.name}")
        appendLine("glass=${p.glass.name}")
        appendLine("iconShape=${p.iconShape.name}")
        appendLine("iconShadow=${p.iconShadow}")
        appendLine("drawerSort=${p.drawerSort.name}")
        p.hiddenApps.toSortedSet().forEach { appendLine("hidden=${enc(it)}") }
        p.categoryOverrides.toSortedMap().forEach { (k, v) -> appendLine("category=${enc(k)}=$v") }
        p.layout.forEach { appendLine("layout=${LayoutCodec.encode(it)}") }
        appendLine("focusOn=${p.focusOn}")
        appendLine("dockStyle=${p.dockStyle.name}")
        appendLine("labelSize=${p.labelSize.name}")
        appendLine("labelBold=${p.labelBold}")
        appendLine("indicator=${p.indicator.name}")
        appendLine("dockOpacity=${p.dockOpacity}")
        p.focusBlocked.toSortedSet().forEach { appendLine("focusBlocked=${enc(it)}") }
        p.folders.forEach { appendLine("folder=${FolderCodec.encode(it)}") }
    }

    fun decode(text: String): Result {
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        val head = lines.firstOrNull()?.split(' ')
        if (head == null || head.size != 2 || head[0] != HEADER) return Result.Fail("Arquivo não é um backup da Vértice")
        val version = head[1].toIntOrNull() ?: return Result.Fail("Versão do backup ilegível")
        if (version > VERSION) return Result.Fail("Backup de uma versão mais nova do app (v$version); atualize a Vértice")

        var prefs = Prefs(seeded = true)
        val layout = ArrayList<LayoutItem>()
        val categories = LinkedHashMap<String, String>()
        val hidden = LinkedHashSet<String>()
        val folders = ArrayList<Folder>()
        val blocked = LinkedHashSet<String>()
        var ignored = 0
        for (line in lines.drop(1)) {
            val i = line.indexOf('=')
            if (i <= 0) { ignored++; continue }
            val key = line.substring(0, i)
            val value = line.substring(i + 1)
            when (key) {
                "profile" -> PerformanceProfile.fromName(value)?.let { prefs = prefs.copy(profile = it) } ?: ignored++
                "clockStyle" -> prefs = prefs.copy(clockStyle = ClockStyle.fromName(value))
                "clockFormat" -> prefs = prefs.copy(clockFormat = ClockFormat.fromName(value))
                "showDate" -> prefs = prefs.copy(showDate = value != "false")
                "wallpaper" -> prefs = prefs.copy(wallpaper = Wallpaper.fromName(value))
                "gridColumns" -> value.toIntOrNull()?.let { prefs = prefs.copy(grid = GridConfig.coerced(it, prefs.grid.rows)) } ?: ignored++
                "gridRows" -> value.toIntOrNull()?.let { prefs = prefs.copy(grid = GridConfig.coerced(prefs.grid.columns, it)) } ?: ignored++
                "iconSize" -> prefs = prefs.copy(iconSize = IconSize.fromName(value))
                "showLabels" -> prefs = prefs.copy(showLabels = value != "false")
                "accent" -> prefs = prefs.copy(accent = Accent.fromName(value))
                "glass" -> prefs = prefs.copy(glass = GlassLevel.fromName(value))
                "iconShape" -> prefs = prefs.copy(iconShape = IconShape.fromName(value))
                "iconShadow" -> prefs = prefs.copy(iconShadow = value == "true")
                "drawerSort" -> prefs = prefs.copy(drawerSort = DrawerSort.fromName(value))
                "hidden" -> try { hidden += dec(value) } catch (_: IllegalArgumentException) { ignored++ }
                "category" -> {
                    val j = value.lastIndexOf('=')
                    if (j <= 0) ignored++ else try { categories[dec(value.substring(0, j))] = value.substring(j + 1) } catch (_: IllegalArgumentException) { ignored++ }
                }
                "layout" -> LayoutCodec.decode(value)?.let { layout += it } ?: ignored++
                "labelSize" -> prefs = prefs.copy(labelSize = LabelSize.fromName(value))
                "indicator" -> prefs = prefs.copy(indicator = IndicatorStyle.fromName(value))
                "labelBold" -> prefs = prefs.copy(labelBold = value == "true")
                "dockStyle" -> prefs = prefs.copy(dockStyle = DockStyle.fromName(value))
                "dockOpacity" -> prefs = prefs.copy(dockOpacity = DockOpacity.coerce(value.toIntOrNull() ?: DockOpacity.DEFAULT))
                "focusOn" -> prefs = prefs.copy(focusOn = value == "true")
                "focusBlocked" -> try { blocked += dec(value) } catch (_: IllegalArgumentException) { ignored++ }
                "folder" -> FolderCodec.decode(value)?.let { folders += it } ?: ignored++
                else -> ignored++
            }
        }
        return Result.Ok(prefs.copy(layout = layout, categoryOverrides = categories, hiddenApps = hidden, folders = folders, focusBlocked = blocked), ignored)
    }
}
