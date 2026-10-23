package dev.penetra.mixin;

import dev.penetra.PenetraFeatures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudShortcutNoticeMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void penetra$drawShortcutNotices(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker,
            CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.screen() != null) return;

        int y = 8;
        for (PenetraFeatures.ShortcutNotice notice : PenetraFeatures.activeShortcutNotices()) {
            String label = notice.getLabel() + (notice.getEnabled() ? " ON" : " OFF");
            int textWidth = minecraft.font.width(label);
            int height = minecraft.font.lineHeight + 4;
            graphics.fill(8, y, 18 + textWidth, y + height, 0xB0000000);
            graphics.fill(8, y, 10, y + height, notice.getEnabled() ? 0xFF55DD88 : 0xFF999999);
            graphics.text(minecraft.font, Component.literal(label), 13, y + 2, 0xFFFFFFFF);
            y += height + 3;
        }
    }
}
