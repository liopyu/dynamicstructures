package net.liopyu.dynamicstructures.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static net.liopyu.dynamicstructures.data.StructureLoader.STRUCTURE_DIR;

public class ContextUtils {
    public static class StructureContext {
        /**
         * Default values if not specified
         */
        private static final float DEFAULT_LADDER_CHANCE = 10.0f;
        private static final int DEFAULT_ROOM_COUNT = 15;
        private static final int DEFAULT_HEIGHT = 8;
        private static final int DEFAULT_WIDTH = 8;
        private static final int DEFAULT_LENGTH = 8;
        private static final int DEFAULT_SIZE_THRESHOLD = 30;
        private static final boolean DEFAULT_GENERATE_SPAWNERS = false;
        private static final int DEFAULT_MAX_SPAWNERS = 1;
        private static final List<EntityType<?>> DEFAULT_SPAWNER_ENTITIES = List.of(EntityType.ZOMBIE, EntityType.CREEPER);
        private final ServerLevel level;
        private final BlockPos startPos;
        private final Direction startDirection;
        private final String structureName;
        private final float ladderRoomChance;
        private final int roomCount;
        private final int height;
        private final int width;
        private final int length;
        private final boolean generatesSpawners;
        private final int maxSpawners;
        private final int sizeThreshold;
        private final List<EntityType<?>> potentialSpawns;
        public StructureContext(String structureName, ServerLevel level, BlockPos startPos, float ladderRoomChance, int roomCount, int height, int width, int length, boolean generatesSpawners, int maxSpawners, List<EntityType<?>> potentialSpawns, int sizeThreshold) {
            this.level = level;
            this.startPos = startPos;
            this.sizeThreshold = sizeThreshold;
            this.startDirection = getRandomDirection();
            this.structureName = structureName;
            this.ladderRoomChance = ladderRoomChance;
            this.roomCount = roomCount;
            this.height = height;
            this.width = width;
            this.length = length;
            this.generatesSpawners = generatesSpawners;
            this.maxSpawners = maxSpawners;
            this.potentialSpawns = potentialSpawns;
        }
        public static StructureContext fromJson(JsonObject json, ServerLevel level, BlockPos startPos, String jsonFilePath) {
            String structureName;
            if (json.has("Structure Name")) {
                structureName = json.get("Structure Name").getAsString();
            } else {
                structureName = deriveStructureNameFromPath(jsonFilePath);
                DSHelperClass.logWarningMessageOnce("Structure Name is missing or null in " + jsonFilePath + ". Defaulting to [" + structureName + "].");
            }
            float ladderChance = json.has("Ladder Chance") ? json.get("Ladder Chance").getAsFloat() : logDefault("Ladder Chance", DEFAULT_LADDER_CHANCE, jsonFilePath);
            int roomCount = json.has("Room Count") ? json.get("Room Count").getAsInt() : logDefault("Room Count", DEFAULT_ROOM_COUNT, jsonFilePath);
            int height = json.has("Height") ? json.get("Height").getAsInt() : logDefault("Height", DEFAULT_HEIGHT, jsonFilePath);
            int width = json.has("Width") ? json.get("Width").getAsInt() : logDefault("Width", DEFAULT_WIDTH, jsonFilePath);
            int length = json.has("Length") ? json.get("Length").getAsInt() : logDefault("Length", DEFAULT_LENGTH, jsonFilePath);
            int sizeThreshold = json.has("Size Threshold") ? json.get("Size Threshold").getAsInt() : logDefault("Size Threshold", DEFAULT_SIZE_THRESHOLD, jsonFilePath);
            boolean generateSpawners = json.has("Generate Spawners") ? json.get("Generate Spawners").getAsBoolean() : logDefault("Generate Spawners", DEFAULT_GENERATE_SPAWNERS, jsonFilePath);
            int maxSpawners = json.has("Max Spawners") ? json.get("Max Spawners").getAsInt() : logDefault("Max Spawners", DEFAULT_MAX_SPAWNERS, jsonFilePath);

            List<EntityType<?>> spawnerEntities = new ArrayList<>();
            if (json.has("Spawner Entities")) {
                List<EntityType<?>> finalSpawnerEntities = spawnerEntities;
                json.getAsJsonArray("Spawner Entities").forEach(element -> {
                    String entityName = element.getAsString();
                    EntityType.byString(entityName).ifPresent(finalSpawnerEntities::add);
                });
            } else {
                spawnerEntities = DEFAULT_SPAWNER_ENTITIES;
                DSHelperClass.logWarningMessageOnce("Spawner Entities is missing or null in " + jsonFilePath + ". Defaulting to " + DEFAULT_SPAWNER_ENTITIES + ".");
            }
            return new StructureContext(
                    structureName,
                    level,
                    startPos,
                    ladderChance,
                    roomCount,
                    height,
                    width,
                    length,
                    generateSpawners,
                    maxSpawners,
                    spawnerEntities,
                    sizeThreshold
            );
        }
        private static String deriveStructureNameFromPath(String jsonFilePath) {
            String relativePath = jsonFilePath.replace(STRUCTURE_DIR.getAbsolutePath(), "").replace(File.separator, "/");
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
            if (relativePath.endsWith(".json")) {
                relativePath = relativePath.substring(0, relativePath.length() - 5);
            }
            return relativePath;
        }
        private static <T> T logDefault(String fieldName, T defaultValue, String jsonFilePath) {
            DSHelperClass.logWarningMessageOnce(fieldName + " is missing or null in " + jsonFilePath + ". Defaulting to [" + defaultValue + "].");
            return defaultValue;
        }
        public int getSizeThreshold() {
            return sizeThreshold;
        }
        private Direction getRandomDirection() {
            Direction[] cardinalDirections = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
            return cardinalDirections[new Random().nextInt(cardinalDirections.length)];
        }
        public float getLadderRoomChance() {
            return ladderRoomChance;
        }
        public Direction getStartDirection() {
            return startDirection;
        }
        public BlockPos getStartPos() {
            return startPos;
        }
        public ServerLevel getLevel() {
            return level;
        }
        public String getStructureName() {
            return structureName;
        }
        public int getRoomCount() {
            return roomCount;
        }
        public int getHeight() {
            return height;
        }

