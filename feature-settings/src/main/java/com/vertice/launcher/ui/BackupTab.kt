package com.vertice.launcher.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.vertice.launcher.HomeActions
import com.vertice.launcher.diag.LogFormat
import com.vertice.launcher.diag.VLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Backup e restauração do layout e das preferências (arquivo de texto simples). */
@Composable
fun BackupTab(vm: HomeActions) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            message = try {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                } ?: throw IllegalStateException("não foi possível abrir o arquivo")
                vm.restoreBackup(text)
            } catch (e: Exception) {
                VLog.e("Backup", "Falha ao restaurar", e)
                "Falha ao restaurar: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    Text(
        "Guarda posições da grade, dock, categorias, relógio, papel de parede e perfil. " +
            "Restaurar substitui o que está configurado agora.",
        color = Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 10.dp)
    )
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CategoryChip("Exportar backup", true) {
            scope.launch {
                message = try {
                    val text = vm.backupText()
                    val file = withContext(Dispatchers.IO) {
                        val dir = File(context.cacheDir, "export").apply { mkdirs() }
                        File(dir, "vertice-backup-${LogFormat.fileStamp(System.currentTimeMillis())}.txt").also { it.writeText(text) }
                    }
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.logs", file)
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(send, "Salvar backup da Vértice"))
                    VLog.i("Backup", "Backup exportado (${file.length()} bytes)")
                    null
                } catch (e: Exception) {
                    VLog.e("Backup", "Falha ao exportar", e)
                    "Falha ao exportar: ${e.javaClass.simpleName}"
                }
            }
        }
        CategoryChip("Restaurar de arquivo", false) { picker.launch(arrayOf("text/plain", "*/*")) }
    }
    Spacer(Modifier.height(8.dp))
    message?.let { Text(it, color = Mint, fontSize = 12.sp) }
}
