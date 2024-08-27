package net.liopyu.dynamicstructures.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.liopyu.dynamicstructures.util.ContextUtils;
import net.liopyu.dynamicstructures.util.DSHelperClass;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@code StructureLoader} class is responsible for loading structure configurations
 * from JSON files located in the "config/dynamicstructures/structures/" directory.
 * These configurations define the structure's properties and are used during structure generation in Minecraft.
 *
 * <p>This class provides methods to load structures from disk, cache them for reuse, and
 * create default structure configurations if none are found. It supports recursive loading
 * of JSON files from directories, allowing for organized structure configuration storage.</p>
 *
 * <p>The class uses the {@link ContextUtils.StructureContext} class to represent the structure's attributes.
 * JSON files are parsed into {@link JsonObject} instances, which are then converted into
 * {@link ContextUtils.StructureContext} objects. The loaded structures are cached to avoid redundant
 * loading operations.</p>
 *
 * <p><strong>Usage:</strong></p>
 * <ul>
 *   <li>{@link #loadStructures} - Loads and caches structures from JSON files.</li>
 *   <li>{@link #clearCache()} - Clears the cached structures, forcing a reload on the next access.</li>
 * </ul>
 */
public class StructureLoader {
    public static final File STRUCTURE_DIR = new File("config/dynamicstructures/structures/");
    private static final File DEFAULT_STRUCTURE_FILE = new File(STRUCTURE_DIR, "example_structure.json");
    public static Map<String, ContextUtils.StructureContext> cachedStructures = new HashMap<>();
    private static boolean structuresLoaded = false;

    /**
     * Loads structure configurations from the specified directory. If structures have already
     * been loaded, the cached list is returned. If no structures are found, a default structure
     * is loaded.
     *
     * @return A map of loaded {@link ContextUtils.StructureContext} objects and their names.
     */
    public static Map<String, ContextUtils.StructureContext> loadStructures() {
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
     * Clears the cache of loaded structures, allowing them to be reloaded from disk
     * on the next call to {@link #loadStructures}.
     */
    public static void clearCache() {
        cachedStructures.clear();
        structuresLoaded = false;
    }

    /**
     * Recursively loads JSON files from the specified directory, parses them into
     * {@link ContextUtils.StructureContext} objects, and adds them to the provided list.
     * If a structure with the same name has already been registered, it logs an error
     * and skips adding the duplicate structure.
     *
     * @param directory  The directory to search for JSON files.
     * @param structures The map where the parsed {@link ContextUtils.StructureContext}
     *                   objects will be added.
     */
    private static void loadJsonFilesRecursively(File directory, Map<String, ContextUtils.StructureContext> structures) {
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
                            ContextUtils.StructureContext newContext = ContextUtils.StructureContext.fromJson(jsonObject, file.getAbsolutePath());
                            boolean alreadyExists = structures.keySet().stream()
                                    .anyMatch(existingContext -> existingContext.equals(newContext.getStructureName()));
                            if (alreadyExists) {
                                DSHelperClass.logErrorMessage("Structure '" + newContext.getStructureName() + "' is already registered. Skipping duplicate entry in file: " + file.getAbsolutePath());
                            } else {
                                structures.put(newContext.getStructureName(), newContext);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    /**
     * Loads a default structure configuration if no other structures are found.
     * The default structure is defined in {@link #DEFAULT_STRUCTURE_FILE}.
     *
     * @param structures The map to store the loaded {@link ContextUtils.StructureContext} objects and their names.
     */
    private static void loadDefaultStructure(Map<String, ContextUtils.StructureContext> structures) {
        try {
            if (!DEFAULT_STRUCTURE_FILE.exists()) {
                createDefaultStructureFile();
            }
            try (FileReader reader = new FileReader(DEFAULT_STRUCTURE_FILE)) {
                JsonElement jsonElement = JsonParser.parseReader(reader);
                if (jsonElement.isJsonObject()) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    ContextUtils.StructureContext context = ContextUtils.StructureContext.fromJson(jsonObject, DEFAULT_STRUCTURE_FILE.getAbsolutePath());
                    structures.put(context.getStructureName(), context);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a default structure JSON file in the specified directory. This file is used
     * if no other structures are found.
     */
    private static void createDefaultStructureFile() {
        try (FileWriter writer = new FileWriter(DEFAULT_STRUCTURE_FILE)) {
            writer.write("{\n");
            writer.write("    \"Structure Name\": \"example_structure\",\n");
            writer.write("    \"Ladder Chance\": 10,\n");
            writer.write("    \"Room Count\": 10,\n");
            writer.write("    \"Height\": 6,\n");
            writer.write("    \"Width\": 10,\n");
            writer.write("    \"Length\": 10,\n");
            writer.write("    \"Size Threshold\": 20,\n");
            writer.write("    \"Generate Spawners\": true,\n");
            writer.write("    \"Max Spawners\": 2,\n");
            writer.write("    \"Spawner Entities\": [\n");
            writer.write("        \"minecraft:zombie\",\n");
            writer.write("        \"minecraft:skeleton\"\n");
            writer.write("    ]\n");
            writer.write("}\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
