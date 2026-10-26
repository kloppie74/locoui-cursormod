package app.phynx.locoui.cursormod.mixin;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Picks the mouse mixin for the running game. The jar carries it twice, from
 * the same source: {@code intermediary} for 1.21.x (the game runs on Fabric's
 * intermediary names) and {@code mojang} for 26.x (unobfuscated). Only the one
 * that matches is loaded; a single-version build carries just its own.
 */
public final class CursorMixinPlugin implements IMixinConfigPlugin {

    private static final String MIXIN = "MouseHandlerMixin";

    @Override
    public List<String> getMixins() {
        boolean intermediary = "intermediary".equals(FabricLoader.getInstance().getMappingResolver().getCurrentRuntimeNamespace());
        String wanted = intermediary ? "intermediary" : "mojang";
        String other = intermediary ? "mojang" : "intermediary";
        return List.of((carries(wanted) || !carries(other) ? wanted : other) + "." + MIXIN);
    }

    private static boolean carries(String family) {
        String path = "app/phynx/locoui/cursormod/mixin/" + family + "/" + MIXIN + ".class";
        return CursorMixinPlugin.class.getClassLoader().getResource(path) != null;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
