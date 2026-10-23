package dev.penetra

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.KeyMapping
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.multiplayer.ClientChunkCache
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.FishingHook
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.CrossbowItem
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.LevelChunkSection
import net.minecraft.world.level.chunk.status.ChunkStatus
import net.minecraft.world.phys.Vec3
import java.util.Collections

object PenetraFeatures {
    private const val MAX_XRAY_BLOCKS = 2048

    private var killaura = false
    private var multiaura = false
    private var xray = false
    private var freecam = false
    private var autofish = false
    private var autofarm = false
    private var cameraDistance = false
    private var autoaim = false
    private var bowaimbot = false
    private var guiOpen = false
    private enum class FreecamDirection { FORWARD, BACKWARD, LEFT, RIGHT, UP, DOWN }

    private val heldMovementKeys = HashSet<FreecamDirection>()
    private val xrayBlockPositions = ArrayList<BlockPos>()
    private val oreSuffixes = listOf("_ore", "ancient_debris")
    private var currentFreecamOffset = Vec3.ZERO
    private var attackDelay = 0
    private var fishDelay = 0
    private var farmDelay = 0
    private var xrayDelay = 0
    private var xrayChunkIndex = 0
    private var xrayChunkQueue = emptyList<ChunkCoordinate>()
    private var farmTarget: BlockPos? = null

    private data class ChunkCoordinate(val x: Int, val z: Int)

    @JvmStatic
    fun toggle(feature: PenetraHotkeys.Feature) {
        val name = when (feature) {
            PenetraHotkeys.Feature.KILLAURA -> { killaura = !killaura; "KillAura" }
            PenetraHotkeys.Feature.MULTIAURA -> { multiaura = !multiaura; "MultiAura" }
            PenetraHotkeys.Feature.XRAY -> {
                xray = !xray
                xrayBlockPositions.clear()
                xrayChunkQueue = emptyList()
                "X-Ray"
            }
            PenetraHotkeys.Feature.FREECAM -> {
                freecam = !freecam
                currentFreecamOffset = Vec3.ZERO
                heldMovementKeys.clear()
                "Freecam"
            }
            PenetraHotkeys.Feature.AUTOFISH -> { autofish = !autofish; fishDelay = 0; "AutoFish" }
            PenetraHotkeys.Feature.AUTOFARM -> { autofarm = !autofarm; farmTarget = null; "AutoFarm" }
            PenetraHotkeys.Feature.CAMERA_DISTANCE -> { cameraDistance = !cameraDistance; "CameraDistance" }
            PenetraHotkeys.Feature.AUTOAIM -> { autoaim = !autoaim; "AutoAim" }
            PenetraHotkeys.Feature.BOW_AIMBOT -> { bowaimbot = !bowaimbot; "BowAimbot" }
        }

        val minecraft = Minecraft.getInstance()
        minecraft.gui.chatListener().handleSystemMessage(
            Component.literal("[Penetra] $name${if (isEnabled(feature)) " ON" else " OFF"}"), false,
        )
    }

    private fun isEnabled(feature: PenetraHotkeys.Feature): Boolean = when (feature) {
        PenetraHotkeys.Feature.KILLAURA -> killaura
        PenetraHotkeys.Feature.MULTIAURA -> multiaura
        PenetraHotkeys.Feature.XRAY -> xray
        PenetraHotkeys.Feature.FREECAM -> freecam
        PenetraHotkeys.Feature.AUTOFISH -> autofish
        PenetraHotkeys.Feature.AUTOFARM -> autofarm
        PenetraHotkeys.Feature.CAMERA_DISTANCE -> cameraDistance
        PenetraHotkeys.Feature.AUTOAIM -> autoaim
        PenetraHotkeys.Feature.BOW_AIMBOT -> bowaimbot
    }

    @JvmStatic fun freecamEnabled(): Boolean = freecam
    @JvmStatic fun cameraDistanceEnabled(): Boolean = cameraDistance
    @JvmStatic fun freecamOffset(): Vec3 = currentFreecamOffset
    @JvmStatic
    fun xrayBlocks(): List<BlockPos> = Collections.unmodifiableList(ArrayList(xrayBlockPositions))

    @JvmStatic
    fun setGuiOpen(open: Boolean) {
        guiOpen = open
        if (open) heldMovementKeys.clear()
    }

    @JvmStatic fun guiOpen(): Boolean = guiOpen

