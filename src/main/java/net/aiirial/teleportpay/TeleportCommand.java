package net.aiirial.teleportpay;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.InputStreamReader;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class TeleportCommand {

    private static boolean enableTeleportPay = true;
    private static final TreeMap<Double, Integer> costPerDistance = new TreeMap<>();
    private static int cooldownInSeconds = 60;
    private static final Map<UUID, Long> playerCooldowns = new HashMap<>();

    static {
        loadConfig();
    }

    private static void loadConfig() {
        try (InputStreamReader reader = new InputStreamReader(
                TeleportCommand.class.getResourceAsStream("/teleportpay_config.json"))) {
            Gson gson = new Gson();
            JsonObject config = gson.fromJson(reader, JsonObject.class);

            enableTeleportPay = config.has("enableTeleportPay") && config.get("enableTeleportPay").getAsBoolean();

            JsonObject costObj = config.getAsJsonObject("costPerDistance");
            if (costObj != null) {
                costPerDistance.clear();
                for (String key : costObj.keySet()) {
                    int value = costObj.get(key).getAsInt();
                    if (key.equals("max")) {
                        costPerDistance.put(Double.MAX_VALUE, value);
                    } else {
                        costPerDistance.put(Double.parseDouble(key), value);
                    }
                }
            }

            if (config.has("cooldownInSeconds")) {
                cooldownInSeconds = config.get("cooldownInSeconds").getAsInt();
            }

        } catch (Exception e) {
            System.err.println("[TeleportPay] Fehler beim Laden der Konfiguration: " + e.getMessage());
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("teleportpay")
                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                        .executes(context -> {
                                            CommandSourceStack source = context.getSource();
                                            ServerPlayer player = source.getPlayer();

                                            if (!enableTeleportPay) {
                                                source.sendFailure(Component.literal("TeleportPay ist in der Config deaktiviert."));
                                                return 0;
                                            }

                                            // Cooldown prüfen
                                            long currentTime = Instant.now().getEpochSecond();
                                            long lastUsed = playerCooldowns.getOrDefault(player.getUUID(), 0L);
                                            long timeSinceLastUse = currentTime - lastUsed;

                                            if (timeSinceLastUse < cooldownInSeconds) {
                                                long remaining = cooldownInSeconds - timeSinceLastUse;
                                                source.sendFailure(Component.literal("Cooldown aktiv! Bitte warte noch " + remaining + " Sekunden."));
                                                return 0;
                                            }

                                            double x = DoubleArgumentType.getDouble(context, "x");
                                            double y = DoubleArgumentType.getDouble(context, "y");
                                            double z = DoubleArgumentType.getDouble(context, "z");

                                            BlockPos targetPos = BlockPos.containing(x, y, z);

                                            double distance = Math.sqrt(Math.pow(player.getX() - x, 2) + Math.pow(player.getZ() - z, 2));
                                            int cost = calculateCost(distance);

                                            int diamondsAvailable = countDiamonds(player);
                                            if (diamondsAvailable < cost) {
                                                source.sendFailure(Component.literal("Du benötigst " + cost + " Diamanten für diese Teleportation!"));
                                                return 0;
                                            }

                                            // Diamanten abziehen
                                            removeDiamonds(player, cost);

                                            // Teleportation durchführen
                                            player.teleportTo(x, y, z);

                                            // Erfolgsmeldung
                                            source.sendSuccess(() -> Component.literal("Teleportation erfolgreich! Du hast " + cost + " Diamanten bezahlt."), false);

                                            // Cooldown setzen
                                            playerCooldowns.put(player.getUUID(), currentTime);

                                            return 1;
                                        })))));
    }

    private static int calculateCost(double distance) {
        for (Map.Entry<Double, Integer> entry : costPerDistance.entrySet()) {
            if (distance <= entry.getKey()) {
                return entry.getValue();
            }
        }
        return 1; // Fallback
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
                int remove = Math.min(stack.getCount(), remaining);
                stack.shrink(remove);
                remaining -= remove;
                if (remaining <= 0) break;
            }
        }
    }
}