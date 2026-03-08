package com.thatmg393.bettertpa4fabric.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.thatmg393.bettertpa4fabric.BetterTPA4Fabric;
import com.thatmg393.bettertpa4fabric.config.annotations.ConfigComment;
import com.thatmg393.bettertpa4fabric.config.data.ModConfigData;

import net.fabricmc.loader.api.FabricLoader;

public class ModConfigManager {
    private static final Gson GSON = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
    public static final File CONFIG_PATH = new File(Paths.get(
            FabricLoader.getInstance().getConfigDir().toString(),
            BetterTPA4Fabric.MOD_ID + ".json"
        ).toString()
    );

    private static final ModConfigData defaultConfig = new ModConfigData();
    private static ModConfigData loadedConfig;

    public static ModConfigData loadOrGetConfig() {
        if (loadedConfig != null) return loadedConfig;

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(CONFIG_PATH))) {
            ModConfigData parsedConfig = GSON.fromJson(bufferedReader, ModConfigData.class);

            if (parsedConfig.configVersion != defaultConfig.configVersion) {
                BetterTPA4Fabric.LOGGER.warn(
                    "Config version mismatch (got {}, expected {}). Migrating...",
                    parsedConfig.configVersion, defaultConfig.configVersion
                );
                loadedConfig = mergeWithDefaults(parsedConfig, defaultConfig);
                saveConfig();
            } else {
                loadedConfig = parsedConfig;
            }
        } catch (IOException | JsonSyntaxException e) {
            BetterTPA4Fabric.LOGGER.error("Failed to load config! " + e.toString());
            BetterTPA4Fabric.LOGGER.info("Regenerating config with defaults...");
            loadedConfig = new ModConfigData();
            saveConfig();
        }

        return loadedConfig;
    }

    public static ModConfigData getDefaultConfig() {
        return defaultConfig;
    }

    public static void saveConfig() {
        try (FileWriter fileWriter = new FileWriter(CONFIG_PATH)) {
            fileWriter.write(serializeWithComments(loadedConfig));
        } catch (IOException e) {
            BetterTPA4Fabric.LOGGER.error(e.toString());
        }
    }

    private static ModConfigData mergeWithDefaults(ModConfigData loaded, ModConfigData defaults) {
        JsonObject loadedJson = GSON.toJsonTree(loaded).getAsJsonObject();
        JsonObject defaultJson = GSON.toJsonTree(defaults).getAsJsonObject();

        for (Map.Entry<String, JsonElement> entry : defaultJson.entrySet()) {
            if (!loadedJson.has(entry.getKey())) {
                loadedJson.add(entry.getKey(), entry.getValue());
            }
        }

        loadedJson.entrySet().removeIf(e -> !defaultJson.has(e.getKey()));
        loadedJson.addProperty("configVersion", defaults.configVersion);

        return GSON.fromJson(loadedJson, ModConfigData.class);
    }

    private static String serializeWithComments(ModConfigData config) {
        JsonObject json = GSON.toJsonTree(config).getAsJsonObject();
        StringBuilder sb = new StringBuilder("{\n");
        Field[] fields = ModConfigData.class.getDeclaredFields();

        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            ConfigComment comment = field.getAnnotation(ConfigComment.class);
            String key = field.getName();

            if (!json.has(key)) continue;

            boolean isLast = (i == fields.length - 1);

            if (comment != null) {
                sb.append("    \"_comment_").append(key).append("\": \"")
                  .append(comment.value().replace("\"", "\\\""))
                  .append("\",\n");
            }

            sb.append("    \"").append(key).append("\": ")
              .append(GSON.toJson(json.get(key)));

            if (!isLast) sb.append(",");
            sb.append("\n");
        }

        return sb.append("}").toString();
    }
}
