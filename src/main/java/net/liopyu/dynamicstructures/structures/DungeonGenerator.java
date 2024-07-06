package net.liopyu.dynamicstructures.structures;

import net.liopyu.dynamicstructures.util.DSHelperClass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

import java.util.HashSet;
import java.util.Set;

public class DungeonGenerator {
    public enum RoomType {
        STANDARD,
        STAIRCASE  // Room with stairs leading up
    }
    private static final Block[] WALL_BLOCKS = {
            Blocks.OAK_PLANKS, Blocks.STONE_BRICKS, Blocks.BRICKS, Blocks.COBBLESTONE
    };

    private static final Block[] FLOOR_BLOCKS = {
            Blocks.STONE, Blocks.SMOOTH_STONE, Blocks.OAK_PLANKS, Blocks.COBBLESTONE
    };

    private static final Block[] ROOF_BLOCKS = {
            Blocks.OAK_SLAB, Blocks.STONE_SLAB, Blocks.BRICK_SLAB, Blocks.COBBLESTONE_SLAB
    };

    public static void generateDungeon(ServerLevel world, BlockPos startPos, Direction startDirection, RandomSource random, int roomCount, int height) {
        Block wallBlock = selectRandomBlock(WALL_BLOCKS, random);
        Block floorBlock = selectRandomBlock(FLOOR_BLOCKS, random);
        Block roofBlock = selectRandomBlock(ROOF_BLOCKS, random);

        Set<BlockPos> previousRoomWalls = null;
        BlockPos currentPos = startPos;
        Direction currentDirection = startDirection;

        for (int i = 0; i < roomCount; i++) {
            boolean isLadderRoom = random.nextInt(10) < 2; // 20% chance for a ladder room

            if (isLadderRoom) {
                generateLadderRoom(world, currentPos, 10, 10, height, floorBlock, wallBlock, roofBlock);
            } else {
                Set<BlockPos> currentRoomWalls = generateRoom(world, currentPos, 10, 10, height, floorBlock, wallBlock, roofBlock, previousRoomWalls);
                previousRoomWalls = currentRoomWalls;
            }

            placeDoorway(world, currentPos, 10, 10, currentDirection, random);

            BlockPos nextPos = calculateNextRoomPos(currentPos, 10, 10, currentDirection);
            currentDirection = random.nextBoolean() ? currentDirection.getClockWise() : currentDirection.getCounterClockWise();
            currentPos = nextPos;
        }
    }
    private static void generateLadderRoom(ServerLevel world, BlockPos basePos, int width, int length, int height, Block floorBlock, Block wallBlock, Block roofBlock) {
        // Generate the lower room
        Set<BlockPos> roomWalls = generateRoom(world, basePos, width, length, height, floorBlock, wallBlock, roofBlock, null);

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
        generateRoom(world, topRoomPos, width, length, height, floorBlock, wallBlock, roofBlock, roomWalls);

        // Ensure there's an opening at the top of the ladder into the new room
        BlockPos opening = ladderBase.above(height);
        world.setBlock(opening, Blocks.AIR.defaultBlockState(), 3); // Clear the entry point into the upper room

        // Replace the ceiling of the lower room with the floor of the upper room
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos floorPos = topRoomPos.offset(x, 0, z);
                world.setBlock(floorPos, floorBlock.defaultBlockState(), 3);  // Set floor, replacing the old ceiling
            }
        }
    }




    private static void generateStaircaseRoom(ServerLevel world, BlockPos pos, int width, int length, int height, Block floorBlock, Block wallBlock, Block roofBlock, Set<BlockPos> previousRoomWalls, RandomSource random, boolean isSpiral) {
        Set<BlockPos> wallPositions = new HashSet<>();
        // Generate the walls, floor, and roof first
        generateWalls(world, pos, width, length, height, wallBlock, wallPositions, previousRoomWalls);
        generateFloor(world, pos, width, length, floorBlock, wallPositions);
        generateRoof(world, pos, width, length, height, roofBlock);

        // Decide on the type of staircase based on the isSpiral flag
        if (isSpiral) {
            // Place a spiral staircase in the center of the room
            placeSpiralStairs(world, pos, width, length, height, random);
        } else {
            // Place a regular staircase, you can adjust the placement as needed
            placeStairs(world, pos, width, length, height, random);
        }
    }

    private static Set<BlockPos> generateStandardRoom(ServerLevel world, BlockPos pos, int width, int length, int height, Block floorBlock, Block wallBlock, Block roofBlock, Set<BlockPos> previousRoomWalls) {
        return generateRoom(world, pos, width, length, height, floorBlock, wallBlock, roofBlock, previousRoomWalls);
    }

    private static void generateSpiralStaircaseRoom(ServerLevel world, BlockPos pos, int width, int length, int height, Block floorBlock, Block wallBlock, Block roofBlock, RandomSource random) {
        Set<BlockPos> wallPositions = new HashSet<>();
        generateWalls(world, pos, width, length, height, wallBlock, wallPositions, new HashSet<>());
        generateFloor(world, pos, width, length, floorBlock, wallPositions);
        generateRoof(world, pos, width, length, height, roofBlock);
        placeSpiralStairs(world, pos, width, length, height, random); // Assume this method places spiral stairs
    }


    private static void placeStairs(ServerLevel world, BlockPos pos, int width, int length, int height, RandomSource random) {
        Direction stairFacing = Direction.NORTH; // Default facing direction for stairs
        // Determine the direction based on the room or another logical condition
        int facingIndex = random.nextInt(4);
        switch (facingIndex) {
            case 0:
                stairFacing = Direction.NORTH;
                break;
            case 1:
                stairFacing = Direction.SOUTH;
                break;
            case 2:
                stairFacing = Direction.EAST;
                break;
            case 3:
                stairFacing = Direction.WEST;
                break;
        }

        // Determine base position based on the direction
        BlockPos stairBase;
        if (stairFacing == Direction.NORTH || stairFacing == Direction.SOUTH) {
            stairBase = pos.offset(width - 1, 1, length / 2 - 1);  // Place on the east side of the room
        } else {
            stairBase = pos.offset(width / 2 - 1, 1, length - 1);  // Place on the south side of the room
        }

        // Adjust position to ensure stairs do not start exactly at the corner
        stairBase = adjustStairBase(stairBase, stairFacing, facingIndex);

        for (int i = 0; i < 4; i++) { // Example: 4-step staircase
            world.setBlock(stairBase.above(i), Blocks.STONE_STAIRS.defaultBlockState()
                    .setValue(StairBlock.FACING, stairFacing)
                    .setValue(StairBlock.HALF, Half.BOTTOM), 3);
            stairBase = stairBase.relative(stairFacing);
        }
    }
    private static void placeSpiralStairs(ServerLevel world, BlockPos centerPos, int width, int length, int height, RandomSource random) {
        // Base starting point for the staircase, adjusted for center and elevation
        BlockPos stairBase = centerPos.offset(width / 2 - 1, 0, length / 2 - 1);
        Direction stairFacing = Direction.EAST;  // Start facing east

        stairBase = stairBase.above(1);  // Start one block above the floor

        BlockPos lastStairPos = null;
        BlockPos secondLastStairPos = null;
        BlockPos thirdLastStairPos = null; // Track the position of the last three stairs

        // Direction for ceiling holes, locked to the direction before the final two additional steps
        Direction holeDirection = stairFacing;

        // Increase the height by 2 to meet the ceiling
        int adjustedHeight = height;

        for (int i = 0; i < adjustedHeight; i++) {
            world.setBlock(stairBase.above(i), Blocks.STONE_STAIRS.defaultBlockState()
                    .setValue(StairBlock.FACING, stairFacing)
                    .setValue(StairBlock.HALF, Half.BOTTOM), 3);

            // Update the last three stair positions
            thirdLastStairPos = secondLastStairPos;
            secondLastStairPos = lastStairPos;
            lastStairPos = stairBase.above(i);

            if (i % 2 == 0) { // Rotate every two steps within the 3x2 space, stop rotating at original top
                stairFacing = stairFacing.getClockWise();
            }


            // Move the stairBase in the new facing direction
            stairBase = stairBase.relative(stairFacing);
        }

        // Create an opening in the ceiling directly above the last three stairs using the holeDirection
        if (thirdLastStairPos != null) makeCeilingHole(world, thirdLastStairPos, holeDirection);
        if (secondLastStairPos != null) makeCeilingHole(world, secondLastStairPos, holeDirection);
        if (lastStairPos != null) makeCeilingHole(world, lastStairPos, holeDirection);
    }

    private static void makeCeilingHole(ServerLevel world, BlockPos stairPos, Direction facing) {
        BlockPos holeCenter = stairPos.above();
        var holeDirection = facing;
        if (world.getBlockState(holeCenter).getBlock() != Blocks.STONE_STAIRS){
            world.removeBlock(holeCenter, false);
        }
        if (world.getBlockState(holeCenter.relative(holeDirection.getCounterClockWise())).getBlock() != Blocks.STONE_STAIRS){
            world.removeBlock(holeCenter.relative(holeDirection.getCounterClockWise()), false);
        }
        world.removeBlock(holeCenter.relative(holeDirection.getCounterClockWise()).west(), false);
    }
    private static void placeLadders(ServerLevel world, BlockPos basePos, int height, Direction facing) {
        // Ensure the facing direction is valid for placing ladders
        if (!facing.getAxis().isHorizontal()) {
            facing = Direction.NORTH; // Default to North if an invalid direction is provided
        }

        // Place ladders from the base position up to the specified height
        for (int i = 0; i < height; i++) {
            BlockPos ladderPos = basePos.above(i);
            BlockState ladderState = Blocks.LADDER.defaultBlockState()
                    .setValue(LadderBlock.FACING, facing) // Set the facing of the ladder
                    .setValue(LadderBlock.WATERLOGGED, false); // Ensure the ladder is not waterlogged

            world.setBlock(ladderPos, ladderState, 3);
        }

        // Optionally, ensure there is an opening at the top if there is a ceiling
        BlockPos topOpening = basePos.above(height);
        world.removeBlock(topOpening, false);
        world.removeBlock(topOpening.relative(facing), false); // Clear the block in front of the ladder top
    }


    private static BlockPos adjustStairBase(BlockPos base, Direction facing, int index) {
        // Adjust stair base to avoid placing directly in corner
        switch (facing) {
            case NORTH:
                return index == 0 ? base.west(2) : base;  // Move west if placed on north side
            case SOUTH:
                return index == 1 ? base.east(2) : base;  // Move east if placed on south side
            case EAST:
                return index == 2 ? base.north(2) : base;  // Move north if placed on east side
            case WEST:
                return index == 3 ? base.south(2) : base;  // Move south if placed on west side
            default:
                return base;
        }
    }


    private static Set<BlockPos> generateRoom(ServerLevel world, BlockPos pos, int width, int length, int height, Block floorBlock, Block wallBlock, Block roofBlock, Set<BlockPos> overlapWalls) {
        Set<BlockPos> wallPositions = new HashSet<>();

        // Generate the boundaries: floor, walls, and roof
        generateWalls(world, pos, width, length, height, wallBlock, wallPositions, overlapWalls);
        generateFloor(world, pos, width, length, floorBlock, wallPositions);
        generateRoof(world, pos, width, length, height, roofBlock);

        // Fill the room interior with air, ensuring it does not replace the boundaries
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
    private static void addWallBlock(ServerLevel world, BlockPos pos, Block block, Set<BlockPos> wallPositions, Set<BlockPos> overlapWalls) {
        if (overlapWalls != null && overlapWalls.contains(pos) && isSharedWall(pos, overlapWalls, world)) {
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else if (world.getBlockState(pos).isAir()){
                world.setBlock(pos, block.defaultBlockState(), 3);
                wallPositions.add(pos);
        }
    }
    private static void generateRoof(ServerLevel world, BlockPos pos, int width, int length, int height, Block roofBlock) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                world.setBlock(pos.offset(x, height + 1, z), roofBlock.defaultBlockState(), 3);
            }
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