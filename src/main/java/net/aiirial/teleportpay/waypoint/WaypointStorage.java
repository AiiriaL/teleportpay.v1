package net.aiirial.teleportpay.waypoint;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class WaypointStorage {

    private static final Gson GSON = new Gson();
    private static final Type TYPE = new TypeToken<Map<UUID, List<WaypointData>>>() {}.getType();
    private static final String FILE_NAME = "config/teleportpay/waypoints.json";

    public static void load(MinecraftServer server) {
        File file = new File(server.getServerDirectory().toFile(), FILE_NAME);
        if (!file.exists()) return;

        try (FileReader reader = new FileReader(file)) {
            Map<UUID, List<WaypointData>> loaded = GSON.fromJson(reader, TYPE);
            if (loaded != null) {
                for (Map.Entry<UUID, List<WaypointData>> entry : loaded.entrySet()) {
                    WaypointManager.loadWaypoints(entry.getKey(), entry.getValue());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save(MinecraftServer server) {
        File file = new File(server.getServerDirectory().toFile(), FILE_NAME);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        Map<UUID, List<WaypointData>> all = new HashMap<>();
        for (UUID uuid : WaypointManager.getAll()) {
            all.put(uuid, WaypointManager.getWaypoints(uuid));
        }

        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(all, TYPE, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
