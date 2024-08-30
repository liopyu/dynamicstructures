package net.liopyu.dynamicstructures.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.liopyu.dynamicstructures.DynamicStructures;
import net.liopyu.dynamicstructures.util.DSHelperClass;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class NewStructureCommand {

    public static final File STRUCTURE_DIR = new File("config/dynamicstructures/structures/");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("dynamicstructures")
                        .then(Commands.literal("generate_template")
                                .requires((p_214470_) -> p_214470_.hasPermission(2))
                                .then(Commands.argument("fileName", StringArgumentType.string())
                                        .executes(NewStructureCommand::createNewStructure)
                                )
                        )
        );
    }

    private static int createNewStructure(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String fileName = StringArgumentType.getString(context, "fileName");

        if (!fileName.endsWith(".json")) {
            fileName += ".json";
        }

        CommandSourceStack source = context.getSource();

        File targetDir = new File(STRUCTURE_DIR, fileName).getParentFile();
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            source.sendFailure(Component.literal("Failed to create directories for " + targetDir.getAbsolutePath()));
            return 0;
        }

        File newStructureFile = new File(STRUCTURE_DIR, fileName);
        if (newStructureFile.exists()) {
            source.sendFailure(Component.literal("File already exists: " + newStructureFile.getAbsolutePath()));
            return 0;
        }

        try (FileWriter writer = new FileWriter(newStructureFile)) {
            writer.write("{\n");
            writer.write("  \"name\": \"" + fileName.replace(".json", "") + "\",\n");
            writer.write("  \"ladder_chance\": 10,\n");
            writer.write("  \"rooms\": 15,\n");
            writer.write("  \"height\": 8,\n");
            writer.write("  \"width\": 8,\n");
            writer.write("  \"length\": 8,\n");
            writer.write("  \"size_threshold\": 1,\n");
            writer.write("  \"spawners\": {\n");
            writer.write("    \"max\": 10,\n");
            writer.write("    \"room_count\": 1,\n");
            writer.write("    \"mobs\": [\n");
            writer.write("      \"minecraft:zombie\",\n");
            writer.write("      \"minecraft:creeper\"\n");
            writer.write("    ]\n");
            writer.write("  },\n");
            writer.write("  \"walls\": {\n");
            writer.write("    \"blocks\": [\n");
            writer.write("      {\n");
            writer.write("        \"block\": \"minecraft:oak_planks\",\n");
            writer.write("        \"weight\": 10\n");
            writer.write("      },\n");
            writer.write("      {\n");
            writer.write("        \"block\": \"minecraft:stone_bricks\",\n");
            writer.write("        \"weight\": 10\n");
            writer.write("      },\n");
            writer.write("      {\n");
            writer.write("        \"block\": \"minecraft:bricks\",\n");
            writer.write("        \"weight\": 10\n");
            writer.write("      },\n");
            writer.write("      {\n");
            writer.write("        \"block\": \"minecraft:cobblestone\",\n");
            writer.write("        \"weight\": 10,\n");
            writer.write("        \"predicate\": {\n");
            writer.write("          \"biomes\": [\n");
            writer.write("            \"minecraft:plains\",\n");
            writer.write("            \"minecraft:forest\"\n");
            writer.write("          ]\n");
            writer.write("        }\n");
            writer.write("      }\n");
            writer.write("    ]\n");
            writer.write("  },\n");
            writer.write("  \"floor\": {\n");
            writer.write("    \"blocks\": [\n");
            writer.write("      {\n");
            writer.write("        \"block\": \"minecraft:stone\",\n");
            writer.write("        \"weight\": 10,\n");
            writer.write("        \"predicate\": {\n");
            writer.write("          \"height\": {\n");
            writer.write("            \"min\": 0,\n");
            writer.write("            \"max\": 256\n");
            writer.write("          }\n");
            writer.write("        }\n");
            writer.write("      },\n");
            writer.write("      {\n");
            writer.write("        \"block\": \"minecraft:smooth_stone\",\n");
            writer.write("        \"weight\": 10\n");
            writer.write("      },\n");
            writer.write("      {\n");
            writer.write("        \"block\": \"minecraft:oak_planks\",\n");
            writer.write("        \"weight\": 10\n");
            writer.write("      },\n");
            writer.write("      {\n");
            writer.write("        \"block\": \"minecraft:cobblestone\",\n");
            writer.write("        \"weight\": 10\n");
            writer.write("      }\n");
            writer.write("    ]\n");
            writer.write("  },\n");
            writer.write("  \"roof\": {\n");
            writer.write("    \"blocks\": [\n");
            writer.write("      {\n");
            writer.write("        \"block\": \"minecraft:oak_slab\",\n");
            writer.write("        \"weight\": 10\n");
            writer.write("      },\n");
            writer.write("      {\n");
            writer.write("        \"block\": \"minecraft:brick_slab\",\n");
            writer.write("        \"weight\": 10\n");
            writer.write("      },\n");
            writer.write("      {\n");
            writer.write("        \"block\": \"minecraft:stone_slab\",\n");
            writer.write("        \"weight\": 10\n");
            writer.write("      },\n");
            writer.write("      {\n");
            writer.write("        \"block\": \"minecraft:cobblestone_slab\",\n");
            writer.write("        \"weight\": 10\n");
            writer.write("      }\n");
            writer.write("    ]\n");
            writer.write("  }\n");
            writer.write("}\n");
        } catch (IOException e) {
            source.sendFailure(Component.literal("Failed to write structure file: " + e.getMessage()));
            DSHelperClass.logErrorMessageCatchable("Failed to write structure file", e);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Structure file created at " + newStructureFile.getAbsolutePath()), false);
        return 1;
    }
}