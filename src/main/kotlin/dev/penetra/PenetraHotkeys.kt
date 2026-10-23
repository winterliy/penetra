package dev.penetra

import com.mojang.blaze3d.platform.InputConstants

/** Routes physical key presses to the existing feature toggles. */
object PenetraHotkeys {
    private val featureKeyCodes = setOf(
        InputConstants.KEY_Y,
        InputConstants.KEY_U,
        InputConstants.KEY_LBRACKET,
        InputConstants.KEY_RBRACKET,
        InputConstants.KEY_G,
        InputConstants.KEY_H,
        InputConstants.KEY_J,
        InputConstants.KEY_SEMICOLON,
        InputConstants.KEY_N,
    )

    @JvmStatic
    fun onKeyPressed(keyCode: Int) {
        if (keyCode in featureKeyCodes) {
            PenetraFeatures.toggle(keyCode)
        }
    }
}
