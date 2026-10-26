package app.phynx.locoui.cursormod.glue.current;

import app.phynx.locoui.cursormod.core.CursorCore;
import app.phynx.locoui.cursormod.core.CursorGlue;
import app.phynx.locoui.cursormod.core.Versions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/** Minecraft 1.21.5: the payload, the receiver and the pointer on the HUD. */
public final class Glue implements CursorGlue {

    @Override
    public void init() {
        PayloadTypeRegistry.playS2C().register(CursorPayload.TYPE, CursorPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CursorPayload.TYPE, CursorPayload.CODEC);
        CursorCore.INSTANCE.init(new CursorCore.Platform() {
            @Override
            public void send(byte[] payload) {
                ClientPlayNetworking.send(new CursorPayload(payload));
            }

            @Override
            public void image(int hash, byte[] png) {
                PointerTexture.upload(hash, png);
            }

            @Override
            public String modVersion() {
                return Versions.mod();
            }

            @Override
            public String minecraftVersion() {
                return Versions.minecraft();
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(CursorPayload.TYPE, (payload, context) -> CursorCore.INSTANCE.onMessage(payload.data()));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> CursorCore.INSTANCE.onJoin());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> CursorCore.INSTANCE.onDisconnect());
        // After the whole HUD, LocoUI's boss bars included.
        HudRenderCallback.EVENT.register((graphics, tick) -> PointerDraw.draw(graphics));
    }
}
