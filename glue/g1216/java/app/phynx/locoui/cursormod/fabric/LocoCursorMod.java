package app.phynx.locoui.cursormod.fabric;

import app.phynx.locoui.cursormod.core.CursorCore;
import app.phynx.locoui.cursormod.core.Versions;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.ResourceLocation;

/** Minecraft 1.21.6 – 1.21.10. */
public final class LocoCursorMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
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
        // Last: above every HUD element, LocoUI's boss bars included.
        HudElementRegistry.addLast(ResourceLocation.fromNamespaceAndPath("locoui_cursor", "pointer"), (graphics, tick) -> PointerDraw.draw(graphics));
    }
}
