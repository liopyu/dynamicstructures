package net.liopyu.dynamicstructures.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.liopyu.dynamicstructures.DynamicStructures;
import net.liopyu.dynamicstructures.data.StructureLoader;
import net.liopyu.dynamicstructures.data.StructureSetLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static net.liopyu.dynamicstructures.DynamicStructures.LOGGER;

/**
 * Utility class providing helper methods for logging warnings and normalizing JSON objects.
 * This class is part of the {@code DSHelperClass} and includes methods to log default values
 * and normalize JSON objects by converting keys to lowercase and handling different data types.
 */
public class DSHelperClass {
    public static final Set<String> errorMessagesLogged = new HashSet<>();
    public static final Set<String> warningMessagesLogged = new HashSet<>();
    public static final Set<String> infoMessagesLogged = new HashSet<>();

    public static void logErrorMessageOnce(String errorMessage) {
        if (!errorMessagesLogged.contains(errorMessage)) {
            LOGGER.error("[Dynamic Structures]: " + errorMessage);
            errorMessagesLogged.add(errorMessage);
        }
    }

    public static void logErrorMessage(String errorMessage) {
        LOGGER.error("[Dynamic Structures]: " + errorMessage);
    }

    public static void logWarningMessageOnce(String errorMessage) {
        if (!warningMessagesLogged.contains(errorMessage)) {
            LOGGER.warn("[Dynamic Structures]: " + errorMessage);
            warningMessagesLogged.add(errorMessage);
        }
    }

    public static void logWarningMessage(String errorMessage) {
        LOGGER.warn("[Dynamic Structures]: " + errorMessage);
    }

    public static void logErrorMessageCatchable(String errorMessage, Throwable e) {
        LOGGER.error("[Dynamic Structures]: " + errorMessage, e);
    }

    public static void logErrorMessageOnceCatchable(String errorMessage, Throwable e) {
        if (!errorMessagesLogged.contains(errorMessage)) {
            LOGGER.error("[Dynamic Structures]: " + errorMessage, e);
            errorMessagesLogged.add(errorMessage);
        }
    }

    public static void logInfoMessageOnce(String info) {
        if (!infoMessagesLogged.contains(info)) {
            LOGGER.info("[Dynamic Structures]: " + info);
            infoMessagesLogged.add(info);
        }
    }

    public static void logInfoMessage(String info) {
        LOGGER.info("[Dynamic Structures]: " + info);
    }

    public static <T> boolean consumerCallback(Consumer<T> consumer, T value, String errorMessage) {
        try {
            consumer.accept(value);
        } catch (Throwable e) {
            logErrorMessageOnceCatchable(errorMessage, e);
            return false;
        }
        return true;
    }

    public static Object convertObjectToDesired(Object input, String outputType) {
        return switch (outputType.toLowerCase()) {
            case "integer" -> convertToInteger(input);
            case "double" -> convertToDouble(input);
            case "float" -> convertToFloat(input);
            case "boolean" -> convertToBoolean(input);
            case "interactionresult" -> convertToInteractionResult(input);
            case "resourcelocation" -> convertToResourceLocation(input);
            default -> input;
        };
    }

    public static ResourceLocation convertToResourceLocation(Object input) {
        if (input instanceof ResourceLocation) {
            return (ResourceLocation) input;
        } else if (input instanceof String) {
            return new ResourceLocation((String) input);
        }
        return null;
    }

    public static InteractionResult convertToInteractionResult(Object input) {
        if (input instanceof InteractionResult) {
            return (InteractionResult) input;
        } else if (input instanceof String) {
            String stringValue = ((String) input).toLowerCase();
            switch (stringValue) {
                case "success":
                    return InteractionResult.SUCCESS;
                case "consume":
                    return InteractionResult.CONSUME;
                case "pass":
                    return InteractionResult.PASS;
                case "fail":
                    return InteractionResult.FAIL;
                case "consume_partial":
                    return InteractionResult.CONSUME_PARTIAL;
            }
        }
        return null;
    }

