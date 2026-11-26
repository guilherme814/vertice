package com.vertice.launcher.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vertice.launcher.HomeActions
import com.vertice.launcher.core.AppCategory
import com.vertice.launcher.core.IconStyle
import com.vertice.launcher.core.Placement
import com.vertice.launcher.data.AppEntry
import kotlin.math.roundToInt

private class Where { var pos = androidx.compose.ui.geometry.Offset.Zero; var w = 0; var h = 0 }

/**
 * App tocável: toque abre (com animação de zoom a partir do ícone, se o perfil permitir);
 * segurar abre o menu. Arrastar para reorganizar entra numa fase futura.
 */
@Composable
fun AppTile(
    app: AppEntry,
    vm: HomeActions,
    animateLaunch: Boolean,
    showLabel: Boolean,
    onLaunch: (AppEntry, Rect?) -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 52.dp,
    labelLines: Int = 2,
    iconStyle: IconStyle = IconStyle(),
    /** Bloqueado pelo Modo Foco: apagado e não abre. */
    locked: Boolean = false,
    /** Se informado, o menu oferece "Mover" (entra no modo de mover da Home). */
    onMove: (() -> Unit)? = null,
    /** Se informado, segurar começa um arrasto (posição do dedo na raiz); soltar sem mover abre o menu. */
    onDragStart: ((androidx.compose.ui.geometry.Offset) -> Unit)? = null,
    /** O pai pede o menu (soltou sem arrastar). */
    menuOpen: Boolean = false,
    onMenuClosed: () -> Unit = {}
) {
    var menu by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    // Posição só é lida ao tocar: guardada fora do estado, senão cada rolagem da gaveta recomporia todos os ícones.
    val where = remember { Where() }
    val haptic = LocalHapticFeedback.current

    // Retorno ao toque: o ícone "afunda" um pouco (só se o perfil de desempenho permitir animação).
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed && animateLaunch) 0.9f else 1f, tween(90), label = "press")

    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            // Acessibilidade (TalkBack): um único alvo com rótulo, ação de abrir e "mais opções".
            .semantics(mergeDescendants = true) {
                onClick(label = if (locked) "${app.label} bloqueado pelo Modo Foco" else "Abrir ${app.label}") { if (!locked) onLaunch(app, null); true }
                onLongClick(label = "Mais opções") { menu = true; true }
                customActions = listOf(CustomAccessibilityAction("Mais opções") { menu = true; true })
            }
            .onGloballyPositioned { where.pos = it.positionInRoot(); where.w = it.size.width; where.h = it.size.height }
            .pointerInput(app.key, animateLaunch, locked) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = {
                        if (locked) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        } else {
                            val rect = if (animateLaunch)
                                Rect(where.pos, Size(where.w.toFloat(), where.h.toFloat()))
                            else null
                            onLaunch(app, rect)
                        }
                    },
                    onLongPress = { at ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (onDragStart != null) onDragStart(where.pos + at) else menu = true
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppIcon(app, vm.icons, iconSize, Modifier.alpha(if (locked) 0.28f else if (app.model.suspended) 0.4f else 1f), iconStyle)
            if (showLabel) {
                Spacer(Modifier.height(6.dp))
                Text(
                    app.label, color = Snow, fontSize = 11.sp, maxLines = labelLines, lineHeight = 13.sp,
                    overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (picking) CategoryDialog(app, vm) { picking = false }

        val closeMenu = { menu = false; onMenuClosed() }
        // Menu só existe enquanto aberto: 90 menus fechados custavam composição à toa na gaveta.
        if (menu || menuOpen) DropdownMenu(expanded = menu || menuOpen, onDismissRequest = closeMenu) {
            DropdownMenuItem(
                text = { Text("Informações do app") },
                onClick = { closeMenu(); vm.openAppInfo(app) }
            )
            val placement = vm.placementOf(app)
            if (placement == Placement.Home && onMove != null) {
                DropdownMenuItem(text = { Text("Mover") }, onClick = { closeMenu(); onMove() })
            }
            if (placement == Placement.Folder) {
                DropdownMenuItem(text = { Text("Tirar da pasta") }, onClick = { closeMenu(); vm.removeFromFolder(app) })
            }
            if (placement == Placement.Home) {
                DropdownMenuItem(text = { Text("Tirar da tela inicial") }, onClick = { closeMenu(); vm.removeFromHome(app) })
            } else {
                DropdownMenuItem(text = { Text("Colocar na tela inicial") }, onClick = { closeMenu(); vm.addToHome(app) })
            }
            DropdownMenuItem(
                text = { Text(if (placement == Placement.Dock) "Tirar do dock" else "Colocar no dock") },
                onClick = { closeMenu(); vm.toggleDock(app) }
            )
            DropdownMenuItem(
                text = { Text("Mudar categoria") },
                onClick = { closeMenu(); picking = true }
            )
            val focusBlocked = app.key in vm.ui.value.focusBlocked
            DropdownMenuItem(
                text = { Text(if (focusBlocked) "Liberar no Modo Foco" else "Bloquear no Modo Foco") },
                onClick = { closeMenu(); vm.toggleFocusBlocked(app) }
            )
            val isHidden = app.key in vm.ui.value.hidden
            DropdownMenuItem(
                text = { Text(if (isHidden) "Mostrar na gaveta" else "Ocultar da gaveta") },
                onClick = { closeMenu(); vm.setHidden(app, !isHidden) }
            )
            DropdownMenuItem(
                text = { Text("Desinstalar") },
                onClick = { closeMenu(); vm.uninstall(app) }
            )
        }
    }
}

/** Escolha de categoria: uma das fixas ou "Automática" (volta à classificação do app). */
@Composable
private fun CategoryDialog(app: AppEntry, vm: HomeActions, onDismiss: () -> Unit) {
    val ui = vm.ui.value
    val current = ui.categoryOf(app)
    val manual = app.key in ui.manualCategories
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.label) },
        text = {
            Column {
                AppCategory.entries.forEach { c ->
                    CategoryRow(c.label, selected = manual && c == current) {
                        vm.setCategory(app, c); onDismiss()
                    }
                }
                CategoryRow("Automática", selected = !manual) {
                    vm.setCategory(app, null); onDismiss()
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

@Composable
private fun CategoryRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        (if (selected) "●  " else "○  ") + text,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 11.dp),
        fontSize = 15.sp
    )
}
