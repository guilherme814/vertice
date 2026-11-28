package com.vertice.launcher.core.island

/** Decide se uma notificação vira evento da ilha, e de que tipo. Pura: sem Android, sem relógio. */
object IslandClassifier {

    fun classify(
        n: RawNotification,
        settings: IslandSettings,
        apps: Map<String, IslandAppConfig>
    ): IslandEvent? {
        if (!settings.enabled || n.isGroupSummary) return null

        // Chamada: precisa das ações (atender/recusar) para ser uma chamada de verdade.
        if (n.category == RawNotification.CATEGORY_CALL && n.hasCallActions) {
            if (!settings.calls) return null
            return IslandEvent.Call(
                key = n.key, packageName = n.packageName,
                caller = if (n.isSecret) null else n.title,
                inProgress = n.chronometerBase != null, chronometerBase = n.chronometerBase,
                actions = n.actions.take(3)
            )
        }

        // Timer/cronômetro/alarme em contagem.
        if (n.chronometerBase != null && (n.category == RawNotification.CATEGORY_ALARM || n.isOngoing)) {
            if (!settings.timers) return null
            return IslandEvent.Timer(n.key, n.packageName, if (n.isSecret) null else n.title, n.chronometerBase)
        }

        // "App rodando", sincronização e silenciosas são ruído.
        if (n.isOngoing || n.isSilent) return null

        val cfg = apps[n.packageName]?.takeIf { it.enabled } ?: return null
        val hide = !cfg.showContent || n.isSecret
        return IslandEvent.Notification(
            key = n.key, packageName = n.packageName,
            title = if (hide) null else n.title,
            text = if (hide) null else n.text,
            contentHidden = hide,
            persistent = cfg.mode == IslandMode.Persistent
        )
    }
}
