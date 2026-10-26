package app.phynx.locoui.cursormod.mixin;

import app.phynx.locoui.cursormod.core.CursorCore;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * While a LocoUI page with a pointer is open, the mouse moves the pointer
 * instead of the head. The turn is taken exactly where the game would apply
 * it — sensitivity, smooth camera and inverted axes already in — so the
 * pointer feels like the mouse does everywhere else. Nothing else changes:
 * no camera, no field of view, no perspective.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @WrapOperation(
        method = "turnPlayer",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V")
    )
    private void locoui$moveThePointer(LocalPlayer player, double dx, double dy, Operation<Void> original) {
        CursorCore core = CursorCore.INSTANCE;
        if (core.consumesMouse()) {
            core.turn(dx, dy);
            core.flush(System.nanoTime());
            return;
        }
        original.call(player, dx, dy);
    }
}
