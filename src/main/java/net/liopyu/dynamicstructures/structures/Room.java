package net.liopyu.dynamicstructures.structures;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
import java.util.Set;

public class Room {
    protected ServerLevel world;
    protected BlockPos position;
    protected int width;
    protected int length;
    protected int height;
    protected Block floorBlock;
    protected Block wallBlock;
    protected Block roofBlock;
    protected boolean generateRoof;

    public Room(ServerLevel world, BlockPos position, int width, int length, int height, Block floorBlock, Block wallBlock, Block roofBlock, boolean generateRoof) {
        this.world = world;
        this.position = position;
        this.width = width;
        this.length = length;
        this.height = height;
        this.floorBlock = floorBlock;
        this.wallBlock = wallBlock;
        this.roofBlock = roofBlock;
        this.generateRoof = generateRoof;
    }

    public BlockPos getPosition() {
        return position;
    }

    public void generate() {
        Set<BlockPos> wallPositions = new HashSet<>();
        generateWalls(wallPositions);
        generateFloor(wallPositions);
        if (generateRoof) {
            generateRoof();
        }
        fillRoomInteriorWithAir(wallPositions);
    }

    protected void generateWalls(Set<BlockPos> wallPositions) {
        for (int y = 1; y <= height; y++) {
            for (int x = 0; x < width; x++) {
                addWallBlock(position.offset(x, y, 0), wallBlock, wallPositions);
                addWallBlock(position.offset(x, y, length - 1), wallBlock, wallPositions);
            }
            for (int z = 1; z < length - 1; z++) {
                addWallBlock(position.offset(0, y, z), wallBlock, wallPositions);
                addWallBlock(position.offset(width - 1, y, z), wallBlock, wallPositions);
            }
        }
    }

    protected void addWallBlock(BlockPos pos, Block block, Set<BlockPos> wallPositions) {
        world.setBlock(pos, block.defaultBlockState(), 3);
        wallPositions.add(pos);
    }


    protected void generateFloor(Set<BlockPos> wallPositions) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos floorPos = position.offset(x, 0, z);
                if (!wallPositions.contains(floorPos)) {
                    world.setBlock(floorPos, floorBlock.defaultBlockState(), 3);
                }
            }
        }
    }


    protected void generateRoof() {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos roofPos = position.offset(x, height + 1, z);
                world.setBlock(roofPos, roofBlock.defaultBlockState(), 3);
            }
        }
    }
    protected void fillRoomInteriorWithAir(Set<BlockPos> wallPositions) {
        for (int x = 1; x < width - 1; x++) {
            for (int z = 1; z < length - 1; z++) {
                for (int y = 1; y <= height; y++) {
                    BlockPos airPos = position.offset(x, y, z);
                    if (!wallPositions.contains(airPos)) {
                        world.setBlock(airPos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

}
