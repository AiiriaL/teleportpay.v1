package net.aiirial.teleportpay.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.aiirial.teleportpay.TeleportPay;

import java.io.*;

/**
 * Klasse zum Laden und Speichern der TeleportPay-Konfiguration aus JSON.
 */
public class TeleportPayConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "teleportpay_config.json";

    // Singleton-Instanz der geladenen Config
    private static TeleportPayConfigData currentConfig = null;

    /**
     * Lädt die Konfiguration aus dem Mod-spezifischen Config-Verzeichnis
     * und speichert sie in currentConfig.
     */
    public static void load() {
        File configDir = TeleportPay.getConfigDirectory();
        currentConfig = load(configDir);
    }

    /**
     * Gibt die aktuell geladene Konfiguration zurück.
     * Falls noch keine geladen ist, wird einmal geladen.
     */
    public static TeleportPayConfigData get() {
        if (currentConfig == null) {
            load();
        }
        return currentConfig;
    }

    /**
     * Lädt die Konfiguration aus einem bestimmten Verzeichnis.
     */
    public static TeleportPayConfigData load(File configDir) {
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File configFile = new File(configDir, CONFIG_FILE_NAME);

        if (!configFile.exists()) {
            TeleportPayConfigData defaultConfig = getDefaultConfig();
            save(configDir, defaultConfig);
            return defaultConfig;
        }

        try (Reader reader = new FileReader(configFile)) {
            TeleportPayConfigData configData = GSON.fromJson(reader, TeleportPayConfigData.class);
            if (configData == null) {
                return getDefaultConfig();
            }
            return configData;
        } catch (IOException e) {
            e.printStackTrace();
            return getDefaultConfig();
        }
    }

    /**
     * Speichert die Konfiguration als JSON in das Verzeichnis.
     */
    public static void save(File configDir, TeleportPayConfigData config) {
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File configFile = new File(configDir, CONFIG_FILE_NAME);

        try (Writer writer = new FileWriter(configFile)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Liefert die Standardkonfiguration zurück.
     */
    public static TeleportPayConfigData getDefaultConfig() {
        return new TeleportPayConfigData();
    }

    /**
     * Reload der Config aus dem Config-Verzeichnis.
     */
    public static void reload() {
        load();
    }
}
