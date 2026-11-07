package com.idroambiente.giornalieradicantiere.screenshots

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.idroambiente.giornalieradicantiere.ui.theme.GiornalieraDiCantiereTheme
import java.io.File

/** Cartella degli screenshot: app/build/screenshots */
val SCREENSHOT_DIR = File("build/screenshots").apply { mkdirs() }

fun ComposeContentTestRule.shot(name: String) {
    waitForIdle()
    mainClock.advanceTimeBy(600)
    waitForIdle()
    onRoot().captureRoboImage(File(SCREENSHOT_DIR, "$name.png").path)
}

fun ComposeContentTestRule.render(dark: Boolean = false, content: @Composable () -> Unit) {
    setContent { GiornalieraDiCantiereTheme(darkTheme = dark) { content() } }
}
