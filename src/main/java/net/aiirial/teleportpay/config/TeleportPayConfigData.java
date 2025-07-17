package net.aiirial.teleportpay.config;

/**
 * Konfigurationsdaten für TeleportPay, werden in JSON gespeichert und sind ingame per Befehl änderbar.
 *
 * Die Range-Werte definieren die maximale Entfernung für Tier 1 und Tier 2.
 * Tier 3 gilt für alle Entfernungen größer als rangeTier2 (also ab rangeTier2 + 1 bis unendlich).
 */
public class TeleportPayConfigData {

    public String paymentItem = "minecraft:diamond";

    // Maximalentfernung für Tier 1 (inklusive)
    public int rangeTier1 = 1000;

    // Maximalentfernung für Tier 2 (inklusive)
    public int rangeTier2 = 2500;

    public int rangeTier3 = Integer.MAX_VALUE; // Oder eine sehr große Zahl


    // Kosten pro Tier
    public int costTier1 = 1;
    public int costTier2 = 10;
    public int costTier3 = 25;

    // Cooldowns pro Tier in Sekunden
    public int cooldownTier1 = 60;
    public int cooldownTier2 = 300;
    public int cooldownTier3 = 900;

    public boolean confirmTeleport = true;
    public boolean allowTeleportAboveY120InNether = false;

    // Neu: Maximale Anzahl Waypoints pro Spieler
    public int maxWaypointsPerPlayer = 5;
}
