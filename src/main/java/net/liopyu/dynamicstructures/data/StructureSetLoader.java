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
import java.util.Random;

public class StructureSetLoader {
    private static boolean structuresLoaded = false;
    public static final File STRUCTURE_DIR = new File("config/dynamicstructures/structure_set/");
    private static final File DEFAULT_STRUCTURE_FILE = new File(STRUCTURE_DIR, "example_structure.json");
    private static List<ContextUtils.SpawnContext> cachedStructures = new ArrayList<>();

    public static List<ContextUtils.SpawnContext> loadStructures() {
        if (structuresLoaded) {
            return cachedStructures;
        }

        if (!STRUCTURE_DIR.exists()) {
            STRUCTURE_DIR.mkdirs();
        }

        loadJsonFilesRecursively(STRUCTURE_DIR, cachedStructures);

        if (cachedStructures.isEmpty()) {
            loadDefaultStructure( cachedStructures);
        }

        structuresLoaded = true;
        return cachedStructures;
    }
    public static void clearCache() {
        cachedStructures.clear();
        structuresLoaded = false;
    }

    private static void loadJsonFilesRecursively(File directory, List<ContextUtils.SpawnContext> structures) {
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
                    ContextUtils.SpawnContext context = ContextUtils.SpawnContext.fromJson(jsonObject, DEFAULT_STRUCTURE_FILE.getAbsolutePath());
                    structures.add(context);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createDefaultStructureFile() {
        Random random = new Random();
        int randomSalt = 1_000_000_000 + random.nextInt(1_000_000_000);

        try (FileWriter writer = new FileWriter(DEFAULT_STRUCTURE_FILE)) {
            writer.write("{\n");
            writer.write("    \"Structure Name\": \"test\",\n");
            writer.write("    \"Salt\": " + randomSalt + ",\n");
            writer.write("    \"Separation\": 7,\n");
            writer.write("    \"Spacing\": 8,\n");
            writer.write("    \"y Min\": -32,\n");
            writer.write("    \"y Max\": 0\n");
            writer.write("    \"Max Distance\": 35,\n");
            writer.write("}\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
