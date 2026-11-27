package com.vertice.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FoldersTest {
    private val grid = GridConfig.Default
    private val present = setOf("a", "b", "c", "d")
    private fun home(key: String, page: Int = 0, x: Int = 0, y: Int = 0) = LayoutItem(key, Container.Home, page, x, y)
    private fun data(vararg items: LayoutItem, folders: List<Folder> = emptyList()) = HomeData(items.toList(), folders)
    private fun clean(d: HomeData) = FolderOps.sanitize(d, present, grid)

    @Test fun `soltar app sobre app cria pasta na celula do alvo`() {
        val d = clean(FolderOps.createFromDrop(data(home("a", 0, 1, 1), home("b", 0, 2, 2)), draggedKey = "b", targetKey = "a"))
        val f = d.folders.single()
        assertEquals(listOf("a", "b"), f.keys)
        val item = d.items.single()
        assertEquals(f.itemKey, item.key)
        assertEquals(Triple(0, 1, 1), Triple(item.page, item.x, item.y))
    }

    @Test fun `nao cria pasta sobre si mesmo, sobre pasta ou alvo fora da Home`() {
        val base = data(home("a"), LayoutItem("b", Container.Dock, 0, 0, 0))
        assertEquals(base, FolderOps.createFromDrop(base, "a", "a"))
        assertEquals(base, FolderOps.createFromDrop(base, "a", "b"))
        assertEquals(base, FolderOps.createFromDrop(base, "folder:f1", "a"))
    }

    @Test fun `adicionar tira o app da grade e do dock e de outra pasta`() {
        val f1 = Folder("f1", keys = listOf("a", "b"))
        val f2 = Folder("f2", keys = listOf("c", "d"))
        val d = clean(FolderOps.addToFolder(clean(data(home(f1.itemKey), home(f2.itemKey, 0, 1, 0), folders = listOf(f1, f2))), "f1", "c"))
        assertEquals(listOf("a", "b", "c"), d.folders.first { it.id == "f1" }.keys)
        // f2 ficou com 1 app: desfeita, o app que sobrou ocupa a célula da pasta
        assertEquals(listOf("f1"), d.folders.map { it.id })
        assertTrue(d.items.any { it.key == "d" })
        assertFalse(d.items.any { it.key == "c" })
    }

    @Test fun `pasta com um app se desfaz e o app assume a celula`() {
        val f = Folder("f1", keys = listOf("a", "b"))
        val start = clean(data(home(f.itemKey, 0, 3, 4), folders = listOf(f)))
        val d = clean(FolderOps.removeFromFolder(start, "f1", "b", grid))
        assertTrue(d.folders.isEmpty())
        val a = d.items.first { it.key == "a" }
        assertEquals(Triple(0, 3, 4), Triple(a.page, a.x, a.y))
        assertTrue(d.items.any { it.key == "b" })
    }

    @Test fun `desfazer pasta devolve todos os apps para a Home`() {
        val f = Folder("f1", keys = listOf("a", "b", "c"))
        val d = clean(FolderOps.dissolve(clean(data(home(f.itemKey), folders = listOf(f))), "f1", grid))
        assertTrue(d.folders.isEmpty())
        assertEquals(setOf("a", "b", "c"), d.items.map { it.key }.toSet())
    }

    @Test fun `sanitize remove membros inexistentes, repetidos e pasta vazia, e e idempotente`() {
        val f1 = Folder("f1", keys = listOf("a", "zzz", "b"))
        val f2 = Folder("f2", keys = listOf("a", "c"))          // "a" ja esta em f1
        val f3 = Folder("f3", keys = listOf("nao-existe"))
        val raw = data(home("a"), home(f1.itemKey, 0, 1, 0), home(f2.itemKey, 0, 2, 0), home("ghost", 0, 3, 0), folders = listOf(f1, f2, f3))
        val d = clean(raw)
        assertEquals(listOf("f1"), d.folders.map { it.id })
        assertEquals(listOf("a", "b"), d.folders.single().keys)
        assertTrue(d.items.none { it.key == "a" || it.key == "ghost" || it.key == f2.itemKey })
        assertEquals(d, clean(d))
    }

    @Test fun `pasta sem posicao ganha celula e pasta no dock volta para a Home`() {
        val f = Folder("f1", keys = listOf("a", "b"))
        assertEquals(Container.Home, clean(data(folders = listOf(f))).items.single().container)
        val inDock = clean(data(LayoutItem(f.itemKey, Container.Dock, 0, 0, 0), folders = listOf(f)))
        assertEquals(Container.Home, inDock.items.single().container)
    }

    @Test fun `nome vazio vira Pasta e nome longo e cortado`() {
        val f = Folder("f1", keys = listOf("a", "b"))
        val d = data(home(f.itemKey), folders = listOf(f))
        assertEquals("Pasta", FolderOps.rename(d, "f1", "   ").folders.single().name)
        assertEquals(Folder.MAX_NAME, FolderOps.rename(d, "f1", "x".repeat(99)).folders.single().name.length)
        assertEquals("Jogos", clean(FolderOps.rename(d, "f1", "  Jogos ")).folders.single().name)
    }

    @Test fun `ids novos nao colidem`() {
        val f = Folder("f1", keys = listOf("a", "b"))
        val d = FolderOps.createFromDrop(data(home("c"), home("d", 0, 1, 0), home(f.itemKey, 0, 2, 0), folders = listOf(f)), "d", "c")
        assertEquals(setOf("f1", "f2"), d.folders.map { it.id }.toSet())
    }

    @Test fun `codec faz ida e volta e recusa linha invalida`() {
        val f = Folder("f1", "Jogos & Cia, 100%", FolderTint.Rose, FolderCover.Stack, listOf("com.a/.Main", "x,y|z"))
        assertEquals(f, FolderCodec.decode(FolderCodec.encode(f)))
        assertNull(FolderCodec.decode("lixo"))
        assertEquals(Folder("f9"), FolderCodec.decode("f9|Pasta|Glass|Grid|"))
    }

    @Test fun `pasta aparece no HomeState com os apps em Folder`() {
        val apps = present.map { AppModel(it, "p.$it", it, 0, -1) }
        val f = Folder("f1", keys = listOf("a", "b"))
        val s = HomeState.from(apps, Prefs(layout = listOf(home(f.itemKey), home("c", 0, 1, 0)), folders = listOf(f), seeded = true))
        assertEquals(Placement.Folder, s.placement["a"])
        assertEquals(Placement.Home, s.placement["c"])
        assertEquals(listOf("f1"), s.folders.map { it.id })
        assertTrue(s.grid.any { it.key == f.itemKey })
    }
}
