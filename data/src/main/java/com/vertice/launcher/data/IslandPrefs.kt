package com.vertice.launcher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vertice.launcher.core.island.IslandAppConfig
import com.vertice.launcher.core.island.IslandAppsCodec
import com.vertice.launcher.core.island.IslandSettings
import com.vertice.launcher.core.island.IslandSnapshot
import com.vertice.launcher.core.island.IslandStore
import com.vertice.launcher.core.island.IslandTheme
import com.vertice.launcher.core.island.IslandWidth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Arquivo próprio: o serviço da ilha lê só isto e não acorda as preferências da launcher.
private val Context.islandStore: DataStore<Preferences> by preferencesDataStore(name = "vertice_island")

class IslandPrefs(context: Context) : IslandStore {
    private val store = context.applicationContext.islandStore

    private object K {
        val enabled = booleanPreferencesKey("enabled")
        val media = booleanPreferencesKey("media")
        val calls = booleanPreferencesKey("calls")
        val timers = booleanPreferencesKey("timers")
        val charging = booleanPreferencesKey("charging")
        val bluetooth = booleanPreferencesKey("bluetooth")
        val animate = booleanPreferencesKey("animate")
        val tempSeconds = intPreferencesKey("temp_seconds")
        val offsetDp = intPreferencesKey("offset_dp")
        val width = stringPreferencesKey("width")
        val theme = stringPreferencesKey("theme")
        val apps = stringPreferencesKey("apps_v1")
    }

    override val settings: Flow<IslandSettings> = store.data.map { p ->
        val d = IslandSettings()
        IslandSettings(
            enabled = p[K.enabled] ?: d.enabled,
            media = p[K.media] ?: d.media,
            calls = p[K.calls] ?: d.calls,
            timers = p[K.timers] ?: d.timers,
            charging = p[K.charging] ?: d.charging,
            bluetooth = p[K.bluetooth] ?: d.bluetooth,
            animate = p[K.animate] ?: d.animate,
            tempSeconds = p[K.tempSeconds] ?: d.tempSeconds,
            offsetDp = p[K.offsetDp] ?: d.offsetDp,
            width = IslandWidth.fromName(p[K.width]),
            theme = IslandTheme.fromName(p[K.theme])
        ).sanitized()
    }

    override val apps: Flow<Map<String, IslandAppConfig>> =
        store.data.map { IslandAppsCodec.decode(it[K.apps]) }

    override suspend fun setSettings(settings: IslandSettings) {
        val s = settings.sanitized()
        store.edit {
            it[K.enabled] = s.enabled
            it[K.media] = s.media
            it[K.calls] = s.calls
            it[K.timers] = s.timers
            it[K.charging] = s.charging
            it[K.bluetooth] = s.bluetooth
            it[K.animate] = s.animate
            it[K.tempSeconds] = s.tempSeconds
            it[K.offsetDp] = s.offsetDp
            it[K.width] = s.width.name
            it[K.theme] = s.theme.name
        }
    }

    override suspend fun setApp(config: IslandAppConfig) {
        store.edit {
            val map = IslandAppsCodec.decode(it[K.apps]).toMutableMap()
            map[config.packageName] = config
            it[K.apps] = IslandAppsCodec.encode(map.values)
        }
    }

    override suspend fun replaceAll(snapshot: IslandSnapshot) {
        setSettings(snapshot.settings)
        store.edit { it[K.apps] = IslandAppsCodec.encode(snapshot.apps) }
    }

    override suspend fun removeApp(packageName: String) {
        store.edit {
            val map = IslandAppsCodec.decode(it[K.apps]).toMutableMap()
            map.remove(packageName)
            it[K.apps] = IslandAppsCodec.encode(map.values)
        }
    }
}
