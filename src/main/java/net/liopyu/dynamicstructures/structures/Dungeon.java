package net.liopyu.dynamicstructures.structures;

import com.google.gson.JsonObject;
import net.liopyu.dynamicstructures.data.enums.KeyWordType;
import net.liopyu.dynamicstructures.data.json.BlockInterpreter;
import net.liopyu.dynamicstructures.data.enums.BlockType;
import net.liopyu.dynamicstructures.util.ContextUtils;
import net.liopyu.dynamicstructures.util.DSHelperClass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

import static net.liopyu.dynamicstructures.util.DSHelperClass.normalizeJson;

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
    protected static int defaultDoorwayRadius;
    public final ContextUtils.StructureContext structureContext;
    public final ServerLevel level;
    public Block floorBlock;
    public Block wallBlock;
    public Block roofBlock;
    public Block centerBlock;
    public Block doorwayBlock;
    public Block fillerBlock;
    public ContextUtils.WeightedBlock floorBlockW;
    public ContextUtils.WeightedBlock wallBlockW;
    public ContextUtils.WeightedBlock roofBlockW;
    public ContextUtils.WeightedBlock centerBlockW;
    public ContextUtils.WeightedBlock doorwayBlockW;
    public ContextUtils.WeightedBlock fillerBlockW;
    public int maxSpawners;
    public int maxSpawnersPerRoom;
    public int currentSpawners;
    public List<EntityType<?>> potentialSpawns;
    public boolean generatesSpawners;
    public int sizeThreshold;
    public int baseLength;
    public int baseWidth;
    public int height;
    public int roomCount;
    public float ladderRoomChance;
    public int width;
    public int length;

    public Dungeon(ContextUtils.StructureContext structureContext, ServerLevel level) {
        this.structureContext = structureContext;
        this.level = level;
        var random = level.random;
        var blockContext = new ContextUtils.BlockContext(structureContext.normalizedJson);
        structureContext.setBlockContext(blockContext);
        blockContext.setLevel(level);
        roofBlockW = blockContext.roofBlock;
        wallBlockW = blockContext.wallBlock;
        floorBlockW = blockContext.floorBlock;
        fillerBlockW = blockContext.fillerBlock;
        doorwayBlockW = blockContext.doorwayBlock;
        centerBlockW = blockContext.centerBlock;
        defaultDoorwayRadius = blockContext.defaultDoorwayRadius;
        if (roofBlockW == null) {
            roofBlockW = new ContextUtils.WeightedBlock(selectRandomBlock(Arrays.stream(ROOF_BLOCKS).toList(), random), 1, null, BlockType.ROOF);
        }
        if (centerBlockW == null) {
            centerBlockW = new ContextUtils.WeightedBlock(selectRandomBlock(Arrays.stream(CENTER_BLOCKS).toList(), random), 1, null, BlockType.CENTER);
        }
        if (doorwayBlockW == null) {
            doorwayBlockW = new ContextUtils.WeightedBlock(selectRandomBlock(Arrays.stream(DOORWAY_BLOCKS).toList(), random), 1, null, BlockType.DOOR);
        }
        if (fillerBlockW == null) {
            fillerBlockW = new ContextUtils.WeightedBlock(selectRandomBlock(Arrays.stream(FILLER_BLOCKS).toList(), random), 1, null, BlockType.FILLER);
        }
        if (floorBlockW == null) {
            floorBlockW = new ContextUtils.WeightedBlock(selectRandomBlock(Arrays.stream(FLOOR_BLOCKS).toList(), random), 1, null, BlockType.FLOOR);
        }
        if (wallBlockW == null) {
            wallBlockW = new ContextUtils.WeightedBlock(selectRandomBlock(Arrays.stream(WALL_BLOCKS).toList(), random), 1, null, BlockType.WALLS);
        }

        blockContext.roofBlock = roofBlockW;
        blockContext.wallBlock = wallBlockW;
        blockContext.centerBlock = centerBlockW;
        blockContext.doorwayBlock = doorwayBlockW;
        blockContext.fillerBlock = fillerBlockW;
        blockContext.floorBlock = floorBlockW;

        roofBlock = blockContext.roofBlock.block;
        wallBlock = blockContext.wallBlock.block;
        floorBlock = blockContext.floorBlock.block;
        fillerBlock = blockContext.fillerBlock.block;
        doorwayBlock = blockContext.doorwayBlock.block;
        centerBlock = blockContext.centerBlock.block;
    }

    public int getRandomSize(RandomSource random) {
        int variation = (int) (baseWidth * (sizeThreshold / 100.0));
        return baseWidth + random.nextInt(variation * 2 + 1) - variation;
    }


    public RandomSource getRandom() {
        return level.random;
    }


    private void placeSpawners(BlockPos pos, RandomSource random) {
        if (currentSpawners >= maxSpawners) {
            return;
        }

        for (int i = 0; i < maxSpawnersPerRoom; i++) {
            int x = pos.getX() + random.nextInt(width);
            int y = pos.getY() + random.nextInt(height);
            int z = pos.getZ() + random.nextInt(length);
            BlockPos spawnerPos = new BlockPos(x, y, z);
            EntityType<?> entityType = potentialSpawns.get(random.nextInt(potentialSpawns.size()));
            setBlock(spawnerPos, Blocks.SPAWNER.defaultBlockState(), 3, BlockType.CENTER);
            BlockEntity blockEntity = level.getBlockEntity(spawnerPos);
            if (blockEntity instanceof SpawnerBlockEntity spawnerEntity) {
                spawnerEntity.getSpawner().setEntityId(entityType, level, level.random, spawnerPos);
                currentSpawners++;
            }
        }
    }

    public void setBlock(BlockPos pPos, ContextUtils.WeightedBlock weightedBlock, int pFlags) {
        var pNewState = weightedBlock.block.defaultBlockState();
        var blockType = weightedBlock.blockType;
        getStructureContext().getBlockContext().setPos(pPos);
        var blockKey = getLevel().registryAccess().registryOrThrow(ForgeRegistries.BLOCKS.getRegistryKey());
        var blockName = blockKey.getKey(pNewState.getBlock()).toString();
        boolean placeBlock = BlockInterpreter.evaluateConditions(pPos, blockName, blockType, getStructureContext().getBlockContext());
        if (placeBlock) {
            var finalBlock = BlockInterpreter.handleBlockPlacement(pPos, blockType, pNewState, getStructureContext().getBlockContext());
            this.getLevel().setBlock(pPos, finalBlock, pFlags);
        }
    }

    public void setBlock(BlockPos pPos, BlockState pNewState, int pFlags, BlockType blockType) {
        getStructureContext().getBlockContext().setPos(pPos);
        var blockContext = getStructureContext().getBlockContext();
        var blockKey = level.registryAccess().registryOrThrow(ForgeRegistries.BLOCKS.getRegistryKey());
        var blockName = blockKey.getKey(pNewState.getBlock()).toString();
        boolean placeBlock = BlockInterpreter.evaluateConditions(pPos, blockName, blockType, blockContext);
        if (placeBlock) {
            var finalBlock = BlockInterpreter.handleBlockPlacement(pPos, blockType, pNewState, blockContext);
            level.setBlock(pPos, finalBlock, pFlags);
        }
    }

    public void generateLadderRoom(BlockPos basePos, Direction currentDirection, Set<BlockPos> overlapWalls) {
        Set<BlockPos> roomWalls = generateRoom(basePos, overlapWalls, true);

        Direction ladderFacing = Direction.EAST;
        BlockPos ladderBase = basePos.offset(width / 2 - 1, 1, length / 2 - 1);

        for (int i = 0; i < height; i++) {
            BlockPos ladderPos = ladderBase.above(i + 1);
            BlockState ladderState = Blocks.LADDER.defaultBlockState()
                    .setValue(LadderBlock.FACING, ladderFacing)
                    .setValue(LadderBlock.WATERLOGGED, false);
            setBlock(ladderPos, ladderState, 3, BlockType.CENTER);
        }

        BlockPos topRoomPos = basePos.above(height + 1);
        Set<BlockPos> upperRoomWalls = generateRoom(topRoomPos, roomWalls, false);

        BlockPos opening = ladderBase.above(height + 1);
        setBlock(opening, Blocks.AIR.defaultBlockState(), 3, BlockType.CENTER);
        placeDoorway(basePos, currentDirection, level.random);

    }


    /*private Set<BlockPos> generateRoom(ServerLevel world, BlockPos pos, int width, int length, int height, Block floorBlock, Block wallBlock, Block roofBlock, Set<BlockPos> overlapWalls, boolean isBottomRoom) {
        Set<BlockPos> wallPositions = new HashSet<>();
        Set<BlockPos> roofPositions = isBottomRoom ? null : generateRoof(world, pos, width, length, height, roofBlock);

        generateWalls(world, pos, width, length, height, wallBlock, wallPositions, overlapWalls, roofPositions);
        generateFloor(world, pos, width, length, floorBlock, wallPositions);

        fillRoomInteriorWithAir(world, pos, width, length, height, wallPositions);
        placeDoorway(level, pos, width, length, currentDirection, random, defaultDoorwayRadius);

        return wallPositions;
    }*/
    public Set<BlockPos> roofPositions;
    public Set<BlockPos> wallPositions;

    private Set<BlockPos> generateRoom(BlockPos pos, Set<BlockPos> overlapWalls, boolean isBottomRoom) {
        Set<BlockPos> wallPositions = new HashSet<>();
        Set<BlockPos> roofPositions = isBottomRoom ? null : generateRoof(pos);

        generateWalls(pos, wallPositions, overlapWalls, roofPositions);
        generateFloor(pos, wallPositions);

        fillRoomInteriorWithAir(pos, wallPositions);

        return wallPositions;
    }


    private void fillRoomInteriorWithAir(BlockPos pos, Set<BlockPos> wallPositions) {
        for (int x = 1; x < width - 1; x++) {
            for (int z = 1; z < length - 1; z++) {
                for (int y = 1; y <= height; y++) {
                    BlockPos blockPos = pos.offset(x, y, z);
                    if (!wallPositions.contains(blockPos)) {
                        setBlock(blockPos, fillerBlockW, 3);
                    }
                }
            }
        }
    }


    private void generateFloor(BlockPos pos, Set<BlockPos> wallPositions) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos floorPos = pos.offset(x, 0, z);
                if (level.getBlockState(floorPos).getBlock() instanceof LadderBlock) {
                    setBlock(floorPos, Blocks.AIR.defaultBlockState(), 3, BlockType.FLOOR);
                } else if (!wallPositions.contains(floorPos)) {
                    setBlock(floorPos, floorBlockW, 3);
                }
            }
        }
    }

    private void generateWalls(BlockPos pos, Set<BlockPos> wallPositions, Set<BlockPos> overlapWalls, Set<BlockPos> roofPositions) {
        for (int y = 1; y <= height + 1; y++) {
            for (int x = 0; x < width; x++) {
                BlockPos wallPos1 = pos.offset(x, y, 0);
                BlockPos wallPos2 = pos.offset(x, y, length - 1);
                addWallBlock(wallPos1, wallPositions, overlapWalls, roofPositions);
                addWallBlock(wallPos2, wallPositions, overlapWalls, roofPositions);
            }
            for (int z = 1; z < length - 1; z++) {
                BlockPos wallPos1 = pos.offset(0, y, z);
                BlockPos wallPos2 = pos.offset(width - 1, y, z);
                addWallBlock(wallPos1, wallPositions, overlapWalls, roofPositions);
                addWallBlock(wallPos2, wallPositions, overlapWalls, roofPositions);
            }
        }
    }

    private void addWallBlock(BlockPos pos, Set<BlockPos> wallPositions, Set<BlockPos> overlapWalls, Set<BlockPos> roofPositions) {
        BlockState currentState = level.getBlockState(pos);
        Block currentBlock = currentState.getBlock();

        boolean canReplaceBlock =
                currentBlock instanceof LiquidBlock ||
                        currentState.isAir() ||
                        (roofPositions != null &&
                                roofPositions.contains(pos) &&
                                roofBlock.equals(currentBlock));

        if (canReplaceBlock) {
            setBlock(pos, wallBlockW, 3);
            wallPositions.add(pos);
            return;
        }

        if (overlapWalls != null && overlapWalls.contains(pos)) {
            if (isSharedWall(pos, overlapWalls)) {
                setBlock(pos, Blocks.AIR.defaultBlockState(), 3, BlockType.WALLS);
            } else {
                setBlock(pos, wallBlockW, 3);
                wallPositions.add(pos);
            }
            return;
        }

        // Ensure no adjacent walls
        boolean hasAdjacentWall = false;
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = pos.relative(direction);
            if (wallPositions.contains(adjacentPos)) {
                hasAdjacentWall = true;
                break;
            }
        }

        // Only place the wall block if there's no adjacent wall
        if (!hasAdjacentWall) {
            setBlock(pos, wallBlockW, 3);
            wallPositions.add(pos);
        }
    }
  /*  private void addWallBlock(BlockPos pos, Block block, Set<BlockPos> wallPositions, Set<BlockPos> overlapWalls, boolean forceOverlap) {
        if (overlapWalls != null && overlapWalls.contains(pos) && isSharedWall(pos, overlapWalls)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else {
            level.setBlock(pos, block.defaultBlockState(), 3);
            wallPositions.add(pos);
        }
    }*/


    private Set<BlockPos> generateRoof(BlockPos pos) {
        Set<BlockPos> roofPositions = new HashSet<>();
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos roofPos = pos.offset(x, height + 1, z);
                if (level.getBlockState(roofPos).getBlock() instanceof LiquidBlock ||
                        level.getBlockState(roofPos).isAir() ||
                        level.getBlockState(roofPos).is(Blocks.CAVE_AIR) ||
                        wallBlock.equals(level.getBlockState(roofPos).getBlock())) {

                    setBlock(roofPos, roofBlockW, 3);
                    roofPositions.add(roofPos);
                }
            }
        }
        return roofPositions;
    }


    private boolean isSharedWall(BlockPos pos, Set<BlockPos> overlapWalls) {
        return (overlapWalls.contains(pos.north()) && overlapWalls.contains(pos.south())) ||
                (overlapWalls.contains(pos.east()) && overlapWalls.contains(pos.west()));
    }

    private void placeDoorway(BlockPos pos, Direction direction, RandomSource random) {
        placeSingleDoor(pos, direction, random);
        placeSingleDoor(pos, direction.getOpposite(), random);
        placeSingleDoor(pos, Direction.EAST, random);
        placeSingleDoor(pos, Direction.WEST, random);
    }

    private void placeSingleDoor(BlockPos pos, Direction direction, RandomSource random) {
        int offset = random.nextInt(3) - 1;

        BlockPos doorPosBottom1 = pos;
        BlockPos outwardPos;
        switch (direction) {
            case NORTH, SOUTH -> {
                int doorX = (width / 2) + offset;
                doorPosBottom1 = pos.offset(doorX, 1, direction == Direction.NORTH ? 0 : length - 1);
                outwardPos = doorPosBottom1.relative(direction == Direction.NORTH ? Direction.NORTH : Direction.SOUTH);
                for (int i = -defaultDoorwayRadius; i <= defaultDoorwayRadius; i++) {
                    for (int j = -defaultDoorwayRadius; j <= defaultDoorwayRadius; j++) {
                        BlockPos doorPos = doorPosBottom1.offset(0, 0, j);
                        BlockPos doorPosTop = doorPos.above();
                        if (roofBlock.equals(level.getBlockState(outwardPos).getBlock())) {
                            return;
                        }
                        if (!level.getBlockState(doorPos).is(Blocks.LADDER)) {
                            setBlock(doorPos, doorwayBlockW, 3);
                        }
                        if (!level.getBlockState(doorPosTop).is(Blocks.LADDER)) {
                            setBlock(doorPosTop, doorwayBlockW, 3);
                        }
                    }
                }
            }
            case EAST, WEST -> {
                int doorZ = (length / 2) + offset;
                doorPosBottom1 = pos.offset(direction == Direction.WEST ? 0 : width - 1, 1, doorZ);
                outwardPos = doorPosBottom1.relative(direction == Direction.WEST ? Direction.WEST : Direction.EAST);
                for (int i = -defaultDoorwayRadius; i <= defaultDoorwayRadius; i++) {
                    BlockPos doorPos = doorPosBottom1.offset(0, 0, i);
                    BlockPos doorPosTop = doorPos.above();
                    if (roofBlock.equals(level.getBlockState(outwardPos).getBlock())) {
                        return;
                    }
                    setBlock(doorPos, doorwayBlockW, 3);
                    setBlock(doorPosTop, doorwayBlockW, 3);
                }
            }
            default -> {
                DSHelperClass.logErrorMessage("Unexpected direction: " + direction);
                return;
            }
        }
    }

    private BlockPos calculateNextRoomPos(BlockPos pos, Direction direction) {
        return switch (direction) {
            case NORTH -> pos.offset(0, 0, -length + 1);
            case SOUTH -> pos.offset(0, 0, length - 1);
            case WEST -> pos.offset(-width + 1, 0, 0);
            case EAST -> pos.offset(width - 1, 0, 0);
            default -> throw new IllegalStateException("Unexpected value: " + direction);
        };
    }

    private Block selectRandomBlock(List<Block> blocks, RandomSource random) {
        return blocks.get(random.nextInt(blocks.size()));
    }


    public ContextUtils.StructureContext getStructureContext() {
        return structureContext;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public BlockPos upperRoomPos;

    public void generateDungeon() {
        ladderRoomChance = structureContext.getLadderRoomChance();
        roomCount = structureContext.getRoomCount();
        height = structureContext.getHeight();
        baseWidth = structureContext.getWidth();
        baseLength = structureContext.getLength();
        sizeThreshold = structureContext.getSizeThreshold();
        generatesSpawners = structureContext.isGeneratesSpawners();
        maxSpawners = structureContext.getMaxSpawners();
        maxSpawnersPerRoom = structureContext.getMaxSpawnersPerRoom();
        potentialSpawns = structureContext.getPotentialSpawns();
        Set<BlockPos> previousRoomWalls = null;
        var startPos = structureContext.getStartPos();
        var startDirection = structureContext.getStartDirection();
        var currentPos = startPos;
        var currentDirection = startDirection;
        var random = level.random;
        for (int i = 0; i < roomCount; i++) {
            width = getRandomSize(random);
            length = getRandomSize(random);
            boolean isLadderRoom = random.nextInt(100) < ladderRoomChance;


            if (isLadderRoom) {
                generateLadderRoom(currentPos, currentDirection, previousRoomWalls);
                placeDoorway(currentPos, currentDirection, random);
                BlockPos upperRoomPos = currentPos.above(height + 1);
                placeDoorway(upperRoomPos, currentDirection, random);
                if (generatesSpawners) {
                    placeSpawners(currentPos, random);
                    placeSpawners(upperRoomPos, random);
                }
                currentPos = calculateNextRoomPos(upperRoomPos, currentDirection);
            } else {
                Set<BlockPos> currentRoomWalls = generateRoom(currentPos, previousRoomWalls, false);
                previousRoomWalls = currentRoomWalls;
                placeDoorway(currentPos, currentDirection, random);
                if (generatesSpawners) {
                    placeSpawners(currentPos, random);
                }
                currentPos = calculateNextRoomPos(currentPos, currentDirection);
            }

            currentDirection = random.nextBoolean() ? currentDirection.getClockWise() : currentDirection.getCounterClockWise();
        }
    }
}