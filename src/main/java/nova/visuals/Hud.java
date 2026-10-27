package nova.visuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Hud {
    private static final Minecraft mc = Minecraft.getInstance();

    public static void render(GuiGraphics graphics, float tickDelta) {
        if (mc.options.hideGui || mc.player == null) {
            return;
        }

        // 1. Отрисовка Watermark (название, FPS, пинг, время) в левом верхнем углу
        renderWatermark(graphics, 10, 10);

        // 2. Отрисовка Target HUD (появляется при наличии цели / ударе)
        // Здесь можно подставить реальную цель из твоего менеджера (например, TargetManager.getTarget())
        Player target = getTargetPlayer(); 
        if (target != null) {
            renderTargetHud(graphics, mc.getWindow().getGuiScaledWidth() / 2 - 60, mc.getWindow().getGuiScaledHeight() / 2 + 15, target);
        }

        // 3. Отрисовка кулдаунов предметов (например, золотого яблока)
        renderItemCooldowns(graphics, 10, 40);

        // 4. Отрисовка забинженных функций (Keybinds Display)
        renderKeybinds(graphics, 10, 80);
    }

    private static void renderWatermark(GuiGraphics graphics, int x, int y) {
        String fps = "FPS: " + Minecraft.isDebugFps();
        String ping = "Ping: " + (mc.getConnection() != null && mc.player != null && mc.getConnection().getPlayerInfo(mc.player.getUUID()) != null 
                        ? mc.getConnection().getPlayerInfo(mc.player.getUUID()).getLatency() : 0) + "ms";
        String time = new SimpleDateFormat("HH:mm").format(new Date());

        String text = "NovaVisuals | " + fps + " | " + ping + " | " + time;
        int width = mc.font.width(text) + 12;
        int height = 18;

        // Полупрозрачная закругленная плашка (фон)
        drawRoundedRect(graphics, x, y, width, height, 4, 0x90000000);

        // Текст
        graphics.drawString(mc.font, text, x + 6, y + 5, 0xFFFFFFFF, true);
    }

    private static void renderTargetHud(GuiGraphics graphics, int x, int y, Player target) {
        int width = 130;
        int height = 40;

        // Полупрозрачный фон плашки цели
        drawRoundedRect(graphics, x, y, width, height, 6, 0xAA101010);

        // Ник игрока
        graphics.drawString(mc.font, target.getName().getString(), x + 36, y + 6, 0xFFFFFFFF, false);

        // Здоровье
        String hp = "HP: " + (int) target.getHealth();
        graphics.drawString(mc.font, hp, x + 36, y + 18, 0xFFFF5555, false);

        // Иконка / голова игрока (если доступна текстура скина)
        try {
            graphics.blit(target.getSkin().texture(), x + 6, y + 6, 8, 8, 8, 8, 8, 8, 64, 64);
        } catch (Exception ignored) {}

        // Предмет в главной руке цели
        ItemStack mainHandItem = target.getMainHandItem();
        if (!mainHandItem.isEmpty()) {
            graphics.renderItem(mainHandItem, x + 104, y + 12);
            graphics.renderItemDecorations(mc.font, mainHandItem, x + 104, y + 12);
        }
    }

    private static void renderItemCooldowns(GuiGraphics graphics, int x, int y) {
        // Проверяем кулдаун золотого яблока как пример основного PvP-предмета
        Item item = Items.GOLDEN_APPLE;
        float cooldown = mc.player.getCooldowns().getCooldownPercent(item, mc.getFrameTime());

        if (cooldown > 0.0f) {
            int width = 85;
            int height = 22;

            drawRoundedRect(graphics, x, y, width, height, 4, 0x90000000);

            graphics.renderItem(new ItemStack(item), x + 3, y + 3);

            // Переводим проценты кулдауна в секунды (у золотого яблока обычно кулдаун 4.5 сек)
            String text = String.format("G-Apple: %.1fs", cooldown * 4.5f);
            graphics.drawString(mc.font, text, x + 24, y + 7, 0xFFFF5555, true);
        }
    }

    private static void renderKeybinds(GuiGraphics graphics, int startX, int startY) {
        int yOffset = 0;

        // Заголовок плашки биндов
        graphics.drawString(mc.font, "Keybinds", startX, startY + yOffset, 0xFF00D7FF, true);
        yOffset += 12;

        // Пример вывода активных модулей (можно заменить на реальный список из твоего Modules.java)
        // Пример: перебираем модули, которые включены
        for (Module module : Modules.getModules()) {
            if (module.isEnabled()) {
                String line = module.getName() + " [" + module.getKeyName() + "]";
                
                // Рисуем подложку под каждый пункт или общую плашку
                drawRoundedRect(graphics, startX, startY + yOffset - 2, mc.font.width(line) + 8, 12, 2, 0x60000000);
                graphics.drawString(mc.font, line, startX + 4, startY + yOffset, 0xFFFFFFFF, true);
                
                yOffset += 14;
            }
        }
    }

    // Вспомогательный метод для рисования скругленных полупрозрачных прямоугольников
    private static void drawRoundedRect(GuiGraphics graphics, int x, int y, int width, int height, int radius, int color) {
        // Рендерим основную центральную часть и края
        graphics.fill(x + radius, y, x + width - radius, y + height, color);
        graphics.fill(x, y + radius, x + radius, y + height - radius, color);
        graphics.fill(x + width - radius, y + radius, x + width, y + height - radius, color);
        graphics.fill(x + radius, y + radius, x + width - radius, y + height - radius, color);
    }

    // Заглушка для получения цели (если в проекте уже есть логика TargetIndicator, замени этот метод на вызов своего класса)
    private static Player getTargetPlayer() {
        // Если у тебя в TargetIndicatorSettings или State хранится текущая цель, верни ее здесь.
        // Возвращаем null, если никто не атакован.
        return null;
    }
}
