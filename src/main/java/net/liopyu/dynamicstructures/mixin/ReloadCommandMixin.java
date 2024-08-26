package net.liopyu.dynamicstructures.mixin;

import net.liopyu.dynamicstructures.data.StructureLoader;
import net.liopyu.dynamicstructures.data.StructureSetLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.ReloadCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(value = ReloadCommand.class,remap = true)
public class ReloadCommandMixin {

    @Inject(method = "reloadPacks", at = @At("HEAD"))
    private static void onReload(Collection<String> p_138236_, CommandSourceStack p_138237_, CallbackInfo ci) {
        StructureLoader.clearCache();
        StructureSetLoader.clearCache();
    }
}