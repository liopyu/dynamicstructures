package net.liopyu.dynamicstructures.data.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.liopyu.dynamicstructures.data.enums.BlockType;
import net.liopyu.dynamicstructures.util.ContextUtils;
import net.liopyu.dynamicstructures.util.DSHelperClass;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

import static net.liopyu.dynamicstructures.util.DSHelperClass.normalizeJson;

public class BlockInterpreter {
    public static List<String> allowedBlockTypes = new ArrayList<>();
    public static List<String> allowedKeywords = new ArrayList<>();


    public static boolean evaluateConditions(BlockPos blockPos, String blockName, BlockType blockType, ContextUtils.BlockContext context) {
        var currentBiome = (context.getLevel().getBiome(blockPos)).get();
        var biomeKey = context.getLevel().registryAccess().registryOrThrow(ForgeRegistries.BIOMES.getRegistryKey());
        var biomeName = biomeKey.getKey(currentBiome).toString();
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


}
