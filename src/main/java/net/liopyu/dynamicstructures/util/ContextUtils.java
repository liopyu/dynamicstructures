package net.liopyu.dynamicstructures.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.liopyu.dynamicstructures.data.StructureLoader;
import net.liopyu.dynamicstructures.data.StructureSetLoader;
import net.liopyu.dynamicstructures.data.enums.BlockType;
import net.liopyu.dynamicstructures.data.enums.KeyWordType;
import net.liopyu.dynamicstructures.data.json.BlockInterpreter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

import static net.liopyu.dynamicstructures.data.json.BlockInterpreter.allowedBlockTypes;
import static net.liopyu.dynamicstructures.util.DSHelperClass.normalizeJson;

public class ContextUtils {
    public static class WeightedBlock {
        public Block block;
        public final int weight;
        public final JsonObject blockObject;
        public BlockType blockType;

        public WeightedBlock(Block block, int weight, JsonObject blockObject, BlockType blockType) {
            this.block = block;
            this.weight = weight;
            this.blockObject = blockObject;
            this.blockType = blockType;
        }
    }

    public static class BlockContext {
        private final JsonObject json;
        public int defaultDoorwayRadius = 1;
        private ServerLevel level;
        private BlockPos pos;
        public WeightedBlock floorBlock;
        public WeightedBlock wallBlock;
        public WeightedBlock roofBlock;
        public WeightedBlock centerBlock;
        public WeightedBlock doorwayBlock;
        public WeightedBlock fillerBlock;
        private Map<BlockType, JsonObject> predicates = new HashMap<>();
        private Map<BlockType, JsonObject> functions = new HashMap<>();
        public List<WeightedBlock> weightedBlocks = new ArrayList<>();

        public BlockContext(JsonObject normalizedJson) {
            this.json = normalizedJson;
            BlockInterpreter.interpretBlockContext(normalizedJson, this);
        }

        public Map<BlockType, JsonObject> getFunctions() {
            return functions;
        }


        public Map<BlockType, JsonObject> getPredicates() {
            return predicates;
        }

        public void setPredicates(Map<BlockType, JsonObject> predicates) {
            this.predicates = predicates;
        }

        public JsonObject getJson() {
            return json;
        }

        public ServerLevel getLevel() {
            return level;
        }

        public void setLevel(ServerLevel level) {
            this.level = level;
        }

        public BlockPos getPos() {
            return pos;
        }

        public void setPos(BlockPos pos) {
            this.pos = pos;
        }
    }

    /**
     * The {@code ContextUtils$StructureContext} class is a static nested class within {@link ContextUtils}.
     * It encapsulates the context and configuration needed for generating a structure within a Minecraft world.
     * This class holds various parameters such as the structure's name, dimensions, spawning configuration,
     * and other relevant details for structure generation. The context is typically loaded from a JSON file
     * and provides essential information for determining how a structure should be generated.
     */
    public static class StructureContext {
        public static final List<EntityType<?>> DEFAULT_SPAWNER_ENTITIES = List.of(EntityType.ZOMBIE, EntityType.CREEPER);
        private static final float DEFAULT_LADDER_CHANCE = 10.0f;
        private static final int DEFAULT_ROOM_COUNT = 15;
        private static final int DEFAULT_HEIGHT = 8;
        private static final int DEFAULT_WIDTH = 8;
        private static final int DEFAULT_LENGTH = 8;
        private static final int DEFAULT_SIZE_THRESHOLD = 30;
        private static final int DEFAULT_MAX_SPAWNERS_PER_ROOM = 1;
        private static final int DEFAULT_MAX_SPAWNERS = 10;

        private final int maxSpawnersPerRoom;
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

        private BlockPos startPos;
        private BlockContext blockContext;
        public JsonObject normalizedJson;

