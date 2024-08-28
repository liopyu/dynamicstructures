package net.liopyu.dynamicstructures.structures;

import net.liopyu.dynamicstructures.util.BlockInterpreter;
import net.liopyu.dynamicstructures.util.BlockType;
import net.liopyu.dynamicstructures.util.ContextUtils;
import net.liopyu.dynamicstructures.util.DSHelperClass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class Dungeon {
    public static final Block[] WALL_BLOCKS = {
            Blocks.OAK_PLANKS, Blocks.STONE_BRICKS, Blocks.BRICKS, Blocks.COBBLESTONE
    };
    public static final Block[] FLOOR_BLOCKS = {
            Blocks.STONE, Blocks.SMOOTH_STONE, Blocks.OAK_PLANKS, Blocks.COBBLESTONE
    };
    public static final Block[] ROOF_BLOCKS = {
            Blocks.OAK_SLAB, Blocks.BRICK_SLAB, Blocks.STONE_SLAB, Blocks.COBBLESTONE_SLAB
    };
    public static final Block[] CENTER_BLOCKS = {
            Blocks.AIR
    };
    public static final Block[] FILLER_BLOCKS = {
            Blocks.AIR
    };
    public static final Block[] DOORWAY_BLOCKS = {
            Blocks.AIR
    };
    protected static int defaultDoorwayRadius = 3;
    public final ContextUtils.StructureContext structureContext;
    public final ServerLevel level;
    public Block floorBlock;
    public Block wallBlock;
    public Block roofBlock;
    public Block centerBlock;
    public Block doorwayBlock;
    public Block fillerBlock;
    public BlockPos currentPosition;
    public Map<BlockType, Block> blocks;

    /**
     * Constructs a new {@code Dungeon} instance with the given {@link ContextUtils.StructureContext} and {@link ServerLevel}.
     * The structure context provides the configuration details for the dungeon, while the server level specifies the world
     * in which the dungeon will be generated.
     *
     * @param structureContext The {@link ContextUtils.StructureContext} containing the configuration details of the dungeon.
     * @param level            The {@link ServerLevel} in which the dungeon will be generated.
     */
    public Dungeon(ContextUtils.StructureContext structureContext, ServerLevel level) {
        this.structureContext = structureContext;
        this.level = level;
        var random = level.random;
        getStructureContext().getBlockContext().setLevel(level);
        for (Map.Entry<BlockType, List<Block>> entry : getStructureContext().getBlockContext().getBlocks().entrySet()) {
            boolean found = entry.getValue().isEmpty();
            switch (entry.getKey()) {
                case ROOF -> {
                    if (found) {
                        roofBlock = selectRandomBlock(entry.getValue(), random);
                    } else {
                        roofBlock = selectRandomBlock(Arrays.stream(ROOF_BLOCKS).toList(), random);
                    }
                }
                case WALL -> {
                    if (found) {
                        wallBlock = selectRandomBlock(entry.getValue(), random);
                    } else {
                        wallBlock = selectRandomBlock(Arrays.stream(WALL_BLOCKS).toList(), random);
                    }
                }
                case FLOOR -> {
                    if (found) {
                        floorBlock = selectRandomBlock(entry.getValue(), random);
                    } else {
                        floorBlock = selectRandomBlock(Arrays.stream(FLOOR_BLOCKS).toList(), random);
                    }
                }
                case FILLER -> {
                    if (found) {
                        fillerBlock = selectRandomBlock(entry.getValue(), random);
                    } else {
                        fillerBlock = selectRandomBlock(Arrays.stream(FILLER_BLOCKS).toList(), random);
                    }
                }
                case DOORWAY -> {
                    if (found) {
                        doorwayBlock = selectRandomBlock(entry.getValue(), random);
                    } else {
                        doorwayBlock = selectRandomBlock(Arrays.stream(DOORWAY_BLOCKS).toList(), random);
                    }
                }
                case CENTER -> {
                    if (found) {
                        centerBlock = selectRandomBlock(entry.getValue(), random);
                    } else {
                        centerBlock = selectRandomBlock(Arrays.stream(CENTER_BLOCKS).toList(), random);
                    }
                }
            }
        }
    }

    /**
     * Calculates a random size based on a base size and a size threshold. The method adds or subtracts a random
     * variation, determined by the size threshold, from the base size to produce the final size.
     *
     * @param baseSize      The base size from which the random size is derived.
     * @param random        The {@link RandomSource} used to generate random numbers.
     * @param sizeThreshold The percentage threshold for size variation. The variation is calculated as a percentage of the base size.
     * @return The randomly adjusted size within the range of ±{@code sizeThreshold}% of the {@code baseSize}.
     */
    public static int getRandomSize(int baseSize, RandomSource random, int sizeThreshold) {
        int variation = (int) (baseSize * (sizeThreshold / 100.0));
        return baseSize + random.nextInt(variation * 2 + 1) - variation;
    }

    public BlockPos getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(BlockPos currentPosition) {
        this.currentPosition = currentPosition;
    }

    /**
     * Places a random number of spawners within the specified area of the world. The spawners are randomly distributed
     * within the given width, length, and height, and each spawner is assigned a random entity type from the provided list of potential spawns.
     *
     * @param world           The {@link ServerLevel} where the spawners should be placed.
     * @param pos             The starting {@link BlockPos} of the area in the world where the spawners will be placed.
     * @param width           The width of the area within which the spawners will be placed.
     * @param length          The length of the area within which the spawners will be placed.
     * @param height          The height of the area within which the spawners will be placed.
     * @param random          The {@link RandomSource} used to determine the number and positions of the spawners.
     * @param maxSpawners     The maximum number of spawners that can be placed per room.
     * @param potentialSpawns A list of {@link EntityType} representing the types of entities that can be spawned by the spawners.
     */
    private void placeSpawners(ServerLevel world, BlockPos pos, int width, int length, int height, RandomSource random, int maxSpawners, List<EntityType<?>> potentialSpawns) {
        int spawnersToPlace = random.nextInt(maxSpawners + 1); // Randomly decide how many spawners to place, up to maxSpawners
        for (int i = 0; i < spawnersToPlace; i++) {
            int x = pos.getX() + random.nextInt(width);
            int y = pos.getY() + random.nextInt(height);
            int z = pos.getZ() + random.nextInt(length);
            BlockPos spawnerPos = new BlockPos(x, y, z);
            EntityType<?> entityType = potentialSpawns.get(random.nextInt(potentialSpawns.size()));
            setBlock(spawnerPos, Blocks.SPAWNER.defaultBlockState(), 3, BlockType.CENTER);
            BlockEntity blockEntity = world.getBlockEntity(spawnerPos);
            if (blockEntity instanceof SpawnerBlockEntity spawnerEntity) {
                spawnerEntity.getSpawner().setEntityId(entityType, world, world.random, spawnerPos);
            }
        }
    }

    public void setBlock(BlockPos pPos, BlockState pNewState, int pFlags, BlockType blockType) {
        getStructureContext().getBlockContext().setPos(pPos);
        boolean placeBlock = BlockInterpreter.evaluateConditions(blockType, getStructureContext().getBlockContext());
        if (placeBlock) {
            this.getLevel().setBlock(pPos, pNewState, pFlags);
        }
    }

    /**
     * Generates a ladder room within the specified area of the world. The ladder room consists of two stacked rooms connected by a ladder.
     * The method generates the lower room, places a ladder that spans from the floor to the ceiling of the lower room, then generates the upper room,
     * ensuring that the roof of the lower room is opened where the ladder reaches the upper room.
     * Finally, a doorway is placed in the lower room, leading in the specified {@link Direction}.
     *
     * @param world            The {@link ServerLevel} where the ladder room should be generated.
     * @param basePos          The starting {@link BlockPos} of the lower room in the world.
     * @param width            The width of the rooms.
     * @param length           The length of the rooms.
     * @param height           The height of the rooms.
     * @param floorBlock       The {@link Block} type to be used for the floors.
     * @param wallBlock        The {@link Block} type to be used for the walls.
     * @param roofBlock        The {@link Block} type to be used for the roofs.
     * @param forceOverlap     Whether the walls of the room should forcibly overlap existing walls.
     * @param currentDirection The {@link Direction} in which the doorway should be placed.
     */
    public void generateLadderRoom(ServerLevel world, BlockPos basePos, int width, int length, int height, Block floorBlock, Block wallBlock, Block roofBlock, boolean forceOverlap, Direction currentDirection) {
        Set<BlockPos> roomWalls = generateRoom(world, basePos, width, length, height, floorBlock, wallBlock, roofBlock, null, forceOverlap, true);
        Direction ladderFacing = Direction.EAST;
        BlockPos ladderBase = basePos.offset(width / 2 - 1, 1, length / 2 - 1);
        for (int i = 0; i < height; i++) {
            BlockPos ladderPos = ladderBase.above(i);
            BlockState ladderState = Blocks.LADDER.defaultBlockState()
                    .setValue(LadderBlock.FACING, ladderFacing)
                    .setValue(LadderBlock.WATERLOGGED, false);
            setBlock(ladderPos, ladderState, 3, BlockType.CENTER);
        }
        BlockPos topRoomPos = basePos.above(height);
        generateRoom(world, topRoomPos, width, length, height, floorBlock, wallBlock, roofBlock, roomWalls, true, false);
        BlockPos opening = ladderBase.above(height);
        setBlock(opening, Blocks.AIR.defaultBlockState(), 3, BlockType.CENTER);
        placeDoorway(world, basePos, width, length, currentDirection, world.random, defaultDoorwayRadius);
    }

    /**
     * Generates a room within the specified area of the world by creating walls, a floor, and optionally a roof.
     * The method first generates the walls and floor, and if the room is not at the bottom of a structure, it also generates a roof.
     * The interior of the room is then cleared of any non-wall blocks by filling it with air.
     *
     * @param world        The {@link ServerLevel} where the room should be generated.
     * @param pos          The starting {@link BlockPos} of the room in the world.
     * @param width        The width of the room.
     * @param length       The length of the room.
     * @param height       The height of the room.
     * @param floorBlock   The {@link Block} type to be used for the floor.
     * @param wallBlock    The {@link Block} type to be used for the walls.
     * @param roofBlock    The {@link Block} type to be used for the roof, if applicable.
     * @param overlapWalls A set of {@link BlockPos} representing the positions where walls from previous rooms may overlap. Can be {@code null} if not applicable.
     * @param forceOverlap Whether the walls of the room should forcibly overlap existing walls.
     * @param isBottomRoom Whether this room is at the bottom of a structure, in which case a roof is not generated.
     * @return A {@link Set} of {@link BlockPos} representing the positions of the wall blocks that were placed.
     */
    private Set<BlockPos> generateRoom(ServerLevel world, BlockPos pos, int width, int length, int height, Block floorBlock, Block wallBlock, Block roofBlock, Set<BlockPos> overlapWalls, boolean forceOverlap, boolean isBottomRoom) {
        Set<BlockPos> wallPositions = new HashSet<>();
        generateWalls(world, pos, width, length, height, wallBlock, wallPositions, overlapWalls, forceOverlap);
        generateFloor(world, pos, width, length, floorBlock, wallPositions);
        if (!isBottomRoom) {
            generateRoof(world, pos, width, length, height, roofBlock);
        }
        fillRoomInteriorWithAir(world, pos, width, length, height, wallPositions);
        return wallPositions;
    }

    /**
     * Fills the interior of a room with air blocks, clearing out any blocks that are not part of the walls.
     * The method iterates through the interior space of the room and replaces any non-wall blocks with air.
     *
     * @param world         The {@link ServerLevel} where the room's interior should be filled with air.
     * @param pos           The starting {@link BlockPos} of the room in the world.
     * @param width         The width of the room.
     * @param length        The length of the room.
     * @param height        The height of the room.
     * @param wallPositions A set of {@link BlockPos} representing the positions of the wall blocks, where air blocks should not be placed.
     */
    private void fillRoomInteriorWithAir(ServerLevel world, BlockPos pos, int width, int length, int height, Set<BlockPos> wallPositions) {
        for (int x = 1; x < width - 1; x++) {
            for (int z = 1; z < length - 1; z++) {
                for (int y = 1; y <= height; y++) {
                    BlockPos blockPos = pos.offset(x, y, z);
                    if (!wallPositions.contains(blockPos)) {
                        setBlock(blockPos, fillerBlock.defaultBlockState(), 3, BlockType.FILLER);
                    }
                }
            }
        }
    }

    /**
     * Generates the floor of a room within the specified area of the world. The method places the specified
     * {@link Block} floor blocks, ensuring that no blocks are placed where there are wall blocks or ladder blocks.
     * If a ladder block is encountered, it is replaced with an air block.
     *
     * @param world         The {@link ServerLevel} where the floor should be generated.
     * @param pos           The starting {@link BlockPos} of the floor in the world.
     * @param width         The width of the room.
     * @param length        The length of the room.
     * @param floorBlock    The {@link Block} type to be used for the floor of the room.
     * @param wallPositions A set of {@link BlockPos} representing the positions of the wall blocks, where the floor blocks should not be placed.
     */
    private void generateFloor(ServerLevel world, BlockPos pos, int width, int length, Block floorBlock, Set<BlockPos> wallPositions) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos floorPos = pos.offset(x, 0, z);
                if (world.getBlockState(floorPos).getBlock() instanceof LadderBlock) {
                    setBlock(floorPos, Blocks.AIR.defaultBlockState(), 3, BlockType.FLOOR);
                } else if (!wallPositions.contains(floorPos)) {
                    setBlock(floorPos, floorBlock.defaultBlockState(), 3, BlockType.FLOOR);
                }
            }
        }
    }

    /**
     * Generates the walls of a room within the specified area of the world. The method places the specified
     * {@link Block} wall blocks along the edges of the room, adjusting for overlap with previous walls if necessary.
     * The walls are generated to a specified height, with options to force overlap or avoid it.
     *
     * @param world         The {@link ServerLevel} where the walls should be generated.
     * @param pos           The starting {@link BlockPos} of the room in the world.
     * @param width         The width of the room.
     * @param length        The length of the room.
     * @param height        The height of the walls to be generated.
     * @param wallBlock     The {@link Block} type to be used for the walls of the room.
     * @param wallPositions A set of {@link BlockPos} representing the positions of the wall blocks that are placed.
     * @param overlapWalls  A set of {@link BlockPos} where walls from previous rooms may overlap. Can be {@code null} if not applicable.
     * @param forceOverlap  Whether the walls of the room should forcibly overlap existing walls.
     */
    private void generateWalls(ServerLevel world, BlockPos pos, int width, int length, int height, Block wallBlock, Set<BlockPos> wallPositions, Set<BlockPos> overlapWalls, boolean forceOverlap) {
        for (int y = 1; y <= height + 1; y++) {
            for (int x = 0; x < width; x++) {
                addWallBlock(world, pos.offset(x, y, 0), wallBlock, wallPositions, overlapWalls, forceOverlap);
                addWallBlock(world, pos.offset(x, y, length - 1), wallBlock, wallPositions, overlapWalls, forceOverlap);
            }
            for (int z = 1; z < length - 1; z++) {
                addWallBlock(world, pos.offset(0, y, z), wallBlock, wallPositions, overlapWalls, forceOverlap);
                addWallBlock(world, pos.offset(width - 1, y, z), wallBlock, wallPositions, overlapWalls, forceOverlap);
            }
        }
    }

    /**
     * Adds a wall block at the specified {@link BlockPos} in the world, considering various conditions such as
     * existing blocks, overlap with previous walls, and adjacency to other wall blocks. The method ensures that
     * walls are placed correctly and can handle forced overlap or removal of blocks based on the provided parameters.
     *
     * @param world         The {@link ServerLevel} where the wall block should be placed.
     * @param pos           The {@link BlockPos} in the world where the wall block will be placed.
     * @param block         The {@link Block} type to be used for the wall.
     * @param wallPositions A set of {@link BlockPos} representing the positions of the wall blocks that are placed.
     * @param overlapWalls  A set of {@link BlockPos} where walls from previous rooms may overlap. Can be {@code null} if not applicable.
     * @param forceOverlap  Whether the wall block should forcibly overlap existing blocks, even if they are not suitable for wall placement.
     */
    private void addWallBlock(ServerLevel world, BlockPos pos, Block block, Set<BlockPos> wallPositions, Set<BlockPos> overlapWalls, boolean forceOverlap) {
        if (roofBlock.equals(world.getBlockState(pos).getBlock()) ||
                world.getBlockState(pos).getBlock() instanceof LiquidBlock ||
                world.getBlockState(pos).isAir()) {
            setBlock(pos, block.defaultBlockState(), 3, BlockType.WALL);
            wallPositions.add(pos);
        } else if (forceOverlap) {
            setBlock(pos, block.defaultBlockState(), 3, BlockType.WALL);
            wallPositions.add(pos);
        } else if (overlapWalls != null && overlapWalls.contains(pos) && isSharedWall(pos, overlapWalls)) {
            setBlock(pos, Blocks.AIR.defaultBlockState(), 3, BlockType.WALL);
        } else {
            for (Direction direction : Direction.values()) {
                BlockPos adjacentPos = pos.relative(direction);
                if (wallPositions.contains(adjacentPos)) {
                    setBlock(pos, Blocks.AIR.defaultBlockState(), 3, BlockType.WALL);
                    return;
                }
            }
        }
    }

    /**
     * Generates the roof of a room within the specified area of the world. The method places the specified
     * {@link Block} roof block on top of the room, ensuring that the roof is only placed in suitable locations,
     * such as air or liquid blocks, and avoids placing roof blocks over existing walls.
     *
     * @param world     The {@link ServerLevel} where the roof should be generated.
     * @param pos       The starting {@link BlockPos} of the room in the world.
     * @param width     The width of the room.
     * @param length    The length of the room.
     * @param height    The height of the room, which determines the elevation of the roof.
     * @param roofBlock The {@link Block} type to be used for the roof of the room.
     */
    private void generateRoof(ServerLevel world, BlockPos pos, int width, int length, int height, Block roofBlock) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos roofPos = pos.offset(x, height + 1, z);
                if (!(world.getBlockState(roofPos).getBlock() instanceof LiquidBlock ||
                        world.getBlockState(roofPos).is(Blocks.AIR) ||
                        world.getBlockState(roofPos).is(Blocks.CAVE_AIR) ||
                        wallBlock.equals(world.getBlockState(roofPos).getBlock()))) {
                    continue;
                }
                setBlock(roofPos, roofBlock.defaultBlockState(), 3, BlockType.ROOF);
            }
        }
    }

    /**
     * Determines whether a wall at the specified {@link BlockPos} is a shared wall, meaning it has overlapping
     * walls on opposite sides. The method checks if the wall at the given position is part of a continuous
     * wall segment by verifying that there are walls on both the north-south and east-west axes.
     *
     * @param pos          The {@link BlockPos} of the wall block being checked.
     * @param overlapWalls A set of {@link BlockPos} representing walls that may overlap. Used to determine if the wall is shared.
     * @return {@code true} If the wall at the specified position is a shared wall with overlaps on both axes; {@code false} otherwise.
     */
    private boolean isSharedWall(BlockPos pos, Set<BlockPos> overlapWalls) {
        return (overlapWalls.contains(pos.north()) && overlapWalls.contains(pos.south())) ||
                (overlapWalls.contains(pos.east()) && overlapWalls.contains(pos.west()));
    }

    /**
     * Places doorways at the specified position in the world, creating openings in multiple {@link Direction directions}.
     * The method places doorways on both sides of the room in the specified {@code direction}, as well as on the {@link Direction#EAST EAST} and {@link Direction#WEST WEST} sides.
     * Each doorway is created with a configurable {@code radius}.
     *
     * @param world     The {@link ServerLevel} where the doorways should be placed.
     * @param pos       The starting {@link BlockPos} of the room where the doorways will be created.
     * @param width     The width of the room.
     * @param length    The length of the room.
     * @param direction The primary {@link Direction} in which the doorway should be placed.
     * @param random    The {@link RandomSource} used for determining variations in doorway placement.
     * @param radius    The radius of the doorway, determining the size of the opening.
     */
    private void placeDoorway(ServerLevel world, BlockPos pos, int width, int length, Direction direction, RandomSource random, int radius) {
        placeSingleDoor(world, pos, width, length, direction, random, radius);
        placeSingleDoor(world, pos, width, length, direction.getOpposite(), random, radius);
        placeSingleDoor(world, pos, width, length, Direction.EAST, random, radius);
        placeSingleDoor(world, pos, width, length, Direction.WEST, random, radius);
    }

    /**
     * Places a single doorway at the specified position in the world, creating an opening in the given {@link Direction}.
     * The method adjusts the position of the doorway based on the room's {@code width} and {@code length} and the specified {@code doorRadius}.
     * Doorways are created by clearing blocks within the specified radius, ensuring that the doorway is not obstructed by blocks such as {@link Dungeon#ROOF_BLOCKS Roof Blocks}.
     *
     * @param world      The {@link ServerLevel} where the doorway should be placed.
     * @param pos        The starting {@link BlockPos position} of the room where the doorway will be created.
     * @param width      The width of the room.
     * @param length     The length of the room.
     * @param direction  The {@link Direction} in which the doorway should be placed (e.g., {@link Direction#NORTH NORTH}, {@link Direction#SOUTH SOUTH}, {@link Direction#EAST EAST}, {@link Direction#WEST WEST}).
     * @param random     The {@link RandomSource} used for determining variations in doorway placement.
     * @param doorRadius The radius of the doorway, determining the size of the opening.
     */
    private void placeSingleDoor(ServerLevel world, BlockPos pos, int width, int length, Direction direction, RandomSource random, int doorRadius) {
        int offset = random.nextInt(3) - 1;

        BlockPos doorPosBottom1 = pos;
        BlockPos outwardPos;
        switch (direction) {
            case NORTH, SOUTH -> {
                int doorX = (width / 2) + offset;
                doorPosBottom1 = pos.offset(doorX, 1, direction == Direction.NORTH ? 0 : length - 1);
                outwardPos = doorPosBottom1.relative(direction == Direction.NORTH ? Direction.NORTH : Direction.SOUTH);
                for (int i = -doorRadius; i <= doorRadius; i++) {
                    for (int j = -doorRadius; j <= doorRadius; j++) {
                        BlockPos doorPos = doorPosBottom1.offset(0, 0, j);
                        BlockPos doorPosTop = doorPos.above();
                        if (roofBlock.equals(world.getBlockState(outwardPos).getBlock())) {
                            return;
                        }
                        if (!world.getBlockState(doorPos).is(Blocks.LADDER)) {
                            setBlock(doorPos, fillerBlock.defaultBlockState(), 3, BlockType.DOORWAY);
                        }
                        if (!world.getBlockState(doorPosTop).is(Blocks.LADDER)) {
                            setBlock(doorPosTop, fillerBlock.defaultBlockState(), 3, BlockType.DOORWAY);
                        }
                    }
                }
            }
            case EAST, WEST -> {
                int doorZ = (length / 2) + offset;
                doorPosBottom1 = pos.offset(direction == Direction.WEST ? 0 : width - 1, 1, doorZ);
                outwardPos = doorPosBottom1.relative(direction == Direction.WEST ? Direction.WEST : Direction.EAST);
                for (int i = -doorRadius; i <= doorRadius; i++) {
                    BlockPos doorPos = doorPosBottom1.offset(0, 0, i);
                    BlockPos doorPosTop = doorPos.above();
                    if (roofBlock.equals(world.getBlockState(outwardPos).getBlock())) {
                        return;
                    }
                    setBlock(doorPos, fillerBlock.defaultBlockState(), 3, BlockType.DOORWAY);
                    setBlock(doorPosTop, fillerBlock.defaultBlockState(), 3, BlockType.DOORWAY);
                }
            }
            default -> {
                DSHelperClass.logErrorMessage("Unexpected direction: " + direction);
                return;
            }
        }
    }

    /**
     * Calculates the position of the next room based on the current position, room dimensions, and the given {@link Direction}.
     * The new position is offset in the specified direction to place the next room adjacent to the current one.
     *
     * @param pos       The current {@link BlockPos position} of the room.
     * @param width     The width of the current room.
     * @param length    The length of the current room.
     * @param direction The {@link Direction} in which the next room will be placed (e.g., {@link Direction#NORTH NORTH}, {@link Direction#SOUTH SOUTH}, {@link Direction#EAST EAST}, {@link Direction#WEST WEST}).
     * @return The {@link BlockPos position} of the next room.
     * @throws IllegalStateException if the direction is not one of the expected values.
     */
    private BlockPos calculateNextRoomPos(BlockPos pos, int width, int length, Direction direction) {
        return switch (direction) {
            case NORTH -> pos.offset(0, 0, -length + 1);
            case SOUTH -> pos.offset(0, 0, length - 1);
            case WEST -> pos.offset(-width + 1, 0, 0);
            case EAST -> pos.offset(width - 1, 0, 0);
            default -> throw new IllegalStateException("Unexpected value: " + direction);
        };
    }

    /**
     * Selects a random {@link Block} from the provided array of blocks using the given {@link RandomSource}.
     *
     * @param blocks The array of {@link Block blocks} to choose from.
     * @param random The {@link RandomSource} used to select a random block.
     * @return A randomly selected {@link Block} from the array.
     */
    private Block selectRandomBlock(List<Block> blocks, RandomSource random) {
        return blocks.get(random.nextInt(blocks.size()));
    }

    /**
     * Returns the {@link ContextUtils.StructureContext} associated with this dungeon.
     * The structure context contains the configuration and parameters used to generate the dungeon.
     *
     * @return The {@link ContextUtils.StructureContext} associated with this dungeon.
     */
    public ContextUtils.StructureContext getStructureContext() {
        return structureContext;
    }

    /**
     * Returns the {@link ServerLevel} in which this dungeon is being generated.
     * The server level represents the world or dimension where the dungeon will appear.
     *
     * @return The {@link ServerLevel} in which this dungeon is being generated.
     */
    public ServerLevel getLevel() {
        return level;
    }

    /**
     * Generates the dungeon in the specified {@link ServerLevel} based on the configuration provided
     * in the {@link ContextUtils.StructureContext}. This method creates multiple rooms connected by doorways,
     * potentially including ladder rooms and spawners, with randomized room sizes and directions.
     * <p>
     * The dungeon is generated procedurally, starting from the initial position and direction specified in the structure context.
     * Rooms are created with varying widths, lengths, and heights, with the option to include spawners based on the configuration.
     * Doorways are placed between rooms to connect them, and ladder rooms are generated based on the configured chance.
     * <p>
     * The dungeon generation process follows these steps:
     * <ul>
     *     <li>Selects random blocks for walls, floors, and roofs.</li>
     *     <li>Determines the size and type of each room (normal or ladder room) based on random factors.</li>
     *     <li>Generates rooms with walls, floors, roofs, and optionally spawners.</li>
     *     <li>Places doorways between rooms and calculates the position of the next room.</li>
     *     <li>Continues the process until all rooms are generated.</li>
     * </ul>
     */
    public void generateDungeon() {
        BlockPos startPos = structureContext.getStartPos();
        Direction startDirection = structureContext.getStartDirection();
        RandomSource random = level.getRandom();
        float ladderRoomChance = structureContext.getLadderRoomChance();
        int roomCount = structureContext.getRoomCount();
        int height = structureContext.getHeight();
        int baseWidth = structureContext.getWidth();
        int baseLength = structureContext.getLength();
        int sizeThreshold = structureContext.getSizeThreshold();
        boolean generatesSpawners = structureContext.isGeneratesSpawners();
        int maxSpawners = structureContext.getMaxSpawners();
        List<EntityType<?>> potentialSpawns = structureContext.getPotentialSpawns();
        Set<BlockPos> previousRoomWalls = null;
        BlockPos currentPos = startPos;
        Direction currentDirection = startDirection;
        for (int i = 0; i < roomCount; i++) {
            int width = getRandomSize(baseWidth, random, sizeThreshold);
            int length = getRandomSize(baseLength, random, sizeThreshold);
            boolean isLadderRoom = random.nextInt(100) < ladderRoomChance;
            if (isLadderRoom) {
                generateLadderRoom(level, currentPos, width, length, height, floorBlock, wallBlock, roofBlock, true, currentDirection);
                placeDoorway(level, currentPos, width, length, currentDirection, random, defaultDoorwayRadius);
                BlockPos upperRoomPos = currentPos.above(height);
                placeDoorway(level, upperRoomPos, width, length, currentDirection, random, defaultDoorwayRadius);
                if (generatesSpawners) {
                    placeSpawners(level, currentPos, width, length, height, random, maxSpawners, potentialSpawns);
                    placeSpawners(level, upperRoomPos, width, length, height, random, maxSpawners, potentialSpawns);
                }
                currentPos = calculateNextRoomPos(upperRoomPos, width, length, currentDirection);
            } else {
                Set<BlockPos> currentRoomWalls = generateRoom(level, currentPos, width, length, height, floorBlock, wallBlock, roofBlock, previousRoomWalls, false, false);
                previousRoomWalls = currentRoomWalls;
                placeDoorway(level, currentPos, width, length, currentDirection, random, defaultDoorwayRadius);
                if (generatesSpawners) {
                    placeSpawners(level, currentPos, width, length, height, random, maxSpawners, potentialSpawns);
                }
                currentPos = calculateNextRoomPos(currentPos, width, length, currentDirection);
            }
            currentDirection = random.nextBoolean() ? currentDirection.getClockWise() : currentDirection.getCounterClockWise();
        }
    }


}