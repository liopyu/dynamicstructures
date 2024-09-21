package net.liopyu.dynamicstructures.structures.rooms;

import net.minecraft.core.BlockPos;

public class Room {
    public final BlockPos position;
    public final int roomNumber;
    public final boolean ladderRoom;
    public boolean connectedToBottomStairs;
    public final boolean stairRoom;
    public final int height;
    public final int length;
    public final int width;

    public Room(int roomNumber, BlockPos position, boolean ladderRoom, boolean stairRoom, int height, int length, int width) {
        this.roomNumber = roomNumber;
        this.ladderRoom = ladderRoom;
        this.stairRoom = stairRoom;
        this.position = position;

        this.height = height;
        this.length = length;
        this.width = width;
    }

    public void setConnectedToBottomStairs(boolean connectedToBottomStairs) {
        this.connectedToBottomStairs = connectedToBottomStairs;
    }
}
