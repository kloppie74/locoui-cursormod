# LocoUI Cursor

A small client-side Fabric mod that makes the pointer of [LocoUI](https://phynx.app) menus instant.

Without the mod, LocoUI draws its pointer on the server: the mouse turns your head, the
server reads the turn and draws the pointer in a boss bar. That pointer is always one round
trip behind your hand (your ping plus a frame or two). With the mod, the pointer is drawn
by your own game, in the same frame you move the mouse — 0 ms, at any ping.

- **Minecraft 1.21.4 – 26.3**, Fabric Loader 0.16+ and Fabric API.
- **Works with Iris and every shader pack**: the pointer is drawn in the GUI, like the hotbar.
- **No FOV, F5 or camera tricks**: while a LocoUI menu is open, the mouse moves the pointer
  instead of your head. Close the menu and the mouse turns your head again.
- **Does nothing on other servers.** The mod says hello when you join; a server without
  LocoUI never answers, and the mod stays out of the way.
- Players without the mod keep LocoUI's normal server-drawn pointer.

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/) and [Fabric API](https://modrinth.com/mod/fabric-api)
   for your Minecraft version.
2. Download the jar for your Minecraft version from the
   [releases](https://github.com/kloppie74/locoui-cursormod/releases) and put it in `.minecraft/mods`:

   | Minecraft | Jar |
   |---|---|
   | 1.21.4 | `locoui-cursor-1.0.0+1.21.4.jar` |
   | 1.21.5 | `locoui-cursor-1.0.0+1.21.5.jar` |
   | 1.21.6 – 1.21.8 | `locoui-cursor-1.0.0+1.21.6-1.21.8.jar` |
   | 1.21.9 – 1.21.10 | `locoui-cursor-1.0.0+1.21.9-1.21.10.jar` |
   | 1.21.11 | `locoui-cursor-1.0.0+1.21.11.jar` |
   | 26.1.x | `locoui-cursor-1.0.0+26.1.jar` |
   | 26.2 | `locoui-cursor-1.0.0+26.2.jar` |
   | 26.3 | `locoui-cursor-1.0.0+26.3.jar` |

The server needs LocoUI 26.11.1 or newer. There is nothing to configure.

When you open the chat, the pause menu or any other screen, the pointer steps aside for your
normal mouse cursor and comes back where it was when you close it. F1 hides it with the rest
of the HUD.

## How it works

While LocoUI has a page with a pointer open, the mod catches the turn the game was about
to apply to your head (`LocalPlayer.turn`, with your sensitivity, smoothing and inverted
mouse already in it) and moves the pointer by exactly what LocoUI's server would have made
of it. It draws the pointer on top of the HUD and tells the server where it is, so hover
and clicks use the position you see.

The server stays in charge: it opens and closes the pointer, sends its picture and size,
and may move it (for example when a page warps the pointer). The mod only reports where
the mouse moved it.

## Protocol

Plugin channel `locoui:cursor`, protocol 1. One message per payload, big-endian; the
first byte is the message type. Floats are LocoUI canvas pixels (1920 x 1080 by default).
`utf` is Java's modified UTF-8 (`DataOutputStream.writeUTF`).

| Direction | Type | Name | Body |
|---|---|---|---|
| client → server | 0 | HELLO | varint protocol, utf mod version, utf Minecraft version |
| client → server | 1 | POS | varint session, float x, float y |
| server → client | 0 | ACK | varint protocol |
| server → client | 1 | OPEN | varint session, float x, y, minX, maxX, minY, maxY, rangeX, rangeY, speed, canvasW, canvasH, sizePx, int imageHash |
| server → client | 2 | MOVE | varint session, float x, y |
| server → client | 3 | CLOSE | varint session |
| server → client | 4 | VISIBLE | varint session, boolean |
| server → client | 5 | STYLE | varint session, float sizePx |
| server → client | 6 | IMAGE | int hash, float cellPerPx, float tipX, float tipY, varint length, PNG bytes |

The pointer moves `yaw / 360 × rangeX × speed` canvas px across and `pitch / 180 × rangeY × speed`
down, clamped to `[minX, maxX] × [minY, maxY]`. It is drawn with LocoUI's layout contract:
the canvas is fitted into the window with one uniform scale and centred; the picture is a
square cell `sizePx × cellPerPx` canvas px tall whose tip sits at `(tipX, tipY)` of the cell.
The pointer quad is drawn white at alpha 254 — LocoUI's menu shaders leave that colour alone.

## Build

Gradle 9 runs on Java 25 (Fabric Loom 1.19 needs it). The 1.21.x jars are built for Java 21,
the 26.x jars for Java 25, so both JDKs must be installed.

```bash
./gradlew build
```

Every Minecraft generation is its own subproject (`versions/<mc>`, whose `gradle.properties`
says which Minecraft versions it runs on); its jar lands in `versions/<mc>/build/libs`. The shared code is in `common/`, and the few lines that differ
between Minecraft versions are in `glue/<generation>/`:

| Glue | Minecraft | Differences |
|---|---|---|
| `g1214` | 1.21.4 | `ResourceLocation`, `HudRenderCallback`, `PoseStack`, `RenderType::guiTextured`, `DynamicTexture(NativeImage)` |
| `g1215` | 1.21.5 | as 1.21.4, `DynamicTexture(Supplier, NativeImage)` |
| `g1216` | 1.21.6 – 1.21.10 | `HudElementRegistry`, `Matrix3x2fStack`, `RenderPipelines.GUI_TEXTURED` |
| `g12111` | 1.21.11 | as 1.21.6, `Identifier` |
| `g261` | 26.1 | unobfuscated; `GuiGraphicsExtractor`, `clientboundPlay` / `serverboundPlay` |
| `g262` | 26.2 – 26.3 | as 26.1, the open screen and the F1 state live in `Gui` |

## License

MIT — see [LICENSE](LICENSE).