        public StructureContext(String structureName, float ladderRoomChance, int roomCount, int height, int width, int length, boolean generatesSpawners, int maxSpawners, int maxSpawnersPerRoom, List<EntityType<?>> potentialSpawns, int sizeThreshold) {
            this.maxSpawnersPerRoom = maxSpawnersPerRoom;
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

        public static StructureContext fromJson(JsonObject json, String jsonFilePath) {
            JsonObject normalizedJson = normalizeJson(json);

            String structureName = normalizedJson.has("name") ? normalizedJson.get("name").getAsString() :
                    DSHelperClass.deriveStructureNameFromPath(jsonFilePath, StructureLoader.STRUCTURE_DIR);
            if (!normalizedJson.has("name")) {
                DSHelperClass.logWarningMessageOnce("'name' is missing or null in [" + jsonFilePath + "]. Defaulting to '" + structureName + "'.");
            }
            float ladderChance = normalizedJson.has("ladder_chance") ? normalizedJson.get("ladder_chance").getAsFloat() : DSHelperClass.logDefault("'ladder_chance'", DEFAULT_LADDER_CHANCE, jsonFilePath);
            int roomCount = normalizedJson.has("rooms") ? normalizedJson.get("rooms").getAsInt() : DSHelperClass.logDefault("'rooms'", DEFAULT_ROOM_COUNT, jsonFilePath);
            int height = normalizedJson.has("height") ? normalizedJson.get("height").getAsInt() : DSHelperClass.logDefault("'height'", DEFAULT_HEIGHT, jsonFilePath);
            int width = normalizedJson.has("width") ? normalizedJson.get("width").getAsInt() : DSHelperClass.logDefault("'width'", DEFAULT_WIDTH, jsonFilePath);
            int length = normalizedJson.has("length") ? normalizedJson.get("length").getAsInt() : DSHelperClass.logDefault("'length'", DEFAULT_LENGTH, jsonFilePath);
            int sizeThreshold = normalizedJson.has("size_threshold") ? normalizedJson.get("size_threshold").getAsInt() : DSHelperClass.logDefault("'size_threshold'", DEFAULT_SIZE_THRESHOLD, jsonFilePath);

            boolean generateSpawners = false;
            int maxSpawners = DEFAULT_MAX_SPAWNERS;
            int maxSpawnersPerRoom = DEFAULT_MAX_SPAWNERS_PER_ROOM;
            List<EntityType<?>> spawnerEntities = DEFAULT_SPAWNER_ENTITIES;

            if (normalizedJson.has("spawners")) {
                JsonObject spawnersObject = normalizedJson.getAsJsonObject("spawners");

                maxSpawners = spawnersObject.has("max") ? spawnersObject.get("max").getAsInt() : DSHelperClass.logDefault("'max'", DEFAULT_MAX_SPAWNERS, jsonFilePath);
                maxSpawnersPerRoom = spawnersObject.has("room_count") ? spawnersObject.get("room_count").getAsInt() : DSHelperClass.logDefault("'room_count'", DEFAULT_MAX_SPAWNERS_PER_ROOM, jsonFilePath);

                if (spawnersObject.has("mobs")) {
                    spawnerEntities = new ArrayList<>();
                    JsonArray mobsArray = spawnersObject.getAsJsonArray("mobs");
                    for (JsonElement element : mobsArray) {
                        String entityName = element.getAsString();
                        EntityType.byString(entityName).ifPresent(spawnerEntities::add);
                    }
                } else {
                    DSHelperClass.logWarningMessageOnce("'mobs' are missing or null in " + jsonFilePath + ". Defaulting to " + DEFAULT_SPAWNER_ENTITIES + ".");
                }

                generateSpawners = true;
            }

            var structure = new StructureContext(
                    structureName,
                    ladderChance,
                    roomCount,
                    height,
                    width,
                    length,
                    generateSpawners,
                    maxSpawners,
                    maxSpawnersPerRoom,
                    spawnerEntities,
                    sizeThreshold
            );
            structure.normalizedJson = normalizedJson;
            return structure;
        }


        public int getMaxSpawnersPerRoom() {
            return maxSpawnersPerRoom;
        }

        public BlockContext getBlockContext() {
            return blockContext;
        }

        public void setBlockContext(BlockContext blockContext) {
            this.blockContext = blockContext;
        }


        /**
         * Gets the size threshold, which determines the percentage variation allowed in room size.
         *
         * @return The size threshold as a percentage.
         */
        public int getSizeThreshold() {
            return sizeThreshold;
        }

        /**
         * Gets a random cardinal direction (North, South, East, West) for the structure's orientation.
         *
         * @return A random {@link Direction} for the structure's orientation.
         */
        private Direction getRandomDirection() {
            Direction[] cardinalDirections = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
            return cardinalDirections[new Random().nextInt(cardinalDirections.length)];
        }

        /**
         * Gets the chance that a room will be a ladder room, expressed as a percentage.
         *
         * @return The ladder room chance as a float.
         */
        public float getLadderRoomChance() {
            return ladderRoomChance;
        }

        /**
         * Gets the initial direction for the structure's generation.
         *
         * @return The initial {@link Direction} for the structure.
         */
        public Direction getStartDirection() {
            return startDirection;
        }

        /**
         * Gets the starting position for the structure generation.
         *
         * @return The {@link BlockPos} representing the starting position.
         */
        public BlockPos getStartPos() {
            return startPos;
        }

        /**
         * Sets the starting position for the structure generation.
         *
         * @param startPos The {@link BlockPos} representing the new starting position.
         */
        public void setStartPos(BlockPos startPos) {
            this.startPos = startPos;
        }

        /**
         * Gets the name of the structure.
         *
         * @return The structure's name.
         */
        public String getStructureName() {
            return structureName;
        }

        /**
         * Gets the total number of rooms to generate in the structure.
         *
         * @return The room count.
         */
        public int getRoomCount() {
            return roomCount;
        }

        /**
         * Gets the height of each room in the structure.
         *
         * @return The room height.
         */
        public int getHeight() {
            return height;
        }

        /**
         * Gets the width of each room in the structure.
         *
         * @return The room width.
         */
        public int getWidth() {
            return width;
        }

        /**
         * Gets the length of each room in the structure.
         *
         * @return The room length.
         */
        public int getLength() {
            return length;
        }

        /**
         * Determines whether spawners should be generated in the structure.
         *
         * @return {@code true} if spawners should be generated; {@code false} otherwise.
         */
        public boolean isGeneratesSpawners() {
            return generatesSpawners;
        }

        /**
         * Gets the maximum number of spawners that can be placed in the structure.
         *
         * @return The maximum number of spawners.
         */
        public int getMaxSpawners() {
            return maxSpawners;
        }

        /**
         * Gets the list of potential entities that can spawn from the spawners.
         *
         * @return A list of {@link EntityType} representing potential spawn entities.
         */
        public List<EntityType<?>> getPotentialSpawns() {
            return potentialSpawns;
        }
    }

