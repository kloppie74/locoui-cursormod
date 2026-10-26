package app.phynx.locoui.cursormod;

import app.phynx.locoui.cursormod.core.CursorGlue;
import app.phynx.locoui.cursormod.core.Versions;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The entrypoint, the same on every Minecraft version: it starts the glue
 * built for the running game ({@link Versions#glue()}). Glues of other
 * versions sit in the jar untouched; their classes are never loaded.
 */
public final class LocoCursorMod implements ClientModInitializer {

    private static final Logger LOG = LoggerFactory.getLogger("LocoUI Cursor");

    @Override
    public void onInitializeClient() {
        String glue = Versions.glue();
        if (glue == null) {
            LOG.warn("LocoUI Cursor does not support Minecraft {}: the mod stays off.", Versions.minecraft());
            return;
        }
        try {
            Class<?> type = Class.forName("app.phynx.locoui.cursormod.glue." + glue + ".Glue");
            ((CursorGlue) type.getDeclaredConstructor().newInstance()).init();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("LocoUI Cursor: the glue " + glue + " is missing from the jar", e);
        }
    }
}
