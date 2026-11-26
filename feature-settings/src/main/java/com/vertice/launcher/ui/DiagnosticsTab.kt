package com.vertice.launcher.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.vertice.launcher.diag.LogExporter
import com.vertice.launcher.diag.SystemSnapshot
import com.vertice.launcher.diag.VLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Aba de diagnóstico: o que a launcher registrou e como tirar isso do aparelho. */
@Composable
fun DiagnosticsTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var verbose by remember { mutableStateOf(VLog.verbose) }
    var message by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }

    // `refresh` força reler memória e últimas linhas depois de uma ação.
    val memory = remember(refresh) { SystemSnapshot.memoryLine(context) }
    // Listar/medir arquivos é disco: fora da thread principal (o StrictMode do log acusou isso aqui).
    val sizeKb by produceState(initialValue = 0L, refresh) { value = withContext(Dispatchers.IO) { VLog.totalBytes() / 1024 } }
    val lines = remember(refresh) { VLog.recent(14) }

    Text("Memória agora", color = Muted, fontSize = 12.sp)
    Text(memory, color = Snow, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
    Text("Registro guardado: $sizeKb KB (rotaciona sozinho; nada sai do aparelho sem você enviar)", color = Muted, fontSize = 12.sp)
    Spacer(Modifier.height(12.dp))

    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CategoryChip("Marcar travamento", false) {
            // Carimba o instante: o log mostra o que o vigia viu logo antes e depois dele.
            VLog.w("Marca", "USUÁRIO: travou agora | ${SystemSnapshot.memoryLine(context)}")
            message = "Marca registrada."
            refresh++
        }
        CategoryChip("Salvar em Downloads", true) {
            scope.launch {
                message = "Salvando…"
                message = withContext(Dispatchers.IO) {
                    try {
                        LogExporter.saveToDownloads(context)?.let { "Salvo em Downloads/$it" }
                            ?: "Downloads indisponível neste Android; use Compartilhar."
                    } catch (e: Exception) {
                        VLog.e("Exportar", "Falha ao salvar em Downloads", e)
                        "Falha ao salvar: ${e.javaClass.simpleName}"
                    }
                }
                refresh++
            }
        }
        CategoryChip("Compartilhar", true) {
            scope.launch {
                message = "Preparando…"
                message = try {
                    val file = withContext(Dispatchers.IO) { LogExporter.writeShareFile(context) }
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.logs", file)
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(send, "Enviar log da Vértice"))
                    null
                } catch (e: Exception) {
                    VLog.e("Exportar", "Falha ao compartilhar", e)
                    "Falha ao compartilhar: ${e.javaClass.simpleName}"
                }
                refresh++
            }
        }
        CategoryChip("Limpar", false) {
            VLog.clear()
            message = "Registro apagado."
            refresh++
        }
    }
    message?.let { Text(it, color = Mint, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }

    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text("Registro detalhado", color = Snow, fontSize = 15.sp)
            Text("Inclui eventos de depuração (ícones lentos, StrictMode, cada edição da grade).", color = Muted, fontSize = 11.sp)
        }
        Switch(
            checked = verbose,
            onCheckedChange = { verbose = it; VLog.setVerbose(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Ink, checkedTrackColor = Mint,
                uncheckedThumbColor = Muted, uncheckedTrackColor = PanelSoft
            )
        )
    }

    Spacer(Modifier.height(12.dp))
    Text("Últimas linhas", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Ink.copy(alpha = 0.6f)).padding(10.dp)
    ) {
        if (lines.isEmpty()) Text("(vazio)", color = Muted, fontSize = 10.sp)
        lines.forEach { Text(it.take(220), color = Snow, fontSize = 9.sp, fontFamily = FontFamily.Monospace, lineHeight = 11.sp) }
    }
}
