package net.liopyu.dynamicstructures.events.server;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static net.liopyu.dynamicstructures.DynamicStructures.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.DEDICATED_SERVER)
public class ServerModEvents {

    @SubscribeEvent
    public void onWorldLoad(LevelEvent.Load event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        /*ForgeRegistries.FEATURES.register(new HouseStructure(NoneFeatureConfiguration.CODEC)
                .setRegistryName(new ResourceLocation(MODID, "house_structure")));

        // Adding feature to the biomes
        Registry<Biome> biomeRegistry = event.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        biomeRegistry.stream().forEach(biome -> {
            biome.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES,
                    HOUSE_STRUCTURE.configured(NoneFeatureConfiguration.INSTANCE));
        });*/
    }
   /* @ObjectHolder(registryName = "", value = "dynamicstructures:house_structure")
    public static final Feature<NoneFeatureConfiguration> HOUSE_STRUCTURE = null;
    @SubscribeEvent
    public void register(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.FEATURES,
                helper -> {
                    helper.register(new ResourceLocation(MODID, "house_structure"),new HouseStructure(NoneFeatureConfiguration.CODEC));
                }
        );
    }*/
}
