package dev.penetra

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.minecraft.client.input.KeyEvent
import net.minecraft.resources.Identifier

/** All Penetra shortcuts registered in Minecraft's Controls screen. */
object PenetraHotkeys {
    @JvmField
    val CATEGORY: KeyMapping.Category =
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath("penetra", "general"))

    enum class Feature {
        KILLAURA,
        MULTIAURA,
        XRAY,
        FREECAM,
        AUTOFISH,
        AUTOFARM,
        CAMERA_DISTANCE,
        AUTOAIM,
        BOW_AIMBOT,
    }

    private val featureMappings: Map<Feature, KeyMapping> = linkedMapOf(
        Feature.KILLAURA to mapping("key.penetra.killaura", InputConstants.KEY_Y),
        Feature.MULTIAURA to mapping("key.penetra.multiaura", InputConstants.KEY_U),
        Feature.XRAY to mapping("key.penetra.xray", InputConstants.KEY_LBRACKET),
        Feature.FREECAM to mapping("key.penetra.freecam", InputConstants.KEY_RBRACKET),
        Feature.AUTOFISH to mapping("key.penetra.autofish", InputConstants.KEY_G),
        Feature.AUTOFARM to mapping("key.penetra.autofarm", InputConstants.KEY_H),
        Feature.CAMERA_DISTANCE to mapping("key.penetra.camera_distance", InputConstants.KEY_J),
        Feature.AUTOAIM to mapping("key.penetra.autoaim", InputConstants.KEY_SEMICOLON),
        Feature.BOW_AIMBOT to mapping("key.penetra.bow_aimbot", InputConstants.KEY_N),
    )

    private fun mapping(name: String, defaultKey: Int) =
        KeyMapping(name, InputConstants.Type.KEYBOARD, defaultKey, CATEGORY)

    @JvmStatic
    fun getOrCreateKeyMappings(): List<KeyMapping> =
        featureMappings.values.toList() + PenetraZoom.getOrCreateKeyMapping()

    @JvmStatic
    fun getKeyName(feature: Feature): String =
        featureMappings.getValue(feature).translatedKeyMessage.string

    @JvmStatic
    fun onKeyPressed(event: KeyEvent) {
        featureMappings.forEach { (feature, mapping) ->
            if (mapping.matches(event)) PenetraFeatures.toggle(feature)
        }
    }
}
