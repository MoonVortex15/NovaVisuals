package nova.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.Comparator;
import java.util.List;

import static nova.visuals.Modules.*;

public final class Hud {
    private static List<Module> activeCache;
    private static long activeBuilt;

    public static void invalidate() { activeBuilt = 0; }

    public static void render(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        boolean edit = mc.currentScreen instanceof ChatScreen;

        // 1. Рендеринг кастомных виджетов
        for (Widget wd : Widgets.ALL) {
            if (!wd.module.enabled) continue;
            try {
                wd.draw(ctx, mc, wd.px(sw), wd.py(sh), edit);
            } catch (Throwable t) {
                Safe.fail(wd.module, t);
            }
        }

        // 2. Рендеринг ArrayList (список активных модулей) в стиле Pulse
        if (ARRAY_LIST.enabled) {
            try { arrayList(ctx, mc, sw); } catch (Throwable t) { Safe.fail(ARRAY_LIST, t); }
        }

        // 3. Рендеринг элементов HUD как на скриншоте (Potions, HotKeys, Статус)
        try {
            renderCustomHudElements(ctx, mc, sw, sh);
        } catch (Throwable t) {
            // Защита от краша HUD
        }
    }

    private static void renderCustomHudElements(DrawContext ctx, MinecraftClient mc, int sw, int sh) {
        // --- Верхняя плашка по центру (Текущий режим / Статус) ---
        String statusText = "Маяк / Элитный";
        int statusW = mc.textRenderer.getWidth(statusText) + 40;
        int statusX = (sw - statusW) / 2;
        int statusY = 10;
        Ui.card(ctx, statusX, statusY, statusW, 20, 10);
        Ui.roundRect(ctx, statusX + 6, statusY + 5, 10, 10, 3, 0xFF42E865);
        ctx.drawText(mc.textRenderer, "Текущий", statusX + 22, statusY + 6, 0xFF8E8EA6, false);
        ctx.drawText(mc.textRenderer, statusText, statusX + 70, statusY + 6, 0xFFFFFFFF, true);

        // --- Блок Potions (Слева сверху) ---
        int potX = 16, potY = 60;
        int potW = 130, potH = 24;
        Collection<StatusEffectInstance> effects = mc.player.getStatusEffects();
        if (!effects.isEmpty()) {
            potH = 22 + effects.size() * 16;
            Ui.card(ctx, potX, potY, potW, potH, 10);
            ctx.drawText(mc.textRenderer, "Potions", potX + 10, potY + 8, 0xFF9B8AFF, true);
            int eY = potY + 24;
            for (StatusEffectInstance effect : effects) {
                String name = effect.getEffectType().value().getName().getString();
                ctx.drawText(mc.textRenderer, mc.textRenderer.trimToWidth(name, 85), potX + 10, eY, 0xFFFFFFFF, false);
                ctx.drawText(mc.textRenderer, "∞", potX + potW - 20, eY, 0xFF8E8EA6, false);
                eY += 16;
            }
        }

        // --- Блок Hot Keys (Справа сверху) ---
        int hkW = 120, hkH = 80;
        int hkX = sw - hkW - 16, hkY = 60;
        Ui.card(ctx, hkX, hkY, hkW, hkH, 10);
        ctx.drawText(mc.textRenderer, "Hot Keys", hkX + 10, hkY + 8, 0xFF9B8AFF, true);
        
        String[] keys = {"AutoFish", "Brightness", "HUD", "Music Bar"};
        String[] binds = {"V", "H", "M", "Q"};
        int bY = hkY + 26;
        for (int i = 0; i < keys.length; i++) {
            ctx.drawText(mc.textRenderer, keys[i], hkX + 10, bY, 0xFFB4B5C7, false);
            ctx.drawText(mc.textRenderer, binds[i], hkX + hkW - 20, bY, 0xFFFFFFFF, true);
            bY += 13;
        }
    }

    // ------------------------------------------------------------------ Режим редактирования виджетов (Чат)
    public static void editOverlay(DrawContext ctx, MinecraftClient mc, int mx, int my, int sw, int sh) {
        for (Widget wd : Widgets.ALL) {
            if (!wd.module.enabled || wd.w <= 0) continue;
            int x = wd.px(sw), y = wd.py(sh);
            boolean drag = NovaClient.dragging == wd;
            boolean hov = wd.hit(mx, my, sw, sh);
            int col = drag ? 0xFFFFFFFF : hov ? RenderUtil.gradient(0) : 0x60A89CFF;
            ctx.fill(x - 2, y - 2, x + wd.w + 2, y - 1, col);
            ctx.fill(x - 2, y + wd.h + 1, x + wd.w + 2, y + wd.h + 2, col);
            ctx.fill(x - 2, y - 1, x - 1, y + wd.h + 1, col);
            ctx.fill(x + wd.w + 1, y - 1, x + wd.w + 2, y + wd.h + 1, col);
            if (hov || drag) ctx.drawTextWithShadow(mc.textRenderer, wd.id, x, y - 12, 0xFFD0CCFF);
        }
        String hint = "Drag widgets with the mouse";
        int hw = mc.textRenderer.getWidth(hint) + 16;
        Ui.card(ctx, sw / 2 - hw / 2, 6, hw, 16, 8);
        Ui.text(ctx, mc, hint, sw / 2 - hw / 2 + 8, 10, 0xFFB8B8D0);
    }

    // ------------------------------------------------------------------ ArrayList (Список модулей справа)
    private static void arrayList(DrawContext ctx, MinecraftClient mc, int sw) {
        long nowMs = System.currentTimeMillis();
        if (activeCache == null || nowMs - activeBuilt > 500) {
            activeCache = ALL.stream()
                    .filter(m -> m.enabled && m != ARRAY_LIST)
                    .sorted(Comparator.comparingInt((Module m) -> -mc.textRenderer.getWidth(m.name)))
                    .toList();
            activeBuilt = nowMs;
        }
        int ay = 150, idx = 0;
        for (Module m : activeCache) {
            int tw = mc.textRenderer.getWidth(m.name);
            int col = RenderUtil.gradient(idx * 4);
            Ui.roundRect(ctx, sw - tw - 16, ay, tw + 12, 16, 6, 0xE60F0F17);
            ctx.drawText(mc.textRenderer, m.name, sw - tw - 10, ay + 4, col, false);
            ay += 18;
            idx++;
        }
    }

    private Hud() {}
}
