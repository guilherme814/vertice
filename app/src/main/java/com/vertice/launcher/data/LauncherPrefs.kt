package com.vertice.launcher.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vertice.launcher.core.ClockFormat
import com.vertice.launcher.core.ClockStyle
import com.vertice.launcher.core.Container
import com.vertice.launcher.core.LayoutCodec
import com.vertice.launcher.core.LayoutItem
import com.vertice.launcher.core.PerformanceProfile
import com.vertice.launcher.core.Wallpaper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.launcherStore: DataStore<Preferences> by preferencesDataStore(name = "vertice_launcher")

/** O que é guardado no aparelho. Tudo local: sem conta, sem rede. */
data class Prefs(
    /** null = ainda não escolhido (a primeira execução decide pelo hardware). */
    val profile: PerformanceProfile? = null,
    /** Posição de cada app na Home e no dock (contêiner + página + célula). */
    val layout: List<LayoutItem> = emptyList(),
    val seeded: Boolean = false,
    /** Correções manuais: chave do app -> nome da categoria. Vencem a classificação automática. */
    val categoryOverrides: Map<String, String> = emptyMap(),
    val clockStyle: ClockStyle = ClockStyle.Classic,
    val clockFormat: ClockFormat = ClockFormat.System,
    val showDate: Boolean = true,
    val wallpaper: Wallpaper = Wallpaper.System
)

class LauncherPrefs(context: Context) {

    private val store = context.applicationContext.launcherStore

    private object K {
        val profile = stringPreferencesKey("profile")
        val dock = stringPreferencesKey("dock") // legado (antes da grade); só lido para migrar
        val layout = stringSetPreferencesKey("layout_v2")
        val seeded = booleanPreferencesKey("seeded")
        val categories = stringSetPreferencesKey("category_overrides")
        val clockStyle = stringPreferencesKey("clock_style")
        val clockFormat = stringPreferencesKey("clock_format")
        val showDate = booleanPreferencesKey("show_date")
        val wallpaper = stringPreferencesKey("wallpaper")
    }

    val prefs: Flow<Prefs> = store.data.map { p ->
        Prefs(
            profile = PerformanceProfile.fromName(p[K.profile]),
            layout = readLayout(p),
            seeded = p[K.seeded] ?: false,
            categoryOverrides = (p[K.categories] ?: emptySet()).mapNotNull { entry ->
                val i = entry.lastIndexOf('=')
                if (i <= 0) null else Uri.decode(entry.substring(0, i)) to entry.substring(i + 1)
            }.toMap(),
            clockStyle = ClockStyle.fromName(p[K.clockStyle]),
            clockFormat = ClockFormat.fromName(p[K.clockFormat]),
            showDate = p[K.showDate] ?: true,
            wallpaper = Wallpaper.fromName(p[K.wallpaper])
        )
    }.distinctUntilChanged()

    suspend fun current(): Prefs = prefs.first()

    suspend fun setProfile(profile: PerformanceProfile) {
        store.edit { it[K.profile] = profile.name }
    }

    /**
     * Leitura + transformação + escrita numa única transação do DataStore: duas edições seguidas
     * (ex.: dois toques rápidos) nunca se sobrescrevem, ao contrário de ler o estado na UI e gravar depois.
     */
    suspend fun editLayout(transform: (List<LayoutItem>) -> List<LayoutItem>) {
        store.edit { p -> p[K.layout] = LayoutCodec.encodeAll(transform(readLayout(p))) }
    }

    /** Layout novo, ou (primeira abertura depois da atualização) migrado do dock antigo. */
    private fun readLayout(p: Preferences): List<LayoutItem> {
        p[K.layout]?.let { return LayoutCodec.decodeAll(it) }
        val legacy = p[K.dock]?.split(SEP)?.filter { it.isNotEmpty() } ?: return emptyList()
        return legacy.mapIndexed { i, key -> LayoutItem(key, Container.Dock, 0, i, 0) }
    }

    /** `category = null` volta para a classificação automática. */
    suspend fun setCategory(key: String, category: String?) {
        store.edit { p ->
            val current = (p[K.categories] ?: emptySet())
                .filterNot { Uri.decode(it.substringBeforeLast('=')) == key }
                .toMutableSet()
            if (category != null) current += "${Uri.encode(key)}=$category"
            p[K.categories] = current
        }
    }

    suspend fun setClockStyle(style: ClockStyle) { store.edit { it[K.clockStyle] = style.name } }

    suspend fun setClockFormat(format: ClockFormat) { store.edit { it[K.clockFormat] = format.name } }

    suspend fun setShowDate(show: Boolean) { store.edit { it[K.showDate] = show } }

    suspend fun setWallpaper(w: Wallpaper) { store.edit { it[K.wallpaper] = w.name } }

    suspend fun seed(profile: PerformanceProfile, layout: List<LayoutItem>) {
        store.edit {
            it[K.profile] = profile.name
            it[K.layout] = LayoutCodec.encodeAll(layout)
            it[K.seeded] = true
        }
    }

    private companion object {
        // Chaves de componente não contêm ";" — é seguro como separador.
        const val SEP = ";"
    }
}
