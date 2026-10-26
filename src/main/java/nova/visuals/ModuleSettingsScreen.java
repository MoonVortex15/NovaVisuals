package nova.visuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Per-module configuration opened with right click in ClickGUI. */
public final class ModuleSettingsScreen extends Screen {
    private final Module module;
    private final Screen parent;
    private boolean waitingForKey;
    private int px, py, pw = 330, ph;

    public ModuleSettingsScreen(Module module, Screen parent) {
        super(Text.literal("Настройки: " + module.displayName()));
        this.module = module;
        this.parent = parent;
    }

    private void layout() {
        ph = module == Modules.TARGET_INDICATOR ? 252 : 190;
        px = (width - pw) / 2;
        py = (height - ph) / 2;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        layout();
        ctx.fill(0, 0, width, height, 0x70000000);
        Ui.card(ctx, px, py, pw, ph, 14);
        Ui.text(ctx, net.minecraft.client.MinecraftClient.getInstance(), module.displayName(), px + 18, py + 16, 0xFFFFFFFF);
        String desc = module.description();
        Ui.text(ctx, net.minecraft.client.MinecraftClient.getInstance(), textRenderer.trimToWidth(desc, pw - 36), px + 18, py + 31, Ui.SUB);
        Ui.text(ctx, net.minecraft.client.MinecraftClient.getInstance(), "Категория: " + categoryName(module.category), px + 18, py + 43, Ui.SUB);
        ctx.fill(px + 14, py + 55, px + pw - 14, py + 56, 0x22FFFFFF);

        row(ctx, mx, my, 0, "Функция", module.enabled ? "ВКЛ" : "ВЫКЛ", module.enabled);
        row(ctx, mx, my, 1, "Клавиша", waitingForKey ? "Нажми клавишу..." : NovaClient.keyName(module.keyCode), waitingForKey);
        row(ctx, mx, my, 2, "Убрать клавишу", "BACKSPACE", false);

        if (module == Modules.TARGET_INDICATOR) {
            row(ctx, mx, my, 3, "Форма маркера", shapeName(), false);
            row(ctx, mx, my, 4, "Размер", String.format(java.util.Locale.ROOT, "%.2f", TargetIndicatorSettings.size), false);
            row(ctx, mx, my, 5, "Скорость вращения", String.format(java.util.Locale.ROOT, "%.1fx", TargetIndicatorSettings.speed), false);
            row(ctx, mx, my, 6, "Длительность", (TargetIndicatorSettings.durationMs / 1000.0) + " сек", false);
            String[] colors = {"Радуга", "Голубой", "Фиолетовый"};
            row(ctx, mx, my, 7, "Цвет", colors[TargetIndicatorSettings.colorMode], false);
        }
        Ui.text(ctx, net.minecraft.client.MinecraftClient.getInstance(), "Нажми на строку, чтобы изменить • ESC — назад", px + 16, py + ph - 17, 0xFF77778D);
    }

    private void row(DrawContext ctx, int mx, int my, int index, String label, String value, boolean active) {
        int y = py + 64 + index * 19;
        int bottom = y + 16;
        boolean hover = mx >= px + 14 && mx <= px + pw - 14 && my >= y && my <= bottom;
        Ui.roundRect(ctx, px + 14, y, pw - 28, 16, 5, hover ? 0xFF27273A : 0xFF181820);
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        Ui.text(ctx, mc, label, px + 22, y + 4, 0xFFE5E5F2);
        int color = active ? 0xFFB9ABFF : 0xFF9B9BB2;
        Ui.text(ctx, mc, value, px + pw - 22 - Ui.textW(mc, value), y + 4, color);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        layout();
        int index = (int) ((my - (py + 64)) / 19);
        if (mx < px + 14 || mx > px + pw - 14 || index < 0) return super.mouseClicked(mx, my, button);
        int rowY = py + 64 + index * 19;
        if (my < rowY || my > rowY + 16) return super.mouseClicked(mx, my, button);
        if (index == 0) {
            module.toggle();
        } else if (index == 1) {
            waitingForKey = true;
        } else if (index == 2) {
            module.keyCode = -1;
            waitingForKey = false;
            Config.save();
        } else if (module == Modules.TARGET_INDICATOR) {
            switch (index) {
                case 3 -> TargetIndicatorSettings.shape = (TargetIndicatorSettings.shape + 1) % 3;
                case 4 -> TargetIndicatorSettings.size = TargetIndicatorSettings.size >= 1.75f ? 0.35f : Math.min(1.8f, TargetIndicatorSettings.size + 0.15f);
                case 5 -> TargetIndicatorSettings.speed = TargetIndicatorSettings.speed >= 3.9f ? 0.3f : Math.min(4f, TargetIndicatorSettings.speed + 0.3f);
                case 6 -> TargetIndicatorSettings.durationMs = TargetIndicatorSettings.durationMs >= 5000 ? 300 : Math.min(5000, TargetIndicatorSettings.durationMs + 300);
                case 7 -> TargetIndicatorSettings.colorMode = (TargetIndicatorSettings.colorMode + 1) % 3;
                default -> { return super.mouseClicked(mx, my, button); }
            }
            Config.save();
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (waitingForKey) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                waitingForKey = false;
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                module.keyCode = -1;
                waitingForKey = false;
                Config.save();
            } else if (keyCode != GLFW.GLFW_KEY_RIGHT_SHIFT) {
                module.keyCode = keyCode;
                waitingForKey = false;
                Config.save();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            client.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private String shapeName() {
        return switch (TargetIndicatorSettings.shape) {
            case 1 -> "Круг (3D-кольцо)";
            case 2 -> "Квадрат";
            default -> "3D-куб";
        };
    }

    private String categoryName(Module.Category category) {
        return switch (category) {
            case RENDER -> "Визуалы";
            case PVP -> "Помощники боя";
            case HUD -> "Интерфейс";
            case WORLD -> "Мир";
        };
    }

    @Override public boolean shouldPause() { return false; }
}
