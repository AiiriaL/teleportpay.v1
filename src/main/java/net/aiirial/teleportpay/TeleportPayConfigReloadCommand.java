package net.aiirial.teleportpay.command;

import com.mojang.brigadier.CommandDispatcher;
import net.aiirial.teleportpay.TeleportPayConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.File;

public class TeleportPayConfigReloadCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("teleportpayconfig")
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2)) // Admin-Rechte
                        .executes(context -> {
                            File configDir = new File("config");
                            TeleportPayConfig.reload(configDir);
                            context.getSource().sendSuccess(() -> Component.literal("[TeleportPay] Config erfolgreich neu geladen."), true);
                            return 1;
                        })
                )
        );
    }
}
