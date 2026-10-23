package dev.penetra.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;

public final class PenetraCommandDispatcher {
    private static final CommandDispatcher<ClientSuggestionProvider> DISPATCHER = createDispatcher();

    private PenetraCommandDispatcher() {}

    public static CommandDispatcher<ClientSuggestionProvider> dispatcher() {
        return DISPATCHER;
    }

    private static CommandDispatcher<ClientSuggestionProvider> createDispatcher() {
        CommandDispatcher<ClientSuggestionProvider> dispatcher = new CommandDispatcher<>();
        dispatcher.register(LiteralArgumentBuilder.<ClientSuggestionProvider>literal("find")
                .then(LiteralArgumentBuilder.<ClientSuggestionProvider>literal("clear"))
                .then(RequiredArgumentBuilder.<ClientSuggestionProvider, String>argument("block", word()).suggests((context, builder) -> {
                    String[] blocks = BuiltInRegistries.BLOCK.keySet().stream()
                            .map(PenetraCommandDispatcher::blockName)
                            .toArray(String[]::new);
                    return suggest(blocks, builder);
                })));
        dispatcher.register(LiteralArgumentBuilder.<ClientSuggestionProvider>literal("color")
                .then(RequiredArgumentBuilder.<ClientSuggestionProvider, String>argument("rgb", StringArgumentType.word())
                        .suggests((context, builder) -> builder.suggest("RRGGBB").buildFuture())));
        dispatcher.register(LiteralArgumentBuilder.<ClientSuggestionProvider>literal("showon"));
        dispatcher.register(LiteralArgumentBuilder.<ClientSuggestionProvider>literal("showoff"));
        dispatcher.register(LiteralArgumentBuilder.<ClientSuggestionProvider>literal("reload"));
        dispatcher.register(LiteralArgumentBuilder.<ClientSuggestionProvider>literal("help"));
        return dispatcher;
    }

    private static String blockName(Identifier id) {
        return id.getNamespace().equals("minecraft") ? id.getPath() : id.toString();
    }
}
