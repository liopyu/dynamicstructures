package net.liopyu.dynamicstructures.structures;

import net.liopyu.dynamicstructures.data.enums.BlockType;
import net.liopyu.dynamicstructures.data.json.BlockInterpreter;
import net.liopyu.dynamicstructures.util.ContextUtils;
import net.liopyu.dynamicstructures.util.DSHelperClass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LadderBlock;
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

    // Blocks used for the tower's construction
    public ContextUtils.WeightedBlock wallBlock;
    public ContextUtils.WeightedBlock floorBlock;
    public ContextUtils.WeightedBlock roofBlock;

    public Tower(ContextUtils.StructureContext structureContext, ServerLevel level) {
        this.structureContext = structureContext;
        this.level = level;
        this.structureContext.level = level;

        // Initialize blocks and parameters from context
        var blockContext = new ContextUtils.BlockContext(structureContext);
        this.floorBlock = blockContext.floorBlock != null ? blockContext.floorBlock : new ContextUtils.WeightedBlock(Blocks.STONE_BRICKS, 1, null, BlockType.FLOOR);
        this.wallBlock = blockContext.wallBlock != null ? blockContext.wallBlock : new ContextUtils.WeightedBlock(Blocks.STONE_BRICKS, 1, null, BlockType.WALLS);
        this.roofBlock = blockContext.roofBlock != null ? blockContext.roofBlock : new ContextUtils.WeightedBlock(Blocks.STONE_SLAB, 1, null, BlockType.ROOF);

        this.floorCount = structureContext.getRoomCount(); // Number of floors in the tower
        this.heightPerFloor = structureContext.getHeight(); // Height of each floor
        this.width = structureContext.getWidth(); // Width of the tower
        this.length = structureContext.getLength(); // Length of the tower
    }

    public void generateTower() {
        BlockPos currentPos = structureContext.getStartPos();
        Set<BlockPos> wallPositions = new HashSet<>();

        for (int i = 0; i < floorCount; i++) {
            generateFloor(currentPos, wallPositions);
            generateWalls(currentPos, wallPositions);

            // If it's the last floor, generate the roof
            if (i == floorCount - 1) {
                generateRoof(currentPos.above(heightPerFloor));
            } else {
                // Move to the next floor level
                currentPos = currentPos.above(heightPerFloor);
            }
        }
    }

    private void generateFloor(BlockPos pos, Set<BlockPos> wallPositions) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos floorPos = pos.offset(x, 0, z);
                if (!wallPositions.contains(floorPos)) {
                    setBlock(floorPos, floorBlock, 3);
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

    public ContextUtils.StructureContext getStructureContext() {
        return structureContext;
    }
}
