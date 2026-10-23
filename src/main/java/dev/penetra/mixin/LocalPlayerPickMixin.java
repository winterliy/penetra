package dev.penetra.mixin;

import dev.penetra.PenetraFeatures;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerPickMixin {
    @Inject(method = "isShiftKeyDown()Z", at = @At("HEAD"), cancellable = true)
    private void penetra$disableSneakDuringFreecam(CallbackInfoReturnable<Boolean> cir) {
        if (PenetraFeatures.freecamEnabled()) cir.setReturnValue(false);
    }

    @Redirect(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;pick(DFZ)Lnet/minecraft/world/phys/HitResult;"))
    private static HitResult penetra$pickBlockFromCamera(Entity entity, double maxDistance,
            float partialTicks, boolean includeFluids) {
        if (!PenetraFeatures.freecamEnabled()) return entity.pick(maxDistance, partialTicks, includeFluids);

        Vec3 start = PenetraFeatures.freecamPosition(partialTicks);
        Vec3 direction = Vec3.directionFromRotation(PenetraFeatures.freecamPitch(), PenetraFeatures.freecamYaw());
        Vec3 end = start.add(direction.scale(maxDistance));
        return entity.level().clip(new ClipContext(start, end, ClipContext.Block.OUTLINE,
                includeFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, entity));
    }

    @Redirect(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"))
    private static EntityHitResult penetra$pickEntityFromCamera(Entity entity, Vec3 start, Vec3 end,
            AABB bounds, Predicate<Entity> predicate, double maxDistanceSqr) {
        if (!PenetraFeatures.freecamEnabled()) {
            return ProjectileUtil.getEntityHitResult(entity, start, end, bounds, predicate, maxDistanceSqr);
        }

        Vec3 cameraStart = PenetraFeatures.freecamPosition(1.0F);
        Vec3 direction = Vec3.directionFromRotation(PenetraFeatures.freecamPitch(), PenetraFeatures.freecamYaw());
        double maxDistance = Math.sqrt(maxDistanceSqr);
        Vec3 scaledDirection = direction.scale(maxDistance);
        Vec3 cameraEnd = cameraStart.add(scaledDirection);
        AABB cameraBounds = EntityTypes.PLAYER.getDimensions().makeBoundingBox(cameraStart)
                .expandTowards(scaledDirection).inflate(1.0);
        return ProjectileUtil.getEntityHitResult(entity, cameraStart, cameraEnd,
                cameraBounds, predicate, maxDistanceSqr);
    }
}
