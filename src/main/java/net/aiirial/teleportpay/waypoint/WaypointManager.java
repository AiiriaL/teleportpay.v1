package net.aiirial.teleportpay.waypoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class WaypointManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<UUID, List<WaypointData>> waypoints = new HashMap<>();

    private static Path getPlayerDataFolder(MinecraftServer server) {
        // NeoForge: server.getServerDirectory() liefert das Server-Hauptverzeichnis
        Path configPath = server.getServerDirectory().resolve("config").resolve("teleportpay").resolve("playerdata");
        try {
            Files.createDirectories(configPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return configPath;
    }

    public static List<WaypointData> getWaypoints(UUID playerUUID) {
        return waypoints.computeIfAbsent(playerUUID, k -> new ArrayList<>());
    }

    public static boolean addWaypoint(ServerPlayer player, WaypointData waypoint, int maxWaypoints) {
        List<WaypointData> playerWaypoints = getWaypoints(player.getUUID());
        if (playerWaypoints.size() >= maxWaypoints) return false;

        for (WaypointData wp : playerWaypoints) {
            if (wp.name.equalsIgnoreCase(waypoint.name)) return false;
        }

        playerWaypoints.add(waypoint);
        return true;
    }

    public static boolean removeWaypoint(ServerPlayer player, String name) {
        List<WaypointData> playerWaypoints = getWaypoints(player.getUUID());
        return playerWaypoints.removeIf(wp -> wp.name.equalsIgnoreCase(name));
    }

    public static WaypointData getWaypoint(ServerPlayer player, String name) {
        List<WaypointData> playerWaypoints = getWaypoints(player.getUUID());
        for (WaypointData wp : playerWaypoints) {
            if (wp.name.equalsIgnoreCase(name)) return wp;
        }
        return null;
    }

    // Wird beim Serverstart aufgerufen
    public static void load(MinecraftServer server) {
        waypoints.clear();

        Path folder = getPlayerDataFolder(server);
        if (!Files.exists(folder)) return;

        try {
            Files.list(folder).forEach(path -> {
                if (path.toString().endsWith(".json")) {
                    UUID playerUUID;
                    try {
                        String fileName = path.getFileName().toString();
                        playerUUID = UUID.fromString(fileName.substring(0, fileName.length() - 5));
                    } catch (IllegalArgumentException e) {
                        System.err.println("Ungültige UUID-Datei: " + path);
                        return;
                    }

                    try (FileReader reader = new FileReader(path.toFile())) {
                        WaypointData[] array = GSON.fromJson(reader, WaypointData[].class);
                        if (array != null) {
                            waypoints.put(playerUUID, new ArrayList<>(Arrays.asList(array)));
                        }
                    } catch (IOException e) {
                        System.err.println("Fehler beim Laden der Waypoints von " + playerUUID);
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            System.err.println("Fehler beim Lesen des Waypoint-Ordners");
            e.printStackTrace();
        }
    }

    // Wird z.B. beim Serverstop oder nach Änderungen aufgerufen
    public static void save(MinecraftServer server) {
        Path folder = getPlayerDataFolder(server);

        for (Map.Entry<UUID, List<WaypointData>> entry : waypoints.entrySet()) {
            UUID playerUUID = entry.getKey();
            List<WaypointData> playerWaypoints = entry.getValue();

            File file = folder.resolve(playerUUID + ".json").toFile();

            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(playerWaypoints, writer);
            } catch (IOException e) {
                System.err.println("Fehler beim Speichern der Waypoints von " + playerUUID);
                e.printStackTrace();
            }
        }
    }
}
