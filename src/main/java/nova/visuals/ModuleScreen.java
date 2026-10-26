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

/** Pulse-inspired ClickGUI: dark glass panel, violet accents, sidebar and animated cards. */
public class ModuleScreen extends Screen {
    private static final String[] TABS = {"Визуалы", "Бой", "HUD", "Мир"};
    private static final String[] TAB_HINTS = {
            "Подсветка и эффекты мира", "Визуальные помощники боя", "Элементы интерфейса", "Настройки окружения"
    };
    private static final int CARD_H = 48, GAP_Y = 8, GAP_X = 9;

    private int tab;
    private String search = "";
    private boolean searchFocus;
    private float scroll, maxScroll;
    private final Map<Module, Float> anim = new HashMap<>();

    private int px, py, pw, ph, sidebarW;
    private int areaX, areaY, areaW, areaH, colW;
    private int sx, sy, sw;
    private final int[] tabY = new int[TABS.length];

    public ModuleScreen() { super(Text.literal(Brand.NAME)); }

    private void layout() {
        pw = Math.max(280, Math.min(width - 24, 720));
        ph = Math.max(210, Math.min(height - 24, 400));
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        sidebarW = Math.min(128, Math.max(88, pw / 5));
        areaX = px + sidebarW + 20;
        areaY = py + 78;
        areaW = pw - sidebarW - 38;
        areaH = ph - 108;
        colW = Math.max(90, (areaW - GAP_X) / 2);
        sw = Math.min(166, Math.max(95, areaW / 2));
        sx = px + pw - 18 - sw;
        sy = py + 18;
        for (int i = 0; i < TABS.length; i++) tabY[i] = py + 76 + i * 34;
    }

    private List<Module> visible() {
        List<Module> result = new ArrayList<>();
        String q = search.toLowerCase(java.util.Locale.ROOT);
        for (Module m : Modules.ALL) {
            if (!q.isEmpty()) {
                if (m.name.toLowerCase(java.util.Locale.ROOT).contains(q)
                        || m.displayName().toLowerCase(java.util.Locale.ROOT).contains(q)
                        || m.description().toLowerCase(java.util.Locale.ROOT).contains(q)) result.add(m);
            } else if (m.category.ordinal() == tab) result.add(m);
        }
        return result;
    }

    private int cardX(int i) { return areaX + (i % 2) * (colW + GAP_X); }
    private int cardY(int i) { return areaY + (i / 2) * (CARD_H + GAP_Y) - (int) scroll; }

