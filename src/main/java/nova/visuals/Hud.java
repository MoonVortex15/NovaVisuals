package nova.visuals;

import net.minecraft.client.gui.GuiGraphics;

public class Hud {
    public static void render(GuiGraphics graphics, float tickDelta) {
        Widgets.renderAll(graphics, tickDelta);
    }
}
