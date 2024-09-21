package net.liopyu.dynamicstructures.structures.components;

import net.liopyu.dynamicstructures.data.enums.BlockType;
import net.liopyu.dynamicstructures.util.ContextUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

public class Floor extends AbstractComponent {
    public final Set<BlockPos> floorPositions = new HashSet<>();

    public Floor(int roomNumber, BlockPos position, int length, int width, int height) {
        super(new ContextUtils.ComponentContext(roomNumber, position, height, length, width));
    }

    public Floor(ContextUtils.ComponentContext context) {
        super(context);
    }

    @Override
    public void place(ContextUtils.StructureContext context, BlockPos position) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos floorPos = position.offset(x, 0, z);
                if (context.level.getBlockState(floorPos).getBlock() instanceof LadderBlock) {
                    setBlock(floorPos, Blocks.AIR.defaultBlockState(), 3, BlockType.FLOOR);
                } else if (!wallPositions.contains(floorPos)) {
                    setBlock(floorPos, context.getBlockContext().floorBlock, 3);
                }
            }
        }
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return null;
    }
}
