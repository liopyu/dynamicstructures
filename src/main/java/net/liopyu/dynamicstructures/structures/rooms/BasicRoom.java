package net.liopyu.dynamicstructures.structures.rooms;

import net.liopyu.dynamicstructures.structures.components.Floor;
import net.liopyu.dynamicstructures.structures.components.Roof;
import net.liopyu.dynamicstructures.structures.components.Wall;
import net.minecraft.core.BlockPos;

public class BasicRoom extends Room {
    public BasicRoom(int roomNumber, BlockPos position, int height, int length, int width, Floor floor, Roof roof, Wall wall) {
        super(roomNumber, position, height, length, width, floor, roof, wall);
    }
}
