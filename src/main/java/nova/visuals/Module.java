package nova.visuals;

import net.minecraft.client.MinecraftClient;

public class Module {
    public enum Category { RENDER, PVP, HUD, WORLD }

    public final String name;
    public final Category category;
    public boolean enabled;
    /** GLFW key code; -1 means unbound. */
    public int keyCode = -1;

    public Module(String name, Category category, boolean enabled) {
        this.name = name;
        this.category = category;
        this.enabled = enabled;
    }

    public String displayName() {
        return switch (name) {
            case "LowEnd" -> "Режим слабого ПК";
            case "PlayerESP" -> "Подсветка игроков";
            case "MobESP" -> "Подсветка мобов";
            case "ProjectileESP" -> "Подсветка снарядов";
            case "ItemESP" -> "Подсветка предметов";
            case "StorageESP" -> "Подсветка хранилищ";
            case "Tracers" -> "Линии до игроков";
            case "Glow" -> "Свечение игроков";
            case "Hitboxes" -> "Контуры хитбоксов";
            case "Fullbright" -> "Полная яркость";
            case "Trajectories" -> "Траектория снарядов";
            case "TargetIndicator" -> "Маркер цели";
            case "NoHurtCam" -> "Без тряски камеры";
            case "Zoom (C)" -> "Зум";
            case "Watermark" -> "Название клиента";
            case "Coords" -> "Координаты";
            case "Potions" -> "Эффекты зелий";
            case "HotKeys" -> "Список клавиш";
            case "TargetHUD" -> "Информация о цели";
            case "Cooldowns" -> "Перезарядки предметов";
            case "ArmorHUD" -> "Броня";
            case "Keystrokes" -> "Нажатия клавиш";
            case "ItemCounter" -> "Счётчик предметов";
            case "Radar" -> "Радар";
            case "Combo" -> "Счётчик попаданий";
            case "ArrayList" -> "Список включённых функций";
            case "NoWeather" -> "Без дождя и грозы";
            default -> name;
        };
    }

    public String description() {
        return switch (name) {
            case "LowEnd" -> "Снижает нагрузку визуальных эффектов ради FPS.";
            case "PlayerESP" -> "Показывает контуры других игроков сквозь препятствия.";
            case "MobESP" -> "Отмечает мобов цветными контурами.";
            case "ProjectileESP" -> "Подсвечивает летящие снаряды.";
            case "ItemESP" -> "Отмечает выпавшие на землю предметы.";
            case "StorageESP" -> "Показывает сундуки и другие хранилища.";
            case "Tracers" -> "Рисует линии от камеры к игрокам.";
            case "Glow" -> "Включает эффект свечения у других игроков.";
            case "Hitboxes" -> "Показывает технические границы сущностей.";
            case "Fullbright" -> "Делает тёмные места заметно светлее.";
            case "Trajectories" -> "Показывает примерную траекторию снаряда в руке.";
            case "TargetIndicator" -> "Временно рисует 3D-маркер после удара по цели.";
            case "NoHurtCam" -> "Убирает визуальную тряску при получении урона.";
            case "Zoom (C)" -> "Удерживай назначенную клавишу для приближения.";
            case "Watermark" -> "Показывает название и версию клиента на экране.";
            case "Coords" -> "Отображает текущие координаты игрока.";
            case "Potions" -> "Показывает активные эффекты зелий.";
            case "HotKeys" -> "Показывает назначенные клавиши функций.";
            case "TargetHUD" -> "Показывает здоровье и экипировку цели.";
            case "Cooldowns" -> "Отображает перезарядку выбранных предметов.";
            case "ArmorHUD" -> "Показывает надетую броню и её прочность.";
            case "Keystrokes" -> "Отображает нажатия клавиш движения и атаки.";
            case "ItemCounter" -> "Показывает количество выбранных предметов.";
            case "Radar" -> "Показывает ближайшие сущности на мини-радаре.";
            case "Combo" -> "Считает последовательные попадания.";
            case "ArrayList" -> "Выводит список включённых модулей.";
            case "NoWeather" -> "Скрывает визуальные эффекты дождя и грозы.";
            default -> "Визуальная функция клиента.";
        };
    }

    public void toggle() {
        enabled = !enabled;
        if (enabled) onEnable(); else onDisable();
        Config.save();
        Hud.invalidate();
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick(MinecraftClient mc) {}
}
