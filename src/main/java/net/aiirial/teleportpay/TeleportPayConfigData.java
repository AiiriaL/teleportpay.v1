package net.aiirial.teleportpay;

public class TeleportPayConfigData {
    public int costTier1 = 1;          // Diamanten für 0-1000 Blöcke
    public int costTier2 = 10;         // Diamanten für 1001-2500 Blöcke
    public int costTier3 = 25;         // Diamanten für >2500 Blöcke

    public int cooldownTier1 = 60;      // Sekunden Cooldown für Tier 1
    public int cooldownTier2 = 300;     // Sekunden Cooldown für Tier 2
    public int cooldownTier3 = 900;     // Sekunden Cooldown für Tier 3
}