    public static Boolean convertToBoolean(Object input) {
        if (input instanceof Boolean) {
            return (Boolean) input;
        } else if (input instanceof String) {
            String stringValue = ((String) input).toLowerCase();
            if ("true".equals(stringValue)) {
                return true;
            } else if ("false".equals(stringValue)) {
                return false;
            }
        }
        return null;
    }

    public static Integer convertToInteger(Object input) {
        if (input instanceof Integer) {
            return (Integer) input;
        } else if (input instanceof Double || input instanceof Float) {
            return ((Number) input).intValue();
        } else {
            return null;
        }
    }

    public static Double convertToDouble(Object input) {
        if (input instanceof Double) {
            return (Double) input;
        } else if (input instanceof Integer || input instanceof Float) {
            return ((Number) input).doubleValue();
        } else {
            return null;
        }
    }

    public static Float convertToFloat(Object input) {
        if (input instanceof Float) {
            return (Float) input;
        } else if (input instanceof Integer || input instanceof Double) {
            return ((Number) input).floatValue();
        } else {
            return null;
        }
    }

    /**
     * Derives and returns the structure name from a given JSON file path by converting it to a relative path.
     * This method removes the base path of the provided file and any file extension (e.g., ".json"),
     * converting file separators to standardize the path format.
     * <p>
     * The resulting string represents the relative structure name, which can be used for matching
     * or identifying structures within the game's file system.
     *
     * @param jsonFilePath The absolute path of the JSON file representing the structure.
     * @param file         The base file from which to derive the relative structure name.
     * @return The derived structure name as a relative path string.
     */
    public static String deriveStructureNameFromPath(String jsonFilePath, File file) {
        String relativePath = jsonFilePath.replace(file.getAbsolutePath(), "").replace(File.separator, "/");
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        if (relativePath.endsWith(".json")) {
            relativePath = relativePath.substring(0, relativePath.length() - 5);
        }
        return relativePath;
    }

    /**
     * Logs a warning message indicating that a field is missing or null in the specified JSON file,
     * and returns the provided default value. This method is useful for handling cases where
     * certain fields are optional or might be absent in the JSON configuration.
     *
     * <p>This method logs the warning only once per field name to avoid repetitive log entries.</p>
     *
     * @param <T>          The type of the default value.
     * @param fieldName    The name of the field that is missing or null.
     * @param defaultValue The default value to be returned.
     * @param jsonFilePath The path to the JSON file where the field is missing.
     * @return The default value provided.
     * @see #normalizeJson(JsonObject)
     */
    public static <T> T logDefault(String fieldName, T defaultValue, String jsonFilePath) {
        DSHelperClass.logWarningMessageOnce(fieldName + " is missing or null in " + jsonFilePath + ". Defaulting to [" + defaultValue + "].");
        return defaultValue;
    }

    /**
     * Normalizes a JSON object by converting all keys to lowercase and ensuring that the values are properly handled
     * according to their data types. This method is particularly useful for ensuring case-insensitivity in JSON keys
     * and for processing JSON objects with mixed data types.
     *
     * <p>The method iterates through each entry in the provided JSON object, converts the key to lowercase,
     * and re-adds the value to a new JSON object with the normalized key. It handles primitive types, arrays,
     * and nested JSON objects appropriately.</p>
     *
     * @param json The original JSON object to be normalized.
     * @return A new JSON object with normalized keys and values.
     * @see #logDefault(String, Object, String)
     */
    public static JsonObject normalizeJson(JsonObject json) {
        JsonObject normalizedJson = new JsonObject();
        json.entrySet().forEach(entry -> {
            String key = entry.getKey().toLowerCase();
            JsonElement element = entry.getValue();
            if (element.isJsonPrimitive()) {
                if (element.getAsJsonPrimitive().isString()) {
                    normalizedJson.addProperty(key, element.getAsString());
                } else if (element.getAsJsonPrimitive().isBoolean()) {
                    normalizedJson.addProperty(key, element.getAsBoolean());
                } else if (element.getAsJsonPrimitive().isNumber()) {
                    normalizedJson.addProperty(key, element.getAsNumber());
                }
            } else if (element.isJsonArray()) {
                normalizedJson.add(key, element.getAsJsonArray());
            } else if (element.isJsonObject()) {
                normalizedJson.add(key, element.getAsJsonObject());
            }
        });
        return normalizedJson;
    }

