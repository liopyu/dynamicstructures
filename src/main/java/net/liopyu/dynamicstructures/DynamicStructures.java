package net.liopyu.dynamicstructures;

import com.mojang.logging.LogUtils;
import net.liopyu.dynamicstructures.data.StructureLoader;
import net.liopyu.dynamicstructures.data.StructureSetLoader;
import net.liopyu.dynamicstructures.data.json.BlockInterpreter;
import net.liopyu.dynamicstructures.data.json.BlockType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.Arrays;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(DynamicStructures.MODID)
public class DynamicStructures {
    public static final String MODID = "dynamic_structures";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DynamicStructures() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }


    @SubscribeEvent
    public void commonSetup(final FMLCommonSetupEvent event) {
        Arrays.stream(BlockType.values()).toList().forEach(blockType -> {
            BlockInterpreter.allowedKeywords.add(blockType.name().toLowerCase());
        });
        StructureSetLoader.loadStructures();
        StructureLoader.loadStructures();
    }
}
