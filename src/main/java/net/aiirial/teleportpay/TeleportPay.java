package net.aiirial.teleportpay;

import com.mojang.logging.LogUtils;
import net.aiirial.teleportpay.command.TeleportPayCommand;
import net.aiirial.teleportpay.command.TpConfirmCommand;
import net.aiirial.teleportpay.command.TeleportPayConfigCommand;
import net.aiirial.teleportpay.config.TeleportPayConfigData;
import net.aiirial.teleportpay.util.ConfigUtil;
import net.aiirial.teleportpay.waypoint.WaypointManager;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;

import java.io.File;

@Mod(TeleportPay.MODID)
public class TeleportPay {

    public static final String MODID = "teleportpay";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static File configDirectory;
    private static TeleportPayConfigData configData = new TeleportPayConfigData();
    private static MinecraftServer currentServer;

    public TeleportPay() {
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
    }

    public static TeleportPayConfigData getConfig() {
        return configData;
    }

    public static File getConfigDirectory() {
        if (configDirectory == null) {
            throw new IllegalStateException("Config directory not initialized yet");
        }
        return configDirectory;
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(TeleportPayCommand.register());
        event.getDispatcher().register(TpConfirmCommand.register());
        event.getDispatcher().register(TeleportPayConfigCommand.register());
        LOGGER.info("TeleportPay-Befehle registriert.");
    }

    private void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        currentServer = server;

        File serverDir = server.getServerDirectory().toFile();
        configDirectory = new File(serverDir, "config/" + MODID);

        if (!configDirectory.exists() && !configDirectory.mkdirs()) {
            LOGGER.error("Konnte Config-Verzeichnis nicht erstellen: {}", configDirectory.getAbsolutePath());
        }

        configData = ConfigUtil.loadMainConfig(server);
        WaypointManager.loadAll(server);

        LOGGER.info("TeleportPay-Konfiguration und Waypoints erfolgreich geladen.");
    }

    private void onServerStopping(ServerStoppingEvent event) {
        MinecraftServer server = event.getServer();

        // Vorzeitiges Speichern beim Stoppen
        ConfigUtil.saveMainConfig(configData, server);
        WaypointManager.save(server);

        LOGGER.info("TeleportPay: Konfiguration und Waypoints bei ServerStopping gespeichert.");
    }

    private void onServerStopped(ServerStoppedEvent event) {
        currentServer = null;
        LOGGER.info("TeleportPay: Server vollständig gestoppt.");
    }

    public static void saveConfig(MinecraftServer server, TeleportPayConfigData cfg) {
        configData = cfg;
        ConfigUtil.saveMainConfig(cfg, server);
    }

    public static void saveConfig() {
        if (currentServer != null) {
            saveConfig(currentServer, configData);
            LOGGER.info("Konfiguration gespeichert.");
        } else {
            LOGGER.warn("Speichern fehlgeschlagen – kein aktiver Server.");
        }
    }

    public static void reloadConfig(MinecraftServer server) {
        if (server != null) {
            configData = ConfigUtil.loadMainConfig(server);
            LOGGER.info("Konfiguration neu geladen.");
        } else {
            LOGGER.warn("Neuladen fehlgeschlagen – kein aktiver Server.");
        }
    }

    public static MinecraftServer getCurrentServer() {
        return currentServer;
    }
}
