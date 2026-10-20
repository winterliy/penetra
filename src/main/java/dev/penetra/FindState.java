package dev.penetra;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class FindState {
    private static volatile ClientLevel level;
    private static volatile String blockName;
    private static volatile List<BlockPos> positions = List.of();
    private static volatile int rgb = 0x42FF63;
    private static volatile boolean outlinesEnabled = true;

    private FindState() {}

    public static void set(ClientLevel currentLevel, String name, List<BlockPos> found) {
        level = currentLevel;
        blockName = name;
        positions = List.copyOf(found);
    }

    public static void clear() {
        positions = List.of();
        blockName = null;
        level = null;
    }

    public static boolean belongsTo(ClientLevel currentLevel) { return level == currentLevel; }
    public static String blockName() { return blockName; }
    public static List<BlockPos> positions() { return positions; }
    public static int rgb() { return rgb; }
    public static void setRgb(int color) { rgb = color & 0xFFFFFF; }
    public static boolean outlinesEnabled() { return outlinesEnabled; }
    public static void setOutlinesEnabled(boolean enabled) { outlinesEnabled = enabled; }

    public static void removeDestroyed(ClientLevel currentLevel) {
        String name = blockName;
        if (level != currentLevel || name == null || positions.isEmpty()) return;
        Identifier id = Identifier.tryParse(name);
        if (id == null) return;
        var target = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
        if (target == null) return;
        ClientChunkCache chunks = currentLevel.getChunkSource();
        List<BlockPos> current = positions;
        List<BlockPos> valid = current.stream().filter(pos -> {
                    if (chunks.getChunk(pos.getX() >> 4, pos.getZ() >> 4, null, false) == null) return true;
                    return currentLevel.getBlockState(pos).is(target);
                })
                .toList();
        if (valid.size() != current.size()) positions = valid;
    }
}
