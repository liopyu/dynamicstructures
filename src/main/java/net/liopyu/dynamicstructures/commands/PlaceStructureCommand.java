package net.liopyu.dynamicstructures.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.liopyu.dynamicstructures.DynamicStructures;
import net.liopyu.dynamicstructures.data.StructureLoader;
import net.liopyu.dynamicstructures.structures.Dungeon;
import net.liopyu.dynamicstructures.util.ContextUtils;
import net.liopyu.dynamicstructures.util.DSHelperClass;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

import static net.liopyu.dynamicstructures.commands.FindStructureCommand.quotedNames;
import static net.liopyu.dynamicstructures.events.server.ServerForgeEvents.shouldGenerateStructure;

/**
 * The {@code PlaceStructureCommand} class defines the {@code /dynamicstructures place} command,
 * allowing players with the appropriate permissions to place a custom structure by name
 * at their current location.
 */
public class PlaceStructureCommand {


    /**
     * Registers the {@code /dynamicstructures place} command with the provided {@link CommandDispatcher}.
     *
     * @param dispatcher The {@link CommandDispatcher} to register the command with.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("dynamicstructures")
                        .then(Commands.literal("place")
                                .requires((p_214470_) -> p_214470_.hasPermission(2))
                                .then(Commands.argument("structureName", StringArgumentType.string())
                                        .executes(PlaceStructureCommand::placeStructure).suggests(FindStructureCommand.STRUCTURE_NAMES)
                                )
                        )
        );
    }

    /**
     * Executes the {@code /dynamicstructures place} command. Places the structure
     * matching the provided name at the player's current location.
     *
     * @param context The {@link CommandContext} containing the command source and arguments.
     * @return A command result code. {@code 1} if successful, otherwise {@code 0}.
     * @throws CommandSyntaxException If an error occurs while executing the command.
     */
    private static int placeStructure(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String structureName = StringArgumentType.getString(context, "structureName");
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        BlockPos playerPos = player.blockPosition();
        ContextUtils.StructureContext structureContext = DSHelperClass.getStructureContext(structureName);

        try {
            if (structureContext != null) {
                placeStructureAt(structureContext, level, playerPos);
                source.sendSuccess(() -> Component.literal("Structure '" + structureName + "' placed at your current location.")
                        .withStyle(ChatFormatting.GREEN), false);
            } else {
                source.sendFailure(Component.literal("No structure registered with the name '" + structureName + "'.").withStyle(ChatFormatting.RED));
            }
        } catch (Exception e) {
            DSHelperClass.logErrorMessageCatchable("Failed to place structure '" + structureName + "'", e);
            return 0;
        }

        return 1;
    }

    /**
     * Placeholder method for placing a structure at a specific location.
     * You need to implement the actual placement logic based on your mod's structure generation.
     *
     * @param structureContext The {@link ContextUtils.StructureContext} for the structure to place.
     * @param level            The {@link ServerLevel} where the structure should be placed.
     * @param pos              The {@link BlockPos} position where the structure should be placed.
     */
    private static void placeStructureAt(ContextUtils.StructureContext structureContext, ServerLevel level, BlockPos pos) {
        if (shouldGenerateStructure(structureContext, level, pos)) {
            structureContext.setStartPos(pos);
            new Dungeon(structureContext, level).generateDungeon();
        }
    }
}
