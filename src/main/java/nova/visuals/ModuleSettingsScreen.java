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
        pw = Math.min(330, Math.max(250, width - 16));
        int wanted = module == Modules.TRAJECTORIES ? 365
                : module == Modules.TARGET_INDICATOR ? 252
                : module == Modules.CUSTOM_HAND ? 350
                : (module == Modules.HIT_PARTICLES || module == Modules.DAMAGE_PARTICLES
                    || module == Modules.CRITICAL_EFFECTS || module == Modules.KILL_EFFECTS) ? 255
                : module == Modules.TRAILS ? 235
                : module == Modules.BLOCK_OVERLAY ? 250
                : module == Modules.TIME_CHANGER ? 230 : 190;
        ph = Math.min(wanted, Math.max(220, height - 12));
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
        if (module == Modules.TRAJECTORIES) {
            row(ctx, mx, my, 3, "Стрелы / лук", onOff(TrajectorySettings.arrows), TrajectorySettings.arrows);
            row(ctx, mx, my, 4, "Эндер-жемчуг", onOff(TrajectorySettings.pearls), TrajectorySettings.pearls);
            row(ctx, mx, my, 5, "Зелья", onOff(TrajectorySettings.potions), TrajectorySettings.potions);
            row(ctx, mx, my, 6, "Снежки и яйца", onOff(TrajectorySettings.snowballsEggs), TrajectorySettings.snowballsEggs);
            row(ctx, mx, my, 7, "Стиль линии", new String[]{"Градиент", "Радуга", "Сплошной"}[TrajectorySettings.style], false);
            row(ctx, mx, my, 8, "Палитра", new String[]{"Фиолетовый → циан", "Циан", "Розовый", "Мятный"}[TrajectorySettings.color], false);
            row(ctx, mx, my, 9, "Толщина", TrajectorySettings.thickness + " px", false);
            row(ctx, mx, my, 10, "Точки на линии", onOff(TrajectorySettings.dots), TrajectorySettings.dots);
            row(ctx, mx, my, 11, "Маркер попадания", onOff(TrajectorySettings.impactMarker), TrajectorySettings.impactMarker);
            row(ctx, mx, my, 12, "Свечение конца", onOff(TrajectorySettings.endpointGlow), TrajectorySettings.endpointGlow);
        }
        if (module == Modules.HIT_PARTICLES) {
            row(ctx, mx, my, 3, "Тип частиц", particleStyle(VisualEffectsSettings.hitStyle), false);
            row(ctx, mx, my, 4, "Количество", "" + VisualEffectsSettings.hitCount, false);
        }
        if (module == Modules.DAMAGE_PARTICLES) {
            row(ctx, mx, my, 3, "Количество", "" + VisualEffectsSettings.damageCount, false);
        }
        if (module == Modules.CRITICAL_EFFECTS) {
            row(ctx, mx, my, 3, "Тип частиц", particleStyle(VisualEffectsSettings.criticalStyle), false);
            row(ctx, mx, my, 4, "Количество", "" + VisualEffectsSettings.criticalCount, false);
        }
        if (module == Modules.KILL_EFFECTS) {
            row(ctx, mx, my, 3, "Тип эффекта", particleStyle(VisualEffectsSettings.killStyle), false);
            row(ctx, mx, my, 4, "Количество", "" + VisualEffectsSettings.killCount, false);
        }
        if (module == Modules.TRAILS) {
            row(ctx, mx, my, 3, "Стиль следа", trailStyle(), false);
            row(ctx, mx, my, 4, "Плотность", "" + VisualEffectsSettings.trailDensity, false);
        }
        if (module == Modules.CUSTOM_HAND) {
            row(ctx, mx, my, 3, "Положение X", String.format(java.util.Locale.ROOT, "%.2f", VisualEffectsSettings.handX), false);
            row(ctx, mx, my, 4, "Положение Y", String.format(java.util.Locale.ROOT, "%.2f", VisualEffectsSettings.handY), false);
            row(ctx, mx, my, 5, "Положение Z", String.format(java.util.Locale.ROOT, "%.2f", VisualEffectsSettings.handZ), false);
            row(ctx, mx, my, 6, "Размер предмета", String.format(java.util.Locale.ROOT, "%.1fx", VisualEffectsSettings.handScale), false);
            row(ctx, mx, my, 7, "Поворот X", VisualEffectsSettings.handRotX + "°", false);
            row(ctx, mx, my, 8, "Поворот Y", VisualEffectsSettings.handRotY + "°", false);
            row(ctx, mx, my, 9, "Поворот Z", VisualEffectsSettings.handRotZ + "°", false);
            row(ctx, mx, my, 10, "Анимация взмаха", VisualEffectsSettings.handAnimation == 1 ? "ВЫКЛ" : "ОБЫЧНАЯ", false);
            row(ctx, mx, my, 11, "Скорость анимации", String.format(java.util.Locale.ROOT, "%.2fx", VisualEffectsSettings.handAnimationSpeed), false);
        }
        if (module == Modules.BLOCK_OVERLAY) {
            row(ctx, mx, my, 3, "Цвет контура", new String[]{"Фиолетовый", "Циан", "Розовый", "Мятный"}[VisualSettings.blockColor], false);
            row(ctx, mx, my, 4, "Толщина", VisualSettings.blockThickness + " px", false);
        }
        if (module == Modules.TIME_CHANGER) {
            row(ctx, mx, my, 3, "Время", new String[]{"День", "Ночь", "Рассвет", "Закат"}[VisualSettings.timePreset], false);
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
        } else if (module == Modules.TRAJECTORIES) {
            switch (index) {
                case 3 -> TrajectorySettings.arrows = !TrajectorySettings.arrows;
                case 4 -> TrajectorySettings.pearls = !TrajectorySettings.pearls;
                case 5 -> TrajectorySettings.potions = !TrajectorySettings.potions;
                case 6 -> TrajectorySettings.snowballsEggs = !TrajectorySettings.snowballsEggs;
                case 7 -> TrajectorySettings.style = (TrajectorySettings.style + 1) % 3;
                case 8 -> TrajectorySettings.color = (TrajectorySettings.color + 1) % 4;
                case 9 -> TrajectorySettings.thickness = TrajectorySettings.thickness >= 4 ? 1 : TrajectorySettings.thickness + 1;
                case 10 -> TrajectorySettings.dots = !TrajectorySettings.dots;
                case 11 -> TrajectorySettings.impactMarker = !TrajectorySettings.impactMarker;
                case 12 -> TrajectorySettings.endpointGlow = !TrajectorySettings.endpointGlow;
                default -> { return super.mouseClicked(mx, my, button); }
            }
            Config.save();
        } else if (module == Modules.HIT_PARTICLES) {
            if (index == 3) VisualEffectsSettings.hitStyle = (VisualEffectsSettings.hitStyle + 1) % 4;
            else if (index == 4) VisualEffectsSettings.hitCount = VisualEffectsSettings.hitCount >= 24 ? 3 : VisualEffectsSettings.hitCount + 3;
            else return super.mouseClicked(mx, my, button);
            Config.save();
        } else if (module == Modules.DAMAGE_PARTICLES) {
            if (index != 3) return super.mouseClicked(mx, my, button);
            VisualEffectsSettings.damageCount = VisualEffectsSettings.damageCount >= 16 ? 1 : VisualEffectsSettings.damageCount + 1;
            Config.save();
        } else if (module == Modules.CRITICAL_EFFECTS) {
            if (index == 3) VisualEffectsSettings.criticalStyle = (VisualEffectsSettings.criticalStyle + 1) % 4;
            else if (index == 4) VisualEffectsSettings.criticalCount = VisualEffectsSettings.criticalCount >= 28 ? 3 : VisualEffectsSettings.criticalCount + 3;
            else return super.mouseClicked(mx, my, button);
            Config.save();
        } else if (module == Modules.KILL_EFFECTS) {
            if (index == 3) VisualEffectsSettings.killStyle = (VisualEffectsSettings.killStyle + 1) % 4;
            else if (index == 4) VisualEffectsSettings.killCount = VisualEffectsSettings.killCount >= 40 ? 6 : VisualEffectsSettings.killCount + 4;
            else return super.mouseClicked(mx, my, button);
            Config.save();
        } else if (module == Modules.TRAILS) {
            if (index == 3) VisualEffectsSettings.trailStyle = (VisualEffectsSettings.trailStyle + 1) % 4;
            else if (index == 4) VisualEffectsSettings.trailDensity = VisualEffectsSettings.trailDensity >= 4 ? 1 : VisualEffectsSettings.trailDensity + 1;
            else return super.mouseClicked(mx, my, button);
            Config.save();
        } else if (module == Modules.CUSTOM_HAND) {
            switch (index) {
                case 3 -> VisualEffectsSettings.handX = stepFloat(VisualEffectsSettings.handX, -0.6f, 0.6f, 0.05f);
                case 4 -> VisualEffectsSettings.handY = stepFloat(VisualEffectsSettings.handY, -0.6f, 0.6f, 0.05f);
                case 5 -> VisualEffectsSettings.handZ = stepFloat(VisualEffectsSettings.handZ, -0.6f, 0.6f, 0.05f);
                case 6 -> VisualEffectsSettings.handScale = VisualEffectsSettings.handScale >= 1.5f ? 0.5f : Math.min(1.5f, VisualEffectsSettings.handScale + 0.1f);
                case 7 -> VisualEffectsSettings.handRotX = stepAngle(VisualEffectsSettings.handRotX);
                case 8 -> VisualEffectsSettings.handRotY = stepAngle(VisualEffectsSettings.handRotY);
                case 9 -> VisualEffectsSettings.handRotZ = stepAngle(VisualEffectsSettings.handRotZ);
                case 10 -> VisualEffectsSettings.handAnimation = 1 - VisualEffectsSettings.handAnimation;
                case 11 -> VisualEffectsSettings.handAnimationSpeed = VisualEffectsSettings.handAnimationSpeed >= 3.0f ? 0.25f : Math.min(3.0f, Math.round((VisualEffectsSettings.handAnimationSpeed + 0.25f) * 100.0f) / 100.0f);
                default -> { return super.mouseClicked(mx, my, button); }
            }
            Config.save();
        } else if (module == Modules.BLOCK_OVERLAY) {
            if (index == 3) VisualSettings.blockColor = (VisualSettings.blockColor + 1) % 4;
            else if (index == 4) VisualSettings.blockThickness = VisualSettings.blockThickness >= 4 ? 1 : VisualSettings.blockThickness + 1;
            else return super.mouseClicked(mx, my, button);
            Config.save();
        } else if (module == Modules.TIME_CHANGER) {
            if (index != 3) return super.mouseClicked(mx, my, button);
            VisualSettings.timePreset = (VisualSettings.timePreset + 1) % 4;
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

    private String particleStyle(int style) {
        return new String[]{"Критические", "Зачарованные", "Светящиеся", "Душевное пламя"}[style];
    }
    private String trailStyle() {
        return new String[]{"Светящиеся", "Душевное пламя", "Портал", "Облако"}[VisualEffectsSettings.trailStyle];
    }
    private float stepFloat(float value, float min, float max, float step) {
        return value >= max ? min : Math.min(max, Math.round((value + step) * 100f) / 100f);
    }
    private int stepAngle(int value) { return value >= 180 ? -180 : value + 15; }

    private String onOff(boolean value) { return value ? "ВКЛ" : "ВЫКЛ"; }

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
