package net.liopyu.dynamicstructures.structures.components;

import net.liopyu.dynamicstructures.util.ContextUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class Roof extends AbstractComponent {
    public Roof(int roomNumber, BlockPos position, int length, int width, int height) {
        super(new ContextUtils.ComponentContext(roomNumber, position, height, length, width));
    }

    public Roof(ContextUtils.ComponentContext context) {
        super(context);
    }

    @Override
    public void place(Level world) {

    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return null;
    }
}
