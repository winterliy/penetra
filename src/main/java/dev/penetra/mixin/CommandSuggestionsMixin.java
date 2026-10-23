package dev.penetra.mixin;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import dev.penetra.command.PenetraCommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private EditBox input;
    @Shadow private ParseResults<ClientSuggestionProvider> currentParse;
    @Shadow private CompletableFuture<Suggestions> pendingSuggestions;
    @Shadow private boolean currentParseIsCommand;
    @Shadow private boolean currentParseIsMessage;
    @Shadow @Final private List<FormattedCharSequence> commandUsage;
    @Shadow public abstract void hide();
    @Shadow public abstract void showSuggestions(boolean narrateFirstSuggestion);

    @Inject(method = "updateCommandInfo", at = @At("HEAD"), cancellable = true)
    private void penetra$completeDollarCommands(CallbackInfo ci) {
        String text = input.getValue();
        if (!text.startsWith("$")) return;

        ci.cancel();
        hide();
        commandUsage.clear();
        currentParseIsCommand = true;
        currentParseIsMessage = false;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.player.connection == null) {
            pendingSuggestions = Suggestions.empty();
            currentParse = PenetraCommandDispatcher.dispatcher().parse(new StringReader(text), null);
            return;
        }

        StringReader reader = new StringReader(text);
        reader.setCursor(1); // Keep Brigadier ranges aligned with the visible '$' prefix.
        ClientSuggestionProvider source = client.player.connection.getSuggestionsProvider();
        currentParse = PenetraCommandDispatcher.dispatcher().parse(reader, source);
        int cursor = input.getCursorPosition();
        String inputAtParse = text;
        pendingSuggestions = PenetraCommandDispatcher.dispatcher().getCompletionSuggestions(currentParse, cursor);
        pendingSuggestions.thenAccept(suggestions -> client.execute(() -> {
            if (input.getValue().equals(inputAtParse)) showSuggestions(false);
        }));
    }
}
