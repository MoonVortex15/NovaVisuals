package nova.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Рисование скруглённых элементов в стиле тёмного меню. */
public final class Ui {
    public static final int BG = 0xE60F0F17;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int SUB = 0xFF8E8EA6;
    public static final int DIM = 0xFF5C5C70;

    public static void roundRect(DrawContext ctx, int x, int y, int w, int h, int r, int c) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (Perf.lite()) r = Math.min(r, 3);   // на слабом ПК меньше полосок на скругление
        if (r <= 0) { ctx.fill(x, y, x + w, y + h, c); return; }
        ctx.fill(x, y + r, x + w, y + h - r, c);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
            ctx.fill(x + inset, y + i, x + w - inset, y + i + 1, c);
            ctx.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, c);
        }
    }

    public static void circle(DrawContext ctx, int x, int y, int d, int c) {
        roundRect(ctx, x, y, d, d, d / 2, c);
    }

    public static void text(DrawContext ctx, MinecraftClient mc, String s, int x, int y, int c) {
        ctx.drawText(mc.textRenderer, s, x, y, c, false);
    }

    public static int textW(MinecraftClient mc, String s) {
        return mc.textRenderer.getWidth(s);
    }

    /** Тёмная карточка с тонкой светлой обводкой. */
    public static void card(DrawContext ctx, int x, int y, int w, int h, int r) {
        if (Perf.lite()) { roundRect(ctx, x, y, w, h, r, BG); return; }   // без обводки в lite-режиме
        roundRect(ctx, x - 1, y - 1, w + 2, h + 2, r + 1, 0x30FFFFFF);
        roundRect(ctx, x, y, w, h, r, BG);
    }

    /** Панель с заголовком и иконкой. Возвращает Y, с которого можно рисовать содержимое. */
    public static int panel(DrawContext ctx, MinecraftClient mc, int x, int y, int w, int h, String title) {
        card(ctx, x, y, w, h, 7);
        roundRect(ctx, x + 7, y + 6, 9, 9, 3, RenderUtil.gradient(0));
        text(ctx, mc, title, x + 21, y + 7, TEXT);
        ctx.fill(x + 6, y + 20, x + w - 6, y + 21, 0x1FFFFFFF);
        return y + 25;
    }

    private Ui() {}
}
