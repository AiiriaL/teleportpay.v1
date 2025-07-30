package net.aiirial.teleportpay.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.aiirial.teleportpay.TeleportPay;
import net.aiirial.teleportpay.config.TeleportPayConfigData;
import net.aiirial.teleportpay.waypoint.WaypointData;
import net.aiirial.teleportpay.waypoint.WaypointManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TeleportPayCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("tppay")
                // Teleport zu Koordinaten
                .then(Commands.argument("position", Vec3Argument.vec3())
                        .executes(ctx -> handleTeleport(ctx, Vec3Argument.getVec3(ctx, "position"))))

                // Wegpunkt setzen
                .then(Commands.literal("set")
                        .then(Commands.literal("waypoint")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String name = StringArgumentType.getString(ctx, "name");

                                            int maxWaypoints = TeleportPay.getConfig().maxWaypointsPerPlayer;
                                            boolean success = WaypointManager.addWaypoint(player,
                                                    new WaypointData(name, player.blockPosition(), player.serverLevel().dimension().location()),
                                                    maxWaypoints);

                                            if (!success) {
                                                player.sendSystemMessage(Component.literal("§cWegpunkt konnte nicht hinzugefügt werden (Max erreicht oder Name existiert)."));
                                                return 0;
                                            }

                                            // Speichern
                                            WaypointManager.save(player.server);

                                            player.sendSystemMessage(Component.literal("§aWegpunkt §b" + name + " §agesetzt."));
                                            return 1;
                                        }))))

                // Wegpunkt löschen
                .then(Commands.literal("delete")
                        .then(Commands.literal("waypoint")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String name = StringArgumentType.getString(ctx, "name");

                                            boolean removed = WaypointManager.removeWaypoint(player, name);
                                            if (removed) {
                                                // Speichern
                                                WaypointManager.save(player.server);

                                                player.sendSystemMessage(Component.literal("§aWegpunkt §b" + name + " §aentfernt."));
                                                return 1;
                                            } else {
                                                player.sendSystemMessage(Component.literal("§cWegpunkt §b" + name + " §cnicht gefunden."));
                                                return 0;
                                            }
                                        }))))

                // Wegpunkt-Liste und Teleport zu Wegpunkt
                .then(Commands.literal("waypoint")
                        // Liste aller Wegpunkte anzeigen
                        .then(Commands.literal("list")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    List<WaypointData> waypoints = WaypointManager.getWaypoints(player.getUUID());
                                    if (waypoints.isEmpty()) {
                                        player.sendSystemMessage(Component.literal("§cKeine Wegpunkte gesetzt."));
                                    } else {
                                        player.sendSystemMessage(Component.literal("§aWegpunkte:"));
                                        for (WaypointData wp : waypoints) {
                                            player.sendSystemMessage(Component.literal(" - §b" + wp.name + " §7(" + wp.x + ", " + wp.y + ", " + wp.z + ")"));
                                        }
                                    }
                                    return 1;
                                }))
                        // Teleport zu einem Wegpunkt
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String name = StringArgumentType.getString(ctx, "name");
                                    WaypointData wp = WaypointManager.getWaypoint(player, name);
                                    if (wp == null) {
                                        player.sendSystemMessage(Component.literal("§cWegpunkt nicht gefunden: " + name));
                                        return 0;
                                    }

                                    ResourceKey<Level> dimensionKey = wp.getDimensionKey();
                                    if (dimensionKey == null) {
                                        player.sendSystemMessage(Component.literal("§cUngültige Dimension im Wegpunkt: " + wp.dimension));
                                        return 0;
                                    }

                                    ServerLevel targetLevel = player.getServer().getLevel(dimensionKey);
                                    if (targetLevel == null) {
                                        player.sendSystemMessage(Component.literal("§cZiel-Dimension nicht gefunden: " + wp.dimension));
                                        return 0;
                                    }

                                    // Distanz 2D zwischen Spielerposition und Ziel-Wegpunkt
                                    Vec3 playerPos = player.position();
                                    double distanceXZ = Math.hypot(playerPos.x - wp.x, playerPos.z - wp.z);

                                    TeleportPayConfigData cfg = TeleportPay.getConfig();
                                    int tier, cost, cooldown;
                                    Map<UUID, Long> cooldownMap;
                                    if (distanceXZ <= cfg.rangeTier1) {
                                        tier = 1;
                                        cost = cfg.costTier1;
                                        cooldown = cfg.cooldownTier1;
                                        cooldownMap = lastUsedTier1;
                                    } else if (distanceXZ <= cfg.rangeTier2) {
                                        tier = 2;
                                        cost = cfg.costTier2;
                                        cooldown = cfg.cooldownTier2;
                                        cooldownMap = lastUsedTier2;
                                    } else {
                                        tier = 3;
                                        cost = cfg.costTier3;
                                        cooldown = cfg.cooldownTier3;
                                        cooldownMap = lastUsedTier3;
                                    }

                                    // Prüfe Cooldown
                                    UUID uuid = player.getUUID();
                                    long now = System.currentTimeMillis();
                                    if (cooldownMap.containsKey(uuid) && now - cooldownMap.get(uuid) < cooldown * 1000L) {
                                        long remaining = cooldown - (now - cooldownMap.get(uuid)) / 1000L;
                                        player.sendSystemMessage(Component.literal("§cCooldown für Stufe " + tier + ": " + remaining + " Sekunden"));
                                        return 0;
                                    }

                                    // Item prüfen
                                    Item paymentItem = getItemByName(cfg.paymentItem);
                                    if (paymentItem == null) {
                                        player.sendSystemMessage(Component.literal("§cUngültiges Item in der Konfiguration: " + cfg.paymentItem));
                                        return 0;
                                    }

                                    int count = countItems(player, paymentItem);
                                    if (count < cost) {
                                        player.sendSystemMessage(Component.literal("§cNicht genügend " + paymentItem.getDescription().getString() + " (benötigt: " + cost + ")"));
                                        return 0;
                                    }

                                    // Bestätigung bei confirmTeleport und Tier >= 2
                                    if (cfg.confirmTeleport && tier >= 2) {
                                        pendingTeleport.put(uuid, new PendingTeleportData(
                                                new Vec3(wp.x, wp.y, wp.z),
                                                targetLevel.dimension().location(),
                                                cost,
                                                cooldown,
                                                paymentItem,
                                                tier));
                                        player.sendSystemMessage(Component.literal("§eBitte bestätige mit §b/tpconfirm§e – Kosten: §b" + cost + " §e" + paymentItem.getDescription().getString()));
                                        return 1;
                                    }

                                    // Direkter Teleport mit sicherer Position (SafeCheck)
                                    return executeTeleportCrossDim(player, targetLevel, new Vec3(wp.x, wp.y, wp.z), paymentItem, cost, cooldown, tier);
                                })));

    }

    // Cooldown Maps pro Tier
    private static final Map<UUID, Long> lastUsedTier1 = new HashMap<>();
    private static final Map<UUID, Long> lastUsedTier2 = new HashMap<>();
    private static final Map<UUID, Long> lastUsedTier3 = new HashMap<>();

    // Pending Teleports zur Bestätigung
    private static final Map<UUID, PendingTeleportData> pendingTeleport = new HashMap<>();

    public static class PendingTeleportData {
        public final Vec3 target;
        public final ResourceLocation targetDimension;
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

    private static int handleTeleport(CommandContext<CommandSourceStack> ctx, Vec3 target) {
        ServerPlayer player;
        try {
            player = ctx.getSource().getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            ctx.getSource().sendFailure(Component.literal("§cDieser Befehl kann nur von Spielern ausgeführt werden."));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        Vec3 from = player.position();
        TeleportPayConfigData cfg = TeleportPay.getConfig();

        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        int targetY = (int) target.y;

        if (targetY < minY || targetY > maxY) {
            player.sendSystemMessage(Component.literal("§cTeleportation außerhalb der erlaubten Bauhöhe (" + minY + " - " + maxY + ") ist nicht erlaubt."));
            return 0;
        }

        if (level.dimension() == Level.NETHER && targetY > 120 && !cfg.allowTeleportAboveY120InNether) {
            player.sendSystemMessage(Component.literal("§cTeleportation über Y=120 im Nether ist derzeit nicht erlaubt."));
            return 0;
        }

        double distanceXZ = Math.hypot(from.x - target.x, from.z - target.z);

        int tier, cost, cooldown;
        Map<UUID, Long> cooldownMap;
        if (distanceXZ <= cfg.rangeTier1) {
            tier = 1;
            cost = cfg.costTier1;
            cooldown = cfg.cooldownTier1;
            cooldownMap = lastUsedTier1;
        } else if (distanceXZ <= cfg.rangeTier2) {
            tier = 2;
            cost = cfg.costTier2;
            cooldown = cfg.cooldownTier2;
            cooldownMap = lastUsedTier2;
        } else {
            tier = 3;
            cost = cfg.costTier3;
            cooldown = cfg.cooldownTier3;
            cooldownMap = lastUsedTier3;
        }

        UUID uuid = player.getUUID();
        long now = System.currentTimeMillis();
        if (cooldownMap.containsKey(uuid) && now - cooldownMap.get(uuid) < cooldown * 1000L) {
            long remaining = cooldown - (now - cooldownMap.get(uuid)) / 1000L;
            player.sendSystemMessage(Component.literal("§cCooldown für Stufe " + tier + ": " + remaining + " Sekunden"));
            return 0;
        }

        Item paymentItem = getItemByName(cfg.paymentItem);
        if (paymentItem == null) {
            player.sendSystemMessage(Component.literal("§cUngültiges Item in der Konfiguration: " + cfg.paymentItem));
            return 0;
        }

        int count = countItems(player, paymentItem);
        if (count < cost) {
            player.sendSystemMessage(Component.literal("§cNicht genügend " + paymentItem.getDescription().getString() + " (benötigt: " + cost + ")"));
            return 0;
        }

        if (cfg.confirmTeleport && tier >= 2) {
            pendingTeleport.put(uuid, new PendingTeleportData(target, level.dimension().location(), cost, cooldown, paymentItem, tier));
            player.sendSystemMessage(Component.literal("§eBitte bestätige mit §b/tpconfirm§e – Kosten: §b" + cost + " §e" + paymentItem.getDescription().getString()));
            return 1;
        }

        return executeTeleport(player, target, paymentItem, cost, cooldown, tier);
    }

    public static int executeTeleportCrossDim(ServerPlayer player, ServerLevel targetLevel, Vec3 originalTarget, Item paymentItem, int cost, int cooldown, int tier) {
        BlockPos targetPos = BlockPos.containing(originalTarget);

        BlockPos safe = findSafeTeleportPosition(targetLevel, targetPos);
        if (safe == null) {
            player.sendSystemMessage(Component.literal("§cKeine sichere Position in der Nähe gefunden."));
            return 0;
        }

        if (!player.getAbilities().instabuild) {
            removeItems(player, paymentItem, cost);
        }

        player.teleportTo(targetLevel, safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5, player.getYRot(), player.getXRot());

        getCooldownMap(tier).put(player.getUUID(), System.currentTimeMillis());

        player.sendSystemMessage(Component.literal("§aTeleportiert für §b" + cost + " §a" + paymentItem.getDescription().getString()));
        return 1;
    }

    public static int executeTeleportCrossDimConfirmed(ServerPlayer player, ServerLevel targetLevel, Vec3 target, Item paymentItem, int cost, int cooldown, int tier) {
        if (!player.getAbilities().instabuild) {
            removeItems(player, paymentItem, cost);
        }

        player.teleportTo(targetLevel, target.x + 0.5, target.y, target.z + 0.5, player.getYRot(), player.getXRot());

        getCooldownMap(tier).put(player.getUUID(), System.currentTimeMillis());

        player.sendSystemMessage(Component.literal("§aTeleportiert für §b" + cost + " §a" + paymentItem.getDescription().getString()));
        return 1;
    }

    public static int executeTeleport(ServerPlayer player, Vec3 originalTarget, Item paymentItem, int cost, int cooldown, int tier) {
        BlockPos targetPos = BlockPos.containing(originalTarget);
        ServerLevel level = player.serverLevel();

        BlockPos safe = findSafeTeleportPosition(level, targetPos);
        if (safe == null) {
            player.sendSystemMessage(Component.literal("§cKeine sichere Position in der Nähe gefunden."));
            return 0;
        }

        if (!player.getAbilities().instabuild) {
            removeItems(player, paymentItem, cost);
        }

        player.teleportTo(level, safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5, player.getYRot(), player.getXRot());

        getCooldownMap(tier).put(player.getUUID(), System.currentTimeMillis());

        player.sendSystemMessage(Component.literal("§aTeleportiert für §b" + cost + " §a" + paymentItem.getDescription().getString()));
        return 1;
    }

    private static Map<UUID, Long> getCooldownMap(int tier) {
        return switch (tier) {
            case 1 -> lastUsedTier1;
            case 2 -> lastUsedTier2;
            case 3 -> lastUsedTier3;
            default -> new HashMap<>();
        };
    }

    private static BlockPos findSafeTeleportPosition(ServerLevel level, BlockPos basePos) {
        for (int yOffset = 0; yOffset <= 10; yOffset++) {
            BlockPos posUp = basePos.above(yOffset);
            if (isSafeTeleport(level, posUp)) return posUp;

            if (yOffset != 0) {
                BlockPos posDown = basePos.below(yOffset);
                if (isSafeTeleport(level, posDown)) return posDown;
            }
        }
        return null;
    }

    private static boolean isSafeTeleport(ServerLevel level, BlockPos pos) {
        BlockPos feet = pos;
        BlockPos head = pos.above();
        BlockPos aboveHead = pos.above(2);

        boolean feetClear = isPassable(level, feet);
        boolean headClear = isPassable(level, head);
        boolean aboveHeadClear = isPassable(level, aboveHead);

        BlockPos groundPos = pos.below();
        boolean groundSolid = level.getBlockState(groundPos).isSolid();
        boolean groundSafe = !isDangerousBlock(level.getBlockState(groundPos).getBlock());

        return feetClear && headClear && aboveHeadClear && groundSolid && groundSafe;
    }

    private static boolean isPassable(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced() || state.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean isDangerousBlock(net.minecraft.world.level.block.Block block) {
        return block == Blocks.LAVA ||
                block == Blocks.WATER ||
                block == Blocks.MAGMA_BLOCK ||
                block == Blocks.CACTUS ||
                block == Blocks.FIRE ||
                block == Blocks.POWDER_SNOW ||
                block == Blocks.SWEET_BERRY_BUSH ||
                block == Blocks.COBWEB ||
                block == Blocks.SNOW ||
                block == Blocks.GRAVEL;
    }

    private static int countItems(ServerPlayer player, Item item) {
        return player.getInventory().items.stream()
                .filter(stack -> stack.is(item))
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    private static void removeItems(ServerPlayer player, Item item, int amount) {
        int toRemove = amount;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                int removed = Math.min(stack.getCount(), toRemove);
                stack.shrink(removed);
                toRemove -= removed;
                if (toRemove <= 0) break;
            }
        }
    }

    private static Item getItemByName(String itemName) {
        ResourceLocation rl = ResourceLocation.tryParse(itemName);
        if (rl == null) rl = ResourceLocation.tryParse("minecraft:" + itemName);
        if (rl == null) return null;

        Item item = BuiltInRegistries.ITEM.get(rl);
        return item == Items.AIR ? null : item;
    }

    public static PendingTeleportData getPendingTeleport(UUID uuid) {
        return pendingTeleport.get(uuid);
    }

    public static void clearPending(UUID uuid) {
        pendingTeleport.remove(uuid);
    }
}
