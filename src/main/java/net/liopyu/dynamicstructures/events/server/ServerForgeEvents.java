package net.liopyu.dynamicstructures.events.server;

import net.liopyu.dynamicstructures.structures.DungeonGenerator;
import net.liopyu.dynamicstructures.util.DSHelperClass;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

import static net.liopyu.dynamicstructures.DynamicStructures.MODID;
import static net.liopyu.dynamicstructures.structures.DungeonGenerator.ENTITY_TYPES;
import static net.liopyu.dynamicstructures.structures.DungeonGenerator.generateLadderRoom;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerForgeEvents {
    @SubscribeEvent
    public static void onRightClicked(PlayerInteractEvent.RightClickItem event) {
        doRightClick(event);
    }
    public static List<Block> blockList = new ArrayList<>();
    static {
        blockList.add(Blocks.STONE);
    }
    public static void doRightClick(PlayerInteractEvent.RightClickItem event) {
        var blockPos = event.getPos();
        if (!event.getLevel().isClientSide()){
            var serverLevel = (ServerLevel) event.getLevel();
            Direction direction = event.getEntity().getDirection();

            //generateLadderRoom(serverLevel, blockPos, 10, 10, 10, Blocks.ACACIA_PLANKS, Blocks.BIRCH_WOOD, Blocks.STONE_SLAB, true);
            DungeonGenerator.generateDungeon(serverLevel, blockPos,direction, serverLevel.getRandom(),
            10,
                    15,
                    8,
                    8,
                    8,true,1, List.of(ENTITY_TYPES));
        }
    }
}
