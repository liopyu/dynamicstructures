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

import static net.liopyu.dynamicstructures.util.DSHelperClass.logInfoMessageDev;

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
        this.startPos = structureContext.getStartPos();
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
        Set<BlockPos> openPositions = new HashSet<>();
        for (int i = 0; i < floorCount; i++) {
            boolean isLastFloor = (i == floorCount - 1);
            boolean generateStaircase = isStairRoom(i);

            generateRoom(currentPos, wallPositions, isLastFloor, generateStaircase, stairPositions, i, blockColumnShell, openPositions);

            currentPos = currentPos.above(height);
        }
    }


    private void preCalculateRooms() {
        for (int i = 0; i < floorCount; i++) {
            boolean generateStaircase = true;//(i % 2 == 0);
            if (generateStaircase) {
                roomContexts.add(new ContextUtils.RoomContext(i, false, generateStaircase, height, length, width));
            }
        }
    }

    public BlockPos topPos;
    public BlockPos startPos;

    private void generateRoom(BlockPos pos, Set<BlockPos> wallPositions, boolean isLastFloor, boolean generateStaircase, Set<BlockPos> stairPositions, int roomNumber, Set<BlockPos> blockColumnShell, Set<BlockPos> openPositions) {
        int maxHeight = height * floorCount;
        if (roomNumber == 0) {
            topPos = pos.above(maxHeight);
        }

        if (generateStaircase) {

            BlockPos staircaseStartPos = pos.offset(width / 2, 1, length / 2);

            generateCentralStaircase(staircaseStartPos, maxHeight, stairPositions, roomNumber, blockColumnShell, openPositions);
        }
        generateFloor(pos, wallPositions, stairPositions);
        generateWalls(pos, wallPositions);

        if (isLastFloor) {
            generateRoof(topPos, roomNumber);
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

    private void generateRoof(BlockPos pos, int roomNumber) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos roofPos = pos.offset(x, 0, z);
                Set<BlockPos> stairRoof = new HashSet<>();
                for (int y = 0; y < 4; y++) {
                    var b = roofPos.offset(0, -y, 0);
                    if (level.getBlockState(b).is(stairBlock.block)) {
                        stairRoof.add(roofPos);
                    }
                }
                if (isStairRoom(roomNumber) && !stairRoof.contains(roofPos)) {
                    setBlock(roofPos, roofBlock, 3);
                }
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


    private void generateCentralStaircase(BlockPos startPos, int maxHeight, Set<BlockPos> stairPositions, int roomNumber, Set<BlockPos> blockColumnShell, Set<BlockPos> openPositions) {
        BlockPos currentPos = startPos;
        int currentHeight = 0;
        int currentFloorNumber = 0;
        List<BlockPos> turnPoints = new ArrayList<>();
        var direction = currentDirection;
        var roomContext = getRoomContext(roomNumber);
        var previousRoomStairs = isStairRoom(roomNumber - 1);
        roomContext.setConnectedToBottomStairs(previousRoomStairs && roomContext.stairRoom);
        var isConnectedToBottomStairs = roomContext.connectedToBottomStairs;
        int revisedHeightPerFloor = calculateRevisedHeight(roomNumber);


        while (currentHeight < revisedHeightPerFloor) {

            for (int j = 0; j < staircaseRadius; j++) {
                if (currentHeight < revisedHeightPerFloor && !isConnectedToBottomStairs) {
                    placeStairBlock(currentPos, direction, stairPositions, currentHeight, revisedHeightPerFloor, currentFloorNumber, roomContext, openPositions, blockColumnShell, j);
                }

                for (int x = 0; x < revisedHeightPerFloor; x++) {
                    BlockPos pos1 = currentPos.offset(0, x, 0);
                    blockColumnShell.add(pos1.relative(direction.getClockWise()));
                    BlockPos pos2 = currentPos.offset(0, -x, 0);
                    if (currentPos.getY() > this.startPos.getY()) {
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

                blockColumnShell.add(newpos);
                blockColumnShell.add(newpos2);
                blockColumnShell.add(newpos3);
                blockColumnShell.add(newpos4);
            }

            currentPos = currentPos.relative(direction.getCounterClockWise()).relative(direction.getOpposite());
            direction = direction.getCounterClockWise();

        }
        for (BlockPos pos : blockColumnShell) {
            if (!openPositions.contains(pos)) {
                if (pos.getY() > this.startPos.getY() &&
                        pos.getY() < topPos.getY() &&
                        !level.getBlockState(pos).is(floorBlock.block)
                ) {
                    setBlock(pos, Blocks.STONE.defaultBlockState(), 3, BlockType.STAIRS);
                }
            }
        }
        for (BlockPos pos : openPositions) {
            setBlock(pos, Blocks.AIR.defaultBlockState(), 3, BlockType.STAIRS);
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


    private void placeStairBlock(BlockPos pos, Direction direction, Set<BlockPos> stairPositions, int currentHeight, int finalHeight, int currentFloorNumber, ContextUtils.RoomContext roomContext, Set<BlockPos> openPositions, Set<BlockPos> blockColumnShell, int j) {
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
            if (staircaseRadius - j > 1 && currentHeight > 1) {
                createStairOpening(pos.relative(direction.getOpposite()), direction.getClockWise(), openPositions);
            } else {
                createStairOpening(pos, direction.getOpposite(), openPositions);
            }
        }
    }

    public ContextUtils.StructureContext getStructureContext() {
        return structureContext;
    }
}
