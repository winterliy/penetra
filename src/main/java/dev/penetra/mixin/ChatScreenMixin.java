package dev.penetra.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Shadow protected EditBox input;

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At("TAIL"))
    private void penetra$renderCommandTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
            float partialTick, CallbackInfo ci) {
        if (input == null) return;
        String text = input.getValue();
        if (!text.startsWith("$") || text.contains("  ")) return;

        List<String> options = suggestions(text);
        if (options.isEmpty()) return;

        Minecraft minecraft = Minecraft.getInstance();
        int lineHeight = 12;
        int popupHeight = options.size() * lineHeight + 6;
        int left = input.getX();
        int top = Math.max(4, input.getY() - popupHeight - 3);
        int width = Math.min(320, input.getWidth());
        graphics.fill(left, top, left + width, top + popupHeight, 0xD010141B);
        graphics.fill(left, top, left + 2, top + popupHeight, 0xFF42FF63);
        for (int i = 0; i < options.size(); i++) {
            graphics.text(minecraft.font, options.get(i), left + 7, top + 4 + i * lineHeight,
                    0xFFE8EEF2, false);
        }
    }

    private static List<String> suggestions(String input) {
        String normalized = input.toLowerCase(java.util.Locale.ROOT);
        List<String> options = new ArrayList<>();
        if ("$".equals(normalized) || normalized.matches("\\$(f|fi|fin|find)?")) {
            addIfMatches(options, normalized, "$find <block_id>  — search blocks");
            addIfMatches(options, normalized, "$find clear  — clear outlines");
            addIfMatches(options, normalized, "$color <RRGGBB>  — set outline color");
            addIfMatches(options, normalized, "$showon  — show outlines");
            addIfMatches(options, normalized, "$showoff  — hide outlines");
            addIfMatches(options, normalized, "$reload  — repeat last search");
            addIfMatches(options, normalized, "$help  — show Penetra keymap and commands");
            return options;
        }

        if (normalized.startsWith("$find ")) {
            String query = normalized.substring("$find ".length()).trim();
            if (query.isEmpty()) {
                options.add("$find clear  — clear outlines");
                BuiltInRegistries.BLOCK.keySet().stream()
                        .map(id -> id.getNamespace().equals("minecraft") ? id.getPath() : id.toString())
                        .limit(6)
                        .forEach(id -> options.add("$find " + id));
            } else {
                if ("clear".startsWith(query)) options.add("$find clear  — clear outlines");
                BuiltInRegistries.BLOCK.keySet().stream()
                        .map(id -> id.getNamespace().equals("minecraft") ? id.getPath() : id.toString())
                        .filter(id -> id.toLowerCase(java.util.Locale.ROOT).startsWith(query))
                        .limit(7 - options.size())
                        .forEach(id -> options.add("$find " + id));
            }
            return options;
        }

        if ("$color".equals(normalized) || "$color ".equals(normalized)) {
            return List.of("$color <RRGGBB>  — e.g. FF8800");
        }
        if (normalized.matches("\\$(s|sh|sho|show|showo|showon|showof|showoff)?")) {
            addIfMatches(options, normalized, "$showon  — show outlines");
            addIfMatches(options, normalized, "$showoff  — hide outlines");
            return options;
        }
        if (normalized.matches("\\$(r|re|rel|relo|reloa|reload)?")) {
            addIfMatches(options, normalized, "$reload  — repeat last search");
            return options;
        }
        if (normalized.matches("\\$(c|co|col|colo|color)?")) {
            addIfMatches(options, normalized, "$color <RRGGBB>  — set outline color");
            return options;
        }
        if (normalized.matches("\\$(h|he|hel|help)?")) {
            addIfMatches(options, normalized, "$help  — show Penetra keymap and commands");
            return options;
        }
        return options;
    }

    private static void addIfMatches(List<String> options, String typed, String suggestion) {
        String command = suggestion.substring(0, suggestion.indexOf("  —"));
        if (command.startsWith(typed)) options.add(suggestion);
    }
}
