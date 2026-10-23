package dev.penetra

import net.fabricmc.api.ClientModInitializer

class PenetraClient : ClientModInitializer {
    override fun onInitializeClient() {
        PenetraHotkeys.getOrCreateKeyMappings()
        PenetraRenderer.initialize()
    }
}
