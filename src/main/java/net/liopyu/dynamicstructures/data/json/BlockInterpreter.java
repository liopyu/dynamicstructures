package net.liopyu.dynamicstructures.data.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.liopyu.dynamicstructures.util.ContextUtils;
import net.liopyu.dynamicstructures.util.DSHelperClass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

import static net.liopyu.dynamicstructures.util.DSHelperClass.normalizeJson;

public class BlockInterpreter {
    public static List<String> allowedBlockTypes = new ArrayList<>();
    public static List<String> allowedKeywords = new ArrayList<>();


    public static boolean evaluateConditions(String blockName, BlockType blockType, ContextUtils.BlockContext context) {
        var currentBiome = (context.getLevel().getBiome(context.getPos())).get();
        var biomeKey = context.getLevel().registryAccess().registryOrThrow(ForgeRegistries.BIOMES.getRegistryKey());
        var biomeName = biomeKey.getKey(currentBiome).toString();
        int currentHeight = context.getPos().getY();
        String currentBlock = ForgeRegistries.BLOCKS.getKey(context.getLevel().getBlockState(context.getPos()).getBlock()).toString();

        boolean biomeConditionMet = true;
        boolean heightConditionMet = true;
        boolean existingBlockConditionMet = true;
        String parsedBlock = blockType.name().toLowerCase() + blockName;
        if (!context.getPredicates().containsKey(parsedBlock)) return true;
        for (Map.Entry<String, JsonObject> predicateObject : context.getPredicates().entrySet()) {
            String splitString = predicateObject.getKey().split(",")[0];
            DSHelperClass.logInfoMessageDev("Split string: " + splitString);
            if (BlockType.valueOf(splitString) + blockName == parsedBlock) {
                DSHelperClass.logInfoMessageDev("parsedBlock: " + parsedBlock);
                DSHelperClass.logInfoMessageDev("First Block Type string: " + BlockType.valueOf(splitString) + blockName);
                if (predicateObject.getValue().has("biomes")) {
                    JsonArray biomesArray = predicateObject.getValue().getAsJsonArray("biomes");
                    biomeConditionMet = false;
                    for (JsonElement biomeElement : biomesArray) {
                        if (biomeElement.getAsString().equalsIgnoreCase(biomeName.toLowerCase())) {
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
                    existingBlockConditionMet = existingBlock.equalsIgnoreCase(currentBlock);
                }
            }
            return biomeConditionMet && heightConditionMet && existingBlockConditionMet;
        }
        DSHelperClass.logWarningMessageOnce("No blockType found for : " + blockType);
        return true;
    }

    public static List<String> getBlocksToPlace(List<BlockEntry> blockEntries) {
        List<String> blocksToPlace = new ArrayList<>();
        for (BlockEntry entry : blockEntries) {
            if (entry.shouldPlace()) {
                blocksToPlace.add(entry.getName());
            }
        }
        return blocksToPlace;
    }

    public static List<Block> getBlockList(List<String> blocksToPlace) {
        List<Block> blocks = new ArrayList<>();
        for (String name : blocksToPlace) {
            blocks.add(ForgeRegistries.BLOCKS.getValue(ResourceLocation.tryParse(name)));
        }
        return blocks;
    }

    public static void main(String[] args) {
        /*try {
            List<BlockEntry> blocks = parseJson("run/config/dynamicstructures/structures/test.json");
            List<String> blocksToPlace = getBlocksToPlace(blocks);
            System.out.println("Blocks to place: " + blocksToPlace);
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }


    public static class BlockEntry {
        private final String name;
        private final boolean shouldPlace;
        private final BlockType type;

        public BlockEntry(String name, boolean shouldPlace, String type) {
            this.name = name;
            this.shouldPlace = shouldPlace;
            this.type = BlockType.valueOf(type);
        }

        public String getName() {
            return name;
        }

        public boolean shouldPlace() {
            return shouldPlace;
        }

        public BlockType getType() {
            return type;
        }
    }
}
