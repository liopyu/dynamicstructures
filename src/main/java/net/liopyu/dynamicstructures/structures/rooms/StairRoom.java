package net.liopyu.dynamicstructures.structures.rooms;

import net.minecraft.core.BlockPos;

public class StairRoom extends BasicRoom {
    public StairRoom(int roomNumber, BlockPos position, boolean ladderRoom, boolean stairRoom, int height, int length, int width) {
        super(roomNumber, position, ladderRoom, stairRoom, height, length, width);
    }
}
