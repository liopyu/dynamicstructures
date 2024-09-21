package net.liopyu.dynamicstructures.structures.rooms;

import net.minecraft.core.BlockPos;

public class LadderRoom extends BasicRoom {
    public LadderRoom(int roomNumber, BlockPos position, boolean ladderRoom, boolean stairRoom, int height, int length, int width) {
        super(roomNumber, position, ladderRoom, stairRoom, height, length, width);
    }
}
