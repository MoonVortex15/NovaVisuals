package nova.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.RenderTickCounter;

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

        for (Widget wd : Widgets.ALL) {
            if (!wd.module.enabled) continue;
            try {
                wd.draw(ctx, mc, wd.px(sw), wd.py(sh), edit);
            } catch (Throwable t) {
                Safe.fail(wd.module, t);
            }
        }

        if (ARRAY_LIST.enabled) {
            try { arrayList(ctx, mc, sw); } catch (Throwable t) { Safe.fail(ARRAY_LIST, t); }
        }
    }

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
        String hint = "Перетаскивайте виджеты мышью";
        int hw = mc.textRenderer.getWidth(hint) + 16;
        Ui.card(ctx, sw / 2 - hw / 2, 6, hw, 16, 8);
        Ui.text(ctx, mc, hint, sw / 2 - hw / 2 + 8, 10, 0xFFB8B8D0);
    }

    private static void arrayList(DrawContext ctx, MinecraftClient mc, int sw) {
        long nowMs = System.currentTimeMillis();
        if (activeCache == null || nowMs - activeBuilt > 500) {
            activeCache = ALL.stream()
                    .filter(m -> m.enabled && m != ARRAY_LIST)
                    .sorted(Comparator.comparingInt((Module m) -> -mc.textRenderer.getWidth(m.name)))
                    .toList();
            activeBuilt = nowMs;
        }
        int ay = 140, idx = 0;
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
