package nova.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static nova.visuals.Modules.*;

public final class Widgets {
    public static final List<Widget> ALL = new ArrayList<>();

    // Зарегистрированные виджеты
    public static final Widget STATUS_BAR = register(new Widget("status_bar", HUD, 0.5f, 0.02f, 160, 20) {
        @Override
        public void draw(DrawContext ctx, MinecraftClient mc, int x, int y, boolean edit) {
            String text = "Маяк / Элитный";
            w = mc.textRenderer.getWidth(text) + 60;
            Ui.card(ctx, x, y, w, h, 8);
            Ui.roundRect(ctx, x + 6, y + 5, 10, 10, 3, 0xFF42E865);
            ctx.drawText(mc.textRenderer, "Текущий", x + 22, y + 6, Ui.SUB, false);
            ctx.drawText(mc.textRenderer, text, x + w - mc.textRenderer.getWidth(text) - 8, y + 6, Ui.TEXT, true);
        }
    });

    public static final Widget POTIONS = register(new Widget("potions", HUD, 0.02f, 0.1f, 130, 80) {
        @Override
        public void draw(DrawContext ctx, MinecraftClient mc, int x, int y, boolean edit) {
            Collection<StatusEffectInstance> effects = mc.player != null ? mc.player.getStatusEffects() : List.of();
            h = effects.isEmpty() ? 70 : 26 + effects.size() * 16;
            Ui.card(ctx, x, y, w, h, 10);
            Ui.roundRect(ctx, x + 8, y + 8, 8, 8, 2, Ui.ACCENT);
            ctx.drawText(mc.textRenderer, "Potions", x + 20, y + 8, Ui.TEXT, true);

            if (effects.isEmpty() && edit) {
                ctx.drawText(mc.textRenderer, "Strength II", x + 10, y + 24, Ui.TEXT, false);
                ctx.drawText(mc.textRenderer, "∞", x + w - 18, y + 24, Ui.SUB, false);
                ctx.drawText(mc.textRenderer, "Fire Resistance I", x + 10, y + 38, Ui.TEXT, false);
                ctx.drawText(mc.textRenderer, "1:06", x + w - 28, y + 38, Ui.SUB, false);
            } else {
                int eY = y + 24;
                for (StatusEffectInstance effect : effects) {
                    String name = effect.getEffectType().value().getName().getString();
                    int duration = effect.getDuration() / 20;
                    String timeStr = duration > 999 ? "∞" : String.format("%d:%02d", duration / 60, duration % 60);
                    ctx.drawText(mc.textRenderer, mc.textRenderer.trimToWidth(name, 75), x + 10, eY, Ui.TEXT, false);
                    ctx.drawText(mc.textRenderer, timeStr, x + w - mc.textRenderer.getWidth(timeStr) - 8, eY, Ui.SUB, false);
                    eY += 16;
                }
            }
        }
    });

    public static final Widget HOT_KEYS = register(new Widget("hot_keys", HUD, 0.85f, 0.1f, 115, 80) {
        @Override
        public void draw(DrawContext ctx, MinecraftClient mc, int x, int y, boolean edit) {
            Ui.card(ctx, x, y, w, h, 10);
            Ui.roundRect(ctx, x + 8, y + 8, 8, 8, 2, Ui.ACCENT);
            ctx.drawText(mc.textRenderer, "Hot Keys", x + 20, y + 8, Ui.TEXT, true);

            String[] keys = {"AutoFish", "Brightness", "HUD", "Music Bar"};
            String[] binds = {"V", "H", "M", "Q"};
            int bY = y + 24;
            for (int i = 0; i < keys.length; i++) {
                ctx.drawText(mc.textRenderer, keys[i], x + 10, bY, 0xFFB4B5C7, false);
                ctx.drawText(mc.textRenderer, binds[i], x + w - mc.textRenderer.getWidth(binds[i]) - 10, bY, Ui.TEXT, true);
                bY += 13;
            }
        }
    });

    public static final Widget TARGET_HUD = register(new Widget("target_hud", HUD, 0.4f, 0.55f, 150, 50) {
        @Override
        public void draw(DrawContext ctx, MinecraftClient mc, int x, int y, boolean edit) {
            PlayerEntity target = mc.player;
            if (target == null && !edit) return;

            float hp = target != null ? target.getHealth() : 16.0f;
            float maxHp = target != null ? target.getMaxHealth() : 20.0f;
            String name = target != null ? target.getName().getString() : "GFk31AK";

            Ui.card(ctx, x, y, w, h, 10);
            Ui.roundRect(ctx, x + 8, y + 8, 24, 24, 6, 0xFF252637);
            ctx.drawText(mc.textRenderer, name.substring(0, Math.min(2, name.length())), x + 14, y + 16, Ui.TEXT, true);

            ctx.drawText(mc.textRenderer, name, x + 38, y + 8, Ui.TEXT, true);
            String hpStr = String.format("HP / %.1f", hp);
            ctx.drawText(mc.textRenderer, hpStr, x + 38, y + 20, Ui.SUB, false);

            int barX = x + 8, barY = y + h - 12, barW = w - 16, barH = 5;
            Ui.roundRect(ctx, barX, barY, barW, barH, 2, 0xFF202130);
            int fillW = (int) (barW * Math.min(1.0f, hp / maxHp));
            if (fillW > 0) Ui.roundRect(ctx, barX, barY, fillW, barH, 2, 0xFF8A5CFF);

            int armorY = y + h + 4;
            int armorX = x + (w - 5 * 18) / 2;
            ItemStack[] items = {
                    new ItemStack(Items.DIAMOND_SWORD),
                    new ItemStack(Items.DIAMOND_HELMET),
                    new ItemStack(Items.DIAMOND_CHESTPLATE),
                    new ItemStack(Items.DIAMOND_LEGGINGS),
                    new ItemStack(Items.DIAMOND_BOOTS)
            };
            for (int i = 0; i < items.length; i++) {
                ctx.drawItem(items[i], armorX + i * 18, armorY);
            }
        }
    });

    public static final Widget COOLDOWNS = register(new Widget("cooldowns", HUD, 0.45f, 0.8f, 140, 36) {
        @Override
        public void draw(DrawContext ctx, MinecraftClient mc, int x, int y, boolean edit) {
            Ui.card(ctx, x, y, w, h, 10);
            Ui.roundRect(ctx, x + 8, y + 6, 8, 8, 4, Ui.ACCENT);
            ctx.drawText(mc.textRenderer, "Cooldowns", x + 20, y + 6, Ui.TEXT, true);
            ctx.drawText(mc.textRenderer, "Potion", x + 10, y + 20, 0xFFB4B5C7, false);
            ctx.drawText(mc.textRenderer, "∞", x + w - 18, y + 20, Ui.SUB, false);
        }
    });

    private static Widget register(Widget w) {
        ALL.add(w);
        return w;
    }

    private Widgets() {}
}
