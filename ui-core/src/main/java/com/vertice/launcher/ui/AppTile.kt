package com.vertice.launcher.ui

import androidx.compose.foundation.Image
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
import androidx.compose.ui.input.pointer.pointerInput
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
import com.vertice.launcher.core.Placement
import com.vertice.launcher.data.AppEntry
import kotlin.math.roundToInt

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
    /** Se informado, o menu oferece "Mover" (entra no modo de mover da Home). */
    onMove: (() -> Unit)? = null
) {
    var menu by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    var rootPos by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var boxSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val haptic = LocalHapticFeedback.current

    Box(
        modifier
            .onGloballyPositioned { rootPos = it.positionInRoot(); boxSize = it.size }
            .pointerInput(app.key, animateLaunch) {
                detectTapGestures(
                    onTap = {
                        val rect = if (animateLaunch)
                            Rect(rootPos, Size(boxSize.width.toFloat(), boxSize.height.toFloat()))
                        else null
                        onLaunch(app, rect)
                    },
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menu = true
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppIcon(app, vm.icons, iconSize)
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

        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text("Informações do app") },
                onClick = { menu = false; vm.openAppInfo(app) }
            )
            val placement = vm.placementOf(app)
            if (placement == Placement.Home && onMove != null) {
                DropdownMenuItem(text = { Text("Mover") }, onClick = { menu = false; onMove() })
            }
            if (placement == Placement.Home) {
                DropdownMenuItem(text = { Text("Tirar da tela inicial") }, onClick = { menu = false; vm.removeFromHome(app) })
            } else {
                DropdownMenuItem(text = { Text("Colocar na tela inicial") }, onClick = { menu = false; vm.addToHome(app) })
            }
            DropdownMenuItem(
                text = { Text(if (placement == Placement.Dock) "Tirar do dock" else "Colocar no dock") },
                onClick = { menu = false; vm.toggleDock(app) }
            )
            DropdownMenuItem(
                text = { Text("Mudar categoria") },
                onClick = { menu = false; picking = true }
            )
            DropdownMenuItem(
                text = { Text("Desinstalar") },
                onClick = { menu = false; vm.uninstall(app) }
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
