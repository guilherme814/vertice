package com.vertice.launcher.core.island

import kotlinx.coroutines.flow.Flow

/** Preferências da ilha, num armazenamento próprio (o serviço não acorda o resto das preferências da launcher). */
interface IslandStore {
    val settings: Flow<IslandSettings>
    val apps: Flow<Map<String, IslandAppConfig>>

    suspend fun setSettings(settings: IslandSettings)
    suspend fun setApp(config: IslandAppConfig)
    suspend fun removeApp(packageName: String)

    /** Troca tudo de uma vez (restauração de backup). */
    suspend fun replaceAll(snapshot: IslandSnapshot)
}
