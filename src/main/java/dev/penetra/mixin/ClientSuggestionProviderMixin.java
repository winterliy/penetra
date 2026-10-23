package dev.penetra.mixin;

import dev.penetra.accessor.IChatScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Mixin(ClientSuggestionProvider.class)
public abstract class ClientSuggestionProviderMixin {
    @Inject(method = "getCustomTabSuggestions", at = @At("RETURN"), cancellable = true)
    private void penetra$useVanillaCommandSuggestions(CallbackInfoReturnable<Collection<String>> cir) {
        Screen screen = Minecraft.getInstance().gui.screen();
        if (!(screen instanceof IChatScreen chatScreen)) return;
        EditBox input = chatScreen.penetra$getInput();
        if (input == null) return;

        String text = input.getValue();
        if (!text.startsWith("$")) return;

        List<String> suggestions = new ArrayList<>();
        if (text.startsWith("$find ")) {
            String query = text.substring("$find ".length()).toLowerCase(Locale.ROOT);
            if ("clear".startsWith(query)) suggestions.add("clear");
            for (Identifier id : BuiltInRegistries.BLOCK.keySet()) {
                String name = id.getNamespace().equals("minecraft") ? id.getPath() : id.toString();
                if (name.toLowerCase(Locale.ROOT).startsWith(query)) suggestions.add(name);
            }
        } else if (text.startsWith("$color ")) {
            suggestions.add("RRGGBB");
        } else {
            suggestions.addAll(List.of("$find", "$color", "$showon", "$showoff", "$reload", "$help"));
        }
        cir.setReturnValue(suggestions);
    }
}
