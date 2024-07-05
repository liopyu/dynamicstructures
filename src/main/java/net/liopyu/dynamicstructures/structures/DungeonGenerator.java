package net.liopyu.dynamicstructures.structures;

import net.liopyu.dynamicstructures.util.DSHelperClass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
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

    public static void generateDungeon(ServerLevel world, BlockPos startPos, Direction startDirection, RandomSource random, int roomCount) {
        Block wallBlock = selectRandomBlock(WALL_BLOCKS, random);
        Block floorBlock = selectRandomBlock(FLOOR_BLOCKS, random);
        Block roofBlock = selectRandomBlock(ROOF_BLOCKS, random);

        Set<BlockPos> previousRoomWalls = null;
        BlockPos currentPos = startPos;
        Direction currentDirection = startDirection;

        for (int i = 0; i < roomCount; i++) {
            Set<BlockPos> currentRoomWalls = generateRoom(world, currentPos, 10, 10, 5, floorBlock, wallBlock, roofBlock, previousRoomWalls);

            placeDoorway(world, currentPos, 10, 10, currentDirection, random);
            BlockPos nextPos = calculateNextRoomPos(currentPos, 10, 10, currentDirection);
            currentDirection = random.nextBoolean() ? currentDirection.getClockWise() : currentDirection.getCounterClockWise();

            previousRoomWalls = currentRoomWalls;
            currentPos = nextPos;
        }
    }


    private static Set<BlockPos> generateRoom(ServerLevel world, BlockPos pos, int width, int length, int height, Block floorBlock, Block wallBlock, Block roofBlock, Set<BlockPos> overlapWalls) {
        Set<BlockPos> wallPositions = new HashSet<>();
        generateWalls(world, pos, width, length, height, wallBlock, wallPositions, overlapWalls);
        generateFloor(world, pos, width, length, floorBlock, wallPositions);
        generateRoof(world, pos, width, length, height, roofBlock);

        return wallPositions;
    }

    private static void generateFloor(ServerLevel world, BlockPos pos, int width, int length, Block floorBlock, Set<BlockPos> wallPositions) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos floorPos = pos.offset(x, 0, z);
                if (!wallPositions.contains(floorPos)) {
                    world.setBlock(floorPos, floorBlock.defaultBlockState(), 3);
                }
            }
        }
    }

    private static void generateWalls(ServerLevel world, BlockPos pos, int width, int length, int height, Block wallBlock, Set<BlockPos> wallPositions, Set<BlockPos> overlapWalls) {
        for (int y = 1; y <= height + 1; y++) {
            for (int x = 0; x < width; x++) {
                addWallBlock(world, pos.offset(x, y, 0), wallBlock, wallPositions, overlapWalls);
                addWallBlock(world, pos.offset(x, y, length - 1), wallBlock, wallPositions, overlapWalls);
            }
            for (int z = 1; z < length - 1; z++) {
                addWallBlock(world, pos.offset(0, y, z), wallBlock, wallPositions, overlapWalls);
                addWallBlock(world, pos.offset(width - 1, y, z), wallBlock, wallPositions, overlapWalls);
            }
        }
    }

    private static void generateRoof(ServerLevel world, BlockPos pos, int width, int length, int height, Block roofBlock) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                world.setBlock(pos.offset(x, height + 1, z), roofBlock.defaultBlockState(), 3);
            }
        }
    }

    private static void addWallBlock(ServerLevel world, BlockPos pos, Block block, Set<BlockPos> wallPositions, Set<BlockPos> overlapWalls) {
        if (overlapWalls != null && overlapWalls.contains(pos) && isSharedWall(pos, overlapWalls, world)) {
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else {
            world.setBlock(pos, block.defaultBlockState(), 3);
            wallPositions.add(pos);
        }
    }

    private static boolean isSharedWall(BlockPos pos, Set<BlockPos> overlapWalls, ServerLevel world) {
        boolean horizontalContinuity = (overlapWalls.contains(pos.north()) && overlapWalls.contains(pos.south())) ||
                (overlapWalls.contains(pos.east()) && overlapWalls.contains(pos.west()));

        return horizontalContinuity;
    }

    private static void placeDoorway(ServerLevel world, BlockPos pos, int width, int length, Direction direction, RandomSource random) {
        placeSingleDoor(world, pos, width, length, direction, random);
        placeSingleDoor(world, pos, width, length, direction.getOpposite(), random);
        placeSingleDoor(world, pos, width, length, Direction.EAST, random);
        placeSingleDoor(world, pos, width, length, Direction.WEST, random);
    }


    private static void placeSingleDoor(ServerLevel world, BlockPos pos, int width, int length, Direction direction, RandomSource random) {
        int offset = random.nextInt(3) - 1;

        BlockPos doorPosBottom1 = pos;
        BlockPos doorPosBottom2 = pos;

        switch (direction) {
            case NORTH, SOUTH -> {
                int doorX = (width / 2) + offset; // Adjust door position along the width
                doorPosBottom1 = pos.offset(doorX, 1, direction == Direction.NORTH ? 0 : length - 1);
                doorPosBottom2 = pos.offset(doorX, 2, direction == Direction.NORTH ? 0 : length - 1);
            }
            case EAST, WEST -> {
                int doorZ = (length / 2) + offset; // Adjust door position along the length
                doorPosBottom1 = pos.offset(direction == Direction.WEST ? 0 : width - 1, 1, doorZ);
                doorPosBottom2 = pos.offset(direction == Direction.WEST ? 0 : width - 1, 2, doorZ);
            }
            default -> {
                DSHelperClass.logErrorMessage("Unexpected direction: " + direction);
                return;
            }
        }

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