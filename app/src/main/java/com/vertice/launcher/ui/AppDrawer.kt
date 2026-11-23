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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vertice.launcher.HomeUi
import com.vertice.launcher.LauncherViewModel
import com.vertice.launcher.data.AppEntry
import java.text.Normalizer

private fun String.fold(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

@Composable
fun AppDrawer(
    vm: LauncherViewModel,
    ui: HomeUi,
    onLaunch: (AppEntry, Rect?) -> Unit,
    onClose: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    // Busca ignora acentos e maiúsculas; também acha pelo nome do pacote.
    val results = remember(query, ui.apps) {
        val q = query.fold().trim()
        if (q.isEmpty()) ui.apps
        else ui.apps.filter { it.label.fold().contains(q) || it.packageName.contains(q, ignoreCase = true) }
    }

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
            modifier = Modifier.fillMaxWidth(),
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
        Text(
            if (ui.loading) "Carregando..." else "${results.size} aplicativos",
            color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
        )

        if (!ui.loading && results.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhum aplicativo encontrado.", color = Muted)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(results, key = { it.key }) { app ->
                    AppTile(
                        app = app, vm = vm, animateLaunch = ui.profile.animations,
                        showLabel = true, onLaunch = onLaunch
                    )
                }
            }
        }
    }
}