    /**
     * Represents the context and configuration for spawning a structure within a Minecraft world.
     * The {@code ContextUtils$SpawnContext} class contains parameters related to how and where a structure can be generated,
     * including its placement within a chunk, spacing, and separation from other structures.
     */
    public static class SpawnContext {
        private static final long DEFAULT_SALT = 1738452910L;
        private static final int DEFAULT_SEPARATION = 8;
        private static final int DEFAULT_SPACING = 34;
        private static final int DEFAULT_Y_MIN = -32;
        private static final int DEFAULT_Y_MAX = 0;
        private static final int DEFAULT_MAX_DISTANCE_FROM_CENTER = 35;
        private final int maxDistanceFromCenter;
        private final int yMin;
        private final int yMax;
        private final String name;
        private final long salt;
        private final int separation;
        private final int spacing;

        /**
         * Constructs a {@code ContextUtils$SpawnContext} with the specified parameters.
         *
         * @param name                  The name of the structure.
         * @param salt                  A unique salt value used to seed random generation for this structure.
         * @param separation            The minimum distance between this structure and another instance of the same structure.
         * @param spacing               The spacing between structures of the same type in chunks.
         * @param yMin                  The minimum Y-level where the structure can generate.
         * @param yMax                  The maximum Y-level where the structure can generate.
         * @param maxDistanceFromCenter The maximum distance from the center of the structure where components may be placed.
         */
        public SpawnContext(String name, long salt, int separation, int spacing, int yMin, int yMax, int maxDistanceFromCenter) {
            this.maxDistanceFromCenter = maxDistanceFromCenter;
            this.yMin = yMin;
            this.yMax = yMax;
            this.name = name;
            this.salt = salt;
            this.separation = separation;
            this.spacing = spacing;
        }

