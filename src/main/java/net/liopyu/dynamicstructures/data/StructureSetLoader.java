package net.liopyu.dynamicstructures.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.liopyu.dynamicstructures.util.ContextUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * The {@code StructureSetLoader} class is responsible for loading structure spawning configurations
 * from JSON files located in the "config/dynamicstructures/structure_set/" directory.
 * These configurations define the spawning rules and parameters for structures in Minecraft.
 *
 * <p>This class provides methods to load spawn contexts from disk, cache them for reuse, and
 * create default spawn context configurations if none are found. It supports recursive loading
 * of JSON files from directories, enabling organized storage of spawning configuration files.</p>
 *
 * <p>The class uses the {@link ContextUtils.SpawnContext} class to represent the spawning attributes of structures.
 * JSON files are parsed into {@link JsonObject} instances, which are then converted into
 * {@link ContextUtils.SpawnContext} objects. The loaded spawn contexts are cached to avoid redundant
 * loading operations.</p>
 *
 * <p><strong>Usage:</strong></p>
 * <ul>
 *   <li>{@link #loadStructures()} - Loads and caches spawn contexts from JSON files.</li>
 *   <li>{@link #clearCache()} - Clears the cached spawn contexts, forcing a reload on the next access.</li>
 * </ul>
 */
public class StructureSetLoader {
    public static final File STRUCTURE_DIR = new File("config/dynamicstructures/structure_set/");
    private static final File DEFAULT_STRUCTURE_FILE = new File(STRUCTURE_DIR, "example_structure.json");
    public static Map<String, ContextUtils.SpawnContext> cachedStructures = new HashMap<>();
    private static boolean structuresLoaded = false;

    /**
     * Loads structure spawn context configurations from the specified directory. If contexts have already
     * been loaded, the cached list is returned. If no contexts are found, a default context is loaded.
     *
     * @return A list of loaded {@link ContextUtils.SpawnContext} objects.
     */
    public static Map<String, ContextUtils.SpawnContext> loadStructures() {
        if (structuresLoaded) {
            return cachedStructures;
        }

        if (!STRUCTURE_DIR.exists()) {
            STRUCTURE_DIR.mkdirs();
        }

        loadJsonFilesRecursively(STRUCTURE_DIR, cachedStructures);

        if (cachedStructures.isEmpty()) {
            loadDefaultStructure(cachedStructures);
        }

        structuresLoaded = true;
        return cachedStructures;
    }

    /**
     * Clears the cache of loaded spawn contexts, allowing them to be reloaded from disk
     * on the next call to {@link #loadStructures()}.
     */
    public static void clearCache() {
        cachedStructures.clear();
        structuresLoaded = false;
    }

    /**
     * Recursively loads JSON files from the specified directory and converts them into
     * {@link ContextUtils.SpawnContext} objects.
     *
     * @param directory  The directory to scan for JSON files.
     * @param structures The list to store the loaded {@link ContextUtils.SpawnContext} objects.
     */
    private static void loadJsonFilesRecursively(File directory, Map<String, ContextUtils.SpawnContext> structures) {
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    loadJsonFilesRecursively(file, structures);
                } else if (file.isFile() && file.getName().endsWith(".json")) {
                    try (FileReader reader = new FileReader(file)) {
                        JsonElement jsonElement = JsonParser.parseReader(reader);
                        if (jsonElement.isJsonObject()) {
                            JsonObject jsonObject = jsonElement.getAsJsonObject();
                            ContextUtils.SpawnContext context = ContextUtils.SpawnContext.fromJson(jsonObject, file.getAbsolutePath());
                            structures.put(context.getName(), context);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Loads a default structure spawn context configuration if no other contexts are found.
     * The default context is defined in {@link #DEFAULT_STRUCTURE_FILE}.
     *
     * @param structures The list to store the loaded {@link ContextUtils.SpawnContext} objects.
     */
    private static void loadDefaultStructure(Map<String, ContextUtils.SpawnContext> structures) {
        try {
            if (!DEFAULT_STRUCTURE_FILE.exists()) {
                createDefaultStructureFile();
            }

            try (FileReader reader = new FileReader(DEFAULT_STRUCTURE_FILE)) {
                JsonElement jsonElement = JsonParser.parseReader(reader);
                if (jsonElement.isJsonObject()) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    ContextUtils.SpawnContext context = ContextUtils.SpawnContext.fromJson(jsonObject, DEFAULT_STRUCTURE_FILE.getAbsolutePath());
                    structures.put(context.getName(), context);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a default structure spawn context JSON file in the specified directory.
     * This file is used if no other contexts are found.
     */
    private static void createDefaultStructureFile() {
        Random random = new Random();
        int randomSalt = 1_000_000_000 + random.nextInt(1_000_000_000);
        if (DEFAULT_STRUCTURE_FILE.exists()) {
            DEFAULT_STRUCTURE_FILE.delete();
        }
        try (FileWriter writer = new FileWriter(DEFAULT_STRUCTURE_FILE)) {
            writer.write("{\n");
            writer.write("    \"name\": \"example_structure\",\n");
            writer.write("    \"salt\": " + randomSalt + ",\n");
            writer.write("    \"separation\": 17,\n");
            writer.write("    \"spacing\": 20\n");
            writer.write("    \"y_min\": -32,\n");
            writer.write("    \"y_max\": 0,\n");
            writer.write("    \"max_distance\": 35\n");
            writer.write("}\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
