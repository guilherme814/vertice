package com.vertice.launcher.ui

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Medidas de design em um só lugar. Tela nova usa estes valores (nada de número solto),
 * então mudar o "jeito" da launcher é mexer aqui.
 */
object Space {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 36.dp
}

object Radius {
    val chip = 50          // porcentagem: pílula
    val tile = 14.dp
    val card = 20.dp
    val dock = 30.dp
    val sheet = 28.dp
}

object TextSize {
    val caption = 11.sp
    val body = 14.sp
    val title = 20.sp
    val display = 28.sp
}
