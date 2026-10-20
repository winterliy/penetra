package dev.penetra;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PenetraFeatures {
    private static boolean killaura, multiaura, xray, freecam, autofish, autofarm, cameraDistance, autoaim, bowaimbot;
    private static boolean guiOpen;
    private static final Set<Integer> heldMovementKeys = new HashSet<>();
    private static final List<BlockPos> xrayBlocks = new ArrayList<>();
    private static final String[] ORE_SUFFIXES = {"_ore", "ancient_debris"};
    private static Vec3 freecamOffset = Vec3.ZERO;
    private static int attackDelay, fishDelay, farmDelay, xrayDelay;
    private static int xrayChunkIndex;
    private static List<long[]> xrayChunkQueue = List.of();
    private static BlockPos farmTarget;

    private PenetraFeatures() {}

    public static void toggle(int key) {
        String name;
        switch (key) {
            case 89 -> { killaura = !killaura; name = "KillAura"; }
            case 85 -> { multiaura = !multiaura; name = "MultiAura"; }
            case 91 -> { xray = !xray; xrayBlocks.clear(); xrayChunkQueue = List.of(); name = "X-Ray"; }
            case 93 -> { freecam = !freecam; freecamOffset = Vec3.ZERO; heldMovementKeys.clear(); name = "Freecam"; }
            case 71 -> { autofish = !autofish; fishDelay = 0; name = "AutoFish"; }
            case 72 -> { autofarm = !autofarm; farmTarget = null; name = "AutoFarm"; }
            case 74 -> { cameraDistance = !cameraDistance; name = "CameraDistance"; }
            case 59 -> { autoaim = !autoaim; name = "AutoAim"; }
            case 78 -> { bowaimbot = !bowaimbot; name = "BowAimbot"; }
            default -> { return; }
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui != null) mc.gui.chatListener().handleSystemMessage(
                net.minecraft.network.chat.Component.literal("[Penetra] " + name + (isEnabled(key) ? " ON" : " OFF")), false);
    }

    private static boolean isEnabled(int key) {
        return switch (key) {
            case 89 -> killaura; case 85 -> multiaura; case 91 -> xray; case 93 -> freecam;
            case 71 -> autofish; case 72 -> autofarm; case 74 -> cameraDistance; case 59 -> autoaim;
            case 78 -> bowaimbot; default -> false;
        };
    }

    public static boolean freecamEnabled() { return freecam; }
    public static boolean cameraDistanceEnabled() { return cameraDistance; }
    public static Vec3 freecamOffset() { return freecamOffset; }
    public static List<BlockPos> xrayBlocks() { return List.copyOf(xrayBlocks); }

    public static void setGuiOpen(boolean open) {
        guiOpen = open;
        if (open) heldMovementKeys.clear();
    }

    public static boolean guiOpen() { return guiOpen; }

    public static boolean consumeFreecamKey(int key, int action) {
        if (!freecam || guiOpen || !isMovementKey(key)) return false;
        if (action == 1) heldMovementKeys.add(key);
        else if (action == 0) heldMovementKeys.remove(key);
        return true;
    }

    private static boolean isMovementKey(int key) {
        return key == 87 || key == 65 || key == 83 || key == 68 || key == 32 || key == 340 || key == 344;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        var player = mc.player;

        if (freecam) moveFreecam(player.getYRot());
        if (attackDelay > 0) attackDelay--;
        if ((killaura || multiaura) && attackDelay == 0 && player.getAttackStrengthScale(0.0F) >= 0.9F) {
            List<LivingEntity> targets = targets(player, 3.5);
            if (!targets.isEmpty()) {
                int count = multiaura ? targets.size() : 1;
                for (int i = 0; i < count; i++) mc.gameMode.attack(player, targets.get(i));
                attackDelay = multiaura ? 8 : 1;
            }
        }

        boolean holdingBow = player.getMainHandItem().getItem() instanceof BowItem
                || player.getMainHandItem().getItem() instanceof CrossbowItem
                || player.getOffhandItem().getItem() instanceof BowItem
                || player.getOffhandItem().getItem() instanceof CrossbowItem;
        if (bowaimbot && holdingBow) {
            List<LivingEntity> targets = targets(player, 32.0);
            targets.removeIf(target -> !player.hasLineOfSight(target));
            if (!targets.isEmpty()) aimAt(player, targets.getFirst());
        } else if (autoaim) {
            List<LivingEntity> targets = targets(player, 8.0);
            if (!targets.isEmpty()) aimAt(player, targets.getFirst());
        }
        if (autofish) tickFish(mc);
        if (autofarm) tickFarm(mc);
        if (xray && ++xrayDelay >= 4) {
            xrayDelay = 0;
            tickXray(mc);
        }
    }

    private static void moveFreecam(float yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 right = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        Vec3 delta = Vec3.ZERO;
        if (heldMovementKeys.contains(87)) delta = delta.add(forward);
        if (heldMovementKeys.contains(83)) delta = delta.subtract(forward);
        if (heldMovementKeys.contains(68)) delta = delta.add(right);
        if (heldMovementKeys.contains(65)) delta = delta.subtract(right);
        if (heldMovementKeys.contains(32)) delta = delta.add(0, 1, 0);
        if (heldMovementKeys.contains(340) || heldMovementKeys.contains(344)) delta = delta.add(0, -1, 0);
        if (delta.lengthSqr() > 0) freecamOffset = freecamOffset.add(delta.normalize().scale(0.35));
    }

    private static List<LivingEntity> targets(net.minecraft.client.player.LocalPlayer player, double range) {
        List<LivingEntity> found = new ArrayList<>();
        double rangeSqr = range * range;
        for (Entity entity : Minecraft.getInstance().level.entitiesForRendering()) {
            if (entity instanceof LivingEntity living && living != player && living.isAlive()
                    && !living.isSpectator() && player.distanceToSqr(living) <= rangeSqr) found.add(living);
        }
        found.sort((a, b) -> Double.compare(player.distanceToSqr(a), player.distanceToSqr(b)));
        return found;
    }

    private static void aimAt(net.minecraft.client.player.LocalPlayer player, LivingEntity target) {
        Vec3 delta = target.getEyePosition().subtract(player.getEyePosition());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizontal));
        player.setYRot(lerpAngle(player.getYRot(), yaw, 0.35F));
        player.setXRot(player.getXRot() + (pitch - player.getXRot()) * 0.35F);
    }

    private static float lerpAngle(float from, float to, float amount) {
        float delta = (to - from) % 360.0F;
        if (delta >= 180) delta -= 360;
        if (delta < -180) delta += 360;
        return from + delta * amount;
    }

    private static void tickFish(Minecraft mc) {
        if (fishDelay > 0) { fishDelay--; return; }
        var player = mc.player;
        if (!player.getMainHandItem().is(Items.FISHING_ROD)) return;
        FishingHook hook = player.fishing;
        if (hook == null || hook.getHookedIn() != null) {
            mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
            fishDelay = hook == null ? 30 : 12;
        }
    }

    private static void tickFarm(Minecraft mc) {
        if (farmDelay > 0) { farmDelay--; return; }
        var player = mc.player;
        if (farmTarget != null && !(mc.level.getBlockState(farmTarget).getBlock() instanceof CropBlock)) farmTarget = null;
        if (farmTarget == null) {
            BlockPos origin = player.blockPosition();
            double best = 4.5 * 4.5;
            for (int x = -4; x <= 4; x++) for (int y = -2; y <= 2; y++) for (int z = -4; z <= 4; z++) {
                BlockPos pos = origin.offset(x, y, z);
                BlockState state = mc.level.getBlockState(pos);
                if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
                    double dist = pos.distToCenterSqr(player.position());
                    if (dist < best) { best = dist; farmTarget = pos; }
                }
            }
        }
        if (farmTarget != null) {
            BlockPos target = farmTarget;
            if (player.distanceToSqr(Vec3.atCenterOf(target)) <= 4.5 * 4.5) {
                if (mc.gameMode.destroyBlock(target)) {
                    farmTarget = null;
                    farmDelay = 8;
                } else {
                    mc.gameMode.startDestroyBlock(target, Direction.UP);
                    mc.gameMode.continueDestroyBlock(target, Direction.UP);
                    farmDelay = 3;
                }
            }
        } else farmDelay = 5;
    }

    private static void tickXray(Minecraft mc) {
        if (xrayChunkQueue.isEmpty() || xrayChunkIndex >= xrayChunkQueue.size()) {
            xrayChunkQueue = new ArrayList<>();
            int cx = mc.player.blockPosition().getX() >> 4;
            int cz = mc.player.blockPosition().getZ() >> 4;
            int radius = Math.min(6, mc.options.renderDistance().get());
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++)
                xrayChunkQueue.add(new long[]{cx + x, cz + z});
            xrayChunkIndex = 0;
            xrayBlocks.clear();
        }
        ClientChunkCache chunks = mc.level.getChunkSource();
        for (int batch = 0; batch < 2 && xrayChunkIndex < xrayChunkQueue.size(); batch++) {
            long[] coord = xrayChunkQueue.get(xrayChunkIndex++);
            LevelChunk chunk = chunks.getChunk((int) coord[0], (int) coord[1], null, false);
            if (chunk != null) scanOres(chunk);
        }
    }

    private static void scanOres(LevelChunk chunk) {
        for (int i = 0; i < chunk.getSections().length && xrayBlocks.size() < 2048; i++) {
            LevelChunkSection section = chunk.getSections()[i];
            if (section == null || section.hasOnlyAir()) continue;
            int baseY = chunk.getMinY() + (i << 4);
            for (int y = 0; y < 16 && xrayBlocks.size() < 2048; y++)
                for (int x = 0; x < 16 && xrayBlocks.size() < 2048; x++)
                    for (int z = 0; z < 16 && xrayBlocks.size() < 2048; z++) {
                        BlockState state = section.getStates().get(x, y, z);
                        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
                        boolean ore = false;
                        for (String suffix : ORE_SUFFIXES) if (path.endsWith(suffix)) { ore = true; break; }
                        if (ore) xrayBlocks.add(new BlockPos(chunk.getPos().getMinBlockX() + x,
                                baseY + y, chunk.getPos().getMinBlockZ() + z));
                    }
        }
    }
}
