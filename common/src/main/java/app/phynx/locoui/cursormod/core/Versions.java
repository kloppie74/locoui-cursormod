package app.phynx.locoui.cursormod.core;

import net.fabricmc.loader.api.FabricLoader;

/** This mod's and the game's version, for the hello (the same on every Minecraft version). */
public final class Versions {

    private Versions() {
    }

    public static String mod() {
        return of("locoui_cursor");
    }

    public static String minecraft() {
        return of("minecraft");
    }

    private static String of(String id) {
        return FabricLoader.getInstance().getModContainer(id)
            .map(c -> c.getMetadata().getVersion().getFriendlyString())
            .orElse("?");
    }
}
