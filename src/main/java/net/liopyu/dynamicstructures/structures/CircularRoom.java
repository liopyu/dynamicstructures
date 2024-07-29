package net.liopyu.dynamicstructures.structures;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class CircularRoom extends Room {
    private int radius;

    public CircularRoom(ServerLevel world, BlockPos position, int radius, int height, Block wallBlock, Block floorBlock, Block roofBlock, boolean generateRoof) {
        super(world, position, radius * 2, radius * 2, height, floorBlock, wallBlock, roofBlock, generateRoof);
        this.radius = radius;
    }

    @Override
    public void generate() {
        generateCylinder();
        if (generateRoof) {
            generateRoof();
        }
        fillRoomInteriorWithAir();
    }

    private void generateCylinder() {
        for (int y = 0; y <= height; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z <= radius * radius) {
                        BlockPos blockPos = position.offset(x, y, z);
                        world.setBlock(blockPos, wallBlock.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private void fillRoomInteriorWithAir() {
        for (int y = 1; y < height; y++) {
            for (int x = -radius + 1; x < radius; x++) {
                for (int z = -radius + 1; z < radius; z++) {
                    if (x * x + z * z < (radius - 1) * (radius - 1)) {
                        BlockPos airPos = position.offset(x, y, z);
                        world.setBlock(airPos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }
}