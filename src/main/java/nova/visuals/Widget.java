package nova.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

/** Перетаскиваемый элемент худа. Позиция хранится в долях экрана (0..1), поэтому не ломается при смене размера окна. */
public abstract class Widget {
    public final String id;
    public final Module module;
    public float rx, ry;
    public int w, h;

    protected Widget(String id, Module module, float rx, float ry) {
        this.id = id;
        this.module = module;
        this.rx = rx;
        this.ry = ry;
    }

    /** Рисует виджет с левым верхним углом в (x, y) и обязан выставить w и h. Если показывать нечего — w = h = 0. */
    public abstract void draw(DrawContext ctx, MinecraftClient mc, int x, int y, boolean edit);

    public int px(int sw) { return MathHelper.clamp((int) (rx * sw), 0, Math.max(0, sw - w)); }
    public int py(int sh) { return MathHelper.clamp((int) (ry * sh), 0, Math.max(0, sh - h)); }

    public boolean hit(double mx, double my, int sw, int sh) {
        if (w <= 0 || h <= 0) return false;
        int x = px(sw), y = py(sh);
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
