package net.aiirial.teleportpay.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.aiirial.teleportpay.config.TeleportPayConfigData;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigUtil {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static TeleportPayConfigData loadMainConfig(MinecraftServer server) {
        File configFile = new File(server.getServerDirectory().toFile(), "config/teleportpay/config.json");
        if (!configFile.exists()) {
            // Default Config zurückgeben
            return new TeleportPayConfigData();
        }

        try (FileReader reader = new FileReader(configFile)) {
            return GSON.fromJson(reader, TeleportPayConfigData.class);
        } catch (IOException e) {
            e.printStackTrace();
            return new TeleportPayConfigData();
        }
    }

    public static void saveMainConfig(TeleportPayConfigData config, MinecraftServer server) {
        File configFile = new File(server.getServerDirectory().toFile(), "config/teleportpay/config.json");
        try {
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
