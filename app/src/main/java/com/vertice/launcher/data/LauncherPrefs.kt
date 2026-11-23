package com.vertice.launcher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
    val seeded: Boolean = false
)

class LauncherPrefs(context: Context) {

    private val store = context.applicationContext.launcherStore

    private object K {
        val profile = stringPreferencesKey("profile")
        val dock = stringPreferencesKey("dock")
        val seeded = booleanPreferencesKey("seeded")
    }

    val prefs: Flow<Prefs> = store.data.map { p ->
        Prefs(
            profile = PerformanceProfile.fromName(p[K.profile]),
            dock = p[K.dock]?.split(SEP)?.filter { it.isNotEmpty() } ?: emptyList(),
            seeded = p[K.seeded] ?: false
        )
    }.distinctUntilChanged()

    suspend fun current(): Prefs = prefs.first()

    suspend fun setProfile(profile: PerformanceProfile) {
        store.edit { it[K.profile] = profile.name }
    }

    suspend fun setDock(keys: List<String>) {
        store.edit { it[K.dock] = keys.joinToString(SEP) }
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
