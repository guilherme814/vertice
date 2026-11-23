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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
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
import com.vertice.launcher.LauncherViewModel
import com.vertice.launcher.data.AppEntry
import kotlin.math.roundToInt

/** Ícone assíncrono: usa o cache e só decodifica se faltar. */
@Composable
fun AppIcon(app: AppEntry, vm: LauncherViewModel, size: Dp, modifier: Modifier = Modifier) {
    val px = with(LocalDensity.current) { size.toPx() }.roundToInt()
    var bitmap: ImageBitmap? by remember(app.key, px) { mutableStateOf(vm.icons.peek(app, px)) }
    LaunchedEffect(app.key, px) {
        if (bitmap == null) bitmap = vm.icons.load(app, px)
    }
    Box(modifier.size(size)) {
        bitmap?.let { Image(bitmap = it, contentDescription = app.label, modifier = Modifier.size(size)) }
    }
}

/**
 * App tocável: toque abre (com animação de zoom a partir do ícone, se o perfil permitir);
 * segurar abre o menu. Arrastar para reorganizar entra numa fase futura.
 */
@Composable
fun AppTile(
    app: AppEntry,
    vm: LauncherViewModel,
    animateLaunch: Boolean,
    showLabel: Boolean,
    onLaunch: (AppEntry, Rect?) -> Unit,
    modifier: Modifier = Modifier
) {
    var menu by remember { mutableStateOf(false) }
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
            AppIcon(app, vm, 52.dp)
            if (showLabel) {
                Spacer(Modifier.height(6.dp))
                Text(
                    app.label, color = Snow, fontSize = 11.sp, maxLines = 2, lineHeight = 13.sp,
                    overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text("Informações do app") },
                onClick = { menu = false; vm.openAppInfo(app) }
            )
            DropdownMenuItem(
                text = { Text(if (vm.isInDock(app)) "Tirar do dock" else "Colocar no dock") },
                onClick = { menu = false; vm.toggleDock(app) }
            )
            DropdownMenuItem(
                text = { Text("Desinstalar") },
                onClick = { menu = false; vm.uninstall(app) }
            )
        }
    }
}
