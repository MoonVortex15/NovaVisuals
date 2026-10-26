package nova.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Отрисовка элементов в стиле Glassmorphism / Dark UI. */
public final class Ui {
    public static final int BG = 0xF20D0E16;
    public static final int CARD_BG = 0xF2151622;
    public static final int BORDER = 0x25FFFFFF;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int SUB = 0xFF8E8EA6;
    public static final int ACCENT = 0xFF9B8AFF;

    public static void roundRect(DrawContext ctx, int x, int y, int w, int h, int r, int c) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (Perf.lite()) r = Math.min(r, 3);
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

    /** Стеклянная карточка с аккуратной тонкой обводкой. */
    public static void card(DrawContext ctx, int x, int y, int w, int h, int r) {
        if (Perf.lite()) { roundRect(ctx, x, y, w, h, r, CARD_BG); return; }
        roundRect(ctx, x - 1, y - 1, w + 2, h + 2, r + 1, BORDER);
        roundRect(ctx, x, y, w, h, r, CARD_BG);
    }

    public static int panel(DrawContext ctx, MinecraftClient mc, int x, int y, int w, int h, String title) {
        card(ctx, x, y, w, h, 8);
        roundRect(ctx, x + 7, y + 6, 9, 9, 3, RenderUtil.gradient(0));
        text(ctx, mc, title, x + 21, y + 7, TEXT);
        ctx.fill(x + 6, y + 20, x + w - 6, y + 21, 0x1FFFFFFF);
        return y + 25;
    }

    private Ui() {}
}
