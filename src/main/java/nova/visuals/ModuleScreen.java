package nova.visuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ModuleScreen extends Screen {
    private static final int W = 112, GAP = 8, TOP = 62;

    public ModuleScreen() { super(Text.literal(Brand.NAME)); }

    private static Text label(Module m) {
        return Text.literal(m.name + ": " + (m.enabled ? "§aON" : "§cOFF"));
    }

    private int startX() {
        int n = Module.Category.values().length;
        return width / 2 - (n * W + (n - 1) * GAP) / 2;
    }

    private static int countIn(Module.Category c) {
        int n = 0;
        for (Module m : Modules.ALL) if (m.category == c) n++;
        return n;
    }

    @Override
    protected void init() {
        Module.Category[] cats = Module.Category.values();
        int sx = startX();
        for (int c = 0; c < cats.length; c++) {
            int y = TOP;
            for (Module m : Modules.ALL) {
                if (m.category != cats[c]) continue;
                addDrawableChild(ButtonWidget.builder(label(m), b -> {
                    m.toggle();
                    b.setMessage(label(m));
                }).dimensions(sx + c * (W + GAP), y, W, 20).build());
                y += 22;
            }
        }
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        super.renderBackground(ctx, mx, my, delta);
        ctx.fill(0, 0, width, height, 0x90080810);

        Module.Category[] cats = Module.Category.values();
        int sx = startX();

        // шапка-логотип
        int hw = 190, hx = width / 2 - hw / 2, hy = 8;
        ctx.fill(hx, hy, hx + hw, hy + 30, 0xE00E0E16);
        Hud.gradBar(ctx, hx, hy, hx + hw, 2);
        ctx.fill(hx, hy + 2, hx + 30, hy + 30, RenderUtil.alpha(RenderUtil.gradient(0), 0xC0));
        ctx.drawCenteredTextWithShadow(textRenderer, Brand.NAME.substring(0, 1), hx + 15, hy + 11, 0xFFFFFFFF);

        // панели категорий
        for (int c = 0; c < cats.length; c++) {
            int px = sx + c * (W + GAP) - 4;
            int ph = 22 + countIn(cats[c]) * 22 + 6;
            ctx.fill(px, 40, px + W + 8, 40 + ph, 0xC00E0E16);
            Hud.gradBar(ctx, px, 40, px + W + 8, 2);
        }

        // подпись снизу справа
        String foot = Brand.NAME + " " + Brand.SUBTITLE + " " + Brand.VERSION + " §7| Right Shift - close | C - zoom";
        int fw = textRenderer.getWidth(foot) + 12;
        ctx.fill(width - fw - 4, height - 18, width - 4, height - 4, 0xD00E0E16);
        Hud.gradBar(ctx, width - fw - 4, height - 18, width - 4, 1);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        Module.Category[] cats = Module.Category.values();
        int sx = startX();

        // название в шапке
        int hx = width / 2 - 95;
        Hud.gradText(ctx, client, Brand.NAME.toUpperCase() + " " + Brand.SUBTITLE.toUpperCase(), hx + 40, 15, 0);
        ctx.drawTextWithShadow(textRenderer, "§7" + Brand.VERSION + "  |  client-side visuals", hx + 40, 26, 0xFFAAAAAA);

        for (int c = 0; c < cats.length; c++) {
            ctx.drawCenteredTextWithShadow(textRenderer, cats[c].name(),
                    sx + c * (W + GAP) + W / 2, 48, RenderUtil.gradient(c * 6));
        }

        String foot = Brand.NAME + " " + Brand.SUBTITLE + " " + Brand.VERSION + " §7| Right Shift - close | C - zoom";
        int fw = textRenderer.getWidth(foot) + 12;
        ctx.drawTextWithShadow(textRenderer, foot, width - fw + 2, height - 15, 0xFFFFFFFF);
    }

    @Override
    public boolean shouldPause() { return false; }
}