    private void updateScroll(int count) {
        int rows = (count + 1) / 2;
        int contentH = rows * (CARD_H + GAP_Y);
        maxScroll = Math.max(0, contentH - areaH);
        scroll = MathHelper.clamp(scroll, 0, maxScroll);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        layout();
        List<Module> list = visible();
        updateScroll(list.size());

        ctx.fill(0, 0, width, height, 0x72060810);
        // Main glass-like panel and separate sidebar.
        Ui.roundRect(ctx, px - 1, py - 1, pw + 2, ph + 2, 17, 0x286F62E8);
        Ui.roundRect(ctx, px, py, pw, ph, 16, 0xF20D0E16);
        Ui.roundRect(ctx, px + 1, py + 1, sidebarW + 8, ph - 2, 15, 0xFF11121D);
        Ui.roundRect(ctx, px + sidebarW + 8, py + 12, 1, ph - 24, 1, 0x20FFFFFF);

        // Brand block.
        Ui.roundRect(ctx, px + 15, py + 17, 27, 27, 8, 0xFF7060E8);
        ctx.drawText(textRenderer, "N", px + 24, py + 26, 0xFFFFFFFF, true);
        ctx.drawText(textRenderer, Brand.NAME, px + 49, py + 20, 0xFFFFFFFF, true);
        ctx.drawText(textRenderer, Brand.SUBTITLE + " " + Brand.VERSION, px + 49, py + 32, 0xFF77788F, false);
        ctx.fill(px + 15, py + 57, px + sidebarW - 7, py + 58, 0x20FFFFFF);

        // Sidebar navigation.
        for (int i = 0; i < TABS.length; i++) {
            int ty = tabY[i];
            boolean selected = i == tab && search.isEmpty();
            boolean hover = mx >= px + 10 && mx <= px + sidebarW + 1 && my >= ty - 5 && my <= ty + 23;
            if (selected) {
                Ui.roundRect(ctx, px + 10, ty - 5, sidebarW - 18, 28, 8, 0xFF292443);
                Ui.roundRect(ctx, px + 10, ty + 2, 3, 14, 2, 0xFF9B8AFF);
            } else if (hover) {
                Ui.roundRect(ctx, px + 10, ty - 5, sidebarW - 18, 28, 8, 0xFF1A1B29);
            }
            int dot = selected ? 0xFFB6AAFF : 0xFF65667B;
            Ui.circle(ctx, px + 23, ty + 5, 7, selected ? 0xFF6655C8 : 0xFF252637);
            Ui.circle(ctx, px + 23, ty + 5, 3, dot);
            ctx.drawText(textRenderer, TABS[i], px + 36, ty + 1,
                    selected ? 0xFFFFFFFF : 0xFF9293A8, false);
        }
        ctx.drawText(textRenderer, "RIGHT SHIFT", px + 15, py + ph - 31, 0xFF5D5F75, false);
        ctx.drawText(textRenderer, "закрыть меню", px + 15, py + ph - 20, 0xFF5D5F75, false);

        // Main heading and category count.
        String heading = search.isEmpty() ? TABS[tab] : "Поиск функций";
        ctx.drawText(textRenderer, heading, areaX, py + 19, 0xFFFFFFFF, true);
        String hint = search.isEmpty() ? TAB_HINTS[tab] : "Найдено: " + list.size();
        ctx.drawText(textRenderer, textRenderer.trimToWidth(hint, Math.max(50, sx - areaX - 12)), areaX, py + 34, 0xFF77788F, false);

        // Search field.
        Ui.roundRect(ctx, sx - 1, sy - 1, sw + 2, 22, 8, searchFocus ? 0xFF7767E8 : 0xFF272838);
        Ui.roundRect(ctx, sx, sy, sw, 20, 7, 0xFF171822);
        Ui.circle(ctx, sx + 10, sy + 6, 7, 0xFF77788F);
        Ui.circle(ctx, sx + 10, sy + 6, 4, 0xFF171822);
        ctx.fill(sx + 15, sy + 12, sx + 18, sy + 14, 0xFF77788F);
        String shown = search.isEmpty() && !searchFocus ? "Поиск..." : search + (searchFocus && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
        ctx.drawText(textRenderer, textRenderer.trimToWidth(shown, sw - 30), sx + 24, sy + 6,
                search.isEmpty() && !searchFocus ? 0xFF66677D : 0xFFFFFFFF, false);
        ctx.fill(areaX, py + 61, px + pw - 17, py + 62, 0x20FFFFFF);

        // Feature cards.
        ctx.enableScissor(areaX - 1, areaY, px + pw - 13, areaY + areaH);
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            int cx = cardX(i), cy = cardY(i);
            if (cy + CARD_H < areaY || cy > areaY + areaH) continue;
            float a = anim.getOrDefault(m, m.enabled ? 1f : 0f);
            a += ((m.enabled ? 1f : 0f) - a) * Math.min(1f, delta * 0.8f + 0.12f);
            anim.put(m, a);
            boolean hover = mx >= cx && mx <= cx + colW && my >= cy && my <= cy + CARD_H
                    && my >= areaY && my <= areaY + areaH;
            int bg = RenderUtil.mix(0xFF151620, 0xFF211E39, a);
            if (hover) bg = RenderUtil.mix(bg, 0xFF292640, 0.65);
            Ui.roundRect(ctx, cx, cy, colW, CARD_H, 10, bg);
            if (a > 0.02f) Ui.roundRect(ctx, cx, cy + 8, 2, CARD_H - 16, 1, RenderUtil.alpha(0xFF9D8BFF, (int)(a * 220)));

            int toggleW = 27, toggleH = 14;
            int tx = cx + colW - 10 - toggleW, ty = cy + 9;
            int textMax = Math.max(30, colW - 52);
            ctx.drawText(textRenderer, textRenderer.trimToWidth(m.displayName(), textMax), cx + 10, cy + 8,
                    m.enabled ? 0xFFFFFFFF : 0xFFB4B5C7, false);
            String desc = textRenderer.trimToWidth(m.description(), Math.max(30, colW - 20));
            ctx.drawText(textRenderer, desc, cx + 10, cy + 26, 0xFF77798F, false);
            Ui.roundRect(ctx, tx, ty, toggleW, toggleH, 7, RenderUtil.mix(0xFF343544, 0xFF7664E8, a));
            int knobX = tx + 2 + (int)((toggleW - 14) * a);
            Ui.circle(ctx, knobX, ty + 2, 10, RenderUtil.mix(0xFF85869A, 0xFFF1EEFF, a));
        }
        ctx.disableScissor();

        if (list.isEmpty()) {
            String empty = "Ничего не найдено";
            ctx.drawText(textRenderer, empty, areaX + Math.max(0, (areaW - textRenderer.getWidth(empty)) / 2), areaY + 24, 0xFF77788F, false);
        }
        if (maxScroll > 0) {
            int bx = px + pw - 9;
            Ui.roundRect(ctx, bx, areaY, 3, areaH, 1, 0x20FFFFFF);
            int contentH = areaH + (int) maxScroll;
            int thumbH = Math.max(18, areaH * areaH / contentH);
            int thumbY = areaY + (int)((areaH - thumbH) * (scroll / maxScroll));
            Ui.roundRect(ctx, bx, thumbY, 3, thumbH, 1, 0xFF9B8AFF);
        }
        ctx.drawText(textRenderer, "ЛКМ — переключить   •   ПКМ — настройки", areaX, py + ph - 18, 0xFF66677D, false);
        String footer = "" + list.size() + " функций";
        ctx.drawText(textRenderer, footer, px + pw - 18 - textRenderer.getWidth(footer), py + ph - 18, 0xFF77788F, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        layout();
        if (button == 0) {
            for (int i = 0; i < TABS.length; i++) {
                if (mx >= px + 10 && mx <= px + sidebarW + 1 && my >= tabY[i] - 5 && my <= tabY[i] + 23) {
                    tab = i;
                    scroll = 0;
                    search = "";
                    searchFocus = false;
                    return true;
                }
            }
            boolean inSearch = mx >= sx && mx <= sx + sw && my >= sy && my <= sy + 20;
            searchFocus = inSearch;
            if (inSearch) return true;
        }
        if ((button == 0 || button == 1) && my >= areaY && my <= areaY + areaH) {
            List<Module> list = visible();
            for (int i = 0; i < list.size(); i++) {
                int cx = cardX(i), cy = cardY(i);
                if (mx >= cx && mx <= cx + colW && my >= cy && my <= cy + CARD_H) {
                    if (button == 1) client.setScreen(new ModuleSettingsScreen(list.get(i), this));
                    else list.get(i).toggle();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        scroll = MathHelper.clamp(scroll - (float) vertical * 20f, 0, maxScroll);
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
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
        if (searchFocus) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
                scroll = 0;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { searchFocus = false; return true; }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean shouldPause() { return false; }
}
