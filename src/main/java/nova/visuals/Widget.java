package nova.visuals;

import net.minecraft.client.gui.GuiGraphics;

public abstract class Widget {
    protected int x, y, width, height;
    protected boolean enabled = true;

    public Widget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public abstract void render(GuiGraphics graphics, float tickDelta);

    // Универсальный метод для рисования скругленных полупрозрачных плашек «по красоте»
    public static void drawRoundedRect(GuiGraphics graphics, int x, int y, int width, int height, int radius, int color) {
        graphics.fill(x + radius, y, x + width - radius, y + height, color);
        graphics.fill(x, y + radius, x + radius, y + height - radius, color);
        graphics.fill(x + width - radius, y + radius, x + width, y + height - radius, color);
        graphics.fill(x + radius, y + radius, x + width - radius, y + height - radius, color);
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