        public int getWidth() {
            return width;
        }

        public int getLength() {
            return length;
        }

        public boolean isGeneratesSpawners() {
            return generatesSpawners;
        }

        public int getMaxSpawners() {
            return maxSpawners;
        }

        public List<EntityType<?>> getPotentialSpawns() {
            return potentialSpawns;
        }
    }
    public static class SpawnContext {
        private final String name;
        private final ProtoChunk protoChunk;
        private final ServerLevel level;
        private final long salt;
        private final int separation;
        private final int spacing;

        public SpawnContext(String name, ProtoChunk protoChunk, ServerLevel level, long salt, int separation, int spacing) {
            this.name = name;
            this.protoChunk = protoChunk;
            this.level = level;
            this.salt = salt;
            this.separation = separation;
            this.spacing = spacing;
        }
        public static SpawnContext fromJson(JsonObject json, ProtoChunk protoChunk,ServerLevel level,String jsonFilePath) {
            String structureName;
            if (json.has("Structure Name")) {
                structureName = json.get("Structure Name").getAsString();
            } else {
                structureName = deriveStructureNameFromPath(jsonFilePath);
                DSHelperClass.logWarningMessageOnce("Structure Name is missing or null in " + jsonFilePath + ". Defaulting to [" + structureName + "].");
            }
            long salt = json.has("Salt") ? json.get("Salt").getAsLong() : logDefault("Salt", 1738452910, jsonFilePath);
            int separation = json.has("Separation") ? json.get("Separation").getAsInt() : logDefault("Separation", 8, jsonFilePath);
            int spacing = json.has("Spacing") ? json.get("Spacing").getAsInt() : logDefault("Spacing", 34, jsonFilePath);

            return new SpawnContext(
                    structureName,
                    protoChunk,
                    level,
                    salt,
                    separation,
                    spacing
            );
        }
        private static String deriveStructureNameFromPath(String jsonFilePath) {
            String relativePath = jsonFilePath.replace(STRUCTURE_DIR.getAbsolutePath(), "").replace(File.separator, "/");
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
            if (relativePath.endsWith(".json")) {
                relativePath = relativePath.substring(0, relativePath.length() - 5);
            }
            return relativePath;
        }
        private static <T> T logDefault(String fieldName, T defaultValue, String jsonFilePath) {
            DSHelperClass.logWarningMessageOnce(fieldName + " is missing or null in " + jsonFilePath + ". Defaulting to [" + defaultValue + "].");
            return defaultValue;
        }

        public String getName() {
            return name;
        }

        public ProtoChunk getProtoChunk() {
            return protoChunk;
        }

        public ServerLevel getLevel() {
            return level;
        }

        public long getSalt() {
            return salt;
        }

        public int getSpacing() {
            return spacing;
        }

        public int getSeparation() {
            return separation;
        }
    }
}
