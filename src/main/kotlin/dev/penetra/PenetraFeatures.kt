package dev.penetra

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.KeyMapping
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.FishingHook
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.CrossbowItem
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3

object PenetraFeatures {
    private const val NOTICE_DURATION_NANOS = 2_000_000_000L

    private val xrayBlocks = setOf(
        "amethyst_cluster", "ancient_debris", "anvil", "beacon", "bone_block", "bookshelf",
        "brewing_stand", "budding_amethyst", "chain_command_block", "chest", "coal_block",
        "coal_ore", "command_block", "copper_ore", "crafter", "crafting_table", "creaking_heart",
        "decorated_pot", "deepslate_coal_ore", "deepslate_copper_ore", "deepslate_diamond_ore",
        "deepslate_emerald_ore", "deepslate_gold_ore", "deepslate_iron_ore", "deepslate_lapis_ore",
        "deepslate_redstone_ore", "diamond_block", "diamond_ore", "dispenser", "dropper",
        "emerald_block", "emerald_ore", "enchanting_table", "end_portal", "end_portal_frame",
        "ender_chest", "furnace", "glowstone", "gold_block", "gold_ore", "hopper", "iron_block",
        "iron_ore", "ladder", "lapis_block", "lapis_ore", "lava", "lodestone", "mossy_cobblestone",
        "nether_gold_ore", "nether_portal", "nether_quartz_ore", "raw_copper_block", "raw_gold_block",
        "raw_iron_block", "redstone_block", "redstone_ore", "repeating_command_block",
        "sculk_catalyst", "sculk_sensor", "sculk_shrieker", "spawner", "suspicious_gravel",
        "suspicious_sand", "tnt", "torch", "trapped_chest", "trial_spawner", "vault", "wall_torch",
        "water",
    )

    private var killaura = false
    private var multiaura = false
    @Volatile private var xray = false
    private var autofish = false
    private var autofarm = false
    private var cameraDistance = false
    private var autoaim = false
    private var bowaimbot = false
    private var guiOpen = false
    @Volatile private var freecam = false
    private var freecamSpeed = 1.0
    private var freecamPosition = Vec3.ZERO
    private var previousFreecamPosition = Vec3.ZERO
    private var freecamYaw = 0.0F
    private var freecamPitch = 0.0F
    private var lastFreecamHealth = Float.NaN
    private enum class FreecamDirection { FORWARD, BACKWARD, LEFT, RIGHT, UP, DOWN }

    private val heldMovementKeys = HashSet<FreecamDirection>()
    private val shortcutNotices = ArrayList<ShortcutNotice>()
    private var attackDelay = 0
    private var fishDelay = 0
    private var farmDelay = 0
    private var farmTarget: BlockPos? = null

    data class ShortcutNotice(val label: String, val enabled: Boolean, val expiresAtNanos: Long)

