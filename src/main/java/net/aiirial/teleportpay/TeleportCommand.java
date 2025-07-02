package net.aiirial.teleportpay;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.io.File;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportCommand {

    private static final String COMMAND_NAME = "teleportpay";
    private static final Map<UUID, Long> playerCooldowns = new HashMap<>();
    private static final Map<MinecraftServer, Boolean> netherTeleportRule = new HashMap<>();

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal(COMMAND_NAME)
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            MinecraftServer server = context.getSource().getServer();
                            File configDir = server.getServerDirectory().resolve("config").toFile();
                            TeleportPay.CONFIG = TeleportPayConfig.load(configDir);
                            context.getSource().sendSuccess(() -> Component.literal("[TeleportPay] Konfiguration neu geladen!").withStyle(ChatFormatting.GREEN), true);
                            return 1;
                        }))
                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                        .executes(context -> executeTeleport(context.getSource(),
                                                DoubleArgumentType.getDouble(context, "x"),
                                                DoubleArgumentType.getDouble(context, "y"),
                                                DoubleArgumentType.getDouble(context, "z")))))));

        // Neuer Admin-Befehl für Nether-Regel
        dispatcher.register(Commands.literal("teleportpayrule")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("nether")
                        .then(Commands.literal("tp_over_bedrock")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(context -> {
                                            MinecraftServer server = context.getSource().getServer();
                                            boolean enabled = BoolArgumentType.getBool(context, "enabled");
                                            netherTeleportRule.put(server, enabled);
                                            context.getSource().sendSuccess(() -> Component.literal("[TeleportPay] Nether-Teleport über Bedrock: " + enabled).withStyle(ChatFormatting.YELLOW), true);
                                            return 1;
                                        })))));
    }

    private static int executeTeleport(CommandSourceStack source, double x, double y, double z) {
        ServerPlayer player = source.getPlayer();
        UUID playerId = player.getUUID();

        ServerLevel world = player.serverLevel();
        int minY = world.getMinBuildHeight();
        int maxY = world.getMaxBuildHeight() - 1;

        // Nether Y-Höhenregel prüfen
        MinecraftServer server = source.getServer();
        if (world.dimension().location().toString().equals("minecraft:the_nether")) {
            boolean allowOverBedrock = netherTeleportRule.getOrDefault(server, false);
            int allowedMaxY = allowOverBedrock ? maxY : 120;
            if (y > allowedMaxY) {
                source.sendFailure(Component.literal("Teleportation im Nether ist nur bis Y=" + allowedMaxY + " erlaubt (aktuelle Server-Regel).").withStyle(ChatFormatting.RED));
                return 0;
            }
        }

        if (y < minY || y > maxY) {
            source.sendFailure(Component.literal("Ziel-Y-Koordinate liegt außerhalb der Weltgrenzen: " + minY + " bis " + maxY).withStyle(ChatFormatting.RED));
            return 0;
        }

        double dx = player.getX() - x;
        double dz = player.getZ() - z;
        double distance = Math.sqrt(dx * dx + dz * dz);

        int cost;
        int cooldown;

        // Distanz-Tiers aus der Config
        if (distance <= TeleportPay.CONFIG.rangeTier1) {
            cost = TeleportPay.CONFIG.costTier1;
            cooldown = TeleportPay.CONFIG.cooldownTier1;
        } else if (distance <= TeleportPay.CONFIG.rangeTier2) {
            cost = TeleportPay.CONFIG.costTier2;
            cooldown = TeleportPay.CONFIG.cooldownTier2;
        } else {
            cost = TeleportPay.CONFIG.costTier3;
            cooldown = TeleportPay.CONFIG.cooldownTier3;
        }

        long now = Instant.now().getEpochSecond();
        long lastUse = playerCooldowns.getOrDefault(playerId, 0L);

        if (now - lastUse < cooldown) {
            long wait = cooldown - (now - lastUse);
            source.sendFailure(Component.literal("Cooldown aktiv! Bitte noch " + wait + " Sekunden warten.").withStyle(ChatFormatting.RED));
            return 0;
        }

        Item paymentItem = getPaymentItem();
        if (paymentItem == null) {
            source.sendFailure(Component.literal("[TeleportPay] Ungültiges Bezahl-Item in der Config: " + TeleportPay.CONFIG.paymentItem).withStyle(ChatFormatting.RED));
            return 0;
        }

        int available = countItem(player, paymentItem);
        if (available < cost) {
            source.sendFailure(Component.literal("Du benötigst " + cost + "x " + paymentItem.getDescription().getString() + " für diese Teleportation.").withStyle(ChatFormatting.RED));
            return 0;
        }

        double safeY = findSafeY(world, x, y, z);
        if (safeY == -1) {
            source.sendFailure(Component.literal("Kein sicherer Teleportationspunkt gefunden.").withStyle(ChatFormatting.RED));
            return 0;
        }

        removeItems(player, paymentItem, cost);
        player.teleportTo(x, safeY, z);

        source.sendSuccess(() -> Component.literal("Teleportation erfolgreich! " + cost + "x " + paymentItem.getDescription().getString() + " wurden verbraucht.").withStyle(ChatFormatting.GREEN), false);
        playerCooldowns.put(playerId, now);
        return 1;
    }

    private static Item getPaymentItem() {
        ResourceLocation itemId = ResourceLocation.tryParse(TeleportPay.CONFIG.paymentItem);
        if (itemId == null) return null;
        return BuiltInRegistries.ITEM.get(itemId);
    }

    private static int countItem(ServerPlayer player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void removeItems(ServerPlayer player, Item item, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (stack.getItem() == item) {
                int toRemove = Math.min(stack.getCount(), remaining);
                stack.shrink(toRemove);
                remaining -= toRemove;
                if (remaining <= 0) break;
            }
        }
    }

    private static double findSafeY(ServerLevel world, double x, double startY, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos((int) x, (int) startY, (int) z);

        // Aufwärts
        for (int y = (int) startY; y <= world.getMaxBuildHeight() - 2; y++) {
            pos.setY(y);
            if (world.getBlockState(pos).isAir() && world.getBlockState(pos.above()).isAir()) {
                return y;
            }
        }

        // Abwärts
        for (int y = (int) startY - 1; y >= world.getMinBuildHeight(); y--) {
            pos.setY(y);
            if (world.getBlockState(pos).isAir() && world.getBlockState(pos.above()).isAir()) {
                return y;
            }
        }

        return -1; // Kein sicherer Platz gefunden
    }
}