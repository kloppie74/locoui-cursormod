package app.phynx.locoui.cursormod.core;

/**
 * The few lines that differ per Minecraft generation: registering the payload,
 * the receiver and the HUD drawing. Every generation's glue lives in a package
 * of its own ({@code glue.v1_21_4}, ...), so one jar carries all of them and
 * only the running game's is ever loaded.
 */
public interface CursorGlue {
    void init();
}
