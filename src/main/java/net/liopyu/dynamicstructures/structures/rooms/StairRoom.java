package net.liopyu.dynamicstructures.structures.rooms;

import net.liopyu.dynamicstructures.structures.components.Floor;
import net.liopyu.dynamicstructures.structures.components.Roof;
import net.liopyu.dynamicstructures.structures.components.Wall;
import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Set;

public class StairRoom extends BasicRoom {
    public boolean connectedToBottomStairs;
    Set<BlockPos> blockColumnShell = new HashSet<>();
    Set<BlockPos> openPositions = new HashSet<>();
    Set<BlockPos> stairPositions = new HashSet<>();

    public StairRoom(int roomNumber, BlockPos position, int height, int length, int width, Floor floor, Roof roof, Wall wall) {
        super(roomNumber, position, height, length, width, floor, roof, wall);
    }

    public void setConnectedToBottomStairs(boolean connectedToBottomStairs) {
        this.connectedToBottomStairs = connectedToBottomStairs;
    }
}
