package net.aiirial.teleportpay.command;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

public class PendingTeleportData {
    public final Vec3 target;
    public final ResourceLocation targetDimension;  // NEU: Dimension als ResourceLocation
    public final int cost;
    public final int cooldown;
    public final Item paymentItem;
    public final int tier;

    public PendingTeleportData(Vec3 target, ResourceLocation targetDimension, int cost, int cooldown, Item paymentItem, int tier) {
        this.target = target;
        this.targetDimension = targetDimension;
        this.cost = cost;
        this.cooldown = cooldown;
        this.paymentItem = paymentItem;
        this.tier = tier;
    }
}
