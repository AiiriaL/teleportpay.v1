package net.aiirial.teleportpay.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class TeleportPayConfigData {

    public String paymentItem = "minecraft:diamond";

    public int rangeTier1 = 1000;
    public int rangeTier2 = 2500;
    public int rangeTier3 = Integer.MAX_VALUE;

    public int costTier1 = 1;
    public int costTier2 = 10;
    public int costTier3 = 25;

    public int cooldownTier1 = 60;
    public int cooldownTier2 = 300;
    public int cooldownTier3 = 900;

    public boolean confirmTeleport = true;
    public boolean allowTeleportAboveY120InNether = false;

    public int maxWaypointsPerPlayer = 5;

    /**
     * Gibt das Item zurück, das als Zahlungsmittel genutzt wird.
     * Falls das Item nicht gefunden wird, wird Diamant zurückgegeben.
     */
    public Item getPaymentItem() {
        ResourceLocation rl = ResourceLocation.tryParse(paymentItem);
        if (rl == null) {
            return Items.DIAMOND; // Fallback
        }
        Item item = BuiltInRegistries.ITEM.get(rl);
        if (item == Items.AIR) {
            item = Items.DIAMOND; // Fallback
        }
        return item;
    }
}
