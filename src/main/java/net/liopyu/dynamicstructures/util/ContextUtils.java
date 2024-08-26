package net.liopyu.dynamicstructures.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.liopyu.dynamicstructures.data.StructureLoader;
import net.liopyu.dynamicstructures.data.StructureSetLoader;
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

import static net.liopyu.dynamicstructures.util.DSHelperClass.normalizeJson;

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
        public static final List<EntityType<?>> DEFAULT_SPAWNER_ENTITIES = List.of(EntityType.ZOMBIE, EntityType.CREEPER);
        private final ServerLevel level;
        private BlockPos startPos;
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
            JsonObject normalizedJson = normalizeJson(json);

            String structureName = normalizedJson.has("structure name") ? normalizedJson.get("structure name").getAsString() :
                    DSHelperClass.deriveStructureNameFromPath(jsonFilePath, StructureLoader.STRUCTURE_DIR);
            if (!normalizedJson.has("structure name")) {
                DSHelperClass.logWarningMessageOnce("Structure Name is missing or null in " + jsonFilePath + ". Defaulting to [" + structureName + "].");
            }

            float ladderChance = normalizedJson.has("ladder chance") ? normalizedJson.get("ladder chance").getAsFloat() : DSHelperClass.logDefault("Ladder Chance", DEFAULT_LADDER_CHANCE, jsonFilePath);
            int roomCount = normalizedJson.has("room count") ? normalizedJson.get("room count").getAsInt() : DSHelperClass.logDefault("Room Count", DEFAULT_ROOM_COUNT, jsonFilePath);
            int height = normalizedJson.has("height") ? normalizedJson.get("height").getAsInt() : DSHelperClass.logDefault("Height", DEFAULT_HEIGHT, jsonFilePath);
            int width = normalizedJson.has("width") ? normalizedJson.get("width").getAsInt() : DSHelperClass.logDefault("Width", DEFAULT_WIDTH, jsonFilePath);
            int length = normalizedJson.has("length") ? normalizedJson.get("length").getAsInt() : DSHelperClass.logDefault("Length", DEFAULT_LENGTH, jsonFilePath);
            int sizeThreshold = normalizedJson.has("size threshold") ? normalizedJson.get("size threshold").getAsInt() : DSHelperClass.logDefault("Size Threshold", DEFAULT_SIZE_THRESHOLD, jsonFilePath);
            boolean generateSpawners = normalizedJson.has("generate spawners") ? normalizedJson.get("generate spawners").getAsBoolean() : DSHelperClass.logDefault("Generate Spawners", DEFAULT_GENERATE_SPAWNERS, jsonFilePath);
            int maxSpawners = normalizedJson.has("max spawners") ? normalizedJson.get("max spawners").getAsInt() : DSHelperClass.logDefault("Max Spawners", DEFAULT_MAX_SPAWNERS, jsonFilePath);

            List<EntityType<?>> spawnerEntities = new ArrayList<>();
            if (normalizedJson.has("spawner entities")) {
                JsonArray entitiesArray = normalizedJson.getAsJsonArray("spawner entities");
                for (JsonElement element : entitiesArray) {
                    String entityName = element.getAsString();
                    EntityType.byString(entityName).ifPresent(spawnerEntities::add);
                }
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



        public int getSizeThreshold() {
            return sizeThreshold;
        }
        private Direction getRandomDirection() {
            Direction[] cardinalDirections = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
            return cardinalDirections[new Random().nextInt(cardinalDirections.length)];
        }

        public void setStartPos(BlockPos startPos) {
            this.startPos = startPos;
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
        private final int maxDistanceFromCenter;
        private static final int DEFAULT_MAX_DISTANCE_FROM_CENTER = 35;
        private final int yMin;
        private final int yMax;
        private final String name;
        private final long salt;
        private final int separation;
        private final int spacing;

        public SpawnContext(String name, long salt, int separation, int spacing, int yMin, int yMax,int maxDistanceFromCenter) {
            this.maxDistanceFromCenter = maxDistanceFromCenter;
            this.yMin = yMin;
            this.yMax = yMax;
            this.name = name;
            this.salt = salt;
            this.separation = separation;
            this.spacing = spacing;
        }
        public static SpawnContext fromJson(JsonObject json, String jsonFilePath) {
            // Normalize the JSON object to handle different data types safely
            JsonObject normalizedJson = normalizeJson(json);

            // Retrieve or derive the structure name
            String structureName = normalizedJson.has("structure name") ? normalizedJson.get("structure name").getAsString() :
                    DSHelperClass.deriveStructureNameFromPath(jsonFilePath, StructureSetLoader.STRUCTURE_DIR);
            if (!normalizedJson.has("structure name")) {
                DSHelperClass.logWarningMessageOnce("Structure Name is missing or null in " + jsonFilePath + ". Defaulting to [" + structureName + "].");
            }

            // Extract properties from the normalized JSON
            long salt = normalizedJson.has("salt") ? normalizedJson.get("salt").getAsLong() : DSHelperClass.logDefault("Salt", 1738452910L, jsonFilePath);
            int separation = normalizedJson.has("separation") ? normalizedJson.get("separation").getAsInt() : DSHelperClass.logDefault("Separation", 8, jsonFilePath);
            int spacing = normalizedJson.has("spacing") ? normalizedJson.get("spacing").getAsInt() : DSHelperClass.logDefault("Spacing", 34, jsonFilePath);
            int yMin = normalizedJson.has("y min") ? normalizedJson.get("y min").getAsInt() : DSHelperClass.logDefault("y Min", -32, jsonFilePath);
            int yMax = normalizedJson.has("y max") ? normalizedJson.get("y max").getAsInt() : DSHelperClass.logDefault("y Max", 0, jsonFilePath);
            int maxDistanceFromCenter = normalizedJson.has("max distance") ? normalizedJson.get("max distance").getAsInt() : DSHelperClass.logDefault("Max Distance", DEFAULT_MAX_DISTANCE_FROM_CENTER, jsonFilePath);

            return new SpawnContext(
                    structureName,
                    salt,
                    separation,
                    spacing,
                    yMin,
                    yMax,
                    maxDistanceFromCenter
            );
        }

        public int getMaxDistanceFromCenter() {
            return maxDistanceFromCenter;
        }

        public int getyMin() {
            return yMin;
        }

        public int getyMax() {
            return yMax;
        }

        public String getName() {
            return name;
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
