package net.liopyu.dynamicstructures.structures;

import net.liopyu.dynamicstructures.data.enums.BlockType;
import net.liopyu.dynamicstructures.data.json.BlockInterpreter;
import net.liopyu.dynamicstructures.util.ContextUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;

public class Tower {
    public final ContextUtils.StructureContext structureContext;
    public final ServerLevel level;
    public int floorCount;
    public int heightPerFloor;
    public int width;
    public int length;

    public ContextUtils.WeightedBlock wallBlock;
    public ContextUtils.WeightedBlock floorBlock;
    public ContextUtils.WeightedBlock roofBlock;
    public ContextUtils.WeightedBlock stairBlock;
    public Direction currentDirection;

    public Tower(ContextUtils.StructureContext structureContext, ServerLevel level) {
        this.structureContext = structureContext;
        this.level = level;
        this.structureContext.level = level;
        this.currentDirection = structureContext.getStartDirection();

        var blockContext = new ContextUtils.BlockContext(structureContext);
        this.floorBlock = blockContext.floorBlock != null ? blockContext.floorBlock : new ContextUtils.WeightedBlock(Blocks.STONE_BRICKS, 1, null, BlockType.FLOOR);
        this.wallBlock = blockContext.wallBlock != null ? blockContext.wallBlock : new ContextUtils.WeightedBlock(Blocks.STONE_BRICKS, 1, null, BlockType.WALLS);
        this.roofBlock = blockContext.roofBlock != null ? blockContext.roofBlock : new ContextUtils.WeightedBlock(Blocks.STONE_SLAB, 1, null, BlockType.ROOF);
        this.stairBlock = blockContext.stairBlock != null ? blockContext.stairBlock : new ContextUtils.WeightedBlock(Blocks.STONE_STAIRS, 1, null, BlockType.STAIRS);

        this.floorCount = structureContext.getRoomCount();
        this.heightPerFloor = structureContext.getHeight();
        this.width = structureContext.getWidth();
        this.length = structureContext.getLength();
    }

    public void generateTower() {
        BlockPos currentPos = structureContext.getStartPos();
        Set<BlockPos> wallPositions = new HashSet<>();
        Set<BlockPos> stairPositions = new HashSet<>();
        for (int i = 0; i < floorCount; i++) {
            boolean isLastFloor = (i == floorCount - 1);
            boolean generateStaircase = (i % 2 == 0);

            generateRoom(currentPos, wallPositions, isLastFloor, generateStaircase, stairPositions);

            // Move the position up for the next floor
            currentPos = currentPos.above(heightPerFloor);
        }
    }

    private void generateRoom(BlockPos pos, Set<BlockPos> wallPositions, boolean isLastFloor, boolean generateStaircase, Set<BlockPos> stairPositions) {

        if (generateStaircase) {
            int totalHeight = floorCount * heightPerFloor;
            BlockPos staircaseStartPos = pos.offset(width / 2, 1, length / 2);
            generateCentralStaircase(staircaseStartPos, totalHeight, stairPositions);
        }
        generateFloor(pos, wallPositions, stairPositions);
        generateWalls(pos, wallPositions);

        if (isLastFloor) {
            generateRoof(pos.above(heightPerFloor));
        }
    }

    private void generateFloor(BlockPos pos, Set<BlockPos> wallPositions, Set<BlockPos> stairPositions) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos floorPos = pos.offset(x, 0, z);
                if (!wallPositions.contains(floorPos) && !level.getBlockState(floorPos).is(stairBlock.block)) {
                    if (!stairPositions.contains(floorPos)) {
                        setBlock(floorPos, floorBlock, 3);
                    }
                }
            }
        }

    }

    private void generateWalls(BlockPos pos, Set<BlockPos> wallPositions) {
        for (int y = 1; y <= heightPerFloor; y++) {
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

    private void generateCentralStaircase(BlockPos startPos, int totalHeight, Set<BlockPos> stairPositions) {
        BlockPos currentPos = startPos;
        int currentHeight = 0;
        int columnRadius = (4 - 1) / 2;

        while (currentHeight < heightPerFloor) {
            placeColumn(currentPos.below(currentHeight), currentDirection, columnRadius, heightPerFloor);

            for (int j = 0; j < 4; j++) {
                placeStairBlock(currentPos, currentDirection, stairPositions);

                currentPos = currentPos.relative(currentDirection).above();
                currentHeight++;

                if (currentHeight >= totalHeight) {
                    return;
                }
            }

            currentPos = currentPos.relative(currentDirection.getCounterClockWise()).relative(currentDirection.getOpposite());
            currentDirection = currentDirection.getCounterClockWise();
            if (currentHeight <= heightPerFloor - 1) {
                placeStairBlock(currentPos, currentDirection, stairPositions);

            }
        }
    }

    private void placeColumn(BlockPos basePos, Direction currentDirection, int radius, int roomHeight) {
        for (int y = 0; y < roomHeight; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos columnPos = basePos.offset(x, y, z);

                    placeColumnBlock(columnPos.relative(currentDirection)
                            .relative(currentDirection.getCounterClockWise())
                            .relative(currentDirection.getCounterClockWise()));
                }
            }
        }
    }


    private void placeColumnBlock(BlockPos pos) {
        BlockState cobblestoneState = Blocks.COBBLESTONE.defaultBlockState();
        level.setBlock(pos, cobblestoneState, 3);
    }


    private void placeStairBlock(BlockPos pos, Direction direction, Set<BlockPos> stairPositions) {
        BlockState stairState = stairBlock.block.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
        setBlock(pos, stairState, 3, BlockType.STAIRS);
        stairPositions.add(pos);
        for (int x = 0; x < 3; x++) {
            BlockPos pos2 = pos.offset(0, x + 1, 0);
            if (!level.getBlockState(pos2).is(stairBlock.block)) {
                level.setBlock(pos2, Blocks.AIR.defaultBlockState(), 3);
                stairPositions.add(pos2);
            }

        }
    }

    public ContextUtils.StructureContext getStructureContext() {
        return structureContext;
    }
}
