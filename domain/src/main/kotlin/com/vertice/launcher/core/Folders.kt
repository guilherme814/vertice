package com.vertice.launcher.core

import java.net.URLDecoder
import java.net.URLEncoder

/** Cor da pasta. [argb] null = vidro puro (sem cor). */
enum class FolderTint(val label: String, val argb: Long?) {
    Glass("Vidro", null),
    Mint("Menta", 0xFF3DDC97),
    Sky("Céu", 0xFF4DA3FF),
    Rose("Rosa", 0xFFFF6B9A),
    Amber("Âmbar", 0xFFFFB54D),
    Violet("Violeta", 0xFFA78BFA);

    companion object {
        fun fromName(name: String?): FolderTint = entries.firstOrNull { it.name == name } ?: Glass
    }
}

/** Como a pasta aparece fechada: miniaturas em grade 2x2 ou em pilha (leque). */
enum class FolderCover(val label: String) {
    Grid("Grade"),
    Stack("Pilha");

    companion object {
        fun fromName(name: String?): FolderCover = entries.firstOrNull { it.name == name } ?: Grid
    }
}

/**
 * Pasta de apps. Na grade ela ocupa uma célula como um item de layout de chave [itemKey];
 * os apps dentro dela NÃO aparecem na grade nem no dock (só aqui, em [keys], na ordem).
 */
data class Folder(
    val id: String,
    val name: String = DEFAULT_NAME,
    val tint: FolderTint = FolderTint.Glass,
    val cover: FolderCover = FolderCover.Grid,
    val keys: List<String> = emptyList()
) {
    val itemKey: String get() = KEY_PREFIX + id

    companion object {
        const val DEFAULT_NAME = "Pasta"
        const val MAX_NAME = 24
        const val KEY_PREFIX = "folder:"
    }
}

/** O que muda junto numa edição da Home: posições e pastas (sempre gravados na mesma transação). */
data class HomeData(val items: List<LayoutItem> = emptyList(), val folders: List<Folder> = emptyList())

/** Operações sobre pastas. Funções puras; toda edição termina em [sanitize]. */
object FolderOps {
    fun isFolderKey(key: String) = key.startsWith(Folder.KEY_PREFIX)

    private fun cleanName(raw: String) = raw.trim().take(Folder.MAX_NAME).ifBlank { Folder.DEFAULT_NAME }

    private fun newId(folders: List<Folder>): String =
        generateSequence(1) { it + 1 }.map { "f$it" }.first { id -> folders.none { it.id == id } }

    /** Tira o app de qualquer pasta (usado antes de ele ir para a Home ou o dock). */
    fun detach(d: HomeData, key: String): HomeData =
        d.copy(folders = d.folders.map { if (key in it.keys) it.copy(keys = it.keys - key) else it })

    /**
     * Deixa pastas e layout consistentes: membros que não existem mais saem; cada app fica em uma única pasta;
     * pasta vazia some; pasta com 1 app se desfaz (o app assume a célula); apps de pasta saem da grade e do dock;
     * pasta sem posição ganha a primeira célula livre; pasta nunca fica no dock. Idempotente.
     */
    fun sanitize(d: HomeData, present: Set<String>, grid: GridConfig = GridConfig.Default): HomeData {
        val ids = HashSet<String>()
        val members = HashSet<String>()
        val cleaned = d.folders.filter { ids.add(it.id) }.map { f ->
            f.copy(name = cleanName(f.name), keys = f.keys.filter { it in present && !isFolderKey(it) && members.add(it) })
        }
        val live = cleaned.filter { it.keys.size >= 2 }

        var items = d.items.map {
            // Pasta no dock volta para a Home (o sanitize do layout acha uma célula).
            if (isFolderKey(it.key) && it.container == Container.Dock) LayoutItem(it.key, Container.Home, -1, -1, -1) else it
        }
        for (f in cleaned.filter { it.keys.size == 1 }) {
            val at = items.firstOrNull { it.key == f.itemKey }
            items = items.filter { it.key != f.itemKey }
            val only = f.keys[0]
            if (at != null && items.none { it.key == only }) items = items + at.copy(key = only)
        }
        val inside = live.flatMap { it.keys }.toSet()
        val liveKeys = live.map { it.itemKey }.toSet()
        items = items.filter { it.key !in inside && (!isFolderKey(it.key) || it.key in liveKeys) }
        for (f in live) if (items.none { it.key == f.itemKey }) items = LayoutOps.addToHome(items, f.itemKey, grid)

        return HomeData(LayoutOps.sanitize(items, present + liveKeys, grid), live)
    }

