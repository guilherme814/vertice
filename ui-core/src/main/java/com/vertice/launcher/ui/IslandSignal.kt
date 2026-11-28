package com.vertice.launcher.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Sinal da Vértice Island para a Home: enquanto o painel está aberto ele cobre o topo da tela,
 * então a Home esmaece a linha de botões em vez de deixá-los "sob" a ilha. Quem escreve é o controlador da ilha.
 */
object IslandSignal {
    var expanded by mutableStateOf(false)
}
