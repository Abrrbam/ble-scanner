package com.goodeva.blescannertracker.domain.model

enum class SignalCategory(val label: String, val distance: String) {
    VERY_STRONG("Sangat Kuat", "< 1 m"),
    STRONG("Kuat", "1–3 m"),
    FAIR("Cukup", "3–10 m"),
    WEAK("Lemah", "10–20 m"),
    VERY_WEAK("Sangat Lemah", "> 20 m"),
    LOST("Hilang", "Di luar jangkauan");

    companion object {
        fun fromRssi(rssi: Int): SignalCategory = when {
            rssi >= -30 -> VERY_STRONG
            rssi >= -50 -> STRONG
            rssi >= -70 -> FAIR
            rssi >= -80 -> WEAK
            rssi >= -90 -> VERY_WEAK
            else -> LOST
        }
    }
}