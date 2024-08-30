package net.liopyu.dynamicstructures.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.liopyu.dynamicstructures.DynamicStructures;
import net.liopyu.dynamicstructures.data.StructureLoader;
import net.liopyu.dynamicstructures.util.ContextUtils;
import net.liopyu.dynamicstructures.util.DSHelperClass;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static net.liopyu.dynamicstructures.events.server.ServerForgeEvents.findNearestStructure;

/**
 * The {@code FindStructureCommand} class defines the {@code /dynamicstructures locate} command,
 * allowing players with the appropriate permissions to locate a custom structure by name.
 * The command searches for the nearest instance of a specified structure and returns a clickable
 * link that suggests the teleport command to the player.
 */
public class FindStructureCommand {
    public static final SuggestionProvider<CommandSourceStack> STRUCTURE_NAMES;
    public static List<String> quotedNames = new ArrayList<>();

    static {
        StructureLoader.cachedStructures.keySet().forEach(string -> {
            quotedNames.add("\"" + string + "\"");
        });
        STRUCTURE_NAMES = SuggestionProviders.register(new ResourceLocation(DynamicStructures.MODID, "structure_names"),
                (context, builder) -> SharedSuggestionProvider.suggest(quotedNames, builder));
    }

    /**
     * Registers the {@code /dynamicstructures locate} command with the provided {@link CommandDispatcher}.
     *
     * @param dispatcher The {@link CommandDispatcher} to register the command with.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("dynamicstructures")
                        .then(Commands.literal("locate")
                                .then(Commands.argument("structureName", StringArgumentType.string())
                                        .executes(FindStructureCommand::findStructure)
                                        .suggests(STRUCTURE_NAMES)
                                )
                        )
                        .requires((p_214470_) -> p_214470_.hasPermission(2))
        );
    }

    /**
     * Executes the {@code /dynamicstructures locate} command. Searches for the nearest structure
     * matching the provided name and sends the player a clickable link to teleport to that structure.
     *
     * @param context The {@link CommandContext} containing the command source and arguments.
     * @return A command result code. {@code 1} if successful, otherwise {@code 0}.
     * @throws CommandSyntaxException If an error occurs while executing the command.
     */
    private static int findStructure(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String structureName = StringArgumentType.getString(context, "structureName");
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        BlockPos playerPos = player.blockPosition();
        ContextUtils.StructureContext structureContext = DSHelperClass.getStructureContext(structureName);
        ContextUtils.SpawnContext spawnContext = DSHelperClass.getSpawnContext(structureName);
        try {
            if (spawnContext == null) {
                source.sendFailure(Component.literal("No structure_set with the name '" + structureName + "'.").withStyle(ChatFormatting.RED));
                return 0;
            }
            if (structureContext != null) {
                Optional<BlockPos> nearestStructure = findNearestStructure(structureName, structureContext, spawnContext, level, playerPos);
                if (nearestStructure.isPresent()) {
                    BlockPos structurePos = nearestStructure.get();
                    MutableComponent textComponent = Component.literal("Nearest structure '" + structureName + "' found at ")
                            .append(Component.literal(structurePos.getX() + " " + player.blockPosition().getY() + " " + structurePos.getZ())
                                    .withStyle(style -> style.withColor(0x00FF00)
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + structurePos.getX() + " " + player.blockPosition().getY() + " " + structurePos.getZ()))
                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to teleport"))))
                            );
                    source.sendSuccess(() -> textComponent, false);
                } else {
                    source.sendFailure(Component.literal("Could not find a nearby structure with the name '" + structureName + "'.").withStyle(ChatFormatting.GRAY));
                }
            } else {
                source.sendFailure(Component.literal("No structure registered with the name '" + structureName + "'.").withStyle(ChatFormatting.RED));
            }
        } catch (Exception e) {
            DSHelperClass.logErrorMessageCatchable("", e);
            return 1;
        }
        return 1;
    }
}
