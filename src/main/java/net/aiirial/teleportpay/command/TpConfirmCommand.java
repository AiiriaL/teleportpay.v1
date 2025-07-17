package net.aiirial.teleportpay.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.aiirial.teleportpay.command.TeleportPayCommand.PendingTeleportData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class TpConfirmCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("tpconfirm")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    PendingTeleportData data = TeleportPayCommand.getPendingTeleport(player.getUUID());

                    if (data == null) {
                        player.sendSystemMessage(Component.literal("§cKeine Teleportation zum Bestätigen."));
                        return 0;
                    }

                    // Entferne PendingTeleport, da bestätigt
                    TeleportPayCommand.clearPending(player.getUUID());

                    ResourceKey<Level> dimensionKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, data.targetDimension);
                    ServerLevel targetLevel = player.getServer().getLevel(dimensionKey);

                    if (targetLevel == null) {
                        player.sendSystemMessage(Component.literal("§cZiel-Dimension nicht gefunden."));
                        return 0;
                    }

                    // Teleportation ausführen
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
