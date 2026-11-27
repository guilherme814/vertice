package com.vertice.launcher.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vertice.launcher.core.ClockFormat
import com.vertice.launcher.core.DockOpacity
import com.vertice.launcher.core.DockStyle
import com.vertice.launcher.core.ClockAlign
import com.vertice.launcher.core.ClockColor
import com.vertice.launcher.core.ClockScale
import com.vertice.launcher.core.CustomWallpaper
import com.vertice.launcher.core.DrawerColumns
import com.vertice.launcher.core.DrawerLayout
import com.vertice.launcher.core.FolderCodec
import com.vertice.launcher.core.IndicatorStyle
import com.vertice.launcher.core.LabelSize
import com.vertice.launcher.core.UsageCodec
import com.vertice.launcher.core.UsageOps
import com.vertice.launcher.core.HomeData
import com.vertice.launcher.core.ClockStyle
import com.vertice.launcher.core.Accent
import com.vertice.launcher.core.GlassLevel
import com.vertice.launcher.core.GridConfig
import com.vertice.launcher.core.GridSpec
import com.vertice.launcher.core.DrawerSort
import com.vertice.launcher.core.IconShape
import com.vertice.launcher.core.IconSize
import com.vertice.launcher.core.LayoutCodec
import com.vertice.launcher.core.LayoutItem
import com.vertice.launcher.core.Prefs
import com.vertice.launcher.core.SettingsSchema
import com.vertice.launcher.core.SettingsStore
import com.vertice.launcher.core.PerformanceProfile
import com.vertice.launcher.core.Wallpaper
import com.vertice.launcher.diag.VLog
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.IOException
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.launcherStore: DataStore<Preferences> by preferencesDataStore(name = "vertice_launcher")

class LauncherPrefs(context: Context) : SettingsStore {

    private val store = context.applicationContext.launcherStore

    private object K {
        val profile = stringPreferencesKey("profile")
        val dock = stringPreferencesKey("dock") // legado (antes da grade); só lido para migrar
        val layout = stringSetPreferencesKey("layout_v2")
        val folders = stringSetPreferencesKey("folders_v1")
        val schemaVersion = intPreferencesKey("schema_version")
        val gridColumns = intPreferencesKey("grid_columns")
        val gridRows = intPreferencesKey("grid_rows")
        val iconSize = stringPreferencesKey("icon_size")
        val showLabels = booleanPreferencesKey("show_labels")
        val accent = stringPreferencesKey("accent")
        val glass = stringPreferencesKey("glass")
        val iconShape = stringPreferencesKey("icon_shape")
        val iconShadow = booleanPreferencesKey("icon_shadow")
        val drawerSort = stringPreferencesKey("drawer_sort")
        val hidden = stringSetPreferencesKey("hidden_apps")
        val usage = stringSetPreferencesKey("usage_v1")
        val focusOn = booleanPreferencesKey("focus_on")
        val dockStyle = stringPreferencesKey("dock_style")
        val labelSize = stringPreferencesKey("label_size")
        val labelBold = booleanPreferencesKey("label_bold")
        val indicator = stringPreferencesKey("page_indicator")
        val drawerLayout = stringPreferencesKey("drawer_layout")
        val customA = longPreferencesKey("custom_wallpaper_a")
        val clockColor = stringPreferencesKey("clock_color")
        val clockScale = stringPreferencesKey("clock_scale")
        val clockAlign = stringPreferencesKey("clock_align")
        val customB = longPreferencesKey("custom_wallpaper_b")
        val drawerColumns = intPreferencesKey("drawer_columns")
        val dockOpacity = intPreferencesKey("dock_opacity")
        val focusBlocked = stringSetPreferencesKey("focus_blocked")
        val seeded = booleanPreferencesKey("seeded")
        val categories = stringSetPreferencesKey("category_overrides")
        val clockStyle = stringPreferencesKey("clock_style")
        val clockFormat = stringPreferencesKey("clock_format")
        val showDate = booleanPreferencesKey("show_date")
        val wallpaper = stringPreferencesKey("wallpaper")
    }

