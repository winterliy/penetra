package dev.penetra.mixin;

import dev.penetra.BlockFinder;
import dev.penetra.FindState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "sendChat(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void penetra$handleFind(String message, CallbackInfo ci) {
        boolean findCommand = message.equals("#find") || message.startsWith("#find ") || message.startsWith("#find\t");
        boolean colorCommand = message.equals("#color") || message.startsWith("#color ") || message.startsWith("#color\t");
        if (!findCommand && !colorCommand) return;
        String[] args = message.trim().split("\\s+");
        if (colorCommand) {
            applyColor(args);
        } else if (args.length == 2 && args[1].equalsIgnoreCase("clear")) {
            BlockFinder.cancel();
            FindState.clear();
            report("[Penetra] 표시를 지웠습니다.");
        } else if (args.length == 2) {
            BlockFinder.StartResult result = BlockFinder.start(args[1]);
            if (result == BlockFinder.StartResult.INVALID_BLOCK) report("[Penetra] 블록 ID를 확인하세요. 예: #find minecraft:diamond_ore");
            else if (result == BlockFinder.StartResult.NO_WORLD) report("[Penetra] 월드에 접속한 뒤 사용하세요.");
            else report("[Penetra] 로드된 청크를 검색합니다: " + args[1]);
        } else {
            report("[Penetra] 사용법: #find <namespace:block> | #find clear");
        }
        ci.cancel();
    }

    private static void applyColor(String[] args) {
        if (args.length != 2) {
            report("[Penetra] 사용법: #color <RRGGBB> (예: #color FF8800)");
            return;
        }
        String hex = args[1].startsWith("#") ? args[1].substring(1) : args[1];
        if (!hex.matches("(?i)[0-9a-f]{6}")) {
            report("[Penetra] 색상은 6자리 RGB hex여야 합니다. 예: #color FF8800");
            return;
        }
        FindState.setRgb(Integer.parseInt(hex, 16));
        report("[Penetra] 아웃라인 색상을 #" + hex.toUpperCase(java.util.Locale.ROOT) + "로 변경했습니다.");
    }

    private static void report(String message) {
        Minecraft.getInstance().gui.chatListener().handleSystemMessage(Component.literal(message), false);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void penetra$advanceSearch(CallbackInfo ci) {
        BlockFinder.tick();
    }
}
