package net.liopyu.dynamicstructures.events.server;

import net.liopyu.dynamicstructures.DynamicStructures;
import net.liopyu.dynamicstructures.data.StructureLoader;
import net.liopyu.dynamicstructures.data.StructureSetLoader;
import net.liopyu.dynamicstructures.structures.DungeonGenerator;
import net.liopyu.dynamicstructures.util.ContextUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

import static net.liopyu.dynamicstructures.DynamicStructures.LOGGER;
import static net.liopyu.dynamicstructures.DynamicStructures.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerForgeEvents {
    @SubscribeEvent
    public static void onChunkLoad(ChunkDataEvent.Load event) {
        if (event.getChunk() instanceof ProtoChunk protoChunk) {
            if (protoChunk.getStatus() == ChunkStatus.FULL) {
                if (event.getLevel() instanceof ServerLevel) {
                    performFirstTimeLoadAction(protoChunk, (ServerLevel) event.getLevel(),event.getData());
                }
            }
        }
    }

    private static void performFirstTimeLoadAction(ProtoChunk chunk, ServerLevel level, CompoundTag chunkData) {
        CompoundTag forgeData = chunkData.getCompound("ForgeData");
        if (forgeData.getBoolean("StructuresGenerated")) {
            return;
        }

        List<ContextUtils.SpawnContext> spawnContexts = StructureSetLoader.loadStructureSets(level, chunk);
        List<ContextUtils.StructureContext> structures = StructureLoader.loadStructures(level, chunk.getPos().getWorldPosition());

        // Iterate through all structures and process them based on SpawnContext
        for (ContextUtils.StructureContext structureContext : structures) {
            ContextUtils.SpawnContext spawnContext = findSpawnContextForStructure(spawnContexts, structureContext.getStructureName());

            if (spawnContext != null) {
                if (shouldGenerateStructure(spawnContext, chunk.getPos())) {
                    DungeonGenerator.generateDungeon(structureContext);
                }
            }
        }

        // Mark the chunk as processed
        forgeData.putBoolean("StructuresGenerated", true);
    }

    private static ContextUtils.SpawnContext findSpawnContextForStructure(List<ContextUtils.SpawnContext> spawnContexts, String structureName) {
        for (ContextUtils.SpawnContext context : spawnContexts) {
            if (structureName.equals(context.getName())) {
                return context;
            }
        }
        return null;
    }


    private static boolean shouldGenerateStructure(ContextUtils.SpawnContext spawnContext, ChunkPos chunkPos) {
        ServerLevel level = spawnContext.getLevel();
        long seed = level.getSeed();
        long salt = spawnContext.getSalt();

        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
        random.setLargeFeatureWithSalt(seed, chunkPos.x, chunkPos.z, (int) salt);
        ChunkPos targetPos = new ChunkPos(random.nextInt(4096), random.nextInt(4096));

        int separation = spawnContext.getSeparation();
        int spacing = spawnContext.getSpacing();

        return chunkPos.equals(targetPos) ||
                isFarEnoughFromOtherStructures(targetPos, chunkPos, separation, spacing);
    }

    private static boolean isFarEnoughFromOtherStructures(ChunkPos targetPos, ChunkPos currentPos, int separation, int spacing) {
        int distance = targetPos.getChessboardDistance(currentPos);
        return distance >= separation && distance <= spacing;
    }

    public static List<ChunkPos> generateStructureChunks(ServerLevel level, long salt, int separation, int spacing, int maxChunks) {
        List<ChunkPos> chunkPositions = new ArrayList<>();
        long seed = level.getSeed();
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));

        // Define the range of chunks to consider (for example, within 10,000 blocks around the origin)
        int range = 10000 / 16; // convert to chunk coordinates

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                ChunkPos chunkPos = new ChunkPos(x, z);

                // Set random seed based on seed and salt
                random.setLargeFeatureWithSalt(seed, chunkPos.x, chunkPos.z, (int) salt);

                // Calculate if the chunk should have the structure
                if (random.nextInt(spacing) < separation) {
                    // Add to the list if the conditions are met
                    chunkPositions.add(chunkPos);

                    // Stop if we've reached the max number of chunks
                    if (chunkPositions.size() >= maxChunks) {
                        return chunkPositions;
                    }
                }
            }
        }

        return chunkPositions;
    }

    @SubscribeEvent
    public static void onRightClicked(PlayerInteractEvent.RightClickItem event) {
        doRightClick(event);
    }
    public static void doRightClick(PlayerInteractEvent.RightClickItem event) {
        var blockPos = event.getPos();
        if (!event.getLevel().isClientSide()){
            var serverLevel = (ServerLevel) event.getLevel();
            var chunkPos= generateStructureChunks(serverLevel,1738452910,8,34,100);
            for (ChunkPos pos : chunkPos){
            LOGGER.info("Potential structure: " + pos);
            }
            /*List<ContextUtils.StructureContext> structures = StructureLoader.loadStructures(serverLevel,blockPos);
            // Optionally find a specific structure
            for (ContextUtils.StructureContext context : structures) {
                if ("test".equals(context.getStructureName())) {
                    DungeonGenerator.generateDungeon(context);
                    break;
                }
            }*/
        }
    }
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        // You can access the structure context here if needed
    }
}