    override val prefs: Flow<Prefs> = store.data.catch { e ->
        if (e is IOException) {
            // Arquivo de preferências ilegível: segue com os padrões em vez de derrubar a launcher.
            VLog.e("Prefs", "Falha ao ler as preferências; usando padrões", e)
            emit(emptyPreferences())
        } else throw e
    }.map { p ->
        Prefs(
            profile = PerformanceProfile.fromName(p[K.profile]),
            layout = readLayout(p),
            folders = FolderCodec.decodeAll(p[K.folders] ?: emptySet()),
            seeded = p[K.seeded] ?: false,
            categoryOverrides = (p[K.categories] ?: emptySet()).mapNotNull { entry ->
                val i = entry.lastIndexOf('=')
                if (i <= 0) null else Uri.decode(entry.substring(0, i)) to entry.substring(i + 1)
            }.toMap(),
            clockStyle = ClockStyle.fromName(p[K.clockStyle]),
            clockFormat = ClockFormat.fromName(p[K.clockFormat]),
            showDate = p[K.showDate] ?: true,
            wallpaper = Wallpaper.fromName(p[K.wallpaper]),
            grid = GridConfig.coerced(p[K.gridColumns] ?: GridSpec.COLUMNS, p[K.gridRows] ?: GridSpec.ROWS),
            iconSize = IconSize.fromName(p[K.iconSize]),
            showLabels = p[K.showLabels] ?: true,
            accent = Accent.fromName(p[K.accent]),
            glass = GlassLevel.fromName(p[K.glass]),
            iconShape = IconShape.fromName(p[K.iconShape]),
            iconShadow = p[K.iconShadow] ?: false,
            drawerSort = DrawerSort.fromName(p[K.drawerSort]),
            hiddenApps = p[K.hidden] ?: emptySet(),
            usage = UsageCodec.decodeAll(p[K.usage] ?: emptySet()),
            focusOn = p[K.focusOn] ?: false,
            dockStyle = DockStyle.fromName(p[K.dockStyle]),
            labelSize = LabelSize.fromName(p[K.labelSize]),
            labelBold = p[K.labelBold] ?: false,
            indicator = IndicatorStyle.fromName(p[K.indicator]),
            drawerLayout = DrawerLayout.fromName(p[K.drawerLayout]),
            clockColor = ClockColor.fromName(p[K.clockColor]),
            clockScale = ClockScale.fromName(p[K.clockScale]),
            clockAlign = ClockAlign.fromName(p[K.clockAlign]),
            customWallpaperA = CustomWallpaper.sanitize(p[K.customA] ?: CustomWallpaper.DEFAULT_A),
            customWallpaperB = CustomWallpaper.sanitize(p[K.customB] ?: CustomWallpaper.DEFAULT_B),
            drawerColumns = DrawerColumns.coerce(p[K.drawerColumns] ?: DrawerColumns.DEFAULT),
            dockOpacity = DockOpacity.coerce(p[K.dockOpacity] ?: DockOpacity.DEFAULT),
            focusBlocked = p[K.focusBlocked] ?: emptySet()
        )
    }.distinctUntilChanged()

    override suspend fun setProfile(profile: PerformanceProfile) {
        store.edit { it[K.profile] = profile.name }
    }

    /**
     * Leitura + transformação + escrita numa única transação do DataStore: duas edições seguidas
     * (ex.: dois toques rápidos) nunca se sobrescrevem, ao contrário de ler o estado na UI e gravar depois.
     */
    override suspend fun editHome(transform: (HomeData) -> HomeData) {
        store.edit { p ->
            val next = transform(HomeData(readLayout(p), FolderCodec.decodeAll(p[K.folders] ?: emptySet())))
            p[K.layout] = LayoutCodec.encodeAll(next.items)
            p[K.folders] = FolderCodec.encodeAll(next.folders)
            p[K.schemaVersion] = SettingsSchema.CURRENT
        }
    }

    /** Layout novo, ou (primeira abertura depois da atualização) migrado do dock antigo. */
    private fun readLayout(p: Preferences): List<LayoutItem> {
        p[K.layout]?.let { return LayoutCodec.decodeAll(it) }
        return SettingsSchema.layoutFromLegacyDock(p[K.dock])
    }

    /** `category = null` volta para a classificação automática. */
    override suspend fun setCategory(key: String, category: String?) {
        store.edit { p ->
            val current = (p[K.categories] ?: emptySet())
                .filterNot { Uri.decode(it.substringBeforeLast('=')) == key }
                .toMutableSet()
            if (category != null) current += "${Uri.encode(key)}=$category"
            p[K.categories] = current
        }
    }

    override suspend fun setClockStyle(style: ClockStyle) { store.edit { it[K.clockStyle] = style.name } }

    override suspend fun setClockFormat(format: ClockFormat) { store.edit { it[K.clockFormat] = format.name } }

    override suspend fun setShowDate(show: Boolean) { store.edit { it[K.showDate] = show } }

    override suspend fun setWallpaper(w: Wallpaper) { store.edit { it[K.wallpaper] = w.name } }

    override suspend fun setGrid(grid: GridConfig) {
        store.edit { it[K.gridColumns] = grid.columns; it[K.gridRows] = grid.rows }
    }

    override suspend fun setIconSize(size: IconSize) { store.edit { it[K.iconSize] = size.name } }

    override suspend fun setShowLabels(show: Boolean) { store.edit { it[K.showLabels] = show } }

    override suspend fun setAccent(accent: Accent) { store.edit { it[K.accent] = accent.name } }

    override suspend fun setGlass(glass: GlassLevel) { store.edit { it[K.glass] = glass.name } }

    override suspend fun setIconShape(shape: IconShape) { store.edit { it[K.iconShape] = shape.name } }

    override suspend fun setIconShadow(on: Boolean) { store.edit { it[K.iconShadow] = on } }