    @JvmStatic
    fun consumeFreecamKey(event: KeyEvent, action: Int): Boolean {
        if (!freecam || Minecraft.getInstance().gui.screen() != null) return false
        val options = Minecraft.getInstance().options
        var matched = false
        fun update(mapping: KeyMapping, direction: FreecamDirection) {
            if (!mapping.matches(event)) return
            matched = true
            when (action) {
                InputConstants.PRESS -> heldMovementKeys.add(direction)
                InputConstants.RELEASE -> heldMovementKeys.remove(direction)
            }
        }
        update(options.keyUp, FreecamDirection.FORWARD)
        update(options.keyDown, FreecamDirection.BACKWARD)
        update(options.keyLeft, FreecamDirection.LEFT)
        update(options.keyRight, FreecamDirection.RIGHT)
        update(options.keyJump, FreecamDirection.UP)
        update(options.keyShift, FreecamDirection.DOWN)
        return matched
    }

    @JvmStatic
    fun tick() {
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return
        val level = minecraft.level ?: return
        val gameMode = minecraft.gameMode ?: return

        if (freecam) moveFreecam(player.yRot)
        if (attackDelay > 0) attackDelay--
        if ((killaura || multiaura) && attackDelay == 0 && player.getAttackStrengthScale(0.0F) >= 0.9F) {
            val targets = targets(player, 3.5)
            if (targets.isNotEmpty()) {
                val count = if (multiaura) targets.size else 1
                repeat(count) { gameMode.attack(player, targets[it]) }
                attackDelay = if (multiaura) 8 else 1
            }
        }

        val holdingBow = listOf(player.mainHandItem, player.offhandItem).any {
            it.item is BowItem || it.item is CrossbowItem
        }
        if (bowaimbot && holdingBow) {
            targets(player, 32.0).firstOrNull(player::hasLineOfSight)?.let { aimAt(player, it) }
        } else if (autoaim) {
            targets(player, 8.0).firstOrNull()?.let { aimAt(player, it) }
        }

        if (autofish) tickFish(minecraft, player)
        if (autofarm) tickFarm(minecraft, player)
        if (xray && ++xrayDelay >= 4) {
            xrayDelay = 0
            tickXray(minecraft, player, level)
        }
    }

    private fun moveFreecam(yawDegrees: Float) {
        val yaw = Math.toRadians(yawDegrees.toDouble())
        val forward = Vec3(-kotlin.math.sin(yaw), 0.0, kotlin.math.cos(yaw))
        val right = Vec3(kotlin.math.cos(yaw), 0.0, kotlin.math.sin(yaw))
        var delta = Vec3.ZERO
        if (FreecamDirection.FORWARD in heldMovementKeys) delta = delta.add(forward)
        if (FreecamDirection.BACKWARD in heldMovementKeys) delta = delta.subtract(forward)
        if (FreecamDirection.RIGHT in heldMovementKeys) delta = delta.add(right)
        if (FreecamDirection.LEFT in heldMovementKeys) delta = delta.subtract(right)
        if (FreecamDirection.UP in heldMovementKeys) delta = delta.add(0.0, 1.0, 0.0)
        if (FreecamDirection.DOWN in heldMovementKeys) delta = delta.add(0.0, -1.0, 0.0)
        if (delta.lengthSqr() > 0.0) currentFreecamOffset = currentFreecamOffset.add(delta.normalize().scale(0.35))
    }

    private fun targets(player: LocalPlayer, range: Double): List<LivingEntity> {
        val rangeSqr = range * range
        val level = Minecraft.getInstance().level ?: return emptyList()
        return level.entitiesForRendering()
            .filterIsInstance<LivingEntity>()
            .filter { it !== player && it.isAlive && !it.isSpectator && player.distanceToSqr(it) <= rangeSqr }
            .sortedBy { player.distanceToSqr(it) }
    }

    private fun aimAt(player: LocalPlayer, target: LivingEntity) {
        val delta = target.eyePosition.subtract(player.eyePosition)
        val horizontal = kotlin.math.sqrt(delta.x * delta.x + delta.z * delta.z)
        val yaw = (Math.toDegrees(kotlin.math.atan2(delta.z, delta.x)) - 90.0).toFloat()
        val pitch = -Math.toDegrees(kotlin.math.atan2(delta.y, horizontal)).toFloat()
        player.yRot = lerpAngle(player.yRot, yaw, 0.35F)
        player.xRot += (pitch - player.xRot) * 0.35F
    }

