package dev.penetra.mixin;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Shadow protected EditBox input;

    @Inject(method = "onEdited(Ljava/lang/String;)V", at = @At("TAIL"))
    private void penetra$showCommandHint(String value, CallbackInfo ci) {
        if (input == null || !value.startsWith("$")) {
            if (input != null) input.setSuggestion("");
            return;
        }

        String hint = null;
        if ("$".equals(value)) hint = "find <block> | color <RRGGBB> | showon | showoff | reload";
        else if ("$f".equals(value)) hint = "ind <namespace:block> (or clear)";
        else if ("$c".equals(value)) hint = "olor <RRGGBB>";
        else if ("$s".equals(value)) hint = "howon / $showoff";
        else if ("$fi".equals(value)) hint = "nd <namespace:block> (or clear)";
        else if ("$co".equals(value)) hint = "lor <RRGGBB>";
        else if ("$sh".equals(value)) hint = "owon / $showoff";
        else if ("$r".equals(value)) hint = "eload (repeat last search)";
        else if ("$find".equals(value)) hint = " <namespace:block> (or clear)";
        else if ("$find ".equals(value)) hint = "<namespace:block> (or clear)";
        else if ("$color".equals(value)) hint = " <RRGGBB>";
        else if ("$color ".equals(value)) hint = "<RRGGBB>";
        else if ("$reload".equals(value)) hint = " (repeat last search)";

        input.setSuggestion(hint == null ? "" : hint);
    }
}