    override suspend fun recordUse(key: String, nowMs: Long) {
        store.edit { p ->
            val cur = UsageCodec.decodeAll(p[K.usage] ?: emptySet())
            p[K.usage] = UsageCodec.encodeAll(UsageOps.record(cur, key, nowMs))
        }
    }

    override suspend fun setLabelSize(size: LabelSize) { store.edit { it[K.labelSize] = size.name } }

    override suspend fun setCustomWallpaper(a: Long, b: Long) {
        store.edit {
            it[K.customA] = CustomWallpaper.sanitize(a)
            it[K.customB] = CustomWallpaper.sanitize(b)
        }
    }

    override suspend fun setClockLook(color: ClockColor, scale: ClockScale, align: ClockAlign) {
        store.edit {
            it[K.clockColor] = color.name
            it[K.clockScale] = scale.name
            it[K.clockAlign] = align.name
        }
    }

    override suspend fun setDrawerLayout(layout: DrawerLayout) { store.edit { it[K.drawerLayout] = layout.name } }

    override suspend fun setDrawerColumns(n: Int) { store.edit { it[K.drawerColumns] = DrawerColumns.coerce(n) } }

    override suspend fun setIndicator(style: IndicatorStyle) { store.edit { it[K.indicator] = style.name } }

    override suspend fun setLabelBold(on: Boolean) { store.edit { it[K.labelBold] = on } }

    override suspend fun setDockStyle(style: DockStyle) { store.edit { it[K.dockStyle] = style.name } }

    override suspend fun setDockOpacity(percent: Int) { store.edit { it[K.dockOpacity] = DockOpacity.coerce(percent) } }

    override suspend fun setFocus(on: Boolean) { store.edit { it[K.focusOn] = on } }

    override suspend fun setFocusBlocked(keys: Set<String>) { store.edit { it[K.focusBlocked] = keys } }

    override suspend fun setDrawerSort(sort: DrawerSort) { store.edit { it[K.drawerSort] = sort.name } }

    override suspend fun setHidden(key: String, hidden: Boolean) {
        store.edit { p ->
            val cur = p[K.hidden] ?: emptySet()
            p[K.hidden] = if (hidden) cur + key else cur - key
        }
    }

    override suspend fun restore(prefs: Prefs) {
        store.edit {
            prefs.profile?.let { p -> it[K.profile] = p.name } ?: it.remove(K.profile)
            it[K.layout] = LayoutCodec.encodeAll(prefs.layout)
            it[K.folders] = FolderCodec.encodeAll(prefs.folders)
            it[K.categories] = prefs.categoryOverrides.map { (k, v) -> "${Uri.encode(k)}=$v" }.toSet()
            it[K.clockStyle] = prefs.clockStyle.name
            it[K.clockFormat] = prefs.clockFormat.name
            it[K.showDate] = prefs.showDate
            it[K.wallpaper] = prefs.wallpaper.name
            it[K.gridColumns] = prefs.grid.columns
            it[K.gridRows] = prefs.grid.rows
            it[K.iconSize] = prefs.iconSize.name
            it[K.showLabels] = prefs.showLabels
            it[K.accent] = prefs.accent.name
            it[K.glass] = prefs.glass.name
            it[K.iconShape] = prefs.iconShape.name
            it[K.iconShadow] = prefs.iconShadow
            it[K.drawerSort] = prefs.drawerSort.name
            it[K.hidden] = prefs.hiddenApps
            it[K.focusOn] = prefs.focusOn
            it[K.dockStyle] = prefs.dockStyle.name
            it[K.labelSize] = prefs.labelSize.name
            it[K.labelBold] = prefs.labelBold
            it[K.indicator] = prefs.indicator.name
            it[K.drawerLayout] = prefs.drawerLayout.name
            it[K.clockColor] = prefs.clockColor.name
            it[K.clockScale] = prefs.clockScale.name
            it[K.clockAlign] = prefs.clockAlign.name
            it[K.customA] = CustomWallpaper.sanitize(prefs.customWallpaperA)
            it[K.customB] = CustomWallpaper.sanitize(prefs.customWallpaperB)
            it[K.drawerColumns] = DrawerColumns.coerce(prefs.drawerColumns)
            it[K.dockOpacity] = DockOpacity.coerce(prefs.dockOpacity)
            it[K.focusBlocked] = prefs.focusBlocked
            it[K.schemaVersion] = SettingsSchema.CURRENT
            it[K.seeded] = true
        }
    }

    override suspend fun seed(profile: PerformanceProfile, layout: List<LayoutItem>) {
        store.edit {
            it[K.profile] = profile.name
            it[K.layout] = LayoutCodec.encodeAll(layout)
            it[K.schemaVersion] = SettingsSchema.CURRENT
            it[K.seeded] = true
        }
    }

    private companion object {
        // Chaves de componente não contêm ";" — é seguro como separador.
        const val SEP = ";"
    }
}
