package app.phynx.locoui.cursormod.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * The universal jar carries the turn mixin twice, from the same source:
 * {@code intermediary} for 1.21.x (the game runs on Fabric's intermediary
 * names) and {@code mojang} for 26.x (unobfuscated). Both are listed in the
 * mixin config itself, so launchers that prepare mixins ahead of time (Lunar
 * Client bakes its classes) see them too; this plugin only drops the one whose
 * target class the game does not have. When game classes cannot be looked up
 * (a launcher with class loading of its own), both stay and Mixin skips the
 * one whose target is missing.
 */
public final class CursorMixinPlugin implements IMixinConfigPlugin {

    /** A class every version has under its real name: tells whether game classes can be looked up at all. */
    private static final String PROBE = "net/minecraft/client/main/Main.class";

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        ClassLoader loader = CursorMixinPlugin.class.getClassLoader();
        if (loader.getResource(targetClassName.replace('.', '/') + ".class") != null) {
            return true;
        }
        return loader.getResource(PROBE) == null;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
