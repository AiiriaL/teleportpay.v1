package net.aiirial.teleportpay;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class TeleportCommand {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String COMMAND_NAME = "teleportpay";
    private static final TreeMap<Double, CostCooldownTier> costTiers = new TreeMap<>();
    private static final Map<UUID, Long> playerCooldowns = new HashMap<>();
    private static boolean configLoaded = false;

    static {
        loadConfig();
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(COMMAND_NAME)
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2)) // Nur Operatoren
                        .executes(context -> reloadConfig(context.getSource())))
                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                        .executes(context -> executeTeleport(context.getSource(),
                                                DoubleArgumentType.getDouble(context, "x"),
                                                DoubleArgumentType.getDouble(context, "y"),
                                                DoubleArgumentType.getDouble(context, "z")))))));
    }

    private static int reloadConfig(CommandSourceStack source) {
        loadConfig();
        if (configLoaded) {
            source.sendSuccess(() -> Component.literal("[TeleportPay] Config erfolgreich neu geladen!").withStyle(ChatFormatting.GREEN), true);
        } else {
            source.sendFailure(Component.literal("[TeleportPay] Fehler beim Neuladen der Config! Siehe Logs.").withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    private static void loadConfig() {
        try (InputStreamReader reader = new InputStreamReader(
                TeleportCommand.class.getResourceAsStream("/teleportpay_config.json"))) {

            Gson gson = new Gson();
            JsonObject config = gson.fromJson(reader, JsonObject.class);

            costTiers.clear();
            JsonObject tiersObj = config.getAsJsonObject("tiers");

            for (String distanceStr : tiersObj.keySet()) {
                JsonObject tierObj = tiersObj.getAsJsonObject(distanceStr);
                double distanceLimit = distanceStr.equalsIgnoreCase("max") ? Double.MAX_VALUE : Double.parseDouble(distanceStr);
                int cost = tierObj.get("cost").getAsInt();
                int cooldown = tierObj.get("cooldown").getAsInt();

                costTiers.put(distanceLimit, new CostCooldownTier(cost, cooldown));
            }

            LOGGER.info("[TeleportPay] Config geladen. {} Tiers registriert.", costTiers.size());
            configLoaded = true;

        } catch (Exception e) {
            LOGGER.error("[TeleportPay] Fehler beim Laden der Config!", e);
            configLoaded = false;
        }
    }

    private static int executeTeleport(CommandSourceStack source, double x, double y, double z) {
        if (!configLoaded) {
            source.sendFailure(Component.literal("TeleportPay-Config nicht geladen! Bitte Admin benachrichtigen.").withStyle(ChatFormatting.RED));
            return 0;
        }

        ServerPlayer player = source.getPlayer();
        UUID playerId = player.getUUID();

        // 2D-XZ-Distanz berechnen
        double dx = player.getX() - x;
        double dz = player.getZ() - z;
        double distance = Math.sqrt(dx * dx + dz * dz);

        // Passendes Tier finden
        CostCooldownTier tier = getTierForDistance(distance);
        if (tier == null) {
            source.sendFailure(Component.literal("Kein passender Kostenbereich für diese Entfernung.").withStyle(ChatFormatting.RED));
            return 0;
        }

        // Cooldown prüfen
        long now = Instant.now().getEpochSecond();
        long lastUse = playerCooldowns.getOrDefault(playerId, 0L);
        if (now - lastUse < tier.cooldownInSeconds) {
            long wait = tier.cooldownInSeconds - (now - lastUse);
            source.sendFailure(Component.literal("Cooldown aktiv! Warte noch " + wait + " Sekunden.").withStyle(ChatFormatting.RED));
            return 0;
        }

        // Diamanten prüfen
        int diamonds = countDiamonds(player);
        if (diamonds < tier.costInDiamonds) {
            source.sendFailure(Component.literal("Du benötigst " + tier.costInDiamonds + " Diamanten für diese Teleportation.").withStyle(ChatFormatting.RED));
            return 0;
        }

        // Sichere Y-Position finden
        double safeY = findSafeY(player.serverLevel(), x, y, z);
        if (safeY == -1) {
            source.sendFailure(Component.literal("Kein sicherer Teleportationspunkt gefunden.").withStyle(ChatFormatting.RED));
            return 0;
        }

        // Diamanten abziehen
        removeDiamonds(player, tier.costInDiamonds);

        // Teleport durchführen
        player.teleportTo(x, safeY, z);

        source.sendSuccess(() -> Component.literal("Teleportation erfolgreich! " + tier.costInDiamonds + " Diamanten wurden verbraucht.").withStyle(ChatFormatting.GREEN), false);
        playerCooldowns.put(playerId, now);
        return 1;
    }

    private static CostCooldownTier getTierForDistance(double distance) {
        for (Map.Entry<Double, CostCooldownTier> entry : costTiers.entrySet()) {
            if (distance <= entry.getKey()) {
                return entry.getValue();
            }
        }
        return null;
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

        // Suche erst nach oben
        for (int y = (int) startY; y < world.getMaxBuildHeight(); y++) {
            pos.setY(y);
            if (world.getBlockState(pos).isAir() && world.getBlockState(pos.above()).isAir()) {
                return y;
            }
        }

        // Falls oben nix gefunden → nach unten prüfen
        for (int y = (int) startY; y >= world.getMinBuildHeight(); y--) {
            pos.setY(y);
            if (world.getBlockState(pos).isAir() && world.getBlockState(pos.above()).isAir()) {
                return y;
            }
        }

        return -1;
    }

    private static class CostCooldownTier {
        final int costInDiamonds;
        final int cooldownInSeconds;

        CostCooldownTier(int cost, int cooldown) {
            this.costInDiamonds = cost;
            this.cooldownInSeconds = cooldown;
        }
    }
}