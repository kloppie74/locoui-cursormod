package app.phynx.locoui.cursormod.fabric;

import app.phynx.locoui.cursormod.core.CursorCore;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** The pointer picture the server sent, as a texture (one at a time). */
final class PointerTexture {

    private static ResourceLocation current;
    private static int size = 256;

    private PointerTexture() {
    }

    static ResourceLocation id() {
        return current;
    }

    static int size() {
        return size;
    }

    static void upload(int hash, byte[] png) {
        try {
            NativeImage image = NativeImage.read(png);
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("locoui_cursor", "pointer/" + Integer.toHexString(hash));
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "LocoUI pointer", image));
            if (current != null && !current.equals(id)) {
                Minecraft.getInstance().getTextureManager().release(current);
            }
            current = id;
            size = image.getWidth();
            CursorCore.INSTANCE.imageReady(hash);
        } catch (Exception e) {
            // A picture the game cannot read: the server's boss-bar pointer is not drawn for this client,
            // so say so once in the log.
            System.err.println("[LocoUI Cursor] could not read the pointer picture: " + e);
        }
    }
}