    /** Solta [draggedKey] sobre [targetKey] (ambos apps, o alvo na Home): nasce uma pasta na célula do alvo. */
    fun createFromDrop(d: HomeData, draggedKey: String, targetKey: String): HomeData {
        if (draggedKey == targetKey || isFolderKey(draggedKey) || isFolderKey(targetKey)) return d
        val target = d.items.firstOrNull { it.key == targetKey && it.container == Container.Home } ?: return d
        val folder = Folder(newId(d.folders), keys = listOf(targetKey, draggedKey))
        val items = d.items.filter { it.key != draggedKey && it.key != targetKey } +
            LayoutItem(folder.itemKey, Container.Home, target.page, target.x, target.y)
        return HomeData(items, d.folders + folder)
    }

    fun addToFolder(d: HomeData, folderId: String, key: String): HomeData {
        if (isFolderKey(key) || d.folders.none { it.id == folderId }) return d
        val base = detach(d, key)
        return HomeData(
            base.items.filter { it.key != key },
            base.folders.map { if (it.id == folderId) it.copy(keys = it.keys + key) else it }
        )
    }

    /** Tira o app da pasta e o põe na primeira célula livre da Home. */
    fun removeFromFolder(d: HomeData, folderId: String, key: String, grid: GridConfig = GridConfig.Default): HomeData {
        val f = d.folders.firstOrNull { it.id == folderId } ?: return d
        if (key !in f.keys) return d
        val base = detach(d, key)
        return base.copy(items = LayoutOps.addToHome(base.items, key, grid))
    }

    /** Desfaz a pasta: os apps vão para a Home. */
    fun dissolve(d: HomeData, folderId: String, grid: GridConfig = GridConfig.Default): HomeData {
        val f = d.folders.firstOrNull { it.id == folderId } ?: return d
        var items = d.items.filter { it.key != f.itemKey }
        f.keys.forEach { items = LayoutOps.addToHome(items, it, grid) }
        return HomeData(items, d.folders.filter { it.id != folderId })
    }

    private fun update(d: HomeData, id: String, f: (Folder) -> Folder) =
        d.copy(folders = d.folders.map { if (it.id == id) f(it) else it })

    fun rename(d: HomeData, id: String, name: String) = update(d, id) { it.copy(name = cleanName(name)) }
    fun setTint(d: HomeData, id: String, tint: FolderTint) = update(d, id) { it.copy(tint = tint) }
    fun setCover(d: HomeData, id: String, cover: FolderCover) = update(d, id) { it.copy(cover = cover) }
}

/** Texto <-> pasta (uma linha), com campos codificados. */
object FolderCodec {
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun dec(s: String) = URLDecoder.decode(s, "UTF-8")

    fun encode(f: Folder) =
        "${enc(f.id)}|${enc(f.name)}|${f.tint.name}|${f.cover.name}|${f.keys.joinToString(",") { enc(it) }}"

    fun decode(line: String): Folder? {
        val p = line.split('|')
        if (p.size != 5) return null
        return try {
            val id = dec(p[0]).takeIf { it.isNotBlank() } ?: return null
            Folder(
                id, dec(p[1]), FolderTint.fromName(p[2]), FolderCover.fromName(p[3]),
                if (p[4].isEmpty()) emptyList() else p[4].split(',').map(::dec)
            )
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun encodeAll(folders: List<Folder>): Set<String> = folders.map(::encode).toSet()
    fun decodeAll(lines: Set<String>): List<Folder> = lines.mapNotNull(::decode).sortedBy { it.id }
}
