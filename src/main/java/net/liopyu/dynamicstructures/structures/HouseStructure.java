package net.liopyu.dynamicstructures.structures;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class HouseStructure extends Feature<NoneFeatureConfiguration> {

    public HouseStructure(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        BlockPos pos = context.origin();
        LevelAccessor world = context.level();
        RandomSource random = context.random();
        generateHouse(world, pos, random);
        return true;
    }

    public void generateHouse(LevelAccessor world, BlockPos pos, RandomSource random) {
        for (int x = 0; x < 10; x++) {
            for (int z = 0; z < 10; z++) {
                world.setBlock(pos.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 3); // Floor
                world.setBlock(pos.offset(x, 1, z), Blocks.OAK_PLANKS.defaultBlockState(), 3); // Walls
            }
        }
    }
}