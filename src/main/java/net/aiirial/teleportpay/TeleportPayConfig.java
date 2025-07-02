package net.aiirial.teleportpay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class TeleportPayConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "teleportpay.json";

    public static TeleportPayConfigData load(File configDir) {
        File configFile = new File(configDir, CONFIG_FILE_NAME);

        TeleportPayConfigData config = new TeleportPayConfigData();

        if (!configFile.exists()) {
            try {
                if (!configDir.exists()) {
                    configDir.mkdirs();
                }
                try (FileWriter writer = new FileWriter(configFile)) {
                    GSON.toJson(config, writer);
                }
                System.out.println("[TeleportPay] Standard-Config erstellt unter: " + configFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("[TeleportPay] Fehler beim Erstellen der Standard-Config: " + e.getMessage());
            }
        } else {
            try (FileReader reader = new FileReader(configFile)) {
                TeleportPayConfigData loaded = GSON.fromJson(reader, TeleportPayConfigData.class);
                if (loaded != null) {
                    config = loaded;
                    System.out.println("[TeleportPay] Config geladen von: " + configFile.getAbsolutePath());
                }
            } catch (IOException e) {
                System.err.println("[TeleportPay] Fehler beim Laden der Config: " + e.getMessage());
            }
        }

        return config;
    }

    public static TeleportPayConfigData loadConfig() {
        File configDir = new File("config");
        return load(configDir);
    }
}
