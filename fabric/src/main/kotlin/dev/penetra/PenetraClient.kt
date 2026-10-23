package dev.penetra

import net.fabricmc.api.ClientModInitializer

class PenetraClient : ClientModInitializer {
    override fun onInitializeClient() {
        PenetraRenderer.initialize()
    }
}
