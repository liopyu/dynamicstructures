package net.liopyu.dynamicstructures.events.server;

import net.liopyu.dynamicstructures.DynamicStructures;
import net.liopyu.dynamicstructures.data.StructureLoader;
import net.liopyu.dynamicstructures.data.StructureSetLoader;
import net.liopyu.dynamicstructures.structures.DungeonGenerator;
import net.liopyu.dynamicstructures.util.ContextUtils;
import net.liopyu.dynamicstructures.util.DSHelperClass;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.*;

import static net.liopyu.dynamicstructures.DynamicStructures.LOGGER;
import static net.liopyu.dynamicstructures.DynamicStructures.MODID;
import static net.liopyu.dynamicstructures.util.ContextUtils.StructureContext.DEFAULT_SPAWNER_ENTITIES;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerForgeEvents {
public static boolean loaded = false;
    private static final ExecutorService executor = Executors.newFixedThreadPool(10); // Custom executor for background tasks

    @SubscribeEvent
    public static void onChunkInit(ChunkEvent.Load event) {
        LevelAccessor levelAccessor = event.getLevel();
        //DSHelperClass.logInfoMessage(event.getChunk().getStatus().toString());
        if (!event.isNewChunk()) return;
        if (levelAccessor instanceof Level level) {
            // Get the dimension key
            ResourceKey<Level> dimensionKey = level.dimension();
            // Get the MinecraftServer instance
            MinecraftServer server = level.getServer();
            if (server != null) {
                // Get the ServerLevel for the dimension
                ServerLevel serverLevel = server.getLevel(dimensionKey);
                BlockPos pos = event.getChunk().getPos().getWorldPosition();
                if (serverLevel != null /*&&!loaded*/) {
                    loaded = true;
                    /*if (scheduler.isShutdown()) {
                        scheduler.scheduleWithFixedDelay(() -> {
                            if (serverLevel.isLoaded(pos)) {  // Check if the block position is loaded
                                DSHelperClass.logWarningMessageOnce(pos.toString());
                                performFirstTimeLoadAction(event.getChunk(), serverLevel);
                            *//*DungeonGenerator.generateDungeon(new ContextUtils.StructureContext(
                                    "test",
                                    serverLevel,
                                    pos,
                                    10,
                                    1,
                                    10,
                                    10,
                                    10,
                                    false,
                                    1,
                                    DEFAULT_SPAWNER_ENTITIES,
                                    10
                            ));*//*
                                DSHelperClass.logInfoMessage("Dungeon generation task executed.");
                                // Consider stopping or removing this task now that it has completed.
                                scheduler.shutdown();  // Only shut down if this is the only task scheduled.
                            } else {
                                System.out.println("Position not loaded yet, retrying...");
                            }
                        }, 0, 1, TimeUnit.SECONDS);

                    }*/
                    performIfLoaded(serverLevel, pos);/*.thenAccept(result -> {
                        serverLevel.getServer().execute(() -> {

                            System.out.println("Operation completed for chunk at " + pos);
                        });
                    });*/
                    //
                } else {
                    //DSHelperClass.logInfoMessage("Warning: Unable to retrieve ServerLevel for dimension: " + dimensionKey.location());
                }
            } else {
                DSHelperClass.logInfoMessage("Warning: Unable to retrieve MinecraftServer instance.");
            }
        } else {
            DSHelperClass.logInfoMessage("Warning: LevelAccessor is not an instance of Level.");
        }
    }
    public static CompletableFuture<Void> performIfLoaded(ServerLevel serverLevel, BlockPos pos) {
        return CompletableFuture.supplyAsync(() -> {
                    // Asynchronous check that doesn't interact with the world state directly
                    return serverLevel.isLoaded(pos);
                }, executor) // Run on a custom executor
                .thenAcceptAsync(isLoaded -> {
                    if (isLoaded) {
                        // Schedule the world-modifying task to run on the server thread
                        serverLevel.getServer().execute(() -> {
                            DungeonGenerator.generateDungeon(new ContextUtils.StructureContext(
                                    "test",
                                    serverLevel,
                                    pos,
                                    10,
                                    1,
                                    10,
                                    10,
                                    10,
                                    false,
                                    1,
                                    DEFAULT_SPAWNER_ENTITIES,
                                    10
                            ));
                        });
                    }
                }, serverLevel.getServer()); // Use the server's executor to handle world interaction safely
    }

    private static void performFirstTimeLoadAction(ChunkAccess chunk, ServerLevel level) {

        int yMin = 80;  // example minimum Y value
        int yMax = 100;  // example maximum Y value

        // Create a random number generator
        Random random = new Random();

        // Generate a random Y value between yMin and yMax
        int randomY = yMin + random.nextInt(yMax - yMin + 1);

        // Get the world position with the random Y offset
        BlockPos structurePos = chunk.getPos().getWorldPosition().offset(0, randomY, 0);
        List<ContextUtils.SpawnContext> spawnContexts = StructureSetLoader.loadStructures();
        List<ContextUtils.StructureContext> structures = StructureLoader.loadStructures(level, structurePos);

        for (ContextUtils.StructureContext structureContext : structures) {
            ContextUtils.SpawnContext spawnContext = findSpawnContextForStructure(spawnContexts, structureContext.getStructureName());
            if (spawnContext != null) {
                if (shouldGenerateStructure(spawnContext, chunk.getPos(),level)) {
                    DSHelperClass.logInfoMessage("Spawning structure: " + chunk.getPos());
                    DungeonGenerator.generateDungeon(structureContext);
                }
            }else DSHelperClass.logInfoMessage("Spawning Context is null: " + chunk.getPos());
        }
    }

    private static ContextUtils.SpawnContext findSpawnContextForStructure(List<ContextUtils.SpawnContext> spawnContexts, String structureName) {
        String structureNameFromPath = DSHelperClass.deriveStructureNameFromPath(structureName);

        for (ContextUtils.SpawnContext context : spawnContexts) {
            String structureNameFromPath1 = DSHelperClass.deriveStructureNameFromPath(context.getName());
            //DSHelperClass.logInfoMessage("Searching for spawn context: " + structureName + " :"+ structureNameFromPath1 + " :" + structureNameFromPath);
            if (structureNameFromPath.equals(structureNameFromPath1)) {
                return context;
            }
        }
        return null;
    }

    private static boolean shouldGenerateStructure(ContextUtils.SpawnContext spawnContext, ChunkPos chunkPos,ServerLevel level) {
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

        // Define the range to search for chunks
        int range = 10000 / 16; // Adjust range as needed for your world size

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                ChunkPos chunkPos = new ChunkPos(x, z);

                // Set random seed based on seed, salt, and chunk coordinates
                random.setLargeFeatureWithSalt(seed, chunkPos.x, chunkPos.z, (int) salt);

                // Calculate whether to add this chunk based on spacing/separation criteria
                if (random.nextInt(spacing) < separation) {
                    chunkPositions.add(chunkPos);

                    // Stop when we reach the maximum number of chunks
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
