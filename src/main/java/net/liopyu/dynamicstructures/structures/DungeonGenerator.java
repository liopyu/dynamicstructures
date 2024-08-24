package net.liopyu.dynamicstructures.structures;

import net.liopyu.dynamicstructures.DynamicStructures;
import net.liopyu.dynamicstructures.util.DSHelperClass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DungeonGenerator {
    private static final Block[] WALL_BLOCKS = {
            Blocks.OAK_PLANKS, Blocks.STONE_BRICKS, Blocks.BRICKS, Blocks.COBBLESTONE
    };

    private static final Block[] FLOOR_BLOCKS = {
            Blocks.STONE, Blocks.SMOOTH_STONE, Blocks.OAK_PLANKS, Blocks.COBBLESTONE
    };

    private static final Block[] ROOF_BLOCKS = {
            Blocks.OAK_SLAB, Blocks.STONE_SLAB, Blocks.BRICK_SLAB, Blocks.COBBLESTONE_SLAB
    };
    public static final EntityType<?>[] ENTITY_TYPES = {
            EntityType.ZOMBIE, EntityType.WITCH, EntityType.SKELETON, EntityType.CREEPER
    };
    protected static int defaultDoorwayRadius = 3;
    public static void generateDungeon(ServerLevel world, BlockPos startPos, Direction startDirection, RandomSource random,float ladderRoomChance, int roomCount, int height, int baseWidth, int baseLength, boolean generatesSpawners, int maxSpawners, List<EntityType<?>> potentialSpawns) {
        Block wallBlock = selectRandomBlock(WALL_BLOCKS, random);
        Block floorBlock = selectRandomBlock(FLOOR_BLOCKS, random);
        Block roofBlock = selectRandomBlock(ROOF_BLOCKS, random);

        Set<BlockPos> previousRoomWalls = null;
        BlockPos currentPos = startPos;
        Direction currentDirection = startDirection;

        for (int i = 0; i < roomCount; i++) {
            // Calculate random width and length within 30% of the base values
            int width = getRandomSize(baseWidth, random);
            int length = getRandomSize(baseLength, random);

            boolean isLadderRoom = random.nextInt(10) < ladderRoomChance; // 20% chance for a ladder room

            if (isLadderRoom) {
                // Generate both lower and upper ladder rooms
                generateLadderRoom(world, currentPos, width, length, height, floorBlock, wallBlock, roofBlock, true, currentDirection);

                // Place doorways for both lower and upper rooms
                placeDoorway(world, currentPos, width, length, currentDirection, random, defaultDoorwayRadius);
                BlockPos upperRoomPos = currentPos.above(height);
                placeDoorway(world, upperRoomPos, width, length, currentDirection, random, defaultDoorwayRadius);

                // Place spawners if configured
                if (generatesSpawners) {
                    placeSpawners(world, currentPos, width, length, height, random, maxSpawners, potentialSpawns);
                    placeSpawners(world, upperRoomPos, width, length, height, random, maxSpawners, potentialSpawns);
                }

                // Move to the next room position after the upper room
                currentPos = calculateNextRoomPos(upperRoomPos, width, length, currentDirection);
            } else {
                Set<BlockPos> currentRoomWalls = generateRoom(world, currentPos, width, length, height, floorBlock, wallBlock, roofBlock, previousRoomWalls, false, false);
                previousRoomWalls = currentRoomWalls;

                // Place the doorway for the regular room
                placeDoorway(world, currentPos, width, length, currentDirection, random, defaultDoorwayRadius);

                // Place spawners if configured
                if (generatesSpawners) {
                    placeSpawners(world, currentPos, width, length, height, random, maxSpawners, potentialSpawns);
                }

                // Move to the next room position
                currentPos = calculateNextRoomPos(currentPos, width, length, currentDirection);
            }

            // Randomly change the direction for the next room
            currentDirection = random.nextBoolean() ? currentDirection.getClockWise() : currentDirection.getCounterClockWise();
        }
    }

    public static void generateDungeon(ServerLevel world, BlockPos startPos, Direction startDirection, RandomSource random, int roomCount, int height, int baseWidth, int baseLength) {
        Block wallBlock = selectRandomBlock(WALL_BLOCKS, random);
        Block floorBlock = selectRandomBlock(FLOOR_BLOCKS, random);
        Block roofBlock = selectRandomBlock(ROOF_BLOCKS, random);

        Set<BlockPos> previousRoomWalls = null;
        BlockPos currentPos = startPos;
        Direction currentDirection = startDirection;

        for (int i = 0; i < roomCount; i++) {
            // Calculate random width and length within 30% of the base values
            int width = getRandomSize(baseWidth, random);
            int length = getRandomSize(baseLength, random);

            boolean isLadderRoom = random.nextInt(10) < 2; // 20% chance for a ladder room

            if (isLadderRoom) {
                // Generate both lower and upper ladder rooms
                generateLadderRoom(world, currentPos, width, length, height, floorBlock, wallBlock, roofBlock, true,currentDirection);

                // Place doorways for both lower and upper rooms
                placeDoorway(world, currentPos, width, length, currentDirection, random,defaultDoorwayRadius);
                BlockPos upperRoomPos = currentPos.above(height);
                placeDoorway(world, upperRoomPos, width, length, currentDirection, random,defaultDoorwayRadius);

                // Move to the next room position after the upper room
                currentPos = calculateNextRoomPos(upperRoomPos, width, length, currentDirection);
            } else {
                Set<BlockPos> currentRoomWalls = generateRoom(world, currentPos, width, length, height, floorBlock, wallBlock, roofBlock, previousRoomWalls, false, false);
                previousRoomWalls = currentRoomWalls;

                // Place the doorway for the regular room
                placeDoorway(world, currentPos, width, length, currentDirection, random,defaultDoorwayRadius);

                // Move to the next room position
                currentPos = calculateNextRoomPos(currentPos, width, length, currentDirection);
            }

            // Randomly change the direction for the next room
            currentDirection = random.nextBoolean() ? currentDirection.getClockWise() : currentDirection.getCounterClockWise();
        }
    }

    private static void placeSpawners(ServerLevel world, BlockPos pos, int width, int length, int height, RandomSource random, int maxSpawners, List<EntityType<?>> potentialSpawns) {
        int spawnersToPlace = random.nextInt(maxSpawners + 1); // Randomly decide how many spawners to place, up to maxSpawners

        for (int i = 0; i < spawnersToPlace; i++) {
            // Randomly pick a position within the room
            int x = pos.getX() + random.nextInt(width);
            int y = pos.getY()+ random.nextInt(height);
            int z = pos.getZ() + random.nextInt(length);

            BlockPos spawnerPos = new BlockPos(x, y, z);

            // Select a random entity type from the list
            EntityType<?> entityType = potentialSpawns.get(random.nextInt(potentialSpawns.size()));

            // Place the spawner block and set its entity type
            world.setBlock(spawnerPos, Blocks.SPAWNER.defaultBlockState(), 3);

            BlockEntity blockEntity = world.getBlockEntity(spawnerPos);
            if (blockEntity instanceof SpawnerBlockEntity spawnerEntity) {
                spawnerEntity.getSpawner().setEntityId(entityType,world,world.random,spawnerPos);
            }
        }
    }
    private static int getRandomSize(int baseSize, RandomSource random) {
        // Calculate 30% of the base size
        int variation = (int) (baseSize * 0.3);

        // Randomly adjust the size within ±30% of the base size
        return baseSize + random.nextInt(variation * 2 + 1) - variation;
    }


    public static void generateLadderRoom(ServerLevel world, BlockPos basePos, int width, int length, int height, Block floorBlock, Block wallBlock, Block roofBlock, boolean forceOverlap,Direction currentDirection) {
        // Generate the lower room
        Set<BlockPos> roomWalls = generateRoom(world, basePos, width, length, height, floorBlock, wallBlock, roofBlock, null, forceOverlap,true);

        // Place the ladder on one of the walls
        Direction ladderFacing = Direction.EAST; // Typically the ladder would face towards the interior
        BlockPos ladderBase = basePos.offset(width / 2 - 1, 1, length / 2 - 1); // Positioned on the floor against the wall

        // Install the ladder from the floor to the ceiling of the lower room
        for (int i = 0; i < height; i++) {
            BlockPos ladderPos = ladderBase.above(i);
            BlockState ladderState = Blocks.LADDER.defaultBlockState()
                    .setValue(LadderBlock.FACING, ladderFacing)
                    .setValue(LadderBlock.WATERLOGGED, false);
            world.setBlock(ladderPos, ladderState, 3);
        }

        // Top of the ladder room position at the roof of the lower room
        BlockPos topRoomPos = basePos.above(height);

        // Generate the upper room, making sure to override the ceiling of the lower room
        generateRoom(world, topRoomPos, width, length, height, floorBlock, wallBlock, roofBlock, roomWalls, true,false);

        // Ensure there's an opening at the top of the ladder into the new room
        BlockPos opening = ladderBase.above(height);
        world.setBlock(opening, Blocks.AIR.defaultBlockState(), 3); // Clear the entry point into the upper room
        placeDoorway(world, basePos, width, length, currentDirection, world.random,defaultDoorwayRadius);
    }

    private static Set<BlockPos> generateLadderRoom(ServerLevel world, BlockPos pos, int width, int length, int height, Block floorBlock, Block wallBlock, Block roofBlock, Set<BlockPos> overlapWalls, boolean forceOverlap) {
        Set<BlockPos> wallPositions = new HashSet<>();

        generateWalls(world, pos, width, length, height, wallBlock, wallPositions, overlapWalls,forceOverlap);
        generateFloor(world, pos, width, length, floorBlock, wallPositions);
        generateRoof(world, pos, width, length, height, roofBlock);

        fillRoomInteriorWithAir(world, pos, width, length, height, wallPositions);

        return wallPositions;
    }
    private static Set<BlockPos> generateRoom(ServerLevel world, BlockPos pos, int width, int length, int height, Block floorBlock, Block wallBlock, Block roofBlock, Set<BlockPos> overlapWalls, boolean forceOverlap, boolean isBottomRoom) {
        Set<BlockPos> wallPositions = new HashSet<>();

        generateWalls(world, pos, width, length, height, wallBlock, wallPositions, overlapWalls, forceOverlap);
        generateFloor(world, pos, width, length, floorBlock, wallPositions);

        // Skip roof generation if this is the bottom room of a ladder structure
        if (!isBottomRoom) {
            generateRoof(world, pos, width, length, height, roofBlock);
        }

        fillRoomInteriorWithAir(world, pos, width, length, height, wallPositions);

        return wallPositions;
    }


    private static void fillRoomInteriorWithAir(ServerLevel world, BlockPos pos, int width, int length, int height, Set<BlockPos> wallPositions) {
        for (int x = 1; x < width - 1; x++) {
            for (int z = 1; z < length - 1; z++) {
                for (int y = 1; y <= height; y++) {
                    BlockPos blockPos = pos.offset(x, y, z);
                    // Ensure the position is not part of the walls or boundaries
                    if (!wallPositions.contains(blockPos)) {
                        world.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static void generateFloor(ServerLevel world, BlockPos pos, int width, int length, Block floorBlock, Set<BlockPos> wallPositions) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos floorPos = pos.offset(x, 0, z);

                // Check if the block below the floor position is a ladder
                if (world.getBlockState(floorPos).getBlock() instanceof LadderBlock) {
                    world.setBlock(floorPos, Blocks.AIR.defaultBlockState(), 3);
                } else if (!wallPositions.contains(floorPos)) {
                    world.setBlock(floorPos, floorBlock.defaultBlockState(), 3);
                }
            }
        }
    }


    private static void generateWalls(ServerLevel world, BlockPos pos, int width, int length, int height, Block wallBlock, Set<BlockPos> wallPositions, Set<BlockPos> overlapWalls,boolean forceOverlap) {
        for (int y = 1; y <= height + 1; y++) {
            for (int x = 0; x < width; x++) {
                addWallBlock(world, pos.offset(x, y, 0), wallBlock, wallPositions, overlapWalls,forceOverlap);
                addWallBlock(world, pos.offset(x, y, length - 1), wallBlock, wallPositions, overlapWalls,forceOverlap);
            }
            for (int z = 1; z < length - 1; z++) {
                addWallBlock(world, pos.offset(0, y, z), wallBlock, wallPositions, overlapWalls,forceOverlap);
                addWallBlock(world, pos.offset(width - 1, y, z), wallBlock, wallPositions, overlapWalls,forceOverlap);
            }
        }
    }
    private static void addWallBlock(ServerLevel world, BlockPos pos, Block block, Set<BlockPos> wallPositions, Set<BlockPos> overlapWalls, boolean forceOverlap) {
        // Check if the block is suitable for wall placement
        if (Arrays.stream(ROOF_BLOCKS).toList().contains(world.getBlockState(pos).getBlock()) ||
                world.getBlockState(pos).getBlock() instanceof LiquidBlock ||
                world.getBlockState(pos).isAir()) {

            world.setBlock(pos, block.defaultBlockState(), 3);
            wallPositions.add(pos);
        } else if (forceOverlap) {
            // Force overlap: Place the wall block even if it overlaps with an existing wall
            world.setBlock(pos, block.defaultBlockState(), 3);
            wallPositions.add(pos);
        } else if (overlapWalls != null && overlapWalls.contains(pos) && isSharedWall(pos, overlapWalls)) {
            // If not forcing overlap, clear the position (place air) if it's a shared wall
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else {
            // Check adjacent blocks for double walls
            for (Direction direction : Direction.values()) {
                BlockPos adjacentPos = pos.relative(direction);
                if (wallPositions.contains(adjacentPos)) {
                    world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    return; // Exit early after setting to air to prevent placing a block
                }
            }
        }
    }


    private static void generateRoof(ServerLevel world, BlockPos pos, int width, int length, int height, Block roofBlock) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos roofPos = pos.offset(x, height + 1, z);
                if (!(world.getBlockState(roofPos).getBlock() instanceof LiquidBlock||
                        world.getBlockState(roofPos).is(Blocks.AIR)||
                        world.getBlockState(roofPos).is(Blocks.CAVE_AIR)||
                        Arrays.stream(WALL_BLOCKS).toList().contains(world.getBlockState(roofPos).getBlock()))){
                    continue;
                }
                // Place the roof block if no wall block is present
                world.setBlock(roofPos, roofBlock.defaultBlockState(), 3);
            }
        }
    }

    private static void generateRoof(ServerLevel world, BlockPos pos, int width, int length, int height, Block roofBlock,boolean forceOverlap) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                if (Arrays.stream(WALL_BLOCKS).toList().contains(world.getBlockState(pos.offset(x, height + 1, z)).getBlock())||
                        Arrays.stream(ROOF_BLOCKS).toList().contains(world.getBlockState(pos.offset(x, height + 1, z)).getBlock())||
                        Arrays.stream(FLOOR_BLOCKS).toList().contains(world.getBlockState(pos.offset(x, height + 1, z)).getBlock())) return;
                world.setBlock(pos.offset(x, height + 1, z), roofBlock.defaultBlockState(), 3);
            }
        }
    }


    private static boolean isSharedWall(BlockPos pos, Set<BlockPos> overlapWalls) {
        return (overlapWalls.contains(pos.north()) && overlapWalls.contains(pos.south())) ||
                (overlapWalls.contains(pos.east()) && overlapWalls.contains(pos.west()));
    }

    private static void placeDoorway(ServerLevel world, BlockPos pos, int width, int length, Direction direction, RandomSource random,int radius) {
        placeSingleDoor(world, pos, width, length, direction, random,radius);
        placeSingleDoor(world, pos, width, length, direction.getOpposite(), random,radius);
        placeSingleDoor(world, pos, width, length, Direction.EAST, random,radius);
        placeSingleDoor(world, pos, width, length, Direction.WEST, random,radius);
    }
    private static void placeSingleDoor(ServerLevel world, BlockPos pos, int width, int length, Direction direction, RandomSource random, int doorRadius) {
        int offset = random.nextInt(3) - 1;

        BlockPos doorPosBottom1 = pos;
        BlockPos outwardPos;

        switch (direction) {
            case NORTH, SOUTH -> {
                int doorX = (width / 2) + offset; // Adjust door position along the width
                doorPosBottom1 = pos.offset(doorX, 1, direction == Direction.NORTH ? 0 : length - 1);
                outwardPos = doorPosBottom1.relative(direction == Direction.NORTH ? Direction.NORTH : Direction.SOUTH);

                // Create a doorway with the specified radius
                for (int i = -doorRadius; i <= doorRadius; i++) {
                    for (int j = -doorRadius; j <= doorRadius; j++) {
                        BlockPos doorPos = doorPosBottom1.offset(0, 0, j);
                        BlockPos doorPosTop = doorPos.above();

                        if (Arrays.stream(ROOF_BLOCKS).toList().contains(world.getBlockState(outwardPos).getBlock())) {
                            return; // Skip placing the doorway if blocked by a roof block
                        }
                        if (!world.getBlockState(doorPos).is(Blocks.LADDER)){
                            world.setBlock(doorPos, Blocks.AIR.defaultBlockState(), 3);
                        }
                        if (!world.getBlockState(doorPosTop).is(Blocks.LADDER)){
                            world.setBlock(doorPosTop, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
            }
            case EAST, WEST -> {
                int doorZ = (length / 2) + offset; // Adjust door position along the length
                doorPosBottom1 = pos.offset(direction == Direction.WEST ? 0 : width - 1, 1, doorZ);
                outwardPos = doorPosBottom1.relative(direction == Direction.WEST ? Direction.WEST : Direction.EAST);

                // Create a doorway with the specified radius
                for (int i = -doorRadius; i <= doorRadius; i++) {
                    BlockPos doorPos = doorPosBottom1.offset(0, 0, i);
                    BlockPos doorPosTop = doorPos.above();

                    if (Arrays.stream(ROOF_BLOCKS).toList().contains(world.getBlockState(outwardPos).getBlock())) {
                        return; // Skip placing the doorway if blocked by a roof block
                    }

                    world.setBlock(doorPos, Blocks.AIR.defaultBlockState(), 3);
                    world.setBlock(doorPosTop, Blocks.AIR.defaultBlockState(), 3);
                }
            }
            default -> {
                DSHelperClass.logErrorMessage("Unexpected direction: " + direction);
                return;
            }
        }
    }


    private static void placeSingleDoor(ServerLevel world, BlockPos pos, int width, int length, Direction direction, RandomSource random) {
        int offset = random.nextInt(3) - 1;

        BlockPos doorPosBottom1 = pos;
        BlockPos doorPosBottom2 = pos;
        BlockPos outwardPos;

        switch (direction) {
            case NORTH, SOUTH -> {
                int doorX = (width / 2) + offset; // Adjust door position along the width
                doorPosBottom1 = pos.offset(doorX, 1, direction == Direction.NORTH ? 0 : length - 1);
                doorPosBottom2 = pos.offset(doorX, 2, direction == Direction.NORTH ? 0 : length - 1);
                // Calculate the position one block outward
                outwardPos = doorPosBottom1.relative(direction == Direction.NORTH ? Direction.NORTH : Direction.SOUTH);
            }
            case EAST, WEST -> {
                int doorZ = (length / 2) + offset; // Adjust door position along the length
                doorPosBottom1 = pos.offset(direction == Direction.WEST ? 0 : width - 1, 1, doorZ);
                doorPosBottom2 = pos.offset(direction == Direction.WEST ? 0 : width - 1, 2, doorZ);
                // Calculate the position one block outward
                outwardPos = doorPosBottom1.relative(direction == Direction.WEST ? Direction.WEST : Direction.EAST);
            }
            default -> {
                DSHelperClass.logErrorMessage("Unexpected direction: " + direction);
                return;
            }
        }

        // Check if the block one block outward is in the roof block array
        if (Arrays.stream(ROOF_BLOCKS).toList().contains(world.getBlockState(outwardPos).getBlock())) {
            return; // Do not place the doorway if the outward block is a roof block
        }

        // Place the doorway blocks if the condition is not met
        world.setBlock(doorPosBottom1, Blocks.AIR.defaultBlockState(), 3);
        world.setBlock(doorPosBottom2, Blocks.AIR.defaultBlockState(), 3);
    }


    private static BlockPos calculateNextRoomPos(BlockPos pos, int width, int length, Direction direction) {
        return switch (direction) {
            case NORTH -> pos.offset(0, 0, -length + 1);
            case SOUTH -> pos.offset(0, 0, length - 1);
            case WEST -> pos.offset(-width + 1, 0, 0);
            case EAST -> pos.offset(width - 1, 0, 0);
            default -> throw new IllegalStateException("Unexpected value: " + direction);
        };
    }

    private static Block selectRandomBlock(Block[] blocks, RandomSource random) {
        return blocks[random.nextInt(blocks.length)];
    }
}