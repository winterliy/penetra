package dev.penetra;

import net.fabricmc.api.ClientModInitializer;

public final class PenetraClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PenetraRenderer.initialize();
    }
}
