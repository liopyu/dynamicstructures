package net.liopyu.dynamicstructures.structures.components;

import net.liopyu.dynamicstructures.util.ContextUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Wall extends AbstractComponent {
    public final Set<BlockPos> wallPositions = new HashSet<>();

    public Wall(int roomNumber, BlockPos position, int length, int width, int height,Set<BlockPos> roofPositions) {
        super(new ContextUtils.ComponentContext(roomNumber, position, height, length, width));
    }

    public Wall(ContextUtils.ComponentContext context) {
        super(context);
    }

    @Override
    public void place(ContextUtils.StructureContext context, BlockPos pos) {
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

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return null;
    }
}
