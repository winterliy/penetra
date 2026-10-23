package dev.penetra

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.multiplayer.ClientChunkCache
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.status.ChunkStatus
import java.util.ArrayList
import java.util.Collections

object FindState {
    @Volatile private var level: ClientLevel? = null
    @Volatile private var currentBlockName: String? = null
    @Volatile private var trackedPositions: List<BlockPos> = emptyList()
    @Volatile private var colorRgb = 0x42FF63
    @Volatile private var areOutlinesEnabled = true

    @JvmStatic
    fun set(currentLevel: ClientLevel, name: String, found: List<BlockPos>) {
        level = currentLevel
        currentBlockName = name
        trackedPositions = immutableCopy(found)
    }

    @JvmStatic
    fun clear() {
        trackedPositions = emptyList()
        currentBlockName = null
        level = null
    }

    @JvmStatic
    fun belongsTo(currentLevel: ClientLevel): Boolean = level === currentLevel

    @JvmStatic
    fun blockName(): String? = currentBlockName

    @JvmStatic
    fun positions(): List<BlockPos> = trackedPositions

    @JvmStatic
    fun rgb(): Int = colorRgb

    @JvmStatic
    fun setRgb(color: Int) {
        colorRgb = color and 0xFFFFFF
    }

    @JvmStatic
    fun outlinesEnabled(): Boolean = areOutlinesEnabled

    @JvmStatic
    fun setOutlinesEnabled(enabled: Boolean) {
        areOutlinesEnabled = enabled
    }

    @JvmStatic
    fun removeDestroyed(currentLevel: ClientLevel) {
        val name = currentBlockName ?: return
        val current = trackedPositions
        if (level !== currentLevel || current.isEmpty()) return
        val id = Identifier.tryParse(name) ?: return
        val target = BuiltInRegistries.BLOCK.getOptional(id).orElse(null) ?: return
        val chunks: ClientChunkCache = currentLevel.getChunkSource()
        val valid = current.filter { pos ->
            if (chunks.getChunk(pos.x shr 4, pos.z shr 4, ChunkStatus.FULL, false) == null) true
            else currentLevel.getBlockState(pos).`is`(target)
        }
        if (valid.size != current.size) trackedPositions = immutableCopy(valid)
    }

    @JvmStatic
    fun onBlockChanged(currentLevel: ClientLevel, pos: BlockPos, newState: BlockState) {
        val name = currentBlockName ?: return
        if (level !== currentLevel) return
        val id = Identifier.tryParse(name) ?: return
        val target = BuiltInRegistries.BLOCK.getOptional(id).orElse(null) ?: return

        val current = trackedPositions
        val matches = newState.`is`(target)
        val alreadyTracked = pos in current
        if (matches && !alreadyTracked && current.size < 1024) {
            trackedPositions = immutableCopy(current + pos.immutable())
        } else if (!matches && alreadyTracked) {
            trackedPositions = immutableCopy(current.filterNot { it == pos })
        }
    }

    private fun immutableCopy(values: Collection<BlockPos>): List<BlockPos> =
        Collections.unmodifiableList(ArrayList(values))
}
