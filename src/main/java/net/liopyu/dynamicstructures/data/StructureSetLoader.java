package net.liopyu.dynamicstructures.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.liopyu.dynamicstructures.util.ContextUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ProtoChunk;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StructureSetLoader {
    private static final File STRUCTURE_SET_DIR = new File("config/dynamicstructures/structure_set/");

    public static List<ContextUtils.SpawnContext> loadStructureSets(ServerLevel level, ProtoChunk chunk) {
        List<ContextUtils.SpawnContext> spawnContexts = new ArrayList<>();

        if (!STRUCTURE_SET_DIR.exists()) {
            STRUCTURE_SET_DIR.mkdirs();
        }

        File[] files = STRUCTURE_SET_DIR.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try (FileReader reader = new FileReader(file)) {
                    JsonElement jsonElement = JsonParser.parseReader(reader);
                    if (jsonElement.isJsonObject()) {
                        JsonObject jsonObject = jsonElement.getAsJsonObject();
                        ContextUtils.SpawnContext spawnContext = ContextUtils.SpawnContext.fromJson(jsonObject, chunk, level, file.getAbsolutePath());
                        spawnContexts.add(spawnContext);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return spawnContexts;
    }
}