    private fun lerpAngle(from: Float, to: Float, amount: Float): Float {
        var delta = (to - from) % 360.0F
        if (delta >= 180.0F) delta -= 360.0F
        if (delta < -180.0F) delta += 360.0F
        return from + delta * amount
    }

    private fun tickFish(minecraft: Minecraft, player: LocalPlayer) {
        if (fishDelay > 0) { fishDelay--; return }
        if (!player.mainHandItem.`is`(Items.FISHING_ROD)) return
        val hook: FishingHook? = player.fishing
        if (hook == null || hook.hookedIn != null) {
            minecraft.gameMode?.useItem(player, InteractionHand.MAIN_HAND)
            fishDelay = if (hook == null) 30 else 12
        }
    }

    private fun tickFarm(minecraft: Minecraft, player: LocalPlayer) {
        if (farmDelay > 0) { farmDelay--; return }
        val level = minecraft.level ?: return
        val gameMode = minecraft.gameMode ?: return
        if (farmTarget?.let { level.getBlockState(it).block !is CropBlock } == true) farmTarget = null

        if (farmTarget == null) {
            val origin = player.blockPosition()
            var bestDistance = 4.5 * 4.5
            for (x in -4..4) for (y in -2..2) for (z in -4..4) {
                val pos = origin.offset(x, y, z)
                val state = level.getBlockState(pos)
                val crop = state.block as? CropBlock ?: continue
                if (!crop.isMaxAge(state)) continue
                val distance = pos.distToCenterSqr(player.position())
                if (distance < bestDistance) {
                    bestDistance = distance
                    farmTarget = pos
                }
            }
        }

        val target = farmTarget
        if (target == null) {
            farmDelay = 5
            return
        }
        if (player.distanceToSqr(Vec3.atCenterOf(target)) <= 4.5 * 4.5) {
            if (gameMode.destroyBlock(target)) {
                farmTarget = null
                farmDelay = 8
            } else {
                gameMode.startDestroyBlock(target, Direction.UP)
                gameMode.continueDestroyBlock(target, Direction.UP)
                farmDelay = 3
            }
        }
    }

    private fun tickXray(minecraft: Minecraft, player: LocalPlayer, level: net.minecraft.client.multiplayer.ClientLevel) {
        if (xrayChunkQueue.isEmpty() || xrayChunkIndex >= xrayChunkQueue.size) {
            val centerX = player.blockPosition().x shr 4
            val centerZ = player.blockPosition().z shr 4
            val radius = minOf(6, minecraft.options.renderDistance().get())
            xrayChunkQueue = buildList {
                for (x in -radius..radius) for (z in -radius..radius) {
                    add(ChunkCoordinate(centerX + x, centerZ + z))
                }
            }
            xrayChunkIndex = 0
            xrayBlockPositions.clear()
        }

        val chunks: ClientChunkCache = level.getChunkSource()
        repeat(2) {
            if (xrayChunkIndex >= xrayChunkQueue.size) return@repeat
            val coordinate = xrayChunkQueue[xrayChunkIndex++]
            chunks.getChunk(coordinate.x, coordinate.z, ChunkStatus.FULL, false)?.let(::scanOres)
        }
    }

    private fun scanOres(chunk: LevelChunk) {
        val sections = chunk.getSections()
        for (index in sections.indices) {
            if (xrayBlockPositions.size >= MAX_XRAY_BLOCKS) break
            val section: LevelChunkSection = sections[index]
            if (section.hasOnlyAir()) continue
            val baseY = chunk.getMinY() + (index shl 4)
            for (y in 0 until 16) {
                if (xrayBlockPositions.size >= MAX_XRAY_BLOCKS) break
                for (x in 0 until 16) {
                    if (xrayBlockPositions.size >= MAX_XRAY_BLOCKS) break
                    for (z in 0 until 16) {
                        if (xrayBlockPositions.size >= MAX_XRAY_BLOCKS) break
                        val state: BlockState = section.getStates().get(x, y, z)
                        val path = BuiltInRegistries.BLOCK.getKey(state.block).path
                        if (oreSuffixes.any(path::endsWith)) {
                            xrayBlockPositions += BlockPos(
                                chunk.getPos().getMinBlockX() + x,
                                baseY + y,
                                chunk.getPos().getMinBlockZ() + z,
                            )
                        }
                    }
                }
            }
        }
    }
}
