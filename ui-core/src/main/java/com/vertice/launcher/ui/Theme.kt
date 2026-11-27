package com.vertice.launcher.ui

import android.os.Build
import com.vertice.launcher.core.LabelSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.vertice.launcher.core.Accent
import com.vertice.launcher.core.GlassLevel

// Paleta da Vértice. As superfícies são fixas (escuras); a cor de destaque e o vidro vêm das preferências.
val Ink = Color(0xFF090D14)
val Panel = Color(0xFF141C29)
val PanelSoft = Color(0xFF1B2636)
val Muted = Color(0xFF91A0B5)
val Snow = Color(0xFFF3F6FA)

// Destaque e vidro como estado observável: qualquer lugar (composable, Canvas, desenho) lê `Mint` / `Glass`
// e é redesenhado quando o usuário troca o tema. Quem escreve é só o VerticeTheme.
private var accentState by mutableStateOf(Color(Accent.Mint.argb.toInt()))
private var glassPanelState by mutableFloatStateOf(GlassLevel.Medium.panelAlpha)
private var glassPillState by mutableFloatStateOf(GlassLevel.Medium.pillAlpha)
private var glassRichState by mutableStateOf(true)

/** Cor de destaque atual (o nome "Mint" é histórico). */
val Mint: Color get() = accentState

object Glass {
    /** Opacidade de painéis translúcidos (dock). */
    val panel: Float get() = glassPanelState
    /** Opacidade de botões em pílula. */
    val pill: Float get() = glassPillState
    /** Vidro com reflexos (falso no Ultra Lite: só corpo e borda). */
    val rich: Boolean get() = glassRichState
}

@Composable
fun VerticeTheme(
    accent: Accent = Accent.Mint,
    glass: GlassLevel = GlassLevel.Medium,
    richGlass: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val dynamic = accent == Accent.System && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val color = if (dynamic) dynamicDarkColorScheme(context).primary else Color(accent.argb.toInt())

    SideEffect {
        accentState = color
        glassPanelState = glass.panelAlpha
        glassPillState = glass.pillAlpha
        glassRichState = richGlass
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Ink, surface = Panel, primary = color,
            onPrimary = Ink, onBackground = Snow, onSurface = Snow
        ),
        content = content
    )
}

/** Estilo dos rótulos dos apps, lido por Home, dock, pastas e gaveta. */
data class LabelStyleSpec(val size: LabelSize = LabelSize.Medium, val bold: Boolean = false)

val LocalLabelStyle = androidx.compose.runtime.staticCompositionLocalOf { LabelStyleSpec() }
