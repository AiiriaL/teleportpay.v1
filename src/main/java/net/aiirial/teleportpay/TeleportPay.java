package net.aiirial.teleportpay;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
// import net.neoforged.neoforge.event.server.RegisterCommandsEvent;

@Mod(TeleportPay.MODID)
public class TeleportPay {
    public static final String MODID = "teleportpay";

    public TeleportPay() {
        // Mod-Initialisierung (z.B. Config laden etc.)
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        TeleportCommand.register(dispatcher);
    }
}