# NovaVisuals — Minecraft 1.21.4 / Fabric

Client-side visual mod. Built for Java 21 and Fabric Loader.

## Controls
- **Right Shift** — open/close the ClickGUI.
- **Left-click** a module — toggle it.
- **Right-click** a module — open its settings.
- In a module's settings, click **Keybind** and press a key to assign it. Click **Unbind key** (or press Backspace/Delete while capturing) to clear it.
- **Zoom** uses `C` by default; its key can be changed from the Zoom module settings.
- Open the chat to drag HUD widgets.

## Target Indicator
The target marker appears around the last attacked living entity. Its settings include square/diamond shape, size, rotation speed, display duration and color mode. Settings and keybinds are saved in `config/novavisuals.properties`.

## Performance
The **LowEnd** module is enabled by default and reduces visual range, target counts, fills and some render detail. The renderer also automatically switches to lite rendering when FPS stays low. Disable visual modules you do not need for the best performance on older hardware.

## Build
Use Java 21 and Gradle 8.12:

```sh
gradle build --no-daemon
```

The output JAR is written to `build/libs/`.
