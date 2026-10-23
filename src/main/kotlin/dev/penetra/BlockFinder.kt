package dev.penetra

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.LevelChunkSection
import net.minecraft.world.level.chunk.PalettedContainer
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.status.ChunkStatus
import net.minecraft.network.chat.Component
import java.util.ArrayList

object BlockFinder {
    private const val MAX_RESULTS = 1024
    private const val CHUNKS_PER_TICK = 6
    private var activeSearch: Search? = null
    private var lastBlockId: String? = null
    private var validationTicks = 0

    enum class StartResult { STARTED, NO_WORLD, INVALID_BLOCK, NO_PREVIOUS_SEARCH }

    private data class ChunkCoordinate(val x: Int, val z: Int)

    private data class Search(
        val level: ClientLevel,
        val target: Block,
        val blockName: String,
        val chunks: List<ChunkCoordinate>,
        val positions: MutableList<BlockPos> = ArrayList(),
        var nextChunk: Int = 0,
    )

    @JvmStatic
    fun start(requestedId: String): StartResult {
        activeSearch = null
        val minecraft = Minecraft.getInstance()
        val level = minecraft.level ?: return StartResult.NO_WORLD
        val player = minecraft.player ?: return StartResult.NO_WORLD

        val normalizedId = if (':' in requestedId) requestedId else "minecraft:$requestedId"
        val id = Identifier.tryParse(normalizedId) ?: return StartResult.INVALID_BLOCK
        val target = BuiltInRegistries.BLOCK.getOptional(id).orElse(null)
            ?: return StartResult.INVALID_BLOCK

        val centerX = player.blockPosition().x shr 4
        val centerZ = player.blockPosition().z shr 4
        val radius = minecraft.options.renderDistance().get()
        val coordinates = ArrayList<ChunkCoordinate>((radius * 2 + 1) * (radius * 2 + 1))
        for (ring in 0..radius) {
            for (dx in -ring..ring) {
                for (dz in -ring..ring) {
                    if (maxOf(kotlin.math.abs(dx), kotlin.math.abs(dz)) == ring) {
                        coordinates += ChunkCoordinate(centerX + dx, centerZ + dz)
                    }
                }
            }
        }

        FindState.clear()
        lastBlockId = id.toString()
        activeSearch = Search(level, target, id.toString(), coordinates)
        return StartResult.STARTED
    }

    @JvmStatic
    fun reload(): StartResult = lastBlockId?.let(::start) ?: StartResult.NO_PREVIOUS_SEARCH

    @JvmStatic
    fun cancel() {
        activeSearch = null
    }

    /** Scans only already-loaded chunks, in small batches on the client thread. */
    @JvmStatic
    fun tick() {
        val search = activeSearch
        val minecraft = Minecraft.getInstance()
        minecraft.level?.let { level ->
            if (++validationTicks >= 10) {
                FindState.removeDestroyed(level)
                validationTicks = 0
            }
        }
        if (search == null) return

        if (minecraft.level !== search.level || minecraft.player == null) {
            activeSearch = null
            FindState.clear()
            return
        }

        val chunks = search.level.getChunkSource()
        var processed = 0
        while (search.nextChunk < search.chunks.size && processed < CHUNKS_PER_TICK &&
            search.positions.size < MAX_RESULTS
        ) {
            val coordinate = search.chunks[search.nextChunk++]
            processed++
            val chunk = chunks.getChunk(coordinate.x, coordinate.z, ChunkStatus.FULL, false)
            if (chunk != null) scanChunk(chunk, search.target, search.positions)
        }

        if (search.nextChunk >= search.chunks.size || search.positions.size >= MAX_RESULTS) {
            activeSearch = null
            FindState.set(search.level, search.blockName, search.positions)
            val suffix = if (search.positions.size == MAX_RESULTS) "개 이상 (표시 상한)" else "개"
            minecraft.gui.chatListener().handleSystemMessage(
                Component.literal("[Penetra] ${search.blockName} 검색 완료: ${search.positions.size}$suffix"), false,
            )
        }
    }

    private fun scanChunk(chunk: LevelChunk, target: Block, found: MutableList<BlockPos>) {
        val sections = chunk.getSections()
        val minY = chunk.getMinY()
        val baseX = chunk.getPos().getMinBlockX()
        val baseZ = chunk.getPos().getMinBlockZ()
        for (sectionIndex in sections.indices) {
            if (found.size >= MAX_RESULTS) break
            val section: LevelChunkSection = sections[sectionIndex]
            if (section.hasOnlyAir()) continue
            val states: PalettedContainer<BlockState> = section.getStates()
            if (!states.maybeHas { state -> state.`is`(target) }) continue

            val baseSectionY = minY + (sectionIndex shl 4)
            for (y in 0 until 16) {
                if (found.size >= MAX_RESULTS) break
                for (x in 0 until 16) {
                    if (found.size >= MAX_RESULTS) break
                    for (z in 0 until 16) {
                        if (found.size >= MAX_RESULTS) break
                        if (states.get(x, y, z).`is`(target)) {
                            found += BlockPos(baseX + x, baseSectionY + y, baseZ + z)
                        }
                    }
                }
            }
        }
    }
}
