package net.liopyu.dynamicstructures.structures;

import net.liopyu.dynamicstructures.data.enums.BlockType;
import net.liopyu.dynamicstructures.data.json.BlockInterpreter;
import net.liopyu.dynamicstructures.util.ContextUtils;
import net.liopyu.dynamicstructures.util.DSHelperClass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class Tower {
    public final ContextUtils.StructureContext structureContext;
    public final ServerLevel level;
    public int floorCount;
    public int height;
    public int width;
    public int length;

    public ContextUtils.WeightedBlock wallBlock;
    public ContextUtils.WeightedBlock floorBlock;
    public ContextUtils.WeightedBlock roofBlock;
    public ContextUtils.WeightedBlock stairBlock;
    public Direction currentDirection;
    public int staircaseRadius;
    public int staircaseFactor;
    public List<ContextUtils.RoomContext> roomContexts = new ArrayList<>();

    public Tower(ContextUtils.StructureContext structureContext, ServerLevel level) {
        this.structureContext = structureContext;
        this.level = level;
        this.structureContext.level = level;
        this.currentDirection = structureContext.getStartDirection();
        this.staircaseFactor = structureContext.getStaircaseFactor();
        var blockContext = new ContextUtils.BlockContext(structureContext);
        this.floorBlock = blockContext.floorBlock != null ? blockContext.floorBlock : new ContextUtils.WeightedBlock(Blocks.STONE_BRICKS, 1, null, BlockType.FLOOR);
        this.wallBlock = blockContext.wallBlock != null ? blockContext.wallBlock : new ContextUtils.WeightedBlock(Blocks.STONE_BRICKS, 1, null, BlockType.WALLS);
        this.roofBlock = blockContext.roofBlock != null ? blockContext.roofBlock : new ContextUtils.WeightedBlock(Blocks.STONE_SLAB, 1, null, BlockType.ROOF);
        this.stairBlock = blockContext.stairBlock != null ? blockContext.stairBlock : new ContextUtils.WeightedBlock(Blocks.STONE_STAIRS, 1, null, BlockType.STAIRS);

        this.floorCount = structureContext.getRoomCount();
        this.height = structureContext.getHeight();
        this.width = structureContext.getWidth();
        this.length = structureContext.getLength();

        int originalRadius = structureContext.getStaircaseRadius();
        int minRadius = Math.min(width, length);
        int maxRadius = Math.max(minRadius / this.staircaseFactor, 1);
        this.staircaseRadius = Math.min(originalRadius, maxRadius);

        preCalculateRooms();
    }

    public ContextUtils.RoomContext getRoomContext(int roomNumber) {
        return roomNumber == -1 ? null : roomContexts.get(roomNumber);
    }

    public boolean isStairRoom(int roomNumber) {
        return roomNumber != -1 && roomContexts.get(roomNumber).stairRoom;
    }

    public void generateTower() {
        BlockPos currentPos = structureContext.getStartPos();
        Set<BlockPos> wallPositions = new HashSet<>();
        Set<BlockPos> stairPositions = new HashSet<>();
        Set<BlockPos> blockColumnShell = new HashSet<>();
        for (int i = 0; i < floorCount; i++) {
            boolean isLastFloor = (i == floorCount - 1);
            boolean generateStaircase = isStairRoom(i);

            generateRoom(currentPos, wallPositions, isLastFloor, generateStaircase, stairPositions, i, blockColumnShell);

            currentPos = currentPos.above(height);
        }
    }


    private void preCalculateRooms() {
        for (int i = 0; i < floorCount; i++) {
            boolean generateStaircase = true;//(i % 2 == 0); // Example condition
            if (generateStaircase) {
                roomContexts.add(new ContextUtils.RoomContext(i, false, generateStaircase, height, length, width));
            }
        }
    }

    private void generateRoom(BlockPos pos, Set<BlockPos> wallPositions, boolean isLastFloor, boolean generateStaircase, Set<BlockPos> stairPositions, int roomNumber, Set<BlockPos> blockColumnShell) {

        if (generateStaircase) {
            int maxHeight = height * floorCount;
            BlockPos staircaseStartPos = pos.offset(width / 2, 1, length / 2);

            generateCentralStaircase(staircaseStartPos, maxHeight, stairPositions, roomNumber, blockColumnShell);
        }
        generateFloor(pos, wallPositions, stairPositions);
        generateWalls(pos, wallPositions);

        if (isLastFloor) {
            generateRoof(pos.above(height));
        }
    }

    private void generateFloor(BlockPos pos, Set<BlockPos> wallPositions, Set<BlockPos> stairPositions) {
        Set<BlockPos> floorOpening = new HashSet<>();
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos floorPos = pos.offset(x, 0, z);
                if (!wallPositions.contains(floorPos) && !level.getBlockState(floorPos).is(stairBlock.block)) {
                    for (int y = 0; y < 5; y++) {
                        var openPos = floorPos.offset(0, -y, 0);
                        if (stairPositions.contains(openPos) && level.getBlockState(openPos).is(stairBlock.block)) {
                            floorOpening.add(floorPos);
                        }
                    }
                    setBlock(floorPos, floorBlock, 3);
                    if (floorOpening.contains(floorPos)) {
                        setBlock(floorPos, Blocks.AIR.defaultBlockState(), 3, BlockType.FLOOR);
                    }
                }
            }
        }

    }

    private void generateWalls(BlockPos pos, Set<BlockPos> wallPositions) {
        for (int y = 1; y <= height; y++) {
            for (int x = 0; x < width; x++) {
                BlockPos wallPos1 = pos.offset(x, y, 0);
                BlockPos wallPos2 = pos.offset(x, y, length - 1);
                addWallBlock(wallPos1, wallPositions);
                addWallBlock(wallPos2, wallPositions);
            }
            for (int z = 1; z < length - 1; z++) {
                BlockPos wallPos1 = pos.offset(0, y, z);
                BlockPos wallPos2 = pos.offset(width - 1, y, z);
                addWallBlock(wallPos1, wallPositions);
                addWallBlock(wallPos2, wallPositions);
            }
        }
    }

    private void generateRoof(BlockPos pos) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos roofPos = pos.offset(x, 0, z);
                setBlock(roofPos, roofBlock, 3);
            }
        }
    }

    private void addWallBlock(BlockPos pos, Set<BlockPos> wallPositions) {
        setBlock(pos, wallBlock, 3);
        wallPositions.add(pos);
    }

    public ServerLevel getLevel() {
        return level;
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


    private void generateCentralStaircase(BlockPos startPos, int maxHeight, Set<BlockPos> stairPositions, int roomNumber, Set<BlockPos> blockColumnShell) {
        BlockPos currentPos = startPos;
        int currentHeight = 0;
        int currentFloorNumber = 0;
        List<BlockPos> turnPoints = new ArrayList<>();
        var direction = currentDirection;
        var initialDirection = currentDirection;
        var roomContext = getRoomContext(roomNumber);
        var previousRoomStairs = isStairRoom(roomNumber - 1);
        roomContext.setConnectedToBottomStairs(previousRoomStairs && roomContext.stairRoom);
        var isConnectedToBottomStairs = roomContext.connectedToBottomStairs;
        Set<BlockPos> openPositions = new HashSet<>();

        int revisedHeightPerFloor = calculateRevisedHeight(roomNumber);


        while (currentHeight < revisedHeightPerFloor) {
            for (int j = 0; j < staircaseRadius; j++) {
                if (currentHeight < revisedHeightPerFloor && !isConnectedToBottomStairs) {
                    placeStairBlock(currentPos, direction, stairPositions, currentHeight, revisedHeightPerFloor, currentFloorNumber, roomContext, openPositions, blockColumnShell);
                }
                for (int x = 0; x < revisedHeightPerFloor; x++) {
                    if (currentHeight < revisedHeightPerFloor) {
                        BlockPos pos1 = currentPos.offset(0, x, 0);
                        blockColumnShell.add(pos1.relative(direction.getClockWise()));
                    }
                    if (currentPos.getY() > startPos.getY()) {
                        BlockPos pos2 = currentPos.offset(0, -x, 0);
                        blockColumnShell.add(pos2.relative(direction.getClockWise()));
                    }

                }
                currentPos = currentPos.relative(direction).above();
                currentHeight++;


                if (currentHeight > maxHeight && !isConnectedToBottomStairs) {
                    return;
                }
            }

            if (currentHeight < revisedHeightPerFloor && !isConnectedToBottomStairs) {
                turnPoints.add(currentPos);
            }
            for (int x = 0; x < revisedHeightPerFloor; x++) {

                var newpos = currentPos.relative(direction.getClockWise()).offset(0, x, 0);
                var newpos2 = currentPos.relative(direction.getClockWise()).offset(0, -x, 0);
                var newpos3 = currentPos.relative(direction.getClockWise()).relative(direction.getCounterClockWise()).offset(0, x, 0);
                var newpos4 = currentPos.relative(direction.getClockWise()).relative(direction.getCounterClockWise()).offset(0, -x, 0);
                /*  if (currentHeight % roomContext.height != 0) {*/
                blockColumnShell.add(newpos);
                blockColumnShell.add(newpos2);
                blockColumnShell.add(newpos3);
                blockColumnShell.add(newpos4);
                /*} else {
                    DSHelperClass.logInfoMessageDev("roompos: " + currentPos);
                    setBlock(newpos, Blocks.ACACIA_STAIRS.defaultBlockState(), 3, BlockType.STAIRS);
                }*/


            }

            currentPos = currentPos.relative(direction.getCounterClockWise()).relative(direction.getOpposite());
            direction = direction.getCounterClockWise();

        }
        /*if (!isConnectedToBottomStairs) {
            fillCenterWithCobblestone(startPos, turnPoints, stairPositions, initialDirection.getOpposite(), revisedHeightPerFloor);
        } */

        for (BlockPos pos : blockColumnShell) {
            if (!level.getBlockState(pos).is(Blocks.ACACIA_STAIRS) && !openPositions.contains(pos))
                setBlock(pos, Blocks.BEDROCK.defaultBlockState(), 3, BlockType.STAIRS);
        }
        for (BlockPos pos : openPositions) {
            setBlock(pos, Blocks.ACACIA_STAIRS.defaultBlockState(), 3, BlockType.STAIRS);
        }
    }

    public void createStairOpening(BlockPos initialPos, Direction direction, Set<BlockPos> openPositions) {
        for (int i = 0; i < 3; i++) {
            if (i != 0) {
                var openingPos = initialPos.relative(direction, i);
                openPositions.add(openingPos);
                openPositions.add(openingPos.offset(0, 1, 0));
                openPositions.add(openingPos.offset(0, 2, 0));
            }
        }
    }

    private int calculateRevisedHeight(int startingRoomNumber) {
        int cumulativeHeight = 0;

        for (int i = startingRoomNumber; i < roomContexts.size(); i++) {
            var roomContext = getRoomContext(i);
            if (roomContext.stairRoom) {
                cumulativeHeight += roomContext.height;
            } else {
                break;
            }
        }

        return cumulativeHeight;
    }


    private void fillCenterWithCobblestone(BlockPos initialPos, List<BlockPos> turnPoints, Set<BlockPos> positions, Direction direction, int maxHeight) {
        BlockPos minPos = new BlockPos(
                Math.min(initialPos.getX(), turnPoints.stream().mapToInt(BlockPos::getX).min().orElse(initialPos.getX())),
                initialPos.getY(),
                Math.min(initialPos.getZ(), turnPoints.stream().mapToInt(BlockPos::getZ).min().orElse(initialPos.getZ()))
        );

        BlockPos maxPos = new BlockPos(
                Math.max(initialPos.getX(), turnPoints.stream().mapToInt(BlockPos::getX).max().orElse(initialPos.getX())),
                initialPos.getY() + maxHeight - 2,
                Math.max(initialPos.getZ(), turnPoints.stream().mapToInt(BlockPos::getZ).max().orElse(initialPos.getZ()))
        );

        for (int x = minPos.getX(); x <= maxPos.getX(); x++) {
            for (int y = minPos.getY(); y <= maxPos.getY(); y++) {
                for (int z = minPos.getZ(); z <= maxPos.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).is(stairBlock.block) && !positions.contains(pos)) {
                        placeBlock(pos, Blocks.COBBLESTONE, positions);
                    }
                }
            }
        }

    }

    private void placeBlock(BlockPos pos, Block block, Set<BlockPos> positions) {
        level.setBlock(pos, block.defaultBlockState(), 3);
        positions.add(pos);
    }

    private void placeStairBlock(BlockPos pos, Direction direction, Set<BlockPos> stairPositions, int currentHeight, int finalHeight, int currentFloorNumber, ContextUtils.RoomContext roomContext, Set<BlockPos> openPositions, Set<BlockPos> blockColumnShell) {
        BlockState stairState = stairBlock.block.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
        setBlock(pos, stairState, 3, BlockType.STAIRS);
        stairPositions.add(pos);

        for (int x = 0; x < finalHeight; x++) {
            BlockPos pos2 = pos.offset(0, x + 1, 0);
            if (!level.getBlockState(pos2).is(stairBlock.block) && currentHeight <= finalHeight) {
                level.setBlock(pos2, Blocks.AIR.defaultBlockState(), 3);
                stairPositions.add(pos2);
            }
        }
        if (currentHeight % roomContext.height == 0) {
            createStairOpening(pos, direction.getOpposite(), openPositions);
        }
    }

    public ContextUtils.StructureContext getStructureContext() {
        return structureContext;
    }
}
