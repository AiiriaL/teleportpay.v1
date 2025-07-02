package net.aiirial.teleportpay.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.aiirial.teleportpay.TeleportPay;
import net.aiirial.teleportpay.command.TeleportPayCommand.PendingTeleportData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

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

                    // Entfernen, bevor Teleport durchgeführt wird
                    TeleportPayCommand.clearPending(player.getUUID());

                    return TeleportPayCommand.executeTeleport(
                            player,
                            data.target,
                            data.paymentItem,
                            data.cost,
                            data.cooldown,
                            data.tier
                    );
                });
    }
}
