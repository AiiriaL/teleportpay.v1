package net.aiirial.teleportpay.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.aiirial.teleportpay.TeleportPay;
import net.aiirial.teleportpay.config.TeleportPayConfigData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;

public class TeleportPayConfigCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build(LiteralArgumentBuilder<CommandSourceStack> root) {
        return root.requires(source -> source.hasPermission(2)) // Nur Admins
                .then(Commands.literal("set")
                        .then(Commands.literal("confirmTeleport")
                                .then(Commands.literal("true").executes(ctx -> setBoolean("confirmTeleport", true, ctx.getSource())))
                                .then(Commands.literal("false").executes(ctx -> setBoolean("confirmTeleport", false, ctx.getSource()))))
                        .then(Commands.literal("allowTeleportAboveY120InNether")
                                .then(Commands.literal("true").executes(ctx -> setBoolean("allowTeleportAboveY120InNether", true, ctx.getSource())))
                                .then(Commands.literal("false").executes(ctx -> setBoolean("allowTeleportAboveY120InNether", false, ctx.getSource()))))
                        .then(Commands.literal("paymentItem")
                                .then(Commands.argument("item", StringArgumentType.word())
                                        .executes(ctx -> setPaymentItem(ctx.getSource(), StringArgumentType.getString(ctx, "item")))))
                        .then(Commands.literal("costTier1")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(ctx -> setInt("costTier1", IntegerArgumentType.getInteger(ctx, "value"), ctx.getSource()))))
                        .then(Commands.literal("costTier2")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(ctx -> setInt("costTier2", IntegerArgumentType.getInteger(ctx, "value"), ctx.getSource()))))
                        .then(Commands.literal("costTier3")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(ctx -> setInt("costTier3", IntegerArgumentType.getInteger(ctx, "value"), ctx.getSource()))))
                        .then(Commands.literal("cooldownTier1")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(ctx -> setInt("cooldownTier1", IntegerArgumentType.getInteger(ctx, "value"), ctx.getSource()))))
                        .then(Commands.literal("cooldownTier2")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(ctx -> setInt("cooldownTier2", IntegerArgumentType.getInteger(ctx, "value"), ctx.getSource()))))
                        .then(Commands.literal("cooldownTier3")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(ctx -> setInt("cooldownTier3", IntegerArgumentType.getInteger(ctx, "value"), ctx.getSource()))))
                )
                .then(Commands.literal("reload")
                        .executes(ctx -> reloadConfig(ctx.getSource())));
    }

    private static int setBoolean(String key, boolean value, CommandSourceStack source) {
        TeleportPayConfigData cfg = TeleportPay.getConfig();
        switch (key) {
            case "confirmTeleport" -> cfg.confirmTeleport = value;
            case "allowTeleportAboveY120InNether" -> cfg.allowTeleportAboveY120InNether = value;
            default -> {
                source.sendSystemMessage(Component.literal("§cUnbekannter Konfigurationswert."));
                return 0;
            }
        }
        saveAndConfirm(source, key, String.valueOf(value));
        return 1;
    }

    private static int setInt(String key, int value, CommandSourceStack source) {
        TeleportPayConfigData cfg = TeleportPay.getConfig();
        switch (key) {
            case "costTier1" -> cfg.costTier1 = value;
            case "costTier2" -> cfg.costTier2 = value;
            case "costTier3" -> cfg.costTier3 = value;
            case "cooldownTier1" -> cfg.cooldownTier1 = value;
            case "cooldownTier2" -> cfg.cooldownTier2 = value;
            case "cooldownTier3" -> cfg.cooldownTier3 = value;
            default -> {
                source.sendSystemMessage(Component.literal("§cUnbekannter Konfigurationswert."));
                return 0;
            }
        }
        saveAndConfirm(source, key, String.valueOf(value));
        return 1;
    }

    private static int setPaymentItem(CommandSourceStack source, String itemId) {
        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) {
            source.sendSystemMessage(Component.literal("§cUngültige Item-ID: " + itemId));
            return 0;
        }

        TeleportPayConfigData cfg = TeleportPay.getConfig();
        cfg.paymentItem = rl.toString();

        saveAndConfirm(source, "paymentItem", cfg.paymentItem);
        return 1;
    }

    private static void saveAndConfirm(CommandSourceStack source, String key, String value) {
        MinecraftServer server = source.getServer();
        TeleportPay.saveConfig(server, TeleportPay.getConfig());
        source.sendSystemMessage(Component.literal("§a" + key + " wurde auf §b" + value + " §agesetzt."));
    }

    private static int reloadConfig(CommandSourceStack source) {
        try {
            TeleportPay.reloadConfig(source.getServer());
            source.sendSystemMessage(Component.literal("§aTeleportPay-Konfiguration neu geladen."));
            return 1;
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("§cFehler beim Neuladen der Konfiguration: " + e.getMessage()));
            return 0;
        }
    }
}
