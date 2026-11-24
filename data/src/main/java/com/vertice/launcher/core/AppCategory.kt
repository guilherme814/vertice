package com.vertice.launcher.core

import android.content.pm.ApplicationInfo

/**
 * Categorias de apps. A ordem aqui é a ordem em que aparecem na gaveta.
 *
 * A classificação automática usa a categoria que o próprio app declara
 * (ApplicationInfo.category). Muitos apps não declaram nada, então há uma heurística
 * simples por nome do pacote como segunda tentativa. O usuário sempre pode corrigir
 * manualmente (a correção é salva e vence a automática).
 */
enum class AppCategory(val label: String) {
    Social("Social"),
    Entertainment("Entretenimento"),
    Photos("Fotos e vídeo"),
    Games("Jogos"),
    Productivity("Produtividade"),
    News("Leitura e notícias"),
    Travel("Mapas e viagem"),
    Tools("Utilitários");

    companion object {
        val default = Tools

        fun fromName(name: String?): AppCategory? = entries.firstOrNull { it.name == name }

        fun detect(declared: Int, packageName: String): AppCategory = when (declared) {
            ApplicationInfo.CATEGORY_SOCIAL -> Social
            ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO -> Entertainment
            ApplicationInfo.CATEGORY_IMAGE -> Photos
            ApplicationInfo.CATEGORY_GAME -> Games
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> Productivity
            ApplicationInfo.CATEGORY_NEWS -> News
            ApplicationInfo.CATEGORY_MAPS -> Travel
            else -> guess(packageName)
        }

        private fun guess(pkg: String): AppCategory {
            val p = pkg.lowercase()
            return when {
                p.hasAny("game", "games") -> Games
                p.hasAny("whatsapp", "telegram", "instagram", "facebook", "messenger", "twitter", "tiktok", "discord", "snapchat", "signal") -> Social
                p.hasAny("camera", "gallery", "photos", "photo") -> Photos
                p.hasAny("youtube", "music", "spotify", "netflix", "video", "player", "radio", "podcast") -> Entertainment
                p.hasAny("maps", "uber", "waze", "navigation", "travel", "booking") -> Travel
                p.hasAny("news", "reader", "books", "kindle") -> News
                p.hasAny("docs", "sheets", "slides", "office", "calendar", "mail", "notes", "keep", "drive", "github", "devcheck") -> Productivity
                else -> Tools
            }
        }

        private fun String.hasAny(vararg parts: String) = parts.any { contains(it) }
    }
}
