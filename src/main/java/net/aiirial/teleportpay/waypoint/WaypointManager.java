package net.aiirial.teleportpay.waypoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class WaypointManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, List<WaypointData>> playerWaypoints = new HashMap<>();

    // Lädt alle Spieler-Waypoints aus Einzeldateien im Ordner
    public static void loadAll(MinecraftServer server) {
        File dir = getBaseDirectory(server);
        if (!dir.exists()) {
            dir.mkdirs();
            return;
        }

        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.isFile() && file.getName().endsWith(".json")) {
                try (FileReader reader = new FileReader(file)) {
                    UUID uuid = UUID.fromString(file.getName().replace(".json", ""));
                    WaypointData[] waypoints = GSON.fromJson(reader, WaypointData[].class);
                    playerWaypoints.put(uuid, new ArrayList<>(Arrays.asList(waypoints)));
                } catch (Exception ignored) {
                }
            }
        }
    }

    // Speichert die Waypoints eines Spielers in eine eigene Datei
    public static void savePlayer(UUID uuid, MinecraftServer server) {
        List<WaypointData> list = playerWaypoints.getOrDefault(uuid, new ArrayList<>());
        File file = new File(getBaseDirectory(server), uuid.toString() + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(list, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Holt die Waypoints eines Spielers (oder neue leere Liste)
    public static List<WaypointData> getWaypoints(UUID uuid) {
        return playerWaypoints.computeIfAbsent(uuid, k -> new ArrayList<>());
    }

    // Fügt Waypoint hinzu, prüft max Anzahl und Namenskonflikte
    public static boolean addWaypoint(ServerPlayer player, WaypointData data, int maxPerPlayer) {
        List<WaypointData> list = getWaypoints(player.getUUID());
        if (list.size() >= maxPerPlayer) return false;
        if (list.stream().anyMatch(w -> w.name.equalsIgnoreCase(data.name))) return false;

        list.add(data);
        return true;
    }

    // Entfernt Waypoint nach Namen
    public static boolean removeWaypoint(ServerPlayer player, String name) {
        List<WaypointData> list = getWaypoints(player.getUUID());
        return list.removeIf(w -> w.name.equalsIgnoreCase(name));
    }

    // Holt einen Waypoint nach Namen
    public static WaypointData getWaypoint(ServerPlayer player, String name) {
        return getWaypoints(player.getUUID()).stream()
                .filter(w -> w.name.equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    // --- Neu hinzugefügt für WaypointStorage ---

    // Lädt Waypoints für eine UUID (z.B. aus WaypointStorage)
    public static void loadWaypoints(UUID uuid, List<WaypointData> waypoints) {
        playerWaypoints.put(uuid, new ArrayList<>(waypoints));
    }

    // Gibt alle UUIDs zurück, die Waypoints gespeichert haben
    public static Set<UUID> getAll() {
        return playerWaypoints.keySet();
    }

    private static File getBaseDirectory(MinecraftServer server) {
        return new File(server.getServerDirectory().toFile(), "config/teleportpay/waypoints");
    }
}