    @JvmStatic
    fun toggle(feature: PenetraHotkeys.Feature) {
        val name = when (feature) {
            PenetraHotkeys.Feature.KILLAURA -> { killaura = !killaura; "KillAura" }
            PenetraHotkeys.Feature.MULTIAURA -> { multiaura = !multiaura; "MultiAura" }
            PenetraHotkeys.Feature.XRAY -> {
                xray = !xray
                "X-Ray"
            }
            PenetraHotkeys.Feature.FREECAM -> {
                freecam = !freecam
                heldMovementKeys.clear()
                if (freecam) startFreecam() else lastFreecamHealth = Float.NaN
                "Freecam"
            }
            PenetraHotkeys.Feature.AUTOFISH -> { autofish = !autofish; fishDelay = 0; "AutoFish" }
            PenetraHotkeys.Feature.AUTOFARM -> { autofarm = !autofarm; farmTarget = null; "AutoFarm" }
            PenetraHotkeys.Feature.CAMERA_DISTANCE -> { cameraDistance = !cameraDistance; "CameraDistance" }
            PenetraHotkeys.Feature.AUTOAIM -> { autoaim = !autoaim; "AutoAim" }
            PenetraHotkeys.Feature.BOW_AIMBOT -> { bowaimbot = !bowaimbot; "BowAimbot" }
        }

        val enabled = isEnabled(feature)
        showShortcutNotice(name, enabled)
        if (feature == PenetraHotkeys.Feature.XRAY) refreshXrayRendering()
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

    @JvmStatic
    fun showShortcutNotice(label: String, enabled: Boolean) {
        val now = System.nanoTime()
        shortcutNotices.removeIf { it.expiresAtNanos <= now }
        shortcutNotices += ShortcutNotice(label, enabled, now + NOTICE_DURATION_NANOS)
        while (shortcutNotices.size > 4) shortcutNotices.removeAt(0)
    }

    @JvmStatic
    fun activeShortcutNotices(): List<ShortcutNotice> {
        val now = System.nanoTime()
        shortcutNotices.removeIf { it.expiresAtNanos <= now }
        return shortcutNotices.toList()
    }

    private fun refreshXrayRendering() {
        val minecraft = Minecraft.getInstance()
        val level = minecraft.level ?: return
        minecraft.levelRenderer.clearVisibleSections()
        minecraft.levelRenderer.invalidateCompiledGeometry(
            level, minecraft.options, minecraft.gameRenderer.mainCamera(), minecraft.getBlockColors(),
        )
    }

    @JvmStatic fun freecamEnabled(): Boolean = freecam
    @JvmStatic fun cameraDistanceEnabled(): Boolean = cameraDistance
    @JvmStatic fun freecamPosition(partialTick: Float): Vec3 =
        previousFreecamPosition.lerp(freecamPosition, partialTick.toDouble())
    @JvmStatic fun freecamYaw(): Float = freecamYaw
    @JvmStatic fun freecamPitch(): Float = freecamPitch
    @JvmStatic fun xrayEnabled(): Boolean = xray
    @JvmStatic fun isXrayTarget(state: BlockState): Boolean =
        BuiltInRegistries.BLOCK.getKey(state.block).let { it.namespace == "minecraft" && it.path in xrayBlocks }
    @JvmStatic fun shouldHideXrayBlockEntity(pos: BlockPos): Boolean {
        val level = Minecraft.getInstance().level ?: return false
        return xray && !isXrayTarget(level.getBlockState(pos))
    }

    @JvmStatic
    fun setGuiOpen(open: Boolean) {
        guiOpen = open
        if (open) heldMovementKeys.clear()
    }

    @JvmStatic fun guiOpen(): Boolean = guiOpen

    private fun startFreecam() {
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return
        val camera = minecraft.gameRenderer.mainCamera()
        val position = if (camera.isInitialized) camera.position() else player.eyePosition
        freecamPosition = position
        previousFreecamPosition = position
        freecamYaw = if (camera.isInitialized) camera.yaw() else player.yRot
        freecamPitch = if (camera.isInitialized) camera.xRot() else player.xRot
        freecamSpeed = 1.0
        lastFreecamHealth = player.health
    }

    @JvmStatic
    fun turnFreecam(deltaYaw: Double, deltaPitch: Double) {
        if (!freecam || guiOpen) return
        freecamYaw += (deltaYaw * 0.15).toFloat()
        freecamPitch = (freecamPitch + deltaPitch * 0.15).toFloat().coerceIn(-90.0F, 90.0F)
    }

    @JvmStatic fun freecamControlsScroll(): Boolean = freecam && !guiOpen && !PenetraZoom.isZooming()

    @JvmStatic
    fun adjustFreecamSpeed(amount: Double) {
        if (amount > 0) freecamSpeed = (freecamSpeed + 0.05).coerceAtMost(10.0)
        else if (amount < 0) freecamSpeed = (freecamSpeed - 0.05).coerceAtLeast(0.05)
    }

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

        if (freecam && !guiOpen) {
            if (!lastFreecamHealth.isNaN() && player.health < lastFreecamHealth) {
                toggle(PenetraHotkeys.Feature.FREECAM)
            } else {
                lastFreecamHealth = player.health
                moveFreecam()
            }
        } else if (freecam) {
            previousFreecamPosition = freecamPosition
        }
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
    }

    private fun moveFreecam() {
        val yaw = Math.toRadians(freecamYaw.toDouble())
        val forward = Vec3(-kotlin.math.sin(yaw), 0.0, kotlin.math.cos(yaw))
        val right = Vec3(kotlin.math.cos(yaw), 0.0, kotlin.math.sin(yaw))
        var delta = Vec3.ZERO
        if (FreecamDirection.FORWARD in heldMovementKeys) delta = delta.add(forward)
        if (FreecamDirection.BACKWARD in heldMovementKeys) delta = delta.subtract(forward)
        if (FreecamDirection.RIGHT in heldMovementKeys) delta = delta.add(right)
        if (FreecamDirection.LEFT in heldMovementKeys) delta = delta.subtract(right)
        if (FreecamDirection.UP in heldMovementKeys) delta = delta.add(0.0, 1.0, 0.0)
        if (FreecamDirection.DOWN in heldMovementKeys) delta = delta.add(0.0, -1.0, 0.0)
        previousFreecamPosition = freecamPosition
        if (delta.lengthSqr() > 0.0) freecamPosition = freecamPosition.add(delta.normalize().scale(freecamSpeed))
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

}
