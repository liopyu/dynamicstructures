package net.liopyu.dynamicstructures.data.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.liopyu.dynamicstructures.data.StructureLoader;
import net.liopyu.dynamicstructures.data.enums.BlockType;
import net.liopyu.dynamicstructures.data.enums.KeyWordType;
import net.liopyu.dynamicstructures.data.enums.StructureType;
import net.liopyu.dynamicstructures.util.ContextUtils;
import net.liopyu.dynamicstructures.util.DSHelperClass;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

import static net.liopyu.dynamicstructures.util.DSHelperClass.normalizeJson;

public class BlockInterpreter {
    public static List<String> allowedBlockTypes = new ArrayList<>();

    public static BlockState handleBlockPlacement(BlockPos pos, BlockType blockType, BlockState pNewState, ContextUtils.BlockContext blockContext) {
        if (blockContext.getFunctions().isEmpty() || !blockContext.getFunctions().containsKey(blockType)) {
            return pNewState;
        }

        JsonObject functionObject = blockContext.getFunctions().get(blockType);
        if (functionObject.has("replace")) {
            JsonObject replaceObject = functionObject.getAsJsonObject("replace");
            if (replaceObject != null) {
                String fromBlockName = replaceObject.get("from").getAsString();
                String toBlockName = replaceObject.get("to").getAsString();

                Block fromBlock = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(fromBlockName));
                Block toBlock = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(toBlockName));

                if (fromBlock != null && toBlock != null) {
                    if (blockContext.getLevel().getBlockState(pos).getBlock() == fromBlock) {
                        return toBlock.defaultBlockState();
                    }
                } else {
                    DSHelperClass.logWarningMessageOnce("Failed replacement: Block registry objects not found for '"
                            + fromBlockName + "' or '" + toBlockName + "' at " + pos);
                }
            }
        }

        return pNewState;
    }

    public static boolean evaluateConditions(BlockPos blockPos, String blockName, BlockType blockType, ContextUtils.BlockContext context) {
        var currentBiome = (context.getLevel().getBiome(blockPos));
        var biomeKey = context.getLevel().registryAccess().registryOrThrow(ForgeRegistries.BIOMES.getRegistryKey());
        var biomeName = biomeKey.getKey(currentBiome.get()).toString();
        int currentHeight = blockPos.getY();

        boolean biomeConditionMet = true;
        boolean heightConditionMet = true;
        boolean existingBlockConditionMet = true;
        if (!context.getPredicates().containsKey(blockType)) return true;
        for (Map.Entry<BlockType, JsonObject> predicateObject : context.getPredicates().entrySet()) {
            if (predicateObject.getValue().has("biomes")) {
                JsonArray biomesArray = predicateObject.getValue().getAsJsonArray("biomes");
                biomeConditionMet = false;
                for (JsonElement biomeElement : biomesArray) {
                    if (biomeElement.getAsString().equalsIgnoreCase(biomeName.toLowerCase())) {
                        biomeConditionMet = true;
                        break;
                    } else if (biomeElement.getAsString().startsWith("#") &&
                            currentBiome.is(new ResourceLocation(biomeElement.getAsString().toLowerCase().replaceFirst("#", "")))) {
                        biomeConditionMet = true;
                        break;
                    }
                }
            }
            if (predicateObject.getValue().has("height")) {
                JsonObject heightObject = normalizeJson(predicateObject.getValue().getAsJsonObject("height"));
                int minHeight = heightObject.has("min") ? heightObject.get("min").getAsInt() : Integer.MIN_VALUE;
                int maxHeight = heightObject.has("max") ? heightObject.get("max").getAsInt() : Integer.MAX_VALUE;
                heightConditionMet = currentHeight >= minHeight && currentHeight <= maxHeight;
            }

            if (predicateObject.getValue().has("existing_block")) {
                String existingBlock = predicateObject.getValue().get("existing_block").getAsString();
                existingBlockConditionMet = existingBlock.equalsIgnoreCase(blockName);
            }
        }
        return biomeConditionMet && heightConditionMet && existingBlockConditionMet;
    }

    public static void interpretBlockContext(JsonObject normalizedJson, ContextUtils.BlockContext context) {
        for (Map.Entry<String, JsonElement> entry : normalizedJson.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (allowedBlockTypes.contains(key)) {
                JsonObject categoryObject = entry.getValue().getAsJsonObject();
                JsonArray blockArray = categoryObject.getAsJsonArray("blocks");

                if (key.equalsIgnoreCase("door")) {
                    JsonElement doorRadius = categoryObject.get("radius");
                    if (doorRadius != null) {
                        context.defaultDoorwayRadius = doorRadius.getAsInt();
                    }
                }
                if (blockArray != null) {
                    context.weightedBlocks = new ArrayList<>();
                    int totalWeight = 0;
                    if (!blockArray.isEmpty()) {
                        for (int i = 0; i < blockArray.size(); i++) {
                            JsonObject blockObject = blockArray.get(i).getAsJsonObject();
                            String name = blockObject.get("block").getAsString();
                            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(name));
                            if (block != null) {
                                int weight = blockObject.has("weight") ? blockObject.get("weight").getAsInt() : 1;
                                totalWeight += weight;
                                //DSHelperClass.logInfoMessageDev("adding weight " + blockObject);
                                context.weightedBlocks.add(new ContextUtils.WeightedBlock(block, weight, blockObject, BlockType.valueOf(entry.getKey().toUpperCase())));
                            } else {
                                DSHelperClass.logErrorMessage("Block '" + name + "' could not be found in the registry.");
                            }
                        }
                    }

                    int randomWeight = totalWeight > 0 ? new Random().nextInt(totalWeight) : 0;
                    ContextUtils.WeightedBlock selectedBlock = null;
                    for (ContextUtils.WeightedBlock weightedBlock : context.weightedBlocks) {
                        randomWeight -= weightedBlock.weight;
                        if (randomWeight < 0) {
                            selectedBlock = weightedBlock;
                            break;
                        }
                    }

                    if (selectedBlock != null) {
                        JsonObject blockObject = selectedBlock.blockObject;
                        ContextUtils.WeightedBlock finalSelectedBlock = selectedBlock;
                        var blockType = finalSelectedBlock.blockType;
                        if (key.equalsIgnoreCase(blockType.name())) {
                            //DSHelperClass.logInfoMessageDev("Selected '" + finalSelectedBlock.block + "' for [" + key + "] list.");
                            Arrays.stream(KeyWordType.values()).toList().forEach(keyword -> {
                                var keywordString = keyword.name().toLowerCase();
                                if (blockObject.getAsJsonObject(keywordString) != null) {
                                    JsonObject keywordObject = blockObject.getAsJsonObject(keyword.name().toLowerCase());
                                    switch (keywordString) {
                                        case "predicate":
                                            //DSHelperClass.logInfoMessageDev("Adding '" + keywordString + "' to [" + finalSelectedBlock.block + "] as predicate: " + blockType);
                                            context.getPredicates().put(blockType, normalizeJson(keywordObject));
                                            break;
                                        case "function":
                                            //DSHelperClass.logInfoMessageDev("Adding '" + keywordString + "' to [" + finalSelectedBlock.block + "] as function: " + blockType);
                                            context.getFunctions().put(blockType, normalizeJson(keywordObject));
                                            break;
                                    }
                                }
                            });
                            switch (blockType) {
                                case ROOF -> {
                                    context.roofBlock = finalSelectedBlock;
                                }
                                case WALLS -> {
                                    context.wallBlock = finalSelectedBlock;
                                }
                                case FLOOR -> {
                                    context.floorBlock = finalSelectedBlock;
                                }
                                case FILLER -> {
                                    context.fillerBlock = finalSelectedBlock;
                                }
                                case DOOR -> {
                                    context.doorwayBlock = finalSelectedBlock;
                                }
                                case CENTER -> {
                                    context.centerBlock = finalSelectedBlock;
                                }
                                case STAIRS -> {
                                    context.stairBlock = finalSelectedBlock;
                                }
                            }
                        }

                    }
                }
            }
        }
    }

    public static ContextUtils.StructureContext fromJson(JsonObject json, String jsonFilePath) {
        JsonObject normalizedJson = normalizeJson(json);

        String structureName = normalizedJson.has("name") ? normalizedJson.get("name").getAsString() :
                DSHelperClass.deriveStructureNameFromPath(jsonFilePath, StructureLoader.STRUCTURE_DIR);
        if (!normalizedJson.has("name")) {
            DSHelperClass.logWarningMessageOnce("'name' is missing or null in [" + jsonFilePath + "]. Defaulting to '" + structureName + "'.");
        }
        float ladderChance = normalizedJson.has("ladder_chance") ? normalizedJson.get("ladder_chance").getAsFloat() : DSHelperClass.logDefault("'ladder_chance'", ContextUtils.StructureContext.DEFAULT_LADDER_CHANCE, jsonFilePath);
        int roomCount = normalizedJson.has("rooms") ? normalizedJson.get("rooms").getAsInt() : DSHelperClass.logDefault("'rooms'", ContextUtils.StructureContext.DEFAULT_ROOM_COUNT, jsonFilePath);
        int height = normalizedJson.has("height") ? normalizedJson.get("height").getAsInt() : DSHelperClass.logDefault("'height'", ContextUtils.StructureContext.DEFAULT_HEIGHT, jsonFilePath);
        int width = normalizedJson.has("width") ? normalizedJson.get("width").getAsInt() : DSHelperClass.logDefault("'width'", ContextUtils.StructureContext.DEFAULT_WIDTH, jsonFilePath);
        int length = normalizedJson.has("length") ? normalizedJson.get("length").getAsInt() : DSHelperClass.logDefault("'length'", ContextUtils.StructureContext.DEFAULT_LENGTH, jsonFilePath);
        int sizeThreshold = normalizedJson.has("size_threshold") ? normalizedJson.get("size_threshold").getAsInt() : DSHelperClass.logDefault("'size_threshold'", ContextUtils.StructureContext.DEFAULT_SIZE_THRESHOLD, jsonFilePath);
        String type = normalizedJson.has("type") ? normalizedJson.get("type").getAsString() : DSHelperClass.logDefault("'type'", ContextUtils.StructureContext.DEFAULT_STRUCTURE_TYPE, jsonFilePath);

        boolean generateSpawners = false;
        int maxSpawners = ContextUtils.StructureContext.DEFAULT_MAX_SPAWNERS;
        int maxSpawnersPerRoom = ContextUtils.StructureContext.DEFAULT_MAX_SPAWNERS_PER_ROOM;
        List<EntityType<?>> spawnerEntities = ContextUtils.StructureContext.DEFAULT_SPAWNER_ENTITIES;

        if (normalizedJson.has("spawners")) {
            JsonObject spawnersObject = normalizedJson.getAsJsonObject("spawners");

            maxSpawners = spawnersObject.has("max") ? spawnersObject.get("max").getAsInt() : DSHelperClass.logDefault("'max'", ContextUtils.StructureContext.DEFAULT_MAX_SPAWNERS, jsonFilePath);
            maxSpawnersPerRoom = spawnersObject.has("room_count") ? spawnersObject.get("room_count").getAsInt() : DSHelperClass.logDefault("'room_count'", ContextUtils.StructureContext.DEFAULT_MAX_SPAWNERS_PER_ROOM, jsonFilePath);

            if (spawnersObject.has("mobs")) {
                spawnerEntities = new ArrayList<>();
                JsonArray mobsArray = spawnersObject.getAsJsonArray("mobs");
                for (JsonElement element : mobsArray) {
                    String entityName = element.getAsString();
                    EntityType.byString(entityName).ifPresent(spawnerEntities::add);
                }
            } else {
                DSHelperClass.logWarningMessageOnce("'mobs' are missing or null in " + jsonFilePath + ". Defaulting to " + ContextUtils.StructureContext.DEFAULT_SPAWNER_ENTITIES + ".");
            }

            generateSpawners = true;
        }

        var structure = new ContextUtils.StructureContext(
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
        structure.setStructureType(StructureType.valueOf(type.toUpperCase()));
        return structure;
    }
}
