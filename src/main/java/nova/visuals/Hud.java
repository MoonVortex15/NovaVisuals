package nova.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;

import java.util.Comparator;
import java.util.List;

import static nova.visuals.Modules.*;

public final class Hud {
    private static List<Module> activeCache;
    private static long activeBuilt;

    // кэш строк (обновляется 4 раза в секунду, а не каждый кадр)
    static String wmInfo = "", wmXyz = "";
    static int wmInfoW, wmXyzW, nameW = -1;
    private static long wmAt;

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

    // ------------------------------------------------------------------ режим перетаскивания (открыт чат)
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

    // ------------------------------------------------------------------ gradients (в lite-режиме — один цвет)
    public static void gradBar(DrawContext ctx, int x1, int y, int x2, int h) {
        if (Perf.lite()) {
            ctx.fill(x1, y, x2, y + h, RenderUtil.gradient(0));
            return;
        }
        for (int x = x1; x < x2; x += 6) {
            ctx.fill(x, y, Math.min(x + 6, x2), y + h, RenderUtil.gradient((x - x1) / 6.0));
        }
    }

    public static void gradText(DrawContext ctx, MinecraftClient mc, String text, int x, int y, double off) {
        if (Perf.lite()) {
            ctx.drawText(mc.textRenderer, text, x, y, RenderUtil.gradient(off), false);
            return;
        }
        int cx = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            ctx.drawText(mc.textRenderer, ch, cx, y, RenderUtil.gradient(off + i * 2), false);
            cx += mc.textRenderer.getWidth(ch);
        }
    }

    // ------------------------------------------------------------------ info cache
    static void updateInfo(MinecraftClient mc) {
        long n = System.currentTimeMillis();
        if (n - wmAt < 250 && nameW >= 0) return;
        wmAt = n;
        int ping = 0;
        if (mc.getNetworkHandler() != null) {
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (e != null) ping = e.getLatency();
        }
        String server = mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "singleplayer";
        wmInfo = " §7| §f" + mc.getCurrentFps() + " fps §7| §f" + ping + " ms §7| §f" + server;
        double bps = Math.hypot(mc.player.getX() - mc.player.prevX, mc.player.getZ() - mc.player.prevZ) * 20;
        wmXyz = "XYZ §f" + (int) mc.player.getX() + " " + (int) mc.player.getY() + " " + (int) mc.player.getZ()
                + " §7| §f" + (Math.round(bps * 10) / 10.0) + " b/s";
        nameW = mc.textRenderer.getWidth(Brand.NAME);
        wmInfoW = mc.textRenderer.getWidth(wmInfo);
        wmXyzW = mc.textRenderer.getWidth(wmXyz);
    }

    // ------------------------------------------------------------------ arraylist (по желанию)
    private static void arrayList(DrawContext ctx, MinecraftClient mc, int sw) {
        long nowMs = System.currentTimeMillis();
        if (activeCache == null || nowMs - activeBuilt > 500) {
            activeCache = ALL.stream()
                    .filter(m -> m.enabled && m != ARRAY_LIST)
                    .sorted(Comparator.comparingInt((Module m) -> -mc.textRenderer.getWidth(m.name)))
                    .toList();
            activeBuilt = nowMs;
        }
        int ay = 4, idx = 0;
        for (Module m : activeCache) {
            int tw = mc.textRenderer.getWidth(m.name);
            int col = RenderUtil.gradient(idx * 4);
            Ui.roundRect(ctx, sw - tw - 12, ay, tw + 10, 12, 4, 0xC00F0F17);
            ctx.drawText(mc.textRenderer, m.name, sw - tw - 7, ay + 2, col, false);
            ay += 14;
            idx++;
        }
    }

    private Hud() {}
}
