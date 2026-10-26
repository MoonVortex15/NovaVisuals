package nova.visuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class ModuleSettingsScreen extends Screen {
    private final Module module;
    private final Screen parent;

    public ModuleSettingsScreen(Module module, Screen parent) {
        super(Text.literal(module.displayName()));
        this.module = module;
        this.parent = parent;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        ctx.fill(0, 0, width, height, 0x85000000);

        int pw = 280, ph = 200;
        int px = (width - pw) / 2;
        int py = (height - ph) / 2;

        Ui.card(ctx, px, py, pw, ph, 14);
        ctx.drawText(textRenderer, module.displayName(), px + 16, py + 16, Ui.TEXT, true);
        ctx.drawText(textRenderer, module.description(), px + 16, py + 30, Ui.SUB, false);

        ctx.fill(px + 16, py + 46, px + pw - 16, py + 47, 0x20FFFFFF);
        ctx.drawText(textRenderer, "Настройки будут доступны здесь", px + 16, py + 60, Ui.SUB, false);

        int btnW = 80, btnH = 20;
        int btnX = px + pw - 16 - btnW, btnY = py + ph - 16 - btnH;
        Ui.roundRect(ctx, btnX, btnY, btnW, btnH, 6, Ui.ACCENT);
        ctx.drawText(textRenderer, "Назад", btnX + 24, btnY + 6, Ui.TEXT, true);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int pw = 280, ph = 200;
        int px = (width - pw) / 2;
        int py = (height - ph) / 2;
        int btnW = 80, btnH = 20;
        int btnX = px + pw - 16 - btnW, btnY = py + ph - 16 - btnH;

        if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
            client.setScreen(parent);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            client.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
