package dev.penetra.mixin;

import dev.penetra.accessor.IChatScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin implements IChatScreen {
    @Shadow protected EditBox input;

    @Override
    public EditBox penetra$getInput() {
        return input;
    }
}
