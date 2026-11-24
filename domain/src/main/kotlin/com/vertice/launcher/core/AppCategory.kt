package com.vertice.launcher.core

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

        // Valores de ApplicationInfo.CATEGORY_* (constantes estáveis do Android), copiados para o domínio
        // não depender do SDK. Os testes travam estes números.
        const val CAT_GAME = 0
        const val CAT_AUDIO = 1
        const val CAT_VIDEO = 2
        const val CAT_IMAGE = 3
        const val CAT_SOCIAL = 4
        const val CAT_NEWS = 5
        const val CAT_MAPS = 6
        const val CAT_PRODUCTIVITY = 7

        fun fromName(name: String?): AppCategory? = entries.firstOrNull { it.name == name }

        fun detect(declared: Int, packageName: String): AppCategory = when (declared) {
            CAT_SOCIAL -> Social
            CAT_AUDIO, CAT_VIDEO -> Entertainment
            CAT_IMAGE -> Photos
            CAT_GAME -> Games
            CAT_PRODUCTIVITY -> Productivity
            CAT_NEWS -> News
            CAT_MAPS -> Travel
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
