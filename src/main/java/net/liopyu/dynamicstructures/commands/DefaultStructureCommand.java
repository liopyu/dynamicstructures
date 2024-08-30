package net.liopyu.dynamicstructures.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.liopyu.dynamicstructures.Config;
import net.liopyu.dynamicstructures.DynamicStructures;
import net.liopyu.dynamicstructures.data.StructureLoader;
import net.liopyu.dynamicstructures.data.StructureSetLoader;
import net.liopyu.dynamicstructures.data.enums.StructureType;
import net.liopyu.dynamicstructures.util.DSHelperClass;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class DefaultStructureCommand {
    public static final SuggestionProvider<CommandSourceStack> STRUCTURE_TYPES;
    public static List<String> structureTypes = new ArrayList<>();

    static {
        Arrays.stream(StructureType.values()).toList().forEach(structureType -> {
            structureTypes.add(structureType.name());
        });
        STRUCTURE_TYPES = SuggestionProviders.register(new ResourceLocation(DynamicStructures.MODID, "structure_types"),
                (context, builder) -> SharedSuggestionProvider.suggest(structureTypes, builder));
    }


    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("dynamicstructures")
                        .then(Commands.literal("generate_examples")
                                .requires((p_214470_) -> p_214470_.hasPermission(2))
                                .then(Commands.argument("structureType", StringArgumentType.string())
                                        .suggests(STRUCTURE_TYPES)
                                        .executes(context -> structureVerify(context, createNewStructure(context, false), createNewStructureSet(context, false)))
                                        .then(Commands.argument("overwrite", BoolArgumentType.bool())
                                                .executes(context -> structureVerify(context, createNewStructure(context, BoolArgumentType.getBool(context, "overwrite")), createNewStructureSet(context, BoolArgumentType.getBool(context, "overwrite"))))
                                        )
                                )
                        )
        );
    }

    private static int createNewStructure(CommandContext<CommandSourceStack> context, boolean overwrite) {
        String fileName = StringArgumentType.getString(context, "structureType").toLowerCase();

        if (!fileName.endsWith(".json")) {
            fileName += ".json";
        }
        var fileNameType = "example_" + fileName;
        CommandSourceStack source = context.getSource();
        var filePath = StructureLoader.STRUCTURE_DIR + fileName;
        File targetDir = new File(StructureLoader.STRUCTURE_DIR, fileNameType).getParentFile();
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            source.sendFailure(Component.literal("Failed to create directories for " + filePath));
            return 0;
        }

        File newStructureFile = new File(StructureLoader.STRUCTURE_DIR, fileNameType);
        if (newStructureFile.exists() && !overwrite) {
            source.sendFailure(Component.literal("File already exists: " + filePath));
            return 0;
        }

        try (FileWriter writer = new FileWriter(newStructureFile)) {
            StructureLoader.defaultStructureJson(writer, fileNameType);
        } catch (IOException e) {
            source.sendFailure(Component.literal("Failed to write structure file: " + e.getMessage()));
            DSHelperClass.logErrorMessageCatchable("Failed to write structure file", e);
            return 0;
        }

        return 1;
    }

    private static int createNewStructureSet(CommandContext<CommandSourceStack> context, boolean overwrite) {
        Random random = new Random();
        int randomSalt = 1_000_000_000 + random.nextInt(1_000_000_000);
        String fileName = StringArgumentType.getString(context, "structureType").toLowerCase();

        if (!fileName.endsWith(".json")) {
            fileName += ".json";
        }
        var fileNameType = "example_" + fileName;

        CommandSourceStack source = context.getSource();
        var filePath = StructureSetLoader.STRUCTURE_DIR + fileName;
        File targetDir = new File(StructureSetLoader.STRUCTURE_DIR, fileNameType).getParentFile();
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            source.sendFailure(Component.literal("Failed to create directories for " + filePath));
            return 0;
        }

        File newStructureFile = new File(StructureSetLoader.STRUCTURE_DIR, fileNameType);
        if (newStructureFile.exists() && !overwrite) {
            source.sendFailure(Component.literal("File already exists: " + filePath));
            return 0;
        }

        try (FileWriter writer = new FileWriter(newStructureFile)) {
            StructureSetLoader.defaultStructureSetJson(writer, fileNameType, randomSalt);
        } catch (IOException e) {
            source.sendFailure(Component.literal("Failed to write structure_set file: " + e.getMessage()));
            DSHelperClass.logErrorMessageCatchable("Failed to write structure_set file", e);
            return 0;
        }

        return 1;
    }

    private static int structureVerify(CommandContext<CommandSourceStack> context, int structure, int structureSet) {
        CommandSourceStack source = context.getSource();
        String fileName = StringArgumentType.getString(context, "structureType").toLowerCase();
        var fileNameType = "example_" + fileName;
        if (structure == 1 && structureSet == 1) {
            MutableComponent textComponent = Component.literal("Example Structure and Structure-Set created successfully. ")
                    .withStyle(style -> style.withColor(ChatFormatting.GREEN))
                    .append(Component.literal("[Structure] ")
                            .withStyle(style -> style.withColor(ChatFormatting.BLUE)
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, new File(StructureLoader.STRUCTURE_DIR, fileNameType + ".json").getAbsolutePath()))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to open the structure file")))))
                    .append(Component.literal("[Structure Set]")
                            .withStyle(style -> style.withColor(ChatFormatting.BLUE)
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, new File(StructureSetLoader.STRUCTURE_DIR, fileNameType + ".json").getAbsolutePath()))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to open the structure set file")))));
            source.sendSuccess(() -> textComponent, false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to create example structure and structure set.").withStyle(ChatFormatting.RED));
            return 0;
        }
    }
}
