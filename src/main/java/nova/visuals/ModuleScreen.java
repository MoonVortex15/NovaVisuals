package nova.visuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ModuleScreen extends Screen {
    private static final int W = 110, GAP = 8;

    public ModuleScreen() { super(Text.literal("Nova")); }

    private static Text label(Module m) {
        return Text.literal(m.name + ": " + (m.enabled ? "§aON" : "§cOFF"));
    }

    private int startX() {
        int n = Module.Category.values().length;
        return width / 2 - (n * W + (n - 1) * GAP) / 2;
    }

    @Override
    protected void init() {
        Module.Category[] cats = Module.Category.values();
        int sx = startX();
        for (int c = 0; c < cats.length; c++) {
            int y = 50;
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
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        Module.Category[] cats = Module.Category.values();
        int sx = startX();
        ctx.drawCenteredTextWithShadow(textRenderer, "Nova Visuals", width / 2, 12, RenderUtil.rainbow(0, 0.6f));
        for (int c = 0; c < cats.length; c++) {
            ctx.drawCenteredTextWithShadow(textRenderer, cats[c].name(),
                    sx + c * (W + GAP) + W / 2, 36, RenderUtil.rainbow(c * 60, 0.5f));
        }
    }

    @Override
    public boolean shouldPause() { return false; }
}
