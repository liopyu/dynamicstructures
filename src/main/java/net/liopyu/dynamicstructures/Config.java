package net.liopyu.dynamicstructures;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = DynamicStructures.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue GENERATE_EXAMPLE_STRUCTURE = BUILDER
            .comment("Automatically generate an example structure JSON if none exist.")
            .define("generate_example_structure", true);
    private static final ForgeConfigSpec.ConfigValue<String> STRUCTURE_LOCATION = BUILDER
            .comment("File location for structure files.")
            .define("structure_directory", "config/dynamicstructures/structures/");
    private static final ForgeConfigSpec.BooleanValue GENERATE_EXAMPLE_STRUCTURE_SET = BUILDER
            .comment("Automatically generate a dungeon structure_set JSON if none exist.")
            .comment("This also allows the example structure to spawn in the world.")
            .define("generate_example_structure_set", true);
    private static final ForgeConfigSpec.ConfigValue<String> STRUCTURE_SET_LOCATION = BUILDER
            .comment("File location for structure_set files.")
            .define("structure_set_directory", "config/dynamicstructures/structure_set/");
    static final ForgeConfigSpec ASPEC = BUILDER.build();
    public static boolean generate_example_structure;
    public static boolean generate_example_structure_set;
    public static String structure_set_directory;
    public static String structure_directory;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        generate_example_structure = GENERATE_EXAMPLE_STRUCTURE.get();
        generate_example_structure_set = GENERATE_EXAMPLE_STRUCTURE_SET.get();
        structure_set_directory = STRUCTURE_SET_LOCATION.get();
        structure_directory = STRUCTURE_LOCATION.get();

    }
}
