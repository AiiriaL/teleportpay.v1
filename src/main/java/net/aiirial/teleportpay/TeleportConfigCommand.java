package net.aiirial.teleportpay;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.util.concurrent.CompletableFuture;

public class TeleportConfigCommand {

    private static final String[] CONFIG_KEYS = {
            "paymentItem",
            "rangeTier1",
            "rangeTier2",
            "costTier1",
            "costTier2",
            "costTier3",
            "cooldownTier1",
            "cooldownTier2",
            "cooldownTier3"
    };

    private static final SuggestionProvider<CommandSourceStack> CONFIG_KEYS_SUGGESTIONS = (context, builder) -> {
        for (String key : CONFIG_KEYS) {
            builder.suggest(key);
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("teleportpayconfig")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("set")
                        .then(Commands.argument("key", StringArgumentType.string())
                                .suggests(CONFIG_KEYS_SUGGESTIONS)
                                .then(Commands.argument("value", StringArgumentType.string())
                                        .executes(context -> {
                                            String key = StringArgumentType.getString(context, "key");
                                            String value = StringArgumentType.getString(context, "value");

                                            MinecraftServer server = context.getSource().getServer();
                                            File configDir = server.getServerDirectory().resolve("config").toFile();

                                            boolean result = setConfigValue(key, value, configDir);

                                            if (result) {
                                                context.getSource().sendSuccess(() -> Component.literal("[TeleportPay] Config-Wert '" + key + "' erfolgreich auf '" + value + "' gesetzt und gespeichert.").withStyle(ChatFormatting.GREEN), true);
                                                return 1;
                                            } else {
                                                context.getSource().sendFailure(Component.literal("[TeleportPay] Ungültiger Config-Key oder ungültiger Wert: " + key + " = " + value).withStyle(ChatFormatting.RED));
                                                return 0;
                                            }
                                        })))));
    }

    private static boolean setConfigValue(String key, String value, File configDir) {
        try {
            TeleportPayConfigData config = TeleportPay.CONFIG;

            switch (key) {
                case "paymentItem" -> config.paymentItem = value;
                case "rangeTier1" -> config.rangeTier1 = Integer.parseInt(value);
                case "rangeTier2" -> config.rangeTier2 = Integer.parseInt(value);
                case "costTier1" -> config.costTier1 = Integer.parseInt(value);
                case "costTier2" -> config.costTier2 = Integer.parseInt(value);
                case "costTier3" -> config.costTier3 = Integer.parseInt(value);
                case "cooldownTier1" -> config.cooldownTier1 = Integer.parseInt(value);
                case "cooldownTier2" -> config.cooldownTier2 = Integer.parseInt(value);
                case "cooldownTier3" -> config.cooldownTier3 = Integer.parseInt(value);
                default -> {
                    return false;
                }
            }

            TeleportPayConfig.save(configDir, config);
            return true;

        } catch (NumberFormatException e) {
            return false;
        }
    }
}
