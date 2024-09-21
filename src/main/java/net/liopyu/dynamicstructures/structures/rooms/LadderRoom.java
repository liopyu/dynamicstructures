package net.liopyu.dynamicstructures.structures.rooms;

import net.liopyu.dynamicstructures.data.enums.BlockType;
import net.liopyu.dynamicstructures.structures.components.Floor;
import net.liopyu.dynamicstructures.structures.components.Roof;
import net.liopyu.dynamicstructures.structures.components.Wall;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

public class LadderRoom extends BasicRoom {
    public LadderRoom(int roomNumber, BlockPos position, int height, int length, int width, Floor floor, Roof roof, Wall wall) {
        super(roomNumber, position, height, length, width, floor, roof, wall);
    }

    public void init() {
        Set<BlockPos> roomWalls = wall.wallPositions;

        Direction ladderFacing = Direction.EAST;
        BlockPos ladderBase = position.offset(width / 2 - 1, 1, length / 2 - 1);

        for (int i = 0; i < height; i++) {
            BlockPos ladderPos = ladderBase.above(i + 1);
            BlockState ladderState = Blocks.LADDER.defaultBlockState()
                    .setValue(LadderBlock.FACING, ladderFacing)
                    .setValue(LadderBlock.WATERLOGGED, false);
            setBlock(ladderPos, ladderState, 3, BlockType.CENTER);
        }

        BlockPos topRoomPos = position.above(height + 1);
        Set<BlockPos> upperRoomWalls = generateRoom(topRoomPos, roomWalls, false);

        BlockPos opening = ladderBase.above(height + 1);
        setBlock(opening, Blocks.AIR.defaultBlockState(), 3, BlockType.CENTER);
        placeDoorway(position, currentDirection, level.random);

    }
}
