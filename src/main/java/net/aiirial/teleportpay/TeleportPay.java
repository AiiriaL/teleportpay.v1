package net.aiirial.teleportpay;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(TeleportPay.MOD_ID)
public class TeleportPay {

    public static final String MOD_ID = "teleportpay";
    public static TeleportPayConfigData CONFIG;

    public TeleportPay(IEventBus modEventBus) {
        modEventBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);

        CONFIG = TeleportPayConfig.loadConfig();
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        // Optional: Setup
    }

    public void onRegisterCommands(RegisterCommandsEvent event) {
        TeleportCommand.register(event);
        TeleportConfigCommand.register(event.getDispatcher());
        net.aiirial.teleportpay.command.TeleportPayConfigReloadCommand.register(event.getDispatcher());
    }


}
