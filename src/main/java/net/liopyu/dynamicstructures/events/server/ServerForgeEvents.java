package net.liopyu.dynamicstructures.events.server;

import com.mojang.brigadier.CommandDispatcher;
import net.liopyu.dynamicstructures.commands.FindStructureCommand;
import net.liopyu.dynamicstructures.data.StructureLoader;
import net.liopyu.dynamicstructures.data.StructureSetLoader;
import net.liopyu.dynamicstructures.structures.Dungeon;
import net.liopyu.dynamicstructures.util.ContextUtils;
import net.liopyu.dynamicstructures.util.DSHelperClass;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static net.liopyu.dynamicstructures.DynamicStructures.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerForgeEvents {
    /**
     * Handles chunk initialization when a chunk is loaded, specifically for newly generated chunks.
     * Subscribed to {@link ChunkEvent.Load}, this method dynamically triggers custom structure generation
     * by leveraging {@link ServerForgeEvents#performIfLoaded(ServerLevel, BlockPos, Runnable)}. This allows for the addition or
     * modification of structures based on runtime conditions, mimicking Minecraft's structure set logic.
     * <p>
     * The method checks if the chunk is new, retrieves the relevant {@link ServerLevel}, and then initiates
     * structure generation if applicable. Utility methods like {@link #performFirstTimeLoadActionBoolean(BlockPos, ServerLevel)}
     * and {@link #shouldGenerateStructure(ContextUtils.StructureContext, ContextUtils.SpawnContext, ChunkPos, ServerLevel, BlockPos)}
     * assist in determining whether and how structures should be generated within the chunk.
     *
     * @param event The {@link ChunkEvent.Load} event providing context about the loaded chunk.
     * @see ChunkEvent.Load
     * @see #performIfLoaded(ServerLevel, BlockPos, Runnable)
     * @see net.minecraft.world.level.levelgen.structure.StructureSet
     */
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
                    performIfLoaded(serverLevel, pos, () -> {
                        performFirstTimeLoadAction(serverLevel.getChunk(pos), serverLevel);
                    });
                } else {
                    DSHelperClass.logWarningMessageOnce("Warning: Unable to retrieve ServerLevel for dimension: " + dimensionKey.location());
                }
            } else {
                DSHelperClass.logWarningMessageOnce("Warning: Unable to retrieve MinecraftServer instance.");
            }
        } else {
            DSHelperClass.logWarningMessageOnce("Warning: LevelAccessor is not an instance of Level.");
        }
    }

    /**
     * Asynchronously checks if a {@link BlockPos} within a {@link ServerLevel} is loaded, and if so,
     * executes the provided {@link Runnable} on the server's main thread. This method enhances game
     * performance by offloading the position check from the main game loop, similar to other asynchronous
     * operations in Minecraft.
     *
     * <p>If the specified position is determined to be loaded, the provided {@link Runnable} is executed
     * on the server's main thread, ensuring that any game state modifications occur safely within the
     * appropriate context.</p>
     *
     * @param serverLevel The server level where the operation is to be performed.
     * @param pos         The world position to check.
     * @param runnable    The action to execute if the position is loaded.
     * @see #performFirstTimeLoadActionBoolean(BlockPos, ServerLevel)
     */
    public static void performIfLoaded(ServerLevel serverLevel, BlockPos pos, Runnable runnable) {
        CompletableFuture.supplyAsync(() -> performFirstTimeLoadActionBoolean(pos, serverLevel))
                .thenAcceptAsync(isLoaded -> {
                    if (isLoaded) {
                        serverLevel.getServer().execute(runnable);
                    }
                }, serverLevel.getServer());
    }

    /**
     * Determines whether a custom structure should be generated during the first-time loading of a chunk.
     * This method loads structure contexts and spawn contexts, then evaluates whether the conditions for
     * structure generation are met based on the chunk's position and configured parameters.
     * <p>
     * For each structure context, the method calculates a random Y-coordinate within the allowed range,
     * sets the structure's start position, and checks if the structure should be generated using
     * {@link #shouldGenerateStructure(ContextUtils.StructureContext, ContextUtils.SpawnContext, ChunkPos, ServerLevel, BlockPos)}.
     * <p>
     * If a structure is eligible for generation, the method returns {@code true}; otherwise, it returns {@code false}.
     *
     * @param pos         The position within the chunk to evaluate.
     * @param serverLevel The server level where the chunk is located.
     * @return {@code true} if the structure should be generated, {@code false} otherwise.
     * @see #shouldGenerateStructure(ContextUtils.StructureContext, ContextUtils.SpawnContext, ChunkPos, ServerLevel, BlockPos)
     * @see ContextUtils.SpawnContext
     * @see ContextUtils.StructureContext
     */
    public static boolean performFirstTimeLoadActionBoolean(BlockPos pos, ServerLevel serverLevel) {
        Random random = new Random();
        Map<String, ContextUtils.StructureContext> structures = StructureLoader.loadStructures();
        for (ContextUtils.StructureContext structureContext : structures.values()) {
            ContextUtils.SpawnContext spawnContext = DSHelperClass.getSpawnContext(structureContext.getStructureName());
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
                DSHelperClass.logWarningMessageOnce("Spawning Context is null: " + serverLevel.getChunk(pos).getPos() + " for spawningContext: " + spawnContext.getName() + " with structureContext: " + structureContext.getStructureName());
                return false;
            }
        }
        return false;
    }

    /**
     * Executes the generation of custom structures within a chunk during its first-time load.
     * This method retrieves the relevant structure contexts and spawn contexts, then iterates
     * through them to determine which structures should be generated in the chunk.
     * <p>
     * For each structure context, it finds the corresponding spawn context and, if available,
     * triggers the structure generation using {@link Dungeon#generateDungeon}.
     * <p>
     * Logs informative messages to indicate whether a structure is successfully spawned or if
     * the corresponding spawn context is missing.
     *
     * @param chunk The chunk in which the structure generation is to be performed.
     * @param level The server level where the chunk is located.
     * @see Dungeon#generateDungeon
     * @see ContextUtils.SpawnContext
     * @see ContextUtils.StructureContext
     */
    private static void performFirstTimeLoadAction(ChunkAccess chunk, ServerLevel level) {
        Map<String, ContextUtils.StructureContext> structures = StructureLoader.loadStructures();
        for (ContextUtils.StructureContext structureContext : structures.values()) {
            ContextUtils.SpawnContext spawnContext = DSHelperClass.getSpawnContext(structureContext.getStructureName());
            if (spawnContext != null) {
                if (!FMLEnvironment.production) {
                    DSHelperClass.logInfoMessageOnce("Spawning structure: " + chunk.getPos());
                }
                new Dungeon(structureContext, level).generateDungeon();
            } else {
                DSHelperClass.logWarningMessageOnce(" Spawning Context is null: " + chunk.getPos() + " for structureContext: " + structureContext.getStructureName() + " for spawnContext: " + structureContext.getStructureName());
            }
        }
    }

    /**
     * Finds and returns the {@link ContextUtils.SpawnContext} corresponding to a given structure name.
     * This method compares the derived structure name from the provided list of spawn contexts with
     * the target structure name, matching them based on their paths.
     * <p>
     * If a matching spawn context is found, it is returned; otherwise, the method returns {@code null}.
     *
     * @param spawnContexts The list of {@link ContextUtils.SpawnContext} objects to search through.
     * @param structureName The name of the structure for which the spawn context is being sought.
     * @return The matching {@link ContextUtils.SpawnContext}, or {@code null} if no match is found.
     * @see ContextUtils.SpawnContext
     * @see DSHelperClass#deriveStructureNameFromPath(String, java.io.File)
     */
    private static ContextUtils.SpawnContext findSpawnContextForStructure(List<ContextUtils.SpawnContext> spawnContexts, String structureName) {
        String structureNameFromPath = DSHelperClass.deriveStructureNameFromPath(structureName, StructureSetLoader.STRUCTURE_DIR);
        for (ContextUtils.SpawnContext context : spawnContexts) {
            String structureNameFromPath1 = DSHelperClass.deriveStructureNameFromPath(context.getName(), StructureSetLoader.STRUCTURE_DIR);
            if (structureNameFromPath.equals(structureNameFromPath1)) {
                return context;
            }
        }
        return null;
    }

    /**
     * Calculates the maximum size of a structure based on its base size and a size threshold.
     * The size is adjusted by a percentage of the base size determined by the threshold.
     *
     * @param baseSize      The initial size of the structure.
     * @param sizeThreshold The percentage threshold for size variation.
     * @return The maximum size after applying the variation.
     */
    public static int getMaxSize(int baseSize, int sizeThreshold) {
        int variation = (int) (baseSize * (sizeThreshold / 100.0));
        return baseSize + variation;
    }

    /**
     * Determines whether a structure should be generated in a specific chunk based on its configuration
     * and position relative to other structures. The method checks several factors, including spacing,
     * separation, and the bounding box of the structure, to ensure it is placed correctly without overlapping
     * other structures or exceeding the loaded area.
     * <p>
     * The method uses {@link #getMaxSize(int, int)} to calculate the dimensions of the structure, which is
     * essential for determining whether the structure fits within the chunk and its surrounding area.
     *
     * @param structureContext The context containing details about the structure to generate.
     * @param spawnContext     The context for spawning conditions related to the structure.
     * @param chunkPos         The position of the chunk being evaluated for structure generation.
     * @param level            The server level where the structure generation is being evaluated.
     * @param pos              The world position being evaluated for structure placement.
     * @return {@code true} if the structure should be generated in the chunk; {@code false} otherwise.
     * @see #getMaxSize(int, int)
     * @see Dungeon#getRandomSize(int, net.minecraft.util.RandomSource, int)
     */
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
        BlockPos centerPos = new BlockPos(originX, pos.getY(), originZ);
        BlockPos furthestPos = centerPos.offset(width / 2 + maxDistanceFromCenter, height, length / 2 + maxDistanceFromCenter);
        if (!level.isLoaded(furthestPos)) {
            return false;
        }
        int minX = originX - separation;
        int maxX = originX + separation;
        int minZ = originZ - separation;
        int maxZ = originZ + separation;
        if (chunkPos.x == originX && chunkPos.z == originZ) {
            return true;
        } else if (chunkPos.x >= minX && chunkPos.x <= maxX && chunkPos.z >= minZ && chunkPos.z <= maxZ) {
            return false;
        }
        return false;
    }

    /**
     * Computes a unique seed for structure generation based on the world seed, a salt value,
     * and the chunk coordinates. This seed is used to ensure consistent and reproducible
     * structure placement within the world.
     *
     * @param worldSeed The seed of the world.
     * @param salt      An additional salt value to add randomness.
     * @param chunkX    The X coordinate of the chunk.
     * @param chunkZ    The Z coordinate of the chunk.
     * @return A long value representing the computed structure seed.
     */
    private static long computeStructureSeed(long worldSeed, long salt, int chunkX, int chunkZ) {
        return worldSeed + salt + chunkX * 2345803L + chunkZ * 9236449L + (long) chunkX * chunkZ * 223;
    }

    /**
     * Finds and returns a list of chunk positions within a specified radius around a center chunk
     * where a given structure is eligible to generate. The method checks each chunk within the square
     * radius and determines if the structure should spawn there based on the provided
     * {@link ContextUtils.StructureContext} and {@link ContextUtils.SpawnContext}.
     *
     * @param structureContext The context containing details about the structure to generate.
     * @param spawnContext     The context for spawning conditions related to the structure.
     * @param centerChunk      The central chunk position from which the radius is calculated.
     * @param level            The server level where the chunks are located.
     * @param limit            The radius around the center chunk within which to search for potential structure positions.
     * @return A list of {@link ChunkPos} representing the chunk positions where the structure can spawn.
     * @see #shouldGenerateStructure(ContextUtils.StructureContext, ContextUtils.SpawnContext, ChunkPos, ServerLevel, BlockPos)
     */
    public static List<ChunkPos> findPotentialStructurePositions(ContextUtils.StructureContext structureContext, ContextUtils.SpawnContext spawnContext, ChunkPos centerChunk, ServerLevel level, int limit) {
        List<ChunkPos> potentialPositions = new ArrayList<>();
        int minX = centerChunk.x - limit;
        int maxX = centerChunk.x + limit;
        int minZ = centerChunk.z - limit;
        int maxZ = centerChunk.z + limit;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                ChunkPos chunkPos = new ChunkPos(x, z);
                BlockPos dummyPos = chunkPos.getWorldPosition();
                if (shouldGenerateStructure(structureContext, spawnContext, chunkPos, level, dummyPos)) {
                    potentialPositions.add(chunkPos);
                }
            }
        }
        return potentialPositions;
    }

    /**
     * Finds and returns the position of the nearest structure that matches a given name.
     *
     * <p>This method searches for the nearest structure matching the specified name by iterating over the world
     * in chunk-sized steps. The search continues until the first valid structure is found, at which point the search
     * stops and the position of that structure is returned. If no structure is found, an empty {@link Optional} is returned.</p>
     *
     * @param structureName    The name of the structure to search for.
     * @param structureContext The context containing details about the structure to generate.
     * @param spawnContext     The context for spawning conditions related to the structure.
     * @param level            The server level where the search is being conducted.
     * @param searchPos        The central position from which the search begins.
     * @return An {@link Optional} containing the {@link BlockPos} of the nearest structure if found, or {@link Optional#empty()} if not found.
     * @see ContextUtils.StructureContext#getStructureName()
     * @see ContextUtils.SpawnContext#getName()
     * @see #shouldGenerateStructure(ContextUtils.StructureContext, ContextUtils.SpawnContext, ChunkPos, ServerLevel, BlockPos)
     */
    public static Optional<BlockPos> findNearestStructure(String structureName, ContextUtils.StructureContext structureContext, ContextUtils.SpawnContext spawnContext, ServerLevel level, BlockPos searchPos) {
        int radius = 0;

        while (true) {
            radius++;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    ChunkPos chunkPos = new ChunkPos(searchPos.offset(x * 16, 0, z * 16));
                    if (structureName.equals(structureContext.getStructureName()) || structureName.equals(spawnContext.getName())) {
                        BlockPos dummyPos = chunkPos.getWorldPosition();
                        if (radius > 10000) return Optional.empty();
                        if (shouldGenerateStructure(structureContext, spawnContext, chunkPos, level, dummyPos)) {
                            return Optional.of(dummyPos);
                        }
                    }
                }
            }
        }
    }


    @SubscribeEvent
    public static void onRightClicked(PlayerInteractEvent.RightClickItem event) {
        doRightClick(event);
    }

    public static void doRightClick(PlayerInteractEvent.RightClickItem event) {
        /*if ((!Objects.equals(event.getEntity().getName().toString(), "Liopyu") &&
                FMLEnvironment.production) ||
                event.getEntity().getMainHandItem() != Items.DIAMOND.getDefaultInstance()
        ) return;*/
        var blockPos = event.getPos();
        if (!event.getLevel().isClientSide()) {
            var serverLevel = (ServerLevel) event.getLevel();
            var structureContext = DSHelperClass.getStructureContext("test");
            var spawnContext = DSHelperClass.getSpawnContext("test");
            StructureLoader.loadStructures().keySet().forEach(DSHelperClass::logInfoMessage);
            StructureLoader.loadStructures().values().forEach(context -> DSHelperClass.logInfoMessage(context.getStructureName()));
           /* var list = findPotentialStructurePositions(structureContext,
                    spawnContext,
                    serverLevel.getChunk(blockPos).getPos(),
                    serverLevel,
                    10
            );
            var nearestStructurePos = findNearestStructure("test", structureContext, spawnContext, serverLevel, blockPos);
            DSHelperClass.logInfoMessage(nearestStructurePos.get().toString());*/
            //list.forEach(position -> DSHelperClass.logInfoMessage(position.toString()));
        }
    }

    @SubscribeEvent
    public static void onCommandRegistry(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        FindStructureCommand.register(dispatcher);
    }
}
