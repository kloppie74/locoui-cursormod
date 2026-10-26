package app.phynx.locoui.cursormod.fabric;

import app.phynx.locoui.cursormod.core.CursorCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

/** Draws the pointer on top of the HUD, at sub-pixel precision. */
final class PointerDraw {

    /** White at alpha 254: LocoUI's menu shaders leave a quad of this colour alone (CLIENT_POINTER_EXEMPT). */
    static final int MARKER = 0xFEFFFFFF;

    private PointerDraw() {
    }

    static void draw(GuiGraphics graphics) {
        CursorCore core = CursorCore.INSTANCE;
        core.flush(System.nanoTime());
        Identifier texture = PointerTexture.id();
        if (!core.drawable() || texture == null || hiddenByGame()) {
            return;
        }
        float[] cell = core.placement(graphics.guiWidth(), graphics.guiHeight());
        int size = PointerTexture.size();
        // A stratum of its own: above every layer the HUD and LocoUI's boss bars used.
        graphics.nextStratum();
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(cell[0], cell[1]);
        pose.scale(cell[2] / size, cell[2] / size);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0f, 0f, size, size, size, size, MARKER);
        pose.popMatrix();
    }

    /**
     * A screen (chat, the pause menu, an inventory, a text field) gives the mouse back to
     * the system cursor and covers the HUD: no second, frozen pointer under it. F1 hides
     * the HUD, LocoUI's menu with it.
     */
    private static boolean hiddenByGame() {
        Minecraft mc = Minecraft.getInstance();
        return mc.screen != null || mc.options.hideGui;
    }
}
