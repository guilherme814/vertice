package com.vertice.launcher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta da Vértice (a mesma da versão anterior). Fase de design: temas, claro/escuro, cor de destaque.
val Ink = Color(0xFF090D14)
val Panel = Color(0xFF141C29)
val PanelSoft = Color(0xFF1B2636)
val Mint = Color(0xFFB8F28B)
val Muted = Color(0xFF91A0B5)
val Snow = Color(0xFFF3F6FA)

@Composable
fun VerticeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Ink, surface = Panel, primary = Mint,
            onPrimary = Ink, onBackground = Snow, onSurface = Snow
        ),
        content = content
    )
}
