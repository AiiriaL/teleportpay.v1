package net.aiirial.teleportpay;

import com.mojang.logging.LogUtils;
import net.aiirial.teleportpay.command.TeleportPayCommand;
import net.aiirial.teleportpay.command.TpConfirmCommand;
import net.aiirial.teleportpay.command.TeleportPayConfigCommand;
import net.aiirial.teleportpay.config.TeleportPayConfigData;
import net.aiirial.teleportpay.util.ConfigUtil;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;

import java.io.File;

@Mod(TeleportPay.MODID)
public class TeleportPay {

    public static final String MODID = "teleportpay";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static File configDirectory;

    // Einzelne Config-Klasse mit allen Werten
    private static TeleportPayConfigData configData = new TeleportPayConfigData();

    // Aktueller Server für Config-Operationen (wird bei Serverstart gesetzt)
    private static MinecraftServer currentServer;

    public static TeleportPayConfigData getConfig() {
        return configData;
    }

    public static File getConfigDirectory() {
        if (configDirectory == null) {
            throw new IllegalStateException("Config directory not initialized yet");
        }
        return configDirectory;
    }

    public TeleportPay() {
        // Event-Listener registrieren
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(TeleportPayCommand.build(Commands.literal("tppay")));
        event.getDispatcher().register(TpConfirmCommand.build(Commands.literal("tpconfirm")));
        event.getDispatcher().register(TeleportPayConfigCommand.build(Commands.literal("teleportpayconfig")));
        LOGGER.info("TeleportPay Commands registriert.");
    }

    private void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        currentServer = server;

        File serverDir = server.getServerDirectory().toFile();
        configDirectory = new File(serverDir, "config/" + MODID);
        if (!configDirectory.exists()) {
            configDirectory.mkdirs();
        }

        // Config laden
        configData = ConfigUtil.loadMainConfig(server);

        LOGGER.info("TeleportPay Konfiguration geladen.");
    }

    public static void saveConfig(MinecraftServer server, TeleportPayConfigData cfg) {
        configData = cfg;
        ConfigUtil.saveMainConfig(cfg, server);
    }

    /**
     * Hilfsmethode, um die Config zu speichern, falls der Server bekannt ist.
     */
    public static void saveConfig() {
        if (currentServer != null) {
            saveConfig(currentServer, configData);
            LOGGER.info("TeleportPay Konfiguration gespeichert.");
        } else {
            LOGGER.warn("saveConfig aufgerufen, aber kein Server bekannt.");
        }
    }

    private void onServerStopped(ServerStoppedEvent event) {
        ConfigUtil.saveMainConfig(configData, event.getServer());
        LOGGER.info("TeleportPay Konfiguration gespeichert.");
        currentServer = null;
    }

    public static void reloadConfig(MinecraftServer server) {
        if (server != null) {
            configData = ConfigUtil.loadMainConfig(server);
            LOGGER.info("TeleportPay-Konfiguration wurde neu geladen.");
        } else {
            LOGGER.warn("reloadConfig ohne Server aufgerufen - Abbruch.");
        }
    }
}
