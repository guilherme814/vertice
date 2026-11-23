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
import com.vertice.launcher.core.PerformanceProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.launcherStore: DataStore<Preferences> by preferencesDataStore(name = "vertice_launcher")

/** O que é guardado no aparelho. Tudo local: sem conta, sem rede. */
data class Prefs(
    /** null = ainda não escolhido (a primeira execução decide pelo hardware). */
    val profile: PerformanceProfile? = null,
    val dock: List<String> = emptyList(),
    val seeded: Boolean = false,
    /** Correções manuais: chave do app -> nome da categoria. Vencem a classificação automática. */
    val categoryOverrides: Map<String, String> = emptyMap()
)

class LauncherPrefs(context: Context) {

    private val store = context.applicationContext.launcherStore

    private object K {
        val profile = stringPreferencesKey("profile")
        val dock = stringPreferencesKey("dock")
        val seeded = booleanPreferencesKey("seeded")
        val categories = stringSetPreferencesKey("category_overrides")
    }

    val prefs: Flow<Prefs> = store.data.map { p ->
        Prefs(
            profile = PerformanceProfile.fromName(p[K.profile]),
            dock = p[K.dock]?.split(SEP)?.filter { it.isNotEmpty() } ?: emptyList(),
            seeded = p[K.seeded] ?: false,
            categoryOverrides = (p[K.categories] ?: emptySet()).mapNotNull { entry ->
                val i = entry.lastIndexOf('=')
                if (i <= 0) null else Uri.decode(entry.substring(0, i)) to entry.substring(i + 1)
            }.toMap()
        )
    }.distinctUntilChanged()

    suspend fun current(): Prefs = prefs.first()

    suspend fun setProfile(profile: PerformanceProfile) {
        store.edit { it[K.profile] = profile.name }
    }

    suspend fun setDock(keys: List<String>) {
        store.edit { it[K.dock] = keys.joinToString(SEP) }
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

    suspend fun seed(profile: PerformanceProfile, dock: List<String>) {
        store.edit {
            it[K.profile] = profile.name
            it[K.dock] = dock.joinToString(SEP)
            it[K.seeded] = true
        }
    }

    private companion object {
        // Chaves de componente não contêm ";" — é seguro como separador.
        const val SEP = ";"
    }
}