    /**
     * A generic utility method that attempts to cast an object to the specified type.
     *
     * <p>This method performs an unchecked cast and includes error handling to log
     * any {@link ClassCastException} that occurs during the casting process.</p>
     *
     * <p>If a {@code ClassCastException} is thrown, the exception is logged using
     * {@link DSHelperClass#logErrorMessageCatchable(String, Throwable)} and the method
     * returns {@code null}.</p>
     *
     * @param <T> The type to which the object is to be cast.
     * @param o   The object to be cast.
     * @return The object cast to the specified type {@code T}, or {@code null} if the cast fails.
     */
    public static <T> T cast(Object o) {
        try {
            return (T) o;
        } catch (ClassCastException cce) {
            DSHelperClass.logErrorMessageCatchable("", cce);
            return null;
        }
    }


    /**
     * Runs the provided Runnable if the current environment matches the specified environment.
     *
     * @param prodEnvironment If true, run the code in a production environment. If false, run the code in a development environment.
     * @param runnable        The code to run in the specified environment.
     */
    public static void environmentRunnable(boolean prodEnvironment, Runnable runnable) {
        if (prodEnvironment && FMLEnvironment.production) {
            runnable.run();
        } else if (!FMLEnvironment.production) {
            runnable.run();
        }
    }

    /**
     * Retrieves the {@link ContextUtils.SpawnContext} corresponding to the specified structure name.
     *
     * <p>This method searches through the loaded spawn contexts to find a match for the given
     * structure name. If a match is found, the corresponding {@link ContextUtils.SpawnContext}
     * is returned. If no match is found, a warning is logged, and {@code null} is returned.</p>
     *
     * @param structureName The name of the structure to find the corresponding {@link ContextUtils.SpawnContext}.
     * @return The {@link ContextUtils.SpawnContext} corresponding to the given structure name,
     * or {@code null} if no match is found.
     * @see ContextUtils.SpawnContext
     * @see StructureSetLoader#loadStructures()
     * @see DSHelperClass#logWarningMessage(String)
     */
    public static ContextUtils.SpawnContext getSpawnContext(String structureName) {
        var map = StructureSetLoader.cachedStructures;
        if (map.containsKey(structureName)) {
            return map.get(structureName);
        }
        DSHelperClass.logWarningMessage("SpawnContext with name '" + structureName + "' not found.");
        return null;
    }

    /**
     * Retrieves a {@link ContextUtils.StructureContext} instance based on the provided structure name.
     * This method checks if a structure with the given name exists within the loaded structures.
     * If the structure is found, its corresponding {@link ContextUtils.StructureContext} is returned.
     * If not found, a warning message is logged, and {@code null} is returned.
     *
     * @param structureName The name of the structure to retrieve the {@link ContextUtils.StructureContext} for.
     * @return The {@link ContextUtils.StructureContext} associated with the given structure name,
     * or {@code null} if no such structure is found.
     */
    public static ContextUtils.StructureContext getStructureContext(String structureName) {
        Map<String, ContextUtils.StructureContext> structures = StructureLoader.loadStructures();
        if (structures.containsKey(structureName)) {
            return structures.get(structureName);
        }
        DSHelperClass.logWarningMessage("StructureContext with name '" + structureName + "' not found.");
        return null;
    }
}