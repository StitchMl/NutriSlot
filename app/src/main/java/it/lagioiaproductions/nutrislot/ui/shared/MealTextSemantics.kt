package it.lagioiaproductions.nutrislot.ui.shared

internal fun String.normalizeMealUiLine(): String {
    return replace(Regex("\\s+"), " ").trim()
}