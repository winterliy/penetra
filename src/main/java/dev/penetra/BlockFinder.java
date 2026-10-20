package dev.penetra;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class BlockFinder {
    private static final int MAX_RESULTS = 1024;
    private static final int CHUNKS_PER_TICK = 6;
    private static Search activeSearch;
    private static String lastBlockId;
    private static int validationTicks;

    public enum StartResult { STARTED, NO_WORLD, INVALID_BLOCK, NO_PREVIOUS_SEARCH }

    private record ChunkCoordinate(int x, int z) {}

    private static final class Search {
        private final ClientLevel level;
        private final Block target;
        private final String blockName;
        private final List<ChunkCoordinate> chunks;
        private final List<BlockPos> positions = new ArrayList<>();
        private int nextChunk;

        private Search(ClientLevel level, Block target, String blockName, List<ChunkCoordinate> chunks) {
            this.level = level;
            this.target = target;
            this.blockName = blockName;
            this.chunks = chunks;
        }
    }

    private BlockFinder() {}

    public static StartResult start(String requestedId) {
        activeSearch = null;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) return StartResult.NO_WORLD;

        String normalizedId = requestedId.indexOf(':') < 0 ? "minecraft:" + requestedId : requestedId;
        Identifier id = Identifier.tryParse(normalizedId);
        if (id == null) return StartResult.INVALID_BLOCK;
        Block target = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
        if (target == null) return StartResult.INVALID_BLOCK;

        int centerX = minecraft.player.blockPosition().getX() >> 4;
        int centerZ = minecraft.player.blockPosition().getZ() >> 4;
        int radius = minecraft.options.renderDistance().get();
        List<ChunkCoordinate> coordinates = new ArrayList<>((radius * 2 + 1) * (radius * 2 + 1));
        for (int ring = 0; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == ring) {
                        coordinates.add(new ChunkCoordinate(centerX + dx, centerZ + dz));
                    }
                }
            }
        }

        FindState.clear();
        lastBlockId = id.toString();
        activeSearch = new Search(level, target, id.toString(), coordinates);
        return StartResult.STARTED;
    }

    public static StartResult reload() {
        if (lastBlockId == null) return StartResult.NO_PREVIOUS_SEARCH;
        return start(lastBlockId);
    }

    public static void cancel() {
        activeSearch = null;
    }

    /** Processes a small batch on the client tick; never requests an unloaded chunk. */
    public static void tick() {
        Search search = activeSearch;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && ++validationTicks >= 10) {
            FindState.removeDestroyed(minecraft.level);
            validationTicks = 0;
        }
        if (search == null) return;

        if (minecraft.level != search.level || minecraft.player == null) {
            activeSearch = null;
            FindState.clear();
            return;
        }

        ClientChunkCache chunks = search.level.getChunkSource();
        int processed = 0;
        while (search.nextChunk < search.chunks.size() && processed < CHUNKS_PER_TICK
                && search.positions.size() < MAX_RESULTS) {
            ChunkCoordinate coordinate = search.chunks.get(search.nextChunk++);
            processed++;
            LevelChunk chunk = chunks.getChunk(coordinate.x(), coordinate.z(), null, false);
            if (chunk != null) scanChunk(chunk, search.target, search.positions);
        }

        if (search.nextChunk >= search.chunks.size() || search.positions.size() >= MAX_RESULTS) {
            activeSearch = null;
            FindState.set(search.level, search.blockName, search.positions);
            String suffix = search.positions.size() == MAX_RESULTS ? "개 이상 (표시 상한)" : "개";
            minecraft.gui.chatListener().handleSystemMessage(
                    Component.literal("[Penetra] " + search.blockName + " 검색 완료: " + search.positions.size() + suffix), false);
        }
    }

    private static void scanChunk(LevelChunk chunk, Block target, List<BlockPos> found) {
        LevelChunkSection[] sections = chunk.getSections();
        int minY = chunk.getMinY();
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        for (int sectionIndex = 0; sectionIndex < sections.length && found.size() < MAX_RESULTS; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section == null || section.hasOnlyAir()) continue;
            PalettedContainer<BlockState> states = section.getStates();
            if (!states.maybeHas(state -> state.is(target))) continue;

            int baseSectionY = minY + (sectionIndex << 4);
            for (int y = 0; y < 16 && found.size() < MAX_RESULTS; y++) {
                for (int x = 0; x < 16 && found.size() < MAX_RESULTS; x++) {
                    for (int z = 0; z < 16 && found.size() < MAX_RESULTS; z++) {
                        if (states.get(x, y, z).is(target)) {
                            found.add(new BlockPos(baseX + x, baseSectionY + y, baseZ + z));
                        }
                    }
                }
            }
        }
    }
}
