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
import java.util.ArrayList;
import java.util.List;

public class StructureSetLoader {
    public static final File STRUCTURE_DIR = new File("config/dynamicstructures/structure_set/");
    private static final File DEFAULT_STRUCTURE_FILE = new File(STRUCTURE_DIR, "example_structure.json");

    public static List<ContextUtils.SpawnContext> loadStructures() {
        List<ContextUtils.SpawnContext> structures = new ArrayList<>();

        if (!STRUCTURE_DIR.exists()) {
            STRUCTURE_DIR.mkdirs();
        }

        // Recursively load JSON files from the directory and its subdirectories
        loadJsonFilesRecursively(STRUCTURE_DIR, structures);

        if (structures.isEmpty()) {
            // No JSON files found, load default
            loadDefaultStructure(structures);
        }

        return structures;
    }

    private static void loadJsonFilesRecursively(File directory, List<ContextUtils.SpawnContext> structures) {
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // Recursively search subdirectories
                    loadJsonFilesRecursively(file, structures);
                } else if (file.isFile() && file.getName().endsWith(".json")) {
                    try (FileReader reader = new FileReader(file)) {
                        JsonElement jsonElement = JsonParser.parseReader(reader);
                        if (jsonElement.isJsonObject()) {
                            JsonObject jsonObject = jsonElement.getAsJsonObject();
                            ContextUtils.SpawnContext context = ContextUtils.SpawnContext.fromJson(jsonObject, file.getAbsolutePath());
                            structures.add(context);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static void loadDefaultStructure(List<ContextUtils.SpawnContext> structures) {
        try {
            if (!DEFAULT_STRUCTURE_FILE.exists()) {
                createDefaultStructureFile();
            }

            try (FileReader reader = new FileReader(DEFAULT_STRUCTURE_FILE)) {
                JsonElement jsonElement = JsonParser.parseReader(reader);
                if (jsonElement.isJsonObject()) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    // Pass the default file path for logging
                    ContextUtils.SpawnContext context = ContextUtils.SpawnContext.fromJson(jsonObject, DEFAULT_STRUCTURE_FILE.getAbsolutePath());
                    structures.add(context);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createDefaultStructureFile() {
        /*try (FileWriter writer = new FileWriter(DEFAULT_STRUCTURE_FILE)) {
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
        }*/
    }
}
