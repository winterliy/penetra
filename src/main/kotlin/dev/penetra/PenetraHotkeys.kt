package dev.penetra

/** Routes physical key presses to the existing feature toggles. */
object PenetraHotkeys {
    private val featureKeyCodes = setOf(89, 85, 91, 93, 71, 72, 74, 59, 78)

    @JvmStatic
    fun onKeyPressed(keyCode: Int) {
        if (keyCode in featureKeyCodes) {
            PenetraFeatures.toggle(keyCode)
        }
    }
}
