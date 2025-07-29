package net.aiirial.teleportpay.waypoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.aiirial.teleportpay.TeleportPay;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class WaypointManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, List<WaypointData>> playerWaypoints = new HashMap<>();

    // Lädt alle Spieler-Waypoints beim Serverstart
    public static void loadAll(MinecraftServer server) {
        playerWaypoints.clear();
        File dir = getBaseDirectory(server);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                System.err.println("[TeleportPay] Konnte Waypoint-Verzeichnis nicht erstellen: " + dir.getAbsolutePath());
                return;
            }
        }

        File[] files = dir.listFiles((dir1, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                UUID uuid = UUID.fromString(file.getName().replace(".json", ""));
                WaypointData[] waypoints = GSON.fromJson(reader, WaypointData[].class);
                playerWaypoints.put(uuid, new ArrayList<>(Arrays.asList(waypoints)));
            } catch (Exception e) {
                System.err.println("[TeleportPay] Fehler beim Laden von Waypoints für Datei: " + file.getName());
                e.printStackTrace();
            }
        }
    }

    // Gibt die Waypoints eines Spielers zurück oder erstellt eine neue Liste
    public static List<WaypointData> getWaypoints(UUID uuid) {
        return playerWaypoints.computeIfAbsent(uuid, k -> new ArrayList<>());
    }

    // Fügt neuen Waypoint hinzu (inkl. automatischem Speichern)
    public static boolean addWaypoint(ServerPlayer player, WaypointData data, int maxPerPlayer) {
        List<WaypointData> list = getWaypoints(player.getUUID());
        if (list.size() >= maxPerPlayer) return false;
        if (list.stream().anyMatch(w -> w.name.equalsIgnoreCase(data.name))) return false;

        list.add(data);
        saveImmediately(player);
        return true;
    }

    // Entfernt einen Waypoint (inkl. automatischem Speichern)
    public static boolean removeWaypoint(ServerPlayer player, String name) {
        List<WaypointData> list = getWaypoints(player.getUUID());
        boolean removed = list.removeIf(w -> w.name.equalsIgnoreCase(name));
        if (removed) {
            saveImmediately(player);
        }
        return removed;
    }

    // Gibt einen bestimmten Waypoint nach Name zurück
    public static WaypointData getWaypoint(ServerPlayer player, String name) {
        return getWaypoints(player.getUUID()).stream()
                .filter(w -> w.name.equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    // Gibt alle bekannten Spieler-UUIDs mit gespeicherten Waypoints zurück
    public static Set<UUID> getAll() {
        return playerWaypoints.keySet();
    }

    // Speichert die Waypoints eines einzelnen Spielers
    public static void savePlayer(UUID uuid, MinecraftServer server) {
        List<WaypointData> list = getWaypoints(uuid);
        File file = new File(getBaseDirectory(server), uuid.toString() + ".json");

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            GSON.toJson(list, writer);
        } catch (IOException e) {
            System.err.println("[TeleportPay] Fehler beim Speichern der Waypoints für " + uuid);
            e.printStackTrace();
        }
    }

    // Speichert alle Spieler-Waypoints auf einmal
    public static void save(MinecraftServer server) {
        for (UUID uuid : playerWaypoints.keySet()) {
            savePlayer(uuid, server);
        }
    }

    // Interne Methode zum direkten Speichern mit aktuellem Server
    private static void saveImmediately(ServerPlayer player) {
        MinecraftServer server = player.server; // Sicherer Zugriff auf Serverinstanz
        savePlayer(player.getUUID(), server);
    }

    // Konfigurationspfad: config/teleportpay/waypoints
    private static File getBaseDirectory(MinecraftServer server) {
        return new File(server.getServerDirectory().toFile(), "config/teleportpay/waypoints");
    }
}
