package com.vertice.launcher.core

enum class ClockStyle(val label: String) {
    Classic("Clássico"),
    Stacked("Empilhado"),
    Minimal("Compacto"),
    Analog("Analógico");

    companion object {
        fun fromName(name: String?): ClockStyle = entries.firstOrNull { it.name == name } ?: Classic
    }
}

enum class ClockFormat(val label: String) {
    System("Sistema"),
    H24("24 horas"),
    H12("12 horas");

    companion object {
        fun fromName(name: String?): ClockFormat = entries.firstOrNull { it.name == name } ?: System
    }
}
