package dev.penetra;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

import java.util.List;

public final class FindState {
    private static volatile ClientLevel level;
    private static volatile String blockName;
    private static volatile List<BlockPos> positions = List.of();
    private static volatile int rgb = 0x42FF63;

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
}
