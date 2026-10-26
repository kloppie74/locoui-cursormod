package app.phynx.locoui.cursormod.core;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * THE CLIENT-SIDE POINTER, without anything Minecraft-specific in it.
 *
 * <p>While LocoUI has a page with a pointer open, the mouse no longer turns the
 * head: {@link #turn} takes the very turn the game was about to apply and moves
 * the pointer by what LocoUI's server would have made of it (yaw / 360 x the
 * canvas width x the page's speed, pitch / 180 x its height), in the frame the
 * mouse moved. The pointer is drawn on top of the HUD ({@link #placement}) and
 * the server is told where it is ({@link #flush}), so hover and clicks use
 * exactly the position on screen.
 *
 * <p>Everything runs on the client's main thread: the turn, the drawing and
 * the server's messages (Fabric hands payloads to the main thread).
 */
public final class CursorCore {

    /** What the version glue does for the core. */
    public interface Platform {
        void send(byte[] payload);

        /** A new pointer picture (the server's 256-px cell, PNG). */
        void image(int hash, byte[] png);

        String modVersion();

        String minecraftVersion();
    }

    public static final CursorCore INSTANCE = new CursorCore();

    /** Degrees of yaw / pitch per unit of {@code LocalPlayer.turn} (Entity.turn multiplies by 0.15). */
    private static final double DEGREES_PER_TURN_UNIT = 0.15;

    /** The most positions sent a second: one a frame up to this. */
    private static final long MIN_SEND_GAP_NANOS = 1_000_000_000L / 250;

    private Platform platform;
    private boolean acknowledged;
    private boolean active;
    private boolean visible = true;
    private int session;

    private float x;
    private float y;
    private float minX;
    private float maxX;
    private float minY;
    private float maxY;
    private float rangeX;
    private float rangeY;
    private float speed;
    private float canvasW = 1920f;
    private float canvasH = 1080f;
    private float sizePx = 20f;

    private int imageHash;
    private int readyImage;
    private float cellPerPx = 27f / 20f;
    private float tipX = 1f / 16f;
    private float tipY = 3f / 64f;

    private boolean dirty;
    private long lastSent;

    private CursorCore() {
    }

    public void init(Platform platform) {
        this.platform = platform;
    }

    /** Joined a server: say hello. A server without LocoUI never answers, and nothing changes. */
    public void onJoin() {
        reset();
        if (platform != null) {
            platform.send(CursorProtocol.hello(platform.modVersion(), platform.minecraftVersion()));
        }
    }

    public void onDisconnect() {
        reset();
    }

    private void reset() {
        acknowledged = false;
        active = false;
        visible = true;
        dirty = false;
        session = 0;
    }

    /** Whether the mouse moves the pointer instead of the head right now. */
    public boolean consumesMouse() {
        return active;
    }

    /** Whether there is a pointer to draw this frame. */
    public boolean drawable() {
        return active && visible && readyImage != 0 && readyImage == imageHash;
    }

    /** The texture the glue registered for the current picture. */
    public int imageHash() {
        return imageHash;
    }

    /** The glue registered the picture with this hash. */
    public void imageReady(int hash) {
        readyImage = hash;
    }

    /**
     * A turn the game was about to apply ({@code LocalPlayer.turn(dx, dy)}),
     * with the player's sensitivity, smoothing and inversion already in it.
     */
    public void turn(double dx, double dy) {
        if (!active) {
            return;
        }
        double yaw = dx * DEGREES_PER_TURN_UNIT;
        double pitch = dy * DEGREES_PER_TURN_UNIT;
        float nx = clamp((float) (x + yaw / 360.0 * rangeX * speed), minX, maxX);
        float ny = clamp((float) (y + pitch / 180.0 * rangeY * speed), minY, maxY);
        if (nx != x || ny != y) {
            x = nx;
            y = ny;
            dirty = true;
        }
    }

    /** Sends where the pointer is when it moved — at most one position per {@link #MIN_SEND_GAP_NANOS}. */
    public void flush(long nowNanos) {
        if (!active || !dirty || platform == null) {
            return;
        }
        if (nowNanos - lastSent < MIN_SEND_GAP_NANOS) {
            return;
        }
        lastSent = nowNanos;
        dirty = false;
        platform.send(CursorProtocol.pos(session, x, y));
    }

    /** One message from the server. */
    public void onMessage(byte[] data) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            int type = in.readUnsignedByte();
            switch (type) {
                case CursorProtocol.S_ACK -> acknowledged = CursorProtocol.readVarInt(in) >= 1;
                case CursorProtocol.S_OPEN -> {
                    session = CursorProtocol.readVarInt(in);
                    x = in.readFloat();
                    y = in.readFloat();
                    minX = in.readFloat();
                    maxX = in.readFloat();
                    minY = in.readFloat();
                    maxY = in.readFloat();
                    rangeX = in.readFloat();
                    rangeY = in.readFloat();
                    speed = in.readFloat();
                    canvasW = in.readFloat();
                    canvasH = in.readFloat();
                    sizePx = in.readFloat();
                    imageHash = in.readInt();
                    visible = true;
                    dirty = false;
                    active = acknowledged && canvasW > 0 && canvasH > 0;
                }
                case CursorProtocol.S_MOVE -> {
                    if (CursorProtocol.readVarInt(in) == session) {
                        x = clamp(in.readFloat(), minX, maxX);
                        y = clamp(in.readFloat(), minY, maxY);
                    }
                }
                case CursorProtocol.S_CLOSE -> {
                    if (CursorProtocol.readVarInt(in) == session) {
                        active = false;
                    }
                }
                case CursorProtocol.S_VISIBLE -> {
                    if (CursorProtocol.readVarInt(in) == session) {
                        visible = in.readBoolean();
                    }
                }
                case CursorProtocol.S_STYLE -> {
                    if (CursorProtocol.readVarInt(in) == session) {
                        sizePx = in.readFloat();
                    }
                }
                case CursorProtocol.S_IMAGE -> {
                    int hash = in.readInt();
                    cellPerPx = in.readFloat();
                    tipX = in.readFloat();
                    tipY = in.readFloat();
                    int length = CursorProtocol.readVarInt(in);
                    if (length <= 0 || length > 4 * 1024 * 1024) {
                        return;
                    }
                    byte[] png = new byte[length];
                    in.readFully(png);
                    if (platform != null) {
                        platform.image(hash, png);
                    }
                }
                default -> {
                    // A newer server: a message this mod does not know.
                }
            }
        } catch (IOException e) {
            // A short or garbled payload: nothing to act on.
        }
    }

    /**
     * Where to draw the pointer's cell in GUI pixels, for a GUI [guiW] x [guiH]
     * big: LocoUI's layout contract — the canvas fitted into the window with one
     * uniform scale and centred — and its cursor size rule: the cell is
     * {@code sizePx x cellPerPx} canvas px tall, its tip at (tipX, tipY) of it.
     *
     * @return {left, top, side} of the square cell, in GUI px
     */
    public float[] placement(float guiW, float guiH) {
        float aspect = canvasW / canvasH;
        float regW;
        float regH;
        if (guiW / Math.max(guiH, 1e-6f) > aspect) {
            regH = guiH;
            regW = guiH * aspect;
        } else {
            regW = guiW;
            regH = guiW / aspect;
        }
        float offX = (guiW - regW) * 0.5f;
        float offY = (guiH - regH) * 0.5f;
        float px = offX + x / canvasW * regW;
        float py = offY + y / canvasH * regH;
        float side = sizePx * cellPerPx / canvasH * regH;
        return new float[] {px - tipX * side, py - tipY * side, side};
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
