package app.phynx.locoui.cursormod.core;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * The wire format of channel {@code locoui:cursor}, protocol {@value #VERSION}.
 * The server side is LocoUI's {@code CursorModLink}; both must agree byte for byte.
 *
 * <p>One message per payload, big-endian, the first byte is its type. Floats
 * are canvas pixels (LocoUI's canvas, 1920 x 1080 by default).
 *
 * <pre>
 * client -&gt; server
 *   0 HELLO   varint protocol, utf mod version, utf Minecraft version
 *   1 POS     varint session, float x, float y
 * server -&gt; client
 *   0 ACK     varint protocol
 *   1 OPEN    varint session, float x, y, minX, maxX, minY, maxY,
 *             rangeX, rangeY, speed, canvasW, canvasH, sizePx, int imageHash
 *   2 MOVE    varint session, float x, y
 *   3 CLOSE   varint session
 *   4 VISIBLE varint session, boolean
 *   5 STYLE   varint session, float sizePx
 *   6 IMAGE   int hash, float cellPerPx, float tipX, float tipY, varint length, png bytes
 * </pre>
 * {@code utf} is {@link DataOutputStream#writeUTF(String)}.
 */
public final class CursorProtocol {

    public static final String NAMESPACE = "locoui";
    public static final String PATH = "cursor";
    public static final int VERSION = 1;

    public static final int C_HELLO = 0;
    public static final int C_POS = 1;

    public static final int S_ACK = 0;
    public static final int S_OPEN = 1;
    public static final int S_MOVE = 2;
    public static final int S_CLOSE = 3;
    public static final int S_VISIBLE = 4;
    public static final int S_STYLE = 5;
    public static final int S_IMAGE = 6;

    private CursorProtocol() {
    }

    public static byte[] hello(String modVersion, String minecraftVersion) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(48);
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(C_HELLO);
            writeVarInt(out, VERSION);
            out.writeUTF(modVersion);
            out.writeUTF(minecraftVersion);
            out.flush();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] pos(int session, float x, float y) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(16);
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(C_POS);
            writeVarInt(out, session);
            out.writeFloat(x);
            out.writeFloat(y);
            out.flush();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void writeVarInt(DataOutputStream out, int value) throws IOException {
        int v = value;
        while ((v & ~0x7F) != 0) {
            out.writeByte((v & 0x7F) | 0x80);
            v >>>= 7;
        }
        out.writeByte(v);
    }

    public static int readVarInt(DataInputStream in) throws IOException {
        int value = 0;
        int shift = 0;
        while (true) {
            int b = in.readUnsignedByte();
            value |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return value;
            }
            shift += 7;
            if (shift > 28) {
                throw new IOException("varint too long");
            }
        }
    }
}
