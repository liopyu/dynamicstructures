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

/**
 * A mixin class for intercepting the {@code reloadPacks} method in the {@link ReloadCommand} class.
 * This mixin is used to clear the caches of loaded structures and structure sets whenever the
 * reload command is executed in Minecraft, ensuring that any changes to structure data files are
 * reflected without needing to restart the game.
 *
 * <p>The mixin targets the {@code reloadPacks} method in the {@link ReloadCommand} class and injects
 * code at the method's head to clear caches before the actual reload operation begins.
 * This is useful for mod developers who want to dynamically reload structure configurations
 * without restarting the game.</p>
 *
 * <p><strong>Key Features:</strong></p>
 * <ul>
 *   <li>Automatically clears the cached structure data in {@link StructureLoader} and {@link StructureSetLoader}.</li>
 *   <li>Ensures that updated structure configurations are loaded during the reload process.</li>
 * </ul>
 *
 * <p><strong>Usage:</strong></p>
 * <ul>
 *   <li>This mixin is automatically applied during runtime and does not require direct invocation.</li>
 * </ul>
 *
 * @see ReloadCommand
 * @see StructureLoader#clearCache()
 * @see StructureSetLoader#clearCache()
 */
@Mixin(value = ReloadCommand.class, remap = true)
public class ReloadCommandMixin {
    @Inject(method = "reloadPacks", at = @At("HEAD"))
    private static void onReload(Collection<String> p_138236_, CommandSourceStack p_138237_, CallbackInfo ci) {
        StructureLoader.clearCache();
        StructureSetLoader.clearCache();
    }
}