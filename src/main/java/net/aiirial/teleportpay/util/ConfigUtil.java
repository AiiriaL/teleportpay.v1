package net.aiirial.teleportpay.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.aiirial.teleportpay.TeleportPay;
import net.aiirial.teleportpay.config.TeleportPayConfig;
import net.aiirial.teleportpay.config.TeleportPayConfigData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigUtil {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ==============================
    // Config laden & speichern
    // ==============================

    public static TeleportPayConfigData loadMainConfig(MinecraftServer server) {
        File configFile = new File(server.getServerDirectory().toFile(), "config/teleportpay/config.json");
        if (!configFile.exists()) {
            TeleportPay.LOGGER.info("Config-Datei existiert nicht, erstelle Standard-Config.");
            TeleportPayConfigData defaultConfig = new TeleportPayConfigData();
            saveMainConfig(defaultConfig, server);
            return defaultConfig;
        }

        try (FileReader reader = new FileReader(configFile)) {
            TeleportPay.LOGGER.info("Lade Config-Datei von: " + configFile.getAbsolutePath());
            return GSON.fromJson(reader, TeleportPayConfigData.class);
        } catch (IOException e) {
            TeleportPay.LOGGER.error("Fehler beim Laden der Config-Datei:", e);
            return new TeleportPayConfigData();
        }
    }



    public static void saveMainConfig(TeleportPayConfigData config, MinecraftServer server) {
        File configFile = new File(server.getServerDirectory().toFile(), "config/teleportpay/config.json");
        try {
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==============================
    // Hilfsfunktionen
    // ==============================

    public static String getTranslatedItemName(ItemStack stack, Player player) {
        if (stack.isEmpty()) {
            return "???";
        }
        return stack.getHoverName().copy().withStyle(ChatFormatting.AQUA).getString();
    }

    public static boolean isTeleportDestinationSafe(Level level, BlockPos pos) {
        BlockPos feet = pos;
        BlockPos head = pos.above();

        if (!level.isInWorldBounds(feet) || !level.isInWorldBounds(head)) return false;

        if (!level.getBlockState(feet).isAir() && !level.getBlockState(feet).canBeReplaced()) return false;
        if (!level.getBlockState(head).isAir() && !level.getBlockState(head).canBeReplaced()) return false;

        Block below = level.getBlockState(feet.below()).getBlock();
        if (isUnsafeBlock(below)) return false;

        return true;
    }

    public static boolean isUnsafeBlock(Block block) {
        return block == Blocks.LAVA ||
                block == Blocks.WATER ||
                block == Blocks.POWDER_SNOW ||
                block == Blocks.CACTUS ||
                block == Blocks.MAGMA_BLOCK ||
                block == Blocks.FIRE ||
                block == Blocks.SAND ||  // Sand explizit prüfen
                block == Blocks.GRAVEL;  // Kies explizit prüfen
    }

    public static BlockPos findNextSafePosition(Level level, BlockPos startPos) {
        int maxY = Math.min(level.getMaxBuildHeight() - 2, 319);
        int minY = Math.max(level.getMinBuildHeight(), 0);

        for (int y = startPos.getY(); y <= maxY; y++) {
            BlockPos current = new BlockPos(startPos.getX(), y, startPos.getZ());
            if (isTeleportDestinationSafe(level, current)) {
                return current;
            }
        }

        for (int y = startPos.getY() - 1; y >= minY; y--) {
            BlockPos current = new BlockPos(startPos.getX(), y, startPos.getZ());
            if (isTeleportDestinationSafe(level, current)) {
                return current;
            }
        }

        return null; // Keine sichere Position gefunden
    }
}