        /**
         * Creates a {@code ContextUtils$SpawnContext} from a JSON object. This method reads the structure's spawning configuration from the provided
         * JSON file, normalizes the JSON data, and extracts the necessary fields to create a new {@code SpawnContext} instance.
         *
         * @param json         The {@link JsonObject} containing the structure's spawning configuration.
         * @param jsonFilePath The file path of the JSON file, used for logging and deriving missing fields.
         * @return A new {@code ContextUtils.SpawnContext} instance initialized with the values from the JSON object.
         */
        public static SpawnContext fromJson(JsonObject json, String jsonFilePath) {
            JsonObject normalizedJson = normalizeJson(json);
            String structureName = normalizedJson.has("name") ? normalizedJson.get("name").getAsString() :
                    DSHelperClass.deriveStructureNameFromPath(jsonFilePath, StructureSetLoader.STRUCTURE_DIR);
            if (!normalizedJson.has("name")) {
                DSHelperClass.logWarningMessageOnce("'name' is missing or null in [" + jsonFilePath + "]. Defaulting to '" + structureName + "'.");
            }
            long salt = normalizedJson.has("salt") ? normalizedJson.get("salt").getAsLong() : DSHelperClass.logDefault("'salt'", DEFAULT_SALT, jsonFilePath);
            int separation = normalizedJson.has("separation") ? normalizedJson.get("separation").getAsInt() : DSHelperClass.logDefault("'separation'", DEFAULT_SEPARATION, jsonFilePath);
            int spacing = normalizedJson.has("spacing") ? normalizedJson.get("spacing").getAsInt() : DSHelperClass.logDefault("'spacing'", DEFAULT_SPACING, jsonFilePath);
            int yMin = normalizedJson.has("y_min") ? normalizedJson.get("y_min").getAsInt() : DSHelperClass.logDefault("'y_min'", DEFAULT_Y_MIN, jsonFilePath);
            int yMax = normalizedJson.has("y_max") ? normalizedJson.get("y_max").getAsInt() : DSHelperClass.logDefault("'y_max'", DEFAULT_Y_MAX, jsonFilePath);
            int maxDistanceFromCenter = normalizedJson.has("max_distance") ? normalizedJson.get("max_distance").getAsInt() : DSHelperClass.logDefault("'max_distance'", DEFAULT_MAX_DISTANCE_FROM_CENTER, jsonFilePath);
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

        /**
         * Gets the maximum distance from the center of the structure where components may be placed.
         *
         * @return The maximum distance from the center in blocks.
         */
        public int getMaxDistanceFromCenter() {
            return maxDistanceFromCenter;
        }

        /**
         * Gets the minimum Y-level where the structure can generate.
         *
         * @return The minimum Y-level.
         */
        public int getyMin() {
            return yMin;
        }

        /**
         * Gets the maximum Y-level where the structure can generate.
         *
         * @return The maximum Y-level.
         */
        public int getyMax() {
            return yMax;
        }

        /**
         * Gets the name of the structure.
         *
         * @return The name of the structure.
         */
        public String getName() {
            return name;
        }

        /**
         * Gets the salt value used for random generation of this structure.
         *
         * @return The salt value.
         */
        public long getSalt() {
            return salt;
        }

        /**
         * Gets the spacing between structures of the same type in chunks.
         *
         * @return The spacing between structures in chunks.
         */
        public int getSpacing() {
            return spacing;
        }

        /**
         * Gets the minimum separation distance between this structure and another instance of the same structure.
         *
         * @return The minimum separation distance in blocks.
         */
        public int getSeparation() {
            return separation;
        }
    }

}
