package app.phynx.locoui.cursormod.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;

/** This mod's and the game's version, and which glue the game needs (the same on every Minecraft version). */
public final class Versions {

    /** Written by the build: one line per glue in the jar, "<glue> <Minecraft versions>". */
    private static final String GLUE_TABLE = "/locoui_cursor_glues.txt";

    private Versions() {
    }

    public static String mod() {
        return of("locoui_cursor");
    }

    public static String minecraft() {
        return of("minecraft");
    }

    /** The glue package built for the running game ({@code v1_21_4}, ...), or null when none fits. */
    public static String glue() {
        Version game = FabricLoader.getInstance().getModContainer("minecraft")
            .map(c -> c.getMetadata().getVersion())
            .orElse(null);
        if (game == null) {
            return null;
        }
        try (InputStream in = Versions.class.getResourceAsStream(GLUE_TABLE)) {
            if (in == null) {
                return null;
            }
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                String row = line.trim();
                int space = row.indexOf(' ');
                if (space <= 0) {
                    continue;
                }
                if (VersionPredicate.parse(row.substring(space + 1).trim()).test(game)) {
                    return row.substring(0, space);
                }
            }
        } catch (IOException | VersionParsingException e) {
            throw new IllegalStateException("LocoUI Cursor: unreadable " + GLUE_TABLE, e);
        }
        return null;
    }

    private static String of(String id) {
        return FabricLoader.getInstance().getModContainer(id)
            .map(c -> c.getMetadata().getVersion().getFriendlyString())
            .orElse("?");
    }
}
