package net.liopyu.dynamicstructures.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.liopyu.dynamicstructures.util.ContextUtils;
import net.liopyu.dynamicstructures.data.StructureLoader;
import net.liopyu.dynamicstructures.data.StructureSetLoader;
import net.liopyu.dynamicstructures.util.DSHelperClass;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.awt.*;
import java.util.Optional;

import static net.liopyu.dynamicstructures.events.server.ServerForgeEvents.findNearestStructure;

public class FindStructureCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("dynamicstructures")
                        .then(Commands.literal("locate")
                                .requires((p_214470_) -> p_214470_.hasPermission(2))
                                .then(Commands.argument("structureName", StringArgumentType.string())
                                        .executes(FindStructureCommand::findStructure)
                                )
                        )
        );
    }

    private static int findStructure(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String structureName = StringArgumentType.getString(context, "structureName");
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        BlockPos playerPos = player.blockPosition();
        ContextUtils.StructureContext structureContext = DSHelperClass.getStructureContext(structureName, level, playerPos);
        ContextUtils.SpawnContext spawnContext = DSHelperClass.getSpawnContext(structureName);
        if (structureContext != null && spawnContext != null) {
            Optional<BlockPos> nearestStructure = findNearestStructure(structureName, structureContext, spawnContext, level, playerPos);
            if (nearestStructure.isPresent()) {
                BlockPos structurePos = nearestStructure.get();
                String command = String.format("/tp %d %d %d", structurePos.getX(), structurePos.getY(), structurePos.getZ());
                MutableComponent textComponent = Component.literal("Nearest structure '" + structureName + "' found at ")
                        .append(Component.literal(structurePos.toShortString())
                                .withStyle(style -> style.withColor(0x00FF00)
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy teleport command"))))
                        );
                source.sendSuccess(() -> textComponent, false);
            } else {
                source.sendFailure(Component.literal("No structure found with the name '" + structureName + "'.").withStyle(ChatFormatting.RED));
            }
        } else {
            source.sendFailure(Component.literal("No structure registered with the name '" + structureName + "'.").withStyle(ChatFormatting.RED));
        }

        return 1;
    }
}
