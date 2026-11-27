package com.vertice.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import com.vertice.launcher.core.DrawerColumns
import com.vertice.launcher.core.DrawerLayout
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vertice.launcher.HomeUi
import com.vertice.launcher.HomeActions
import com.vertice.launcher.core.AppCategory
import com.vertice.launcher.data.AppEntry
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.IntSize
import com.vertice.launcher.core.DrawerList
import com.vertice.launcher.core.DrawerSort
import com.vertice.launcher.core.foldForSearch
import kotlinx.coroutines.launch

@Composable
fun AppDrawer(
    vm: HomeActions,
    ui: HomeUi,
    focusSearch: Boolean = false,
    onLaunch: (AppEntry, Rect?) -> Unit,
    onClose: () -> Unit
) {
    val searchFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    // Aberta por "deslizar para baixo": o teclado já sobe com o cursor no campo de busca.
    androidx.compose.runtime.LaunchedEffect(focusSearch) {
        if (focusSearch) try { searchFocus.requestFocus() } catch (_: Exception) {}
    }
    var query by remember { mutableStateOf("") }
    // null = "Todos". Guardado por nome para sobreviver a rotação/recriação.
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    var showHidden by rememberSaveable { mutableStateOf(false) }
    val hiddenCount = remember(ui.apps, ui.hidden) { ui.apps.count { it.key in ui.hidden } }
    // Sem nenhum oculto, o modo "Ocultos" não faz sentido: volta ao normal.
    val hiddenMode = showHidden && hiddenCount > 0
    // Base da gaveta: sempre sem os ocultos (a menos que o modo "Ocultos" esteja ligado).
    val drawerHidden = ui.drawerHidden
    val base = remember(ui.apps, ui.hidden, drawerHidden, ui.drawerSort, ui.usage, hiddenMode) {
        val models = ui.apps.associateBy { it.key }
        val sorted = if (hiddenMode) ui.apps.filter { it.key in ui.hidden }.sortedBy { it.label.foldForSearch() }
        else DrawerList.sorted(ui.apps.map { it.model }, ui.drawerSort, drawerHidden, ui.usage).mapNotNull { models[it.key] }
        sorted
    }
    val counts = remember(base, ui.categories) {
        AppCategory.entries.associateWith { c -> base.count { ui.categoryOf(it) == c } }
            .filterValues { it > 0 }
    }
    // Se a categoria escolhida esvaziou (app movido/desinstalado), volta para "Todos".
    val selected = AppCategory.fromName(selectedName)?.takeIf { it in counts }
    val searching = query.isNotBlank()

    // Busca ignora acentos e maiúsculas, acha também pelo pacote e vale para TODAS as categorias.
    val results = remember(query, selected, base, ui.categories) {
        val q = query.foldForSearch().trim()
        when {
            q.isNotEmpty() -> base.filter { it.label.foldForSearch().contains(q) || it.packageName.contains(q, ignoreCase = true) }
            selected != null -> base.filter { ui.categoryOf(it) == selected }
            else -> base
        }
    }
    val sectioned = !searching && !hiddenMode && ui.drawerSort == DrawerSort.AZ
    val sections = remember(results, sectioned) {
        if (!sectioned) emptyList() else {
            val byKey = results.associateBy { it.key }
            DrawerList.sections(results.map { it.model }).map { s -> s.letter to s.items.mapNotNull { byKey[it.key] } }
        }
    }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { } } // nada passa para a tela inicial por baixo
            .background(Ink.copy(alpha = 0.94f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("BIBLIOTECA", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Aplicativos", color = Snow, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "✕", color = Muted, fontSize = 22.sp,
                modifier = Modifier.clip(CircleShape).clickable { onClose() }.padding(12.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().focusRequester(searchFocus),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            placeholder = { Text("Buscar por nome ou pacote", color = Muted) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { results.firstOrNull()?.let { onLaunch(it, null) } }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Mint, unfocusedBorderColor = PanelSoft,
                focusedTextColor = Snow, unfocusedTextColor = Snow, cursorColor = Mint
            )
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            DrawerSort.entries.forEach { s ->
                CategoryChip(s.label, !hiddenMode && ui.drawerSort == s) { showHidden = false; vm.setDrawerSort(s) }
            }
            if (hiddenCount > 0) CategoryChip("Ocultos  $hiddenCount", hiddenMode) { showHidden = !hiddenMode }
        }
        if (!searching && !hiddenMode && selected == null && ui.mostUsed.isNotEmpty()) {
            Text("Mais usados", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp, start = 4.dp))
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                ui.mostUsed.forEach { app -> Box(Modifier.weight(1f)) { DrawerTile(app, vm, ui, onLaunch) } }
                // Mantém as colunas alinhadas quando há menos de 5.
                repeat(5 - ui.mostUsed.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (!searching && counts.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "all") {
                    CategoryChip("Todos  ${base.size}", selected == null) { selectedName = null }
                }
                items(counts.entries.toList(), key = { it.key.name }) { (category, n) ->
                    CategoryChip("${category.label}  $n", selected == category) { selectedName = category.name }
                }
            }
        }
        Text(
            if (ui.loading) "Carregando..." else if (hiddenMode) "${results.size} ocultos · toque e segure para mostrar" else "${results.size} aplicativos",
            color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
        )

        if (!ui.loading && results.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhum aplicativo encontrado.", color = Muted)
            }
        } else {
            Box(Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(if (ui.drawerLayout == DrawerLayout.List) 1 else ui.drawerColumns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp, end = if (sectioned) 22.dp else 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (sectioned) {
                        sections.forEach { (letter, apps) ->
                            item(key = "h$letter", span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    letter.toString(), color = Mint, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 6.dp, top = 8.dp)
                                )
                            }
                            items(apps, key = { it.key }) { app -> DrawerTile(app, vm, ui, onLaunch) }
                        }
                    } else {
                        items(results, key = { it.key }) { app -> DrawerTile(app, vm, ui, onLaunch) }
                    }
                }
                if (sectioned && sections.size > 1) {
                    // Posição de cada cabeçalho na grade: cada letra ocupa 1 item + seus apps.
                    val starts = remember(sections) {
                        var n = 0
                        sections.map { (_, apps) -> n.also { n += 1 + apps.size } }
                    }
                    LetterIndex(
                        letters = sections.map { it.first },
                        modifier = Modifier.align(Alignment.CenterEnd),
                        onPick = { i -> scope.launch { gridState.scrollToItem(starts[i]) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerTile(app: AppEntry, vm: HomeActions, ui: HomeUi, onLaunch: (AppEntry, Rect?) -> Unit) {
    AppTile(
        app = app, vm = vm, animateLaunch = ui.profile.animations,
        showLabel = true, onLaunch = onLaunch,
        listRow = ui.drawerLayout == DrawerLayout.List,
        iconSize = 52.dp * ui.iconSize.scale * (if (ui.drawerLayout == DrawerLayout.List) 0.8f else DrawerColumns.iconScale(ui.drawerColumns)),
        iconStyle = ui.iconStyle,
        locked = ui.isLocked(app.key)
    )
}

/** Índice lateral: toque ou arraste sobre as letras para saltar de seção. */
@Composable
private fun LetterIndex(letters: List<Char>, modifier: Modifier = Modifier, onPick: (Int) -> Unit) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var last by remember { mutableStateOf(-1) }
    fun pick(y: Float) {
        if (size.height == 0 || letters.isEmpty()) return
        val i = (y / size.height * letters.size).toInt().coerceIn(0, letters.size - 1)
        if (i != last) { last = i; onPick(i) }
    }
    Column(
        modifier
            .width(22.dp)
            .liquidGlass(RoundedCornerShape(11.dp), alpha = Glass.pill)
            .onSizeChanged { size = it }
            .pointerInput(letters) {
                detectTapGestures { pick(it.y) }
            }
            .pointerInput(letters) {
                detectVerticalDragGestures(
                    onDragStart = { last = -1; pick(it.y) },
                    onVerticalDrag = { change, _ -> pick(change.position.y) }
                )
            }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        letters.forEach { Text(it.toString(), color = Snow, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
    }
}
