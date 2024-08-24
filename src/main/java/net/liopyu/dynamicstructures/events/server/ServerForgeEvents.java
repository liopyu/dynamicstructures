package net.liopyu.dynamicstructures.events.server;

import net.liopyu.dynamicstructures.data.StructureLoader;
import net.liopyu.dynamicstructures.structures.DungeonGenerator;
import net.liopyu.dynamicstructures.util.ContextUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

import static net.liopyu.dynamicstructures.DynamicStructures.MODID;

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
            List<ContextUtils.StructureContext> structures = StructureLoader.loadStructures(serverLevel,blockPos);

            // Optionally find a specific structure
            for (ContextUtils.StructureContext context : structures) {
                if ("test".equals(context.getStructureName())) {
                    DungeonGenerator.generateDungeon(context);
                    break;
                }
            }
        }
    }
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        // You can access the structure context here if needed
    }
}
