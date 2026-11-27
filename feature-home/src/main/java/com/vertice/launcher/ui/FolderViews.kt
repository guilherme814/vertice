package com.vertice.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vertice.launcher.HomeActions
import com.vertice.launcher.HomeUi
import com.vertice.launcher.PlacedFolder
import com.vertice.launcher.core.Folder
import com.vertice.launcher.core.FolderCover
import com.vertice.launcher.core.FolderTint
import com.vertice.launcher.core.IconStyle
import com.vertice.launcher.data.AppEntry

/** Pasta na grade da Home: toque abre; segurar e arrastar move; segurar sem mover abre a edição. */
@Composable
fun FolderTile(
    placed: PlacedFolder,
    vm: HomeActions,
    iconSize: Dp,
    showLabel: Boolean,
    iconStyle: IconStyle,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onDragStart: (Offset) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var where by remember { mutableStateOf(Offset.Zero) }
    Column(
        modifier
            .onGloballyPositioned { where = it.positionInRoot() }
            .semantics { contentDescription = "Pasta ${placed.folder.name}, ${placed.apps.size} apps" }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onOpen() },
                    onLongPress = { at ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDragStart(where + at)
                    }
                )
            }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FolderIcon(placed.folder, placed.apps, vm.icons, iconSize, iconStyle)
        if (showLabel) {
            Spacer(Modifier.height(6.dp))
            Text(
                placed.folder.name, color = Snow, fontSize = 11.sp, maxLines = 1, lineHeight = 13.sp,
                overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Pasta aberta: painel de liquid glass com o nome editável, os apps, cor e capa.
 * Tocar fora ou voltar fecha; o nome só é gravado ao confirmar ou fechar (não a cada letra).
 */
@Composable
fun FolderSheet(
    placed: PlacedFolder,
    ui: HomeUi,
    vm: HomeActions,
    onClose: () -> Unit,
    onLaunch: (AppEntry, Rect?) -> Unit
) {
    BackHandler(onBack = onClose)
    val folder = placed.folder
    var name by remember(folder.id) { mutableStateOf(folder.name) }
    val commit by rememberUpdatedState { if (name.trim() != folder.name) vm.renameFolder(folder.id, name) }
    DisposableEffect(folder.id) { onDispose { commit() } }

    Box(
        Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.padding(horizontal = 22.dp).fillMaxWidth()
                .liquidGlass(RoundedCornerShape(32.dp), alpha = 0.72f, tint = folder.tint.color())
                .pointerInput(Unit) { detectTapGestures { } } // toques no painel não fecham
                .padding(18.dp)
        ) {
            BasicTextField(
                value = name,
                onValueChange = { name = it.take(Folder.MAX_NAME) },
                singleLine = true,
                textStyle = TextStyle(color = Snow, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
                cursorBrush = SolidColor(Mint),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commit() }),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                contentPadding = PaddingValues(0.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(placed.apps, key = { it.key }) { app ->
                    AppTile(
                        app = app, vm = vm, animateLaunch = ui.profile.animations, showLabel = true,
                        onLaunch = { a, r -> onClose(); onLaunch(a, r) },
                        iconSize = 48.dp * ui.iconScale, iconStyle = ui.iconStyle,
                        locked = ui.isLocked(app.key)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                FolderTint.entries.forEach { t ->
                    val selected = t == folder.tint
                    Box(
                        Modifier.size(28.dp).clip(CircleShape)
                            .background(t.color() ?: Color.White.copy(alpha = 0.25f))
                            .then(if (selected) Modifier.border(2.dp, Snow, CircleShape) else Modifier)
                            .semantics { contentDescription = "Cor ${t.label}" }
                            .clickable { vm.setFolderTint(folder.id, t) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FolderCover.entries.forEach { c ->
                    CategoryChip("Capa: ${c.label}", folder.cover == c) { vm.setFolderCover(folder.id, c) }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "Desfazer", color = Muted, fontSize = 13.sp,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { vm.dissolveFolder(folder.id); onClose() }.padding(8.dp)
                )
            }
        }
    }
}
