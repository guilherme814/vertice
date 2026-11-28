package com.vertice.launcher.island

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.vertice.launcher.core.island.RawNotification

/**
 * Recebe as notificações do sistema (o usuário concede em "Acesso a notificações").
 * Nenhum conteúdo é gravado ou registrado: o retrato segue para a memória do controlador e morre ali.
 */
class IslandNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        IslandRuntime.controller()?.start()
    }

    override fun onListenerDisconnected() {
        IslandRuntime.controller()?.stop()
        // Se o sistema nos desligou por engano (comum em MIUI), pede para ser religado.
        try { requestRebind(android.content.ComponentName(this, IslandNotificationListener::class.java)) } catch (_: Throwable) {}
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return   // as notificações da própria launcher não entram
        val controller = IslandRuntime.controller() ?: return
        controller.onNotification(toRaw(sbn), sbn.notification.contentIntent, sbn.notification.actions?.mapNotNull { it.actionIntent }.orEmpty())
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        IslandRuntime.controller()?.onNotificationRemoved(sbn.key)
    }

    private fun toRaw(sbn: StatusBarNotification): RawNotification {
        val n = sbn.notification
        val extras = n.extras
        val silent = try {
            val r = Ranking()
            currentRanking.getRanking(sbn.key, r) && r.importance < android.app.NotificationManager.IMPORTANCE_DEFAULT
        } catch (_: Throwable) { false }
        return RawNotification(
            key = sbn.key,
            packageName = sbn.packageName,
            category = n.category,
            isOngoing = n.flags and Notification.FLAG_ONGOING_EVENT != 0,
            isGroupSummary = n.flags and Notification.FLAG_GROUP_SUMMARY != 0,
            isSilent = silent,
            isSecret = n.visibility == Notification.VISIBILITY_SECRET,
            chronometerBase = if (extras?.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER) == true) n.`when` else null,
            hasCallActions = n.category == Notification.CATEGORY_CALL && (n.actions?.isNotEmpty() == true),
            actions = n.actions?.map { it.title?.toString().orEmpty().take(24) }.orEmpty(),
            title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.take(120),
            text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.take(200)
        )
    }
}
