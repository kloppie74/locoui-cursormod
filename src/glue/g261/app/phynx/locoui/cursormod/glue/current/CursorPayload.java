package app.phynx.locoui.cursormod.glue.current;

import app.phynx.locoui.cursormod.core.CursorProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** One message on {@code locoui:cursor}: raw bytes, read and written by CursorCore. */
public record CursorPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<CursorPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(CursorProtocol.NAMESPACE, CursorProtocol.PATH));

    public static final StreamCodec<FriendlyByteBuf, CursorPayload> CODEC = StreamCodec.of(
        (buf, payload) -> buf.writeBytes(payload.data()),
        buf -> {
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return new CursorPayload(bytes);
        });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
