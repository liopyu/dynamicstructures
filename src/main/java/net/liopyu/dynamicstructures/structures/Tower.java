package net.liopyu.dynamicstructures.structures;

import net.liopyu.dynamicstructures.data.enums.BlockType;
import net.liopyu.dynamicstructures.data.json.BlockInterpreter;
import net.liopyu.dynamicstructures.structures.rooms.Room;
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

public class Tower extends BaseStructure {
    public int floorCount;


    public Tower(ContextUtils.StructureContext structureContext, ServerLevel level) {
        super(structureContext, level);
        this.structureContext.level = level;

        this.floorCount = structureContext.getRoomCount();


        preCalculateRooms();
    }


    public void generate() {
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


    public void preCalculateRooms() {
        for (int i = 0; i < floorCount; i++) {
            boolean generateStaircase = true;
            roomContexts.add(new Room(i, startPos.offset(0, height * i, 0), false, generateStaircase, height, length, width));
        }
    }

    public BlockPos topPos;

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
                if (!wallPositions.contains(floorPos) && !level.getBlockState(floorPos).is(stairBlockW.block)) {
                    for (int y = 0; y < 5; y++) {
                        var openPos = floorPos.offset(0, -y, 0);
                        if (stairPositions.contains(openPos) && level.getBlockState(openPos).is(stairBlockW.block)) {
                            floorOpening.add(floorPos);
                        }
                    }
                    setBlock(floorPos, floorBlockW, 3);
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
                    if (level.getBlockState(b).is(stairBlockW.block)) {
                        stairRoof.add(roofPos);
                    }
                }
                if (isStairRoom(roomNumber) && !stairRoof.contains(roofPos)) {
                    setBlock(roofPos, roofBlockW, 3);
                }
            }
        }
    }

    private void addWallBlock(BlockPos pos, Set<BlockPos> wallPositions) {
        setBlock(pos, wallBlockW, 3);
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
                    placeStairBlock(currentPos, direction, stairPositions, currentHeight, revisedHeightPerFloor, openPositions, roomNumber, j);
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
                blockColumnShell.add(currentPos.relative(direction.getClockWise()).offset(0, x, 0));
                blockColumnShell.add(currentPos.relative(direction.getClockWise()).offset(0, -x, 0));
                blockColumnShell.add(currentPos.relative(direction.getClockWise()).relative(direction.getCounterClockWise()).offset(0, x, 0));
                blockColumnShell.add(currentPos.relative(direction.getClockWise()).relative(direction.getCounterClockWise()).offset(0, -x, 0));
            }

            currentPos = currentPos.relative(direction.getCounterClockWise()).relative(direction.getOpposite());
            direction = direction.getCounterClockWise();

        }
        for (BlockPos pos : blockColumnShell) {
            if (!openPositions.contains(pos)) {
                if (pos.getY() > this.startPos.getY() &&
                        pos.getY() < topPos.getY() &&
                        !level.getBlockState(pos).is(floorBlockW.block)
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
                openPositions.add(openingPos.above());
                openPositions.add(openingPos.above(2));
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


    private void placeStairBlock(BlockPos pos, Direction direction, Set<BlockPos> stairPositions, int currentHeight, int finalHeight, Set<BlockPos> openPositions, int roomNumber, int j) {
        BlockState stairState = stairBlockW.block.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
        if (currentHeight < finalHeight - 1) {
            setBlock(pos, stairState, 3, BlockType.STAIRS);
            stairPositions.add(pos);
        }

        for (int x = 0; x < finalHeight; x++) {
            BlockPos pos2 = pos.offset(0, x + 1, 0);
            if (!level.getBlockState(pos2).is(stairBlockW.block) && currentHeight < finalHeight) {
                level.setBlock(pos2, Blocks.AIR.defaultBlockState(), 3);
                stairPositions.add(pos2);
            }
        }
        if (currentHeight % getRoomContext(roomNumber).height == 0) {
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
