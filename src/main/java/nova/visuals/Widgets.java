package nova.visuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Widgets {
    private static final Minecraft mc = Minecraft.getInstance();

    public static void renderAll(GuiGraphics graphics, float tickDelta) {
        if (mc.options.hideGui || mc.player == null) return;

        // 1. Watermark (название, фпс, пинг, время)
        renderWatermark(graphics, 10, 10);

        // 2. Target HUD (по центру экрана, если есть цель)
        Player target = TargetIndicatorSettings.getCurrentTarget();
        if (target != null) {
            renderTargetHud(graphics, mc.getWindow().getGuiScaledWidth() / 2 - 65, mc.getWindow().getGuiScaledHeight() / 2 + 15, target);
        }

        // 3. Item Cooldown (кулдаун золотого яблока / предметов)
        renderItemCooldowns(graphics, 10, 36);

        // 4. Keybinds / Active Modules (список забинженных функций)
        renderKeybinds(graphics, 10, 64);
    }

    private static void renderWatermark(GuiGraphics graphics, int x, int y) {
        String fps = "FPS: " + Minecraft.isDebugFps();
        String ping = "Ping: " + (mc.getConnection() != null && mc.player != null && mc.getConnection().getPlayerInfo(mc.player.getUUID()) != null 
                        ? mc.getConnection().getPlayerInfo(mc.player.getUUID()).getLatency() : 0) + "ms";
        String time = new SimpleDateFormat("HH:mm").format(new Date());

        String text = "NovaVisuals | " + fps + " | " + ping + " | " + time;
        int width = mc.font.width(text) + 12;
        int height = 18;

        Widget.drawRoundedRect(graphics, x, y, width, height, 4, 0x90000000);
        graphics.drawString(mc.font, text, x + 6, y + 5, 0xFFFFFFFF, true);
    }

    private static void renderTargetHud(GuiGraphics graphics, int x, int y, Player target) {
        int width = 130;
        int height = 40;

        Widget.drawRoundedRect(graphics, x, y, width, height, 6, 0xAA101010);

        // Ник и здоровье
        graphics.drawString(mc.font, target.getName().getString(), x + 36, y + 6, 0xFFFFFFFF, false);
        graphics.drawString(mc.font, "HP: " + (int) target.getHealth(), x + 36, y + 18, 0xFFFF5555, false);

        // Голова игрока
        try {
            graphics.blit(target.getSkin().texture(), x + 6, y + 6, 8, 8, 8, 8, 8, 8, 64, 64);
        } catch (Exception ignored) {}

        // Предмет в руке
        ItemStack mainHand = target.getMainHandItem();
        if (!mainHand.isEmpty()) {
            graphics.renderItem(mainHand, x + 104, y + 12);
            graphics.renderItemDecorations(mc.font, mainHand, x + 104, y + 12);
        }
    }

    private static void renderItemCooldowns(GuiGraphics graphics, int x, int y) {
        Item item = Items.GOLDEN_APPLE;
        float cooldown = mc.player.getCooldowns().getCooldownPercent(item, mc.getFrameTime());

        if (cooldown > 0.0f) {
            int width = 85;
            int height = 22;

            Widget.drawRoundedRect(graphics, x, y, width, height, 4, 0x90000000);
            graphics.renderItem(new ItemStack(item), x + 3, y + 3);

            String text = String.format("G-Apple: %.1fs", cooldown * 4.5f);
            graphics.drawString(mc.font, text, x + 24, y + 7, 0xFFFF5555, true);
        }
    }

    private static void renderKeybinds(GuiGraphics graphics, int startX, int startY) {
        int yOffset = 0;

        graphics.drawString(mc.font, "Keybinds", startX, startY + yOffset, 0xFF00D7FF, true);
        yOffset += 12;

        // Выводим все включенные модули и их бинды
        for (Module module : Modules.getModules()) {
            if (module.isEnabled()) {
                String line = module.getName() + " [" + module.getKeyName() + "]";
                int lineWidth = mc.font.width(line) + 8;

                Widget.drawRoundedRect(graphics, startX, startY + yOffset - 2, lineWidth, 12, 2, 0x60000000);
                graphics.drawString(mc.font, line, startX + 4, startY + yOffset, 0xFFFFFFFF, true);
                
                yOffset += 14;
            }
        }
    }
}
