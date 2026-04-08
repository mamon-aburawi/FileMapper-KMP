package com.mamon.flexrender

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mamon.filemapper.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "FlexRender",
    ) {
        App()
    }
}