package net.liopyu.dynamicstructures.structures.components;

import net.liopyu.dynamicstructures.util.ContextUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

public class Roof extends AbstractComponent {
    public final Set<BlockPos> roofPositions;

    public Roof(int roomNumber, BlockPos position, int length, int width, int height, Set<BlockPos> roofPositions) {
        super(new ContextUtils.ComponentContext(roomNumber, position, height, length, width));
        this.roofPositions = roofPositions;
    }

    public Roof(int roomNumber, BlockPos position, int length, int width, int height) {
        super(new ContextUtils.ComponentContext(roomNumber, position, height, length, width));
        this.roofPositions = new HashSet<>();
    }

    public Roof(ContextUtils.ComponentContext context) {
        super(context);
        this.roofPositions = new HashSet<>();
    }

    @Override
    public void place(ContextUtils.StructureContext context, BlockPos pos) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos roofPos = pos.offset(x, height + 1, z);
                if (context.level.getBlockState(roofPos).getBlock() instanceof LiquidBlock ||
                        context.level.getBlockState(roofPos).isAir() ||
                        context.level.getBlockState(roofPos).is(Blocks.CAVE_AIR) ||
                        context.getBlockContext().wallBlock.block.equals(context.level.getBlockState(roofPos).getBlock())) {
                    if (!ladderPositions.contains(roofPos)) {
                        setBlock(roofPos, roofBlockW, 3);
                        roofPositions.add(roofPos);
                    }
                }
            }
        }
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return null;
    }
}
