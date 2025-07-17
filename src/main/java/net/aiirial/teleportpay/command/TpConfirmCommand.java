package net.aiirial.teleportpay.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.aiirial.teleportpay.TeleportPay;
import net.aiirial.teleportpay.command.TeleportPayCommand.PendingTeleportData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;


public class TpConfirmCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build(LiteralArgumentBuilder<CommandSourceStack> root) {
        return root.requires(source -> source.hasPermission(0))
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    PendingTeleportData data = TeleportPayCommand.getPendingTeleport(player.getUUID());

                    if (data == null) {
                        player.sendSystemMessage(Component.literal("§cKeine Teleportation zum Bestätigen."));
                        return 0;
                    }

                    // Entferne zuerst den Pending-Eintrag
                    TeleportPayCommand.clearPending(player.getUUID());

                    // Hole Ziel-Dimension (ResourceLocation ist ein Feld in PendingTeleportData)
                    ResourceKey<Level> dimensionKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, data.targetDimension);
                    ServerLevel targetLevel = player.server.getLevel(dimensionKey);

                    if (targetLevel == null) {
                        player.sendSystemMessage(Component.literal("§cZiel-Dimension nicht gefunden."));
                        return 0;
                    }

                    // Führe Teleportation aus - CROSS DIMENSION
                    return TeleportPayCommand.executeTeleportCrossDim(
                            player,
                            targetLevel,
                            data.target,
                            data.paymentItem,
                            data.cost,
                            data.cooldown,
                            data.tier
                    );
                });
    }
}
