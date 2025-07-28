package net.aiirial.teleportpay.event;

import net.aiirial.teleportpay.TeleportPay;
import net.aiirial.teleportpay.config.TeleportPayConfig;
import net.aiirial.teleportpay.waypoint.WaypointStorage;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.SubscribeEvent;

public class CommonLifecycleEvents {

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();

        TeleportPayConfig.load();
        WaypointStorage.load(server);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        MinecraftServer server = event.getServer();

        TeleportPay.saveConfig(server, TeleportPay.getConfig());
        WaypointStorage.save(server);
    }
}
