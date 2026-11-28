package com.vertice.launcher.island

/** O que a cápsula pode pedir ao controlador. */
class IslandCallbacks(
    val onTap: () -> Unit,
    val onDismiss: () -> Unit,
    val onPrevious: () -> Unit,
    val onPlayPause: () -> Unit,
    val onNext: () -> Unit,
    val onOpen: () -> Unit,
    val onAction: (Int) -> Unit
)
