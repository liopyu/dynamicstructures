package net.liopyu.dynamicstructures.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static net.liopyu.dynamicstructures.util.DSHelperClass.normalizeJson;

public class BlockPredicate implements Predicate<ContextUtils.BlockContext> {
    @Override
    public boolean test(ContextUtils.BlockContext context) {
        String currentBiome = context.getLevel().getBiome(context.getPos()).get().toString();
        int currentHeight = context.getPos().getY();
        String currentBlock = ForgeRegistries.BLOCKS.getKey(context.getLevel().getBlockState(context.getPos()).getBlock()).toString();
        boolean biomeConditionMet = true;
        boolean heightConditionMet = true;
        boolean existingBlockConditionMet = true;
        for (JsonObject predicateObject : context.getPredicates()) {
            if (predicateObject.has("biomes")) {
                JsonArray biomesArray = predicateObject.getAsJsonArray("biomes");
                biomeConditionMet = false;
                for (JsonElement biomeElement : biomesArray) {
                    if (biomeElement.getAsString().equals(currentBiome)) {
                        biomeConditionMet = true;
                        break;
                    }
                }
            }
            if (predicateObject.has("height")) {
                JsonObject heightObject = normalizeJson(predicateObject.getAsJsonObject("height"));
                System.out.println("height: " + heightObject);
                int minHeight = heightObject.has("min") ? heightObject.get("min").getAsInt() : Integer.MIN_VALUE;
                int maxHeight = heightObject.has("max") ? heightObject.get("max").getAsInt() : Integer.MAX_VALUE;
                heightConditionMet = currentHeight >= minHeight && currentHeight <= maxHeight;
            }

            if (predicateObject.has("existing_block")) {
                String existingBlock = predicateObject.get("existing_block").getAsString();
                existingBlockConditionMet = existingBlock.equals(currentBlock);
            }
        }
        return biomeConditionMet && heightConditionMet && existingBlockConditionMet;
    }
}
