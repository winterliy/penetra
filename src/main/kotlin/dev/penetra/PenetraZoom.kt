package dev.penetra

import net.minecraft.client.KeyMapping
import net.minecraft.client.input.KeyEvent
import com.mojang.blaze3d.platform.InputConstants
import kotlin.math.pow

/** Hold-to-zoom with a scroll-adjustable magnification. */
object PenetraZoom {
    private const val MIN_ZOOM = 1.0
    private const val MAX_ZOOM = 32.0
    private const val SCROLL_STEP = 1.2

    @Volatile
    private var zoomKeyDown = false
    @Volatile
    private var zoomFactor = 4.0
    @Volatile
    private var zoomKey: KeyMapping? = null

    @JvmStatic
    fun getOrCreateKeyMapping(): KeyMapping = zoomKey ?: synchronized(this) {
        zoomKey ?: KeyMapping(
            "key.penetra.zoom",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_C,
            KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath("penetra", "general"))
        ).also { zoomKey = it }
    }

    @JvmStatic
    fun handleKey(event: KeyEvent, action: Int) {
        val mapping = zoomKey ?: return
        if (!mapping.matches(event)) return
        when (action) {
            InputConstants.PRESS -> if (!zoomKeyDown) {
                zoomFactor = 4.0
                zoomKeyDown = true
            }
            InputConstants.RELEASE -> zoomKeyDown = false
        }
    }

    @JvmStatic
    fun isZooming(): Boolean = zoomKeyDown

    @JvmStatic
    fun releaseKey() {
        zoomKeyDown = false
    }

    @JvmStatic
    fun adjustFromScroll(amount: Double) {
        if (!amount.isFinite() || amount == 0.0) return
        zoomFactor = (zoomFactor * SCROLL_STEP.pow(amount)).coerceIn(MIN_ZOOM, MAX_ZOOM)
    }

    @JvmStatic
    fun applyToFov(fov: Float): Float =
        if (zoomKeyDown) (fov / zoomFactor.toFloat()).coerceAtLeast(1.0F) else fov
}
