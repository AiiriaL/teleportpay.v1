package net.aiirial.teleportpay.config;

/**
 * Konfigurationsdaten für TeleportPay, werden in JSON gespeichert und sind ingame per Befehl änderbar.
 */
public class TeleportPayConfigData {

    public String paymentItem = "minecraft:diamond";

    public int rangeTier1 = 1000;
    public int rangeTier2 = 2500;

    public int costTier1 = 1;
    public int costTier2 = 10;
    public int costTier3 = 25;

    public int cooldownTier1 = 60;
    public int cooldownTier2 = 300;
    public int cooldownTier3 = 900;

    public boolean confirmTeleport = true;
    public boolean allowTeleportAboveY120InNether = false;

}
