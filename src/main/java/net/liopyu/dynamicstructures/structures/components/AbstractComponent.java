package net.liopyu.dynamicstructures.structures.components;

import net.liopyu.dynamicstructures.util.ContextUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractComponent {
    public final int roomNumber;
    protected BlockPos position;
    protected int length;
    protected int width;
    protected int height;

    public AbstractComponent(ContextUtils.ComponentContext context) {
        this.position = context.position;
        this.length = context.length;
        this.width = context.width;
        this.height = context.height;
        this.roomNumber = context.roomNumber;
    }

    public abstract void place(Level world);

    public abstract BlockState getBlockState(BlockPos pos);

    public boolean canPlace(Level world) {
        return true;
    }

    public void rotate(int degrees) {
    }

    public BlockPos getPosition() {
        return position;
    }

    public void setPosition(BlockPos position) {
        this.position = position;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
