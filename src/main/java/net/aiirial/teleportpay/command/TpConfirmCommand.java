package net.aiirial.teleportpay.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class TpConfirmCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("tpconfirm")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    UUID uuid = player.getUUID();

                    TeleportPayCommand.PendingTeleportData data = TeleportPayCommand.getPendingTeleport(uuid);
                    if (data == null) {
                        player.sendSystemMessage(Component.literal("§cKeine ausstehende Teleportation zum Bestätigen."));
                        return 0;
                    }

                    // Dimension laden
                    // Dimension laden
                    ServerLevel targetLevel = player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, data.targetDimension));


                    if (targetLevel == null) {
                        player.sendSystemMessage(Component.literal("§cZiel-Dimension existiert nicht."));
                        return 0;
                    }

                    TeleportPayCommand.clearPending(uuid);

                    if (data.tier >= 2) {
                        // Direkt teleportieren ohne SafeCheck (Wegpunkt-Bestätigung)
                        return TeleportPayCommand.executeTeleportCrossDimConfirmed(player, targetLevel, data.target, data.paymentItem, data.cost, data.cooldown, data.tier);
                    } else {
                        // Normale Teleportation mit SafeCheck (Koordinaten)
                        return TeleportPayCommand.executeTeleportCrossDim(player, targetLevel, data.target, data.paymentItem, data.cost, data.cooldown, data.tier);
                    }
                });

    }
}
