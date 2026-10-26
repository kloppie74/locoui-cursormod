package app.phynx.locoui.cursormod.glue.current;

import app.phynx.locoui.cursormod.core.CursorCore;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Draws the pointer on top of the HUD, at sub-pixel precision. */
final class PointerDraw {

    /** White at alpha 254: LocoUI's menu shaders leave a quad of this colour alone (CLIENT_POINTER_EXEMPT). */
    static final int MARKER = 0xFEFFFFFF;

    /**
     * The GUI still depth-tests here, and every HUD layer sits 200 deeper than the one
     * before it; this is above all of them (the GUI's depth range ends at 10,000).
     */
    private static final float ON_TOP = 9_000f;

    private PointerDraw() {
    }

    static void draw(GuiGraphics graphics) {
        CursorCore core = CursorCore.INSTANCE;
        core.flush(System.nanoTime());
        ResourceLocation texture = PointerTexture.id();
        if (!core.drawable() || texture == null || hiddenByGame()) {
            return;
        }
        float[] cell = core.placement(graphics.guiWidth(), graphics.guiHeight());
        int size = PointerTexture.size();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(cell[0], cell[1], ON_TOP);
        pose.scale(cell[2] / size, cell[2] / size, 1f);
        graphics.blit(RenderType::guiTextured, texture, 0, 0, 0f, 0f, size, size, size, size, MARKER);
        pose.popPose();
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
