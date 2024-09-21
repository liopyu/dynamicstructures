package net.liopyu.dynamicstructures.structures.rooms;

import net.liopyu.dynamicstructures.structures.components.Floor;
import net.liopyu.dynamicstructures.structures.components.Roof;
import net.liopyu.dynamicstructures.structures.components.Wall;
import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Set;

public abstract class Room {
    public final BlockPos position;
    public final int roomNumber;
    public final int height;
    public final int length;
    public final int width;
    public final Floor floor;
    public final Roof roof;
    public final Wall wall;
    public Set<BlockPos> wallPositions = new HashSet<>();

    public Room(int roomNumber, BlockPos position, int height, int length, int width, Floor floor, Roof roof, Wall wall) {
        this.roomNumber = roomNumber;
        this.position = position;
        this.height = height;
        this.length = length;
        this.width = width;
        this.floor = floor;
        this.roof = roof;
        this.wall = wall;
    }


    public abstract Room generate(BlockPos pos, Set<BlockPos> wallPositions, boolean isLastFloor, boolean generateStaircase, Set<BlockPos> stairPositions, int roomNumber, Set<BlockPos> blockColumnShell, Set<BlockPos> openPositions);

    public abstract Room generate(BlockPos pos, Set<BlockPos> overlapWalls, boolean isBottomLadderRoom);
}
