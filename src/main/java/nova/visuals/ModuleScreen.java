package nova.visuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleScreen extends Screen {
    private static final String[] TABS = {"Visuals", "PvP", "HUD", "World"};
    private static final int CARD_H = 30, GAP_Y = 8, GAP_X = 10;

    private int tab;
    private String search = "";
    private boolean searchFocus;
    private float scroll, maxScroll;
    private final Map<Module, Float> anim = new HashMap<>();

    // раскладка
    private int px, py, pw, ph;
    private int areaX, areaY, areaW, areaH, colW;
    private int sx, sy, sw;
    private final int[] tabX = new int[TABS.length];
    private final int[] tabW = new int[TABS.length];

    public ModuleScreen() { super(Text.literal(Brand.NAME)); }

    private void layout() {
        pw = Math.max(300, Math.min(width - 30, 640));
        ph = Math.max(200, Math.min(height - 30, 340));
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        areaX = px + 22;
        areaY = py + 50;
        areaW = pw - 44;
        areaH = ph - 50 - 24;
        colW = (areaW - GAP_X) / 2;
        sw = Math.min(170, pw / 3);
        sx = px + pw - 22 - sw;
        sy = py + 13;
    }

    private List<Module> visible() {
        List<Module> l = new ArrayList<>();
        String q = search.toLowerCase();
        for (Module m : Modules.ALL) {
            if (!q.isEmpty()) {
                if (m.name.toLowerCase().contains(q)) l.add(m);
            } else if (m.category.ordinal() == tab) {
                l.add(m);
            }
        }
        return l;
    }

    private int cardX(int i) { return areaX + (i % 2) * (colW + GAP_X); }
    private int cardY(int i) { return areaY + (i / 2) * (CARD_H + GAP_Y) - (int) scroll; }

    private void updateScroll(int count) {
        int rows = (count + 1) / 2;
        int contentH = rows * (CARD_H + GAP_Y);
        maxScroll = Math.max(0, contentH - areaH);
        scroll = MathHelper.clamp(scroll, 0, maxScroll);
    }

    // ------------------------------------------------------------------ render
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        layout();
        List<Module> list = visible();
        updateScroll(list.size());

        ctx.fill(0, 0, width, height, 0x55000000);

        // окно
        Ui.roundRect(ctx, px - 1, py - 1, pw + 2, ph + 2, 17, 0x30FFFFFF);
        Ui.roundRect(ctx, px, py, pw, ph, 16, 0xF2101018);
        Ui.roundRect(ctx, px + 1, py + 1, pw - 2, 30, 15, 0x14786BFF);   // лёгкий фиолетовый отсвет сверху

        // вкладки
        float ts = 1.3f;
        int x = px + 26;
        for (int i = 0; i < TABS.length; i++) {
            boolean sel = i == tab && search.isEmpty();
            int col = sel ? 0xFFFFFFFF : 0xFF55556A;
            tabX[i] = x;
            tabW[i] = (int) (textRenderer.getWidth(TABS[i]) * ts);
            ctx.getMatrices().push();
            ctx.getMatrices().scale(ts, ts, 1f);
            ctx.drawText(textRenderer, TABS[i], (int) (x / ts), (int) ((py + 16) / ts), col, false);
            ctx.getMatrices().pop();
            x += tabW[i] + 9;
            if (i < TABS.length - 1) {
                ctx.drawText(textRenderer, "/", x, py + 18, 0xFF3A3A4C, false);
                x += textRenderer.getWidth("/") + 9;
            }
        }

        // поиск
        Ui.roundRect(ctx, sx - 1, sy - 1, sw + 2, 20, 10, searchFocus ? RenderUtil.alpha(RenderUtil.gradient(0), 0xAA) : 0x22FFFFFF);
        Ui.roundRect(ctx, sx, sy, sw, 18, 9, searchFocus ? 0xFF1A1A2A : 0xFF13131C);
        Ui.circle(ctx, sx + 8, sy + 4, 8, 0xFF5C5C70);
        Ui.circle(ctx, sx + 10, sy + 6, 4, searchFocus ? 0xFF1A1A2A : 0xFF13131C);
        ctx.fill(sx + 15, sy + 12, sx + 17, sy + 14, 0xFF5C5C70);
        String shown;
        int scol;
        if (search.isEmpty() && !searchFocus) { shown = "Search"; scol = 0xFF55556A; }
        else { shown = search + (searchFocus && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : ""); scol = 0xFFFFFFFF; }
        ctx.drawText(textRenderer, textRenderer.trimToWidth(shown, sw - 30), sx + 24, sy + 5, scol, false);

        ctx.fill(px + 22, py + 40, px + pw - 22, py + 41, 0x18FFFFFF);

        // карточки
        ctx.enableScissor(areaX - 2, areaY, areaX + areaW + 2, areaY + areaH);
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            int cx = cardX(i), cy = cardY(i);
            if (cy + CARD_H < areaY || cy > areaY + areaH) continue;

            float a = anim.getOrDefault(m, m.enabled ? 1f : 0f);
            a += ((m.enabled ? 1f : 0f) - a) * 0.25f;
            anim.put(m, a);

            boolean hov = mx >= cx && mx <= cx + colW && my >= cy && my <= cy + CARD_H
                    && my >= areaY && my <= areaY + areaH;
            int bg = RenderUtil.mix(0xFF111119, 0xFF1A1A30, a);
            if (hov) bg = RenderUtil.mix(bg, 0xFF22223C, 0.6);
            Ui.roundRect(ctx, cx, cy, colW, CARD_H, 12, bg);

            int nameCol = RenderUtil.mix(0xFF5C5C70, 0xFFFFFFFF, a);
            ctx.drawText(textRenderer, m.name, cx + 14, cy + 11, nameCol, false);

            int tw = 26, th = 14;
            int tx = cx + colW - 14 - tw, ty = cy + (CARD_H - th) / 2;
            Ui.roundRect(ctx, tx, ty, tw, th, 7, RenderUtil.mix(0xFF2B2B3B, 0xFF6C5CE7, a));
            int kx = tx + 2 + (int) ((tw - 14) * a);
            Ui.circle(ctx, kx, ty + 2, 10, RenderUtil.mix(0xFF4B4B5E, 0xFFE6E2FF, a));
        }
        ctx.disableScissor();

        if (list.isEmpty()) {
            String t = "Nothing found";
            ctx.drawText(textRenderer, t, px + pw / 2 - textRenderer.getWidth(t) / 2, areaY + 30, 0xFF55556A, false);
        }

        // скроллбар
        if (maxScroll > 0) {
            int bx = px + pw - 13;
            Ui.roundRect(ctx, bx, areaY, 3, areaH, 1, 0x20FFFFFF);
            int contentH = areaH + (int) maxScroll;
            int thumbH = Math.max(20, areaH * areaH / contentH);
            int thumbY = areaY + (int) ((areaH - thumbH) * (scroll / maxScroll));
            Ui.roundRect(ctx, bx, thumbY, 3, thumbH, 1, 0xA0A89CFF);
        }

        // подвал
        ctx.drawText(textRenderer, "LMB toggle  •  RMB settings  •  Chat moves HUD widgets", px + 26, py + ph - 17, 0xFF4A4A5E, false);
        String foot = Brand.NAME + " " + Brand.SUBTITLE + " " + Brand.VERSION;
        ctx.drawText(textRenderer, foot, px + pw - 26 - textRenderer.getWidth(foot), py + ph - 17, 0xFF6C6C86, false);
    }

    // ------------------------------------------------------------------ input
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        layout();
        if (button == 0) {
            for (int i = 0; i < TABS.length; i++) {
                if (mx >= tabX[i] && mx <= tabX[i] + tabW[i] && my >= py + 12 && my <= py + 32) {
                    tab = i;
                    scroll = 0;
                    search = "";
                    return true;
                }
            }
            boolean inSearch = mx >= sx && mx <= sx + sw && my >= sy && my <= sy + 18;
            searchFocus = inSearch;
            if (inSearch) return true;
        }
        if ((button == 0 || button == 1) && my >= areaY && my <= areaY + areaH) {
            List<Module> list = visible();
            for (int i = 0; i < list.size(); i++) {
                int cx = cardX(i), cy = cardY(i);
                if (mx >= cx && mx <= cx + colW && my >= cy && my <= cy + CARD_H) {
                    if (button == 1) {
                        client.setScreen(new ModuleSettingsScreen(list.get(i), this));
                    } else {
                        list.get(i).toggle();
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        scroll = MathHelper.clamp(scroll - (float) vertical * 18f, 0, maxScroll);
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocus && chr >= 32 && chr != 127 && search.length() < 24) {
            search += chr;
            scroll = 0;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            close();
            return true;
        }
        if (searchFocus) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
                scroll = 0;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                searchFocus = false;
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() { return false; }
}
