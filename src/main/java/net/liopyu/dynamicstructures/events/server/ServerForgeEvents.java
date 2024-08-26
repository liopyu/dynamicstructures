package net.liopyu.dynamicstructures.events.server;

import net.liopyu.dynamicstructures.data.StructureLoader;
import net.liopyu.dynamicstructures.data.StructureSetLoader;
import net.liopyu.dynamicstructures.structures.DungeonGenerator;
import net.liopyu.dynamicstructures.util.ContextUtils;
import net.liopyu.dynamicstructures.util.DSHelperClass;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static net.liopyu.dynamicstructures.DynamicStructures.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerForgeEvents {
    @SubscribeEvent
    public static void onChunkInit(ChunkEvent.Load event) {
        LevelAccessor levelAccessor = event.getLevel();
        if (!event.isNewChunk()) return;
        if (levelAccessor instanceof Level level) {
            ResourceKey<Level> dimensionKey = level.dimension();
            MinecraftServer server = level.getServer();
            if (server != null) {
                ServerLevel serverLevel = server.getLevel(dimensionKey);
                BlockPos pos = event.getChunk().getPos().getWorldPosition();
                if (serverLevel != null) {

                    performIfLoaded(serverLevel, pos);
                } else {
                    DSHelperClass.logInfoMessage("Warning: Unable to retrieve ServerLevel for dimension: " + dimensionKey.location());
                }
            } else {
                DSHelperClass.logInfoMessage("Warning: Unable to retrieve MinecraftServer instance.");
            }
        } else {
            DSHelperClass.logInfoMessage("Warning: LevelAccessor is not an instance of Level.");
        }
    }
    /**
     * Asynchronously verifies if a {@link BlockPos} within a {@link ServerLevel} is loaded,
     * executing a subsequent action on the server's main thread if true. This method enhances game performance
     * by offloading the position check from the main game loop. Similar to other asynchronous operations in
     * Minecraft such as {@link net.minecraft.world.level.chunk.ChunkStatus#generate}
     * and {@link net.minecraft.world.level.chunk.ChunkStatus#load}, which manage chunk generation
     * and loading, this method queues actions on the game state to the main thread, aligning with Vanilla Minecraft's
     * best practices for concurrency and performance.
     *
     * @param serverLevel The server level where the operation is to be performed.
     * @param pos The world position to check.
     */
    public static CompletableFuture<Void> performIfLoaded(ServerLevel serverLevel, BlockPos pos) {
        MinecraftServer server = serverLevel.getServer();

        return CompletableFuture.supplyAsync(() -> {
            DSHelperClass.logInfoMessage("Attempting to perform operation");
                    Random random = new Random();
                    List<ContextUtils.SpawnContext> spawnContexts = StructureSetLoader.loadStructures();
                    List<ContextUtils.StructureContext> structures = StructureLoader.loadStructures(serverLevel, serverLevel.getChunk(pos).getPos().getWorldPosition());
                    for (ContextUtils.StructureContext structureContext : structures) {
                        ContextUtils.SpawnContext spawnContext = findSpawnContextForStructure(spawnContexts, structureContext.getStructureName());
                        if (spawnContext != null) {
                            int yMin = spawnContext.getyMin();
                            int yMax = spawnContext.getyMax();
                            int randomY = yMin + random.nextInt(yMax - yMin + 1);
                            BlockPos structurePos = serverLevel.getChunk(pos).getPos().getWorldPosition().offset(0, randomY, 0);
                            structureContext.setStartPos(structurePos);
                            if (shouldGenerateStructure(structureContext, spawnContext, serverLevel.getChunk(pos).getPos(), serverLevel, structurePos)) {
                                return true;
                            }
                        } else {
                            DSHelperClass.logInfoMessage("Spawning Context is null: " + serverLevel.getChunk(pos).getPos());
                            return false;
                        }
                    }
                    return false;
                }, server)  // Use Minecraft's server executor
                .thenAcceptAsync(isLoaded -> {
                    if (isLoaded) {
                        DSHelperClass.logInfoMessage("Attempting to perform operation2");
                        server.execute(() -> {
                            performFirstTimeLoadAction(serverLevel.getChunk(pos), serverLevel);
                        });
                    }
                }, server);
    }

    private static void performFirstTimeLoadAction(ChunkAccess chunk, ServerLevel level) {
        List<ContextUtils.SpawnContext> spawnContexts = StructureSetLoader.loadStructures();
        List<ContextUtils.StructureContext> structures = StructureLoader.loadStructures(level, chunk.getPos().getWorldPosition());
        for (ContextUtils.StructureContext structureContext : structures) {
            ContextUtils.SpawnContext spawnContext = findSpawnContextForStructure(spawnContexts, structureContext.getStructureName());
            if (spawnContext != null) {
                    DSHelperClass.logInfoMessage("Spawning structure: " + chunk.getPos());
                    DungeonGenerator.generateDungeon(structureContext);
            } else {
                DSHelperClass.logInfoMessage("Spawning Context is null: " + chunk.getPos());
            }
        }
    }


    private static ContextUtils.SpawnContext findSpawnContextForStructure(List<ContextUtils.SpawnContext> spawnContexts, String structureName) {
        String structureNameFromPath = DSHelperClass.deriveStructureNameFromPath(structureName,StructureLoader.STRUCTURE_DIR);
        for (ContextUtils.SpawnContext context : spawnContexts) {
            String structureNameFromPath1 = DSHelperClass.deriveStructureNameFromPath(context.getName(),StructureSetLoader.STRUCTURE_DIR);
            if (structureNameFromPath.equals(structureNameFromPath1)) {
                return context;
            }
        }
        return null;
    }
    public static int getMaxSize(int baseSize, int sizeThreshold) {
        int variation = (int) (baseSize * (sizeThreshold / 100.0));
        return baseSize + variation;
    }
    public static boolean shouldGenerateStructure(ContextUtils.StructureContext structureContext, ContextUtils.SpawnContext spawnContext, ChunkPos chunkPos, ServerLevel level, BlockPos pos) {
        int width = getMaxSize(structureContext.getWidth(), structureContext.getSizeThreshold()) + 5;
        int length = getMaxSize(structureContext.getHeight(), structureContext.getSizeThreshold()) + 5;
        int height = structureContext.getHeight() + 5;
        long seed = level.getSeed();
        long salt = spawnContext.getSalt();
        int separation = spawnContext.getSeparation();
        int spacing = spawnContext.getSpacing();
        int maxDistanceFromCenter = spawnContext.getMaxDistanceFromCenter();
        long structureSeed = computeStructureSeed(seed, salt, chunkPos.x, chunkPos.z);

        int regionX = (chunkPos.x < 0) ? (chunkPos.x - spacing + 1) / spacing : chunkPos.x / spacing;
        int regionZ = (chunkPos.z < 0) ? (chunkPos.z - spacing + 1) / spacing : chunkPos.z / spacing;

        Random random = new Random(structureSeed);
        int originX = regionX * spacing + random.nextInt(spacing);
        int originZ = regionZ * spacing + random.nextInt(spacing);

        // Calculate the furthest points from the center based on the structure dimensions and maxDistanceFromCenter
        BlockPos centerPos = new BlockPos(originX, pos.getY(), originZ);
        BlockPos furthestPos = centerPos.offset(width / 2 + maxDistanceFromCenter, height, length / 2 + maxDistanceFromCenter);

        // Check if all relevant blocks from the structure's bounding box are loaded
        if (!level.isLoaded(furthestPos)) {
            return false; // Do not generate the structure if the furthest block is not loaded
        }

        // Verify that the chunk coordinates match the intended origin and that the surrounding area is clear
        int minX = originX - separation;
        int maxX = originX + separation;
        int minZ = originZ - separation;
        int maxZ = originZ + separation;

        if (chunkPos.x == originX && chunkPos.z == originZ) {
            return true; // This chunk is the origin for the structure
        } else if (chunkPos.x >= minX && chunkPos.x <= maxX && chunkPos.z >= minZ && chunkPos.z <= maxZ) {
            return false; // This chunk is within the separation bounds but is not the origin
        }

        return false; // Default to not generating if all checks fail
    }
    public static List<ChunkPos> findPotentialStructurePositions(ContextUtils.SpawnContext spawnContext, ChunkPos centerChunk, ServerLevel level, int limit) {
        long seed = level.getSeed();
        long salt = spawnContext.getSalt();
        int separation = spawnContext.getSeparation();
        int spacing = spawnContext.getSpacing();
        List<ChunkPos> positions = new ArrayList<>();

        int gridSize = (int)Math.ceil(Math.sqrt(limit));

        int regionCenterX = centerChunk.x / spacing;
        int regionCenterZ = centerChunk.z / spacing;

        for (int dx = -gridSize; dx <= gridSize; dx++) {
            for (int dz = -gridSize; dz <= gridSize; dz++) {
                int regionX = regionCenterX + dx;
                int regionZ = regionCenterZ + dz;

                long structureSeed = computeStructureSeed(seed, salt, regionX, regionZ);
                Random random = new Random(structureSeed);

                int originX = regionX * spacing + random.nextInt(spacing);
                int originZ = regionZ * spacing + random.nextInt(spacing);

                ChunkPos originChunkPos = new ChunkPos(originX, originZ);
                if (!positions.contains(originChunkPos)) {
                    positions.add(originChunkPos);
                    if (positions.size() >= limit) {
                        return positions;
                    }
                }
            }
        }

        return positions;
    }
    private static long computeStructureSeed(long worldSeed, long salt, int chunkX, int chunkZ) {
        return worldSeed + salt + chunkX * 2345803L + chunkZ * 9236449L + (long) chunkX * chunkZ * 223;
    }

    @SubscribeEvent
    public static void onRightClicked(PlayerInteractEvent.RightClickItem event) {
        doRightClick(event);
    }
    public static void doRightClick(PlayerInteractEvent.RightClickItem event) {
        var blockPos = event.getPos();
        if (!event.getLevel().isClientSide()){
            var serverLevel = (ServerLevel) event.getLevel();
            /*List<ContextUtils.StructureContext> structures = StructureLoader.loadStructures(serverLevel,blockPos);
            // Optionally find a specific structure
            for (ContextUtils.StructureContext context : structures) {
                if ("test".equals(context.getStructureName())) {
                    *//*DungeonGenerator.generateDungeon(context);*//*
                    break;
                }
            }
            List<ContextUtils.SpawnContext> structuresSets = StructureSetLoader.loadStructures();
            // Optionally find a specific structure
            for (ContextUtils.SpawnContext context : structuresSets) {
                if ("test".equals(context.getName())) {
                    findPotentialStructurePositions(context,serverLevel.getChunk(blockPos).getPos(),serverLevel,100);
                    *//*DungeonGenerator.generateDungeon(context);*//*
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
