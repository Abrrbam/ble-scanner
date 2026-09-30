package com.goodeva.blescannertracker.ui.navigation

import android.net.Uri

object Routes {
    const val SCANNER = "scanner"
    const val HISTORY = "history"

    const val ARG_ADDRESS = "address"
    const val RADAR = "radar/{$ARG_ADDRESS}"

    fun radar(address: String) = "radar/ ${Uri.encode(address)}"
}
