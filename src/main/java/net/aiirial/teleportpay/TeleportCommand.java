package net.aiirial.teleportpay;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.io.File;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportCommand {

    private static final String COMMAND_NAME = "teleportpay";
    private static final Map<UUID, Long> playerCooldowns = new HashMap<>();

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
    }

    private static int executeTeleport(CommandSourceStack source, double x, double y, double z) {
        ServerPlayer player = source.getPlayer();
        UUID playerId = player.getUUID();

        double dx = player.getX() - x;
        double dz = player.getZ() - z;
        double distance = Math.sqrt(dx * dx + dz * dz);

        int cost;
        int cooldown;

        if (distance <= 1000) {
            cost = TeleportPay.CONFIG.costTier1;
            cooldown = TeleportPay.CONFIG.cooldownTier1;
        } else if (distance <= 2500) {
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

        int diamonds = countDiamonds(player);
        if (diamonds < cost) {
            source.sendFailure(Component.literal("Du benötigst " + cost + " Diamanten für diese Teleportation.").withStyle(ChatFormatting.RED));
            return 0;
        }

        double safeY = findSafeY(player.serverLevel(), x, y, z);
        if (safeY == -1) {
            source.sendFailure(Component.literal("Kein sicherer Teleportationspunkt gefunden.").withStyle(ChatFormatting.RED));
            return 0;
        }

        removeDiamonds(player, cost);
        player.teleportTo(x, safeY, z);

        source.sendSuccess(() -> Component.literal("Teleportation erfolgreich! " + cost + " Diamanten wurden verbraucht.").withStyle(ChatFormatting.GREEN), false);
        playerCooldowns.put(playerId, now);
        return 1;
    }

    private static int countDiamonds(ServerPlayer player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == Items.DIAMOND) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void removeDiamonds(ServerPlayer player, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (stack.getItem() == Items.DIAMOND) {
                int toRemove = Math.min(stack.getCount(), remaining);
                stack.shrink(toRemove);
                remaining -= toRemove;
                if (remaining <= 0) break;
            }
        }
    }

    private static double findSafeY(ServerLevel world, double x, double startY, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos((int) x, (int) startY, (int) z);

        for (int y = (int) startY; y < world.getMaxBuildHeight(); y++) {
            pos.setY(y);
            if (world.getBlockState(pos).isAir() && world.getBlockState(pos.above()).isAir()) {
                return y;
            }
        }

        for (int y = (int) startY; y >= world.getMinBuildHeight(); y--) {
            pos.setY(y);
            if (world.getBlockState(pos).isAir() && world.getBlockState(pos.above()).isAir()) {
                return y;
            }
        }

        return -1;
    }
}