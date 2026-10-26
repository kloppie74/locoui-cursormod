package app.phynx.locoui.cursormod.mixin.family;

import app.phynx.locoui.cursormod.core.CursorCore;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While a LocoUI page with a pointer is open, the mouse moves the pointer
 * instead of the head. The turn is taken where it lands — {@code Entity.turn}
 * of the local player, sensitivity, smooth camera and inverted axes already in
 * — so it is caught however the game got there: vanilla's mouse handler, or a
 * launcher or mod with mouse handling of its own (Lunar Client, Ixeris).
 * Nothing else changes: no camera, no field of view, no perspective.
 */
@Mixin(Entity.class)
public abstract class PlayerTurnMixin {

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void locoui$moveThePointer(double dx, double dy, CallbackInfo ci) {
        CursorCore core = CursorCore.INSTANCE;
        if (core.consumesMouse() && (Object) this == Minecraft.getInstance().player) {
            core.turn(dx, dy);
            core.flush(System.nanoTime());
            ci.cancel();
        }
    }
}
