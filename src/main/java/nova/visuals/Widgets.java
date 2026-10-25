package nova.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import static nova.visuals.Modules.*;

public final class Widgets {
    public static final List<Widget> ALL = new ArrayList<>();

    private static final ArrayDeque<Long> LCLICKS = new ArrayDeque<>();
    private static final ArrayDeque<Long> RCLICKS = new ArrayDeque<>();
    private static boolean prevL, prevR;

    private static final int[] COUNTS = new int[4];
    private static long countsAt;
    private static ItemStack[] icons;
    private static ItemStack[] cdIcons;

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final EquipmentSlot[] TARGET_SLOTS = {EquipmentSlot.MAINHAND, EquipmentSlot.HEAD,
            EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private static String roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n >= 1 && n <= 10 ? r[n] : String.valueOf(n);
    }

    private static void durability(DrawContext ctx, ItemStack st, int x, int y) {
        if (st.isEmpty()) return;
        if (st.isDamageable()) {
            float f = 1f - (float) st.getDamage() / st.getMaxDamage();
            ctx.fill(x, y, x + 16, y + 2, 0xFF23232F);
            ctx.fill(x, y, x + (int) (16 * f), y + 2, MathHelper.hsvToRgb(f * 0.33f, 1f, 1f) | 0xFF000000);
        } else {
            ctx.fill(x, y, x + 16, y + 2, 0xFF3A3A4C);
        }
    }

    private static int count(PlayerInventory inv, Item item) {
        int n = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(item)) n += s.getCount();
        }
        return n;
    }

    private static int cps(ArrayDeque<Long> q) {
        long n = System.currentTimeMillis();
        while (!q.isEmpty() && n - q.peekFirst() > 1000) q.pollFirst();
        return q.size();
    }

    private static void key(DrawContext ctx, MinecraftClient mc, int x, int y, int w, String label, boolean down) {
        Ui.roundRect(ctx, x, y, w, 18, 4, down ? 0xE0FFFFFF : 0xB00F0F17);
        int tw = Ui.textW(mc, label);
        Ui.text(ctx, mc, label, x + w / 2 - tw / 2, y + 5, down ? 0xFF111119 : 0xFFFFFFFF);
    }

    static {
        // ---------------------------------------------------------------- Watermark (плашка сверху)
        ALL.add(new Widget("watermark", WATERMARK, 0.40f, 0.03f) {
            @Override public void draw(DrawContext ctx, MinecraftClient mc, int x, int y, boolean edit) {
                Hud.updateInfo(mc);
                this.w = 24 + Hud.nameW + Hud.wmInfoW + 8;
                this.h = 20;
                Ui.card(ctx, x, y, w, h, 10);
                Ui.roundRect(ctx, x + 3, y + 3, 14, 14, 7, RenderUtil.alpha(RenderUtil.gradient(0), 0xE0));
                String l = Brand.NAME.substring(0, 1);
                Ui.text(ctx, mc, l, x + 10 - Ui.textW(mc, l) / 2, y + 6, 0xFFFFFFFF);
                Hud.gradText(ctx, mc, Brand.NAME, x + 22, y + 6, 0);
                Ui.text(ctx, mc, Hud.wmInfo, x + 22 + Hud.nameW, y + 6, 0xFFFFFFFF);
            }
        });

        // ---------------------------------------------------------------- Coords
        ALL.add(new Widget("coords", COORDS, 0.01f, 0.03f) {
            @Override public void draw(DrawContext ctx, MinecraftClient mc, int x, int y, boolean edit) {
                Hud.updateInfo(mc);
                this.w = Hud.wmXyzW + 14;
                this.h = 16;
                Ui.card(ctx, x, y, w, h, 8);
                Ui.text(ctx, mc, Hud.wmXyz, x + 7, y + 4, 0xFFB8B8C8);
            }
        });

        // ---------------------------------------------------------------- Potions
        ALL.add(new Widget("potions", POTION_HUD, 0.04f, 0.20f) {
            private final String[] names = new String[6];
            private final String[] times = new String[6];
            private final boolean[] good = new boolean[6];

            @Override public void draw(DrawContext ctx, MinecraftClient mc, int x, int y, boolean edit) {
                int n = 0, maxW = 110;
                for (StatusEffectInstance e : mc.player.getStatusEffects()) {
                    if (n >= 6) break;
                    names[n] = e.getEffectType().value().getName().getString() + " " + roman(e.getAmplifier() + 1);
                    if (e.isInfinite()) times[n] = "inf";
                    else {
                        int s = e.getDuration() / 20;
                        times[n] = (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
                    }
                    good[n] = e.getEffectType().value().isBeneficial();
                    maxW = Math.max(maxW, Ui.textW(mc, names[n]) + Ui.textW(mc, times[n]) + 36);
                    n++;
                }
                if (n == 0 && !edit) { this.w = 0; this.h = 0; return; }
                this.w = maxW;
                this.h = 25 + Math.max(n, 1) * 14 + 5;
                int cy = Ui.panel(ctx, mc, x, y, w, h, "Potions");
                if (n == 0) Ui.text(ctx, mc, "No effects", x + 8, cy + 3, Ui.DIM);
                for (int i = 0; i < n; i++) {
                    int ry = cy + i * 14 + 2;
                    Ui.circle(ctx, x + 8, ry + 2, 5, good[i] ? 0xFF8F7BFF : 0xFFFF6B6B);
                    Ui.text(ctx, mc, names[i], x + 19, ry, 0xFFE8E8F4);
                    Ui.text(ctx, mc, times[i], x + w - 8 - Ui.textW(mc, times[i]), ry, Ui.SUB);
                }
            }
        });

        // ---------------------------------------------------------------- Hot Keys
        ALL.add(new Widget("hotkeys", HOTKEYS, 0.82f, 0.25f) {
            @Override public void draw(DrawContext ctx, MinecraftClient mc, int x, int y, boolean edit) {
                String[] names = {"Menu", "Zoom"};
                KeyBinding[] keys = {NovaClient.openGui, NovaClient.zoomKey};
                this.w = 112;
                this.h = 25 + names.length * 16 + 3;
                int cy = Ui.panel(ctx, mc, x, y, w, h, "Hot Keys");
                for (int i = 0; i < names.length; i++) {
                    int ry = cy + i * 16 + 1;
                    Ui.text(ctx, mc, names[i], x + 8, ry + 3, 0xFFE8E8F4);
                    String k = keys[i] != null ? keys[i].getBoundKeyLocalizedText().getString() : "-";
                    int kw = Ui.textW(mc, k) + 10;
                    Ui.roundRect(ctx, x + w - 8 - kw, ry, kw, 13, 4, 0xFF1B1B27);
                    Ui.text(ctx, mc, k, x + w - 8 - kw + 5, ry + 3, 0xFFD0CCFF);
                }
            }
        });

        // ---------------------------------------------------------------- Target card
        ALL.add(new Widget("target", TARGET_HUD, 0.26f, 0.54f) {
            @Override public void draw(DrawContext ctx, MinecraftClient mc, int x, int y, boolean edit) {
                long now = System.currentTimeMillis();
                LivingEntity t = State.target;
                if (mc.targetedEntity instanceof LivingEntity le) {
                    t = le;
                    State.target = le;
                    State.targetTime = now;
                }
                boolean has = t != null && !t.isRemoved() && now - State.targetTime <= 5000;
                if (!has) {
                    State.target = null;
                    if (!edit) { this.w = 0; this.h = 0; return; }
                }
                this.w = 150;
                this.h = 66;
                Ui.card(ctx, x, y, w, h, 8);

                // голова
                if (has && t instanceof AbstractClientPlayerEntity p) {
                    PlayerSkinDrawer.draw(ctx, p.getSkinTextures(), x + 8, y + 8, 24);
                } else {
                    Ui.roundRect(ctx, x + 8, y + 8, 24, 24, 5, 0xFF2A2A3A);
                    String q = has ? t.getName().getString().substring(0, 1) : "?";
                    Ui.text(ctx, mc, q, x + 20 - Ui.textW(mc, q) / 2, y + 16, 0xFFFFFFFF);
                }

                String name = has ? t.getName().getString() : "Player";
                name = mc.textRenderer.trimToWidth(name, w - 96);
                Ui.text(ctx, mc, name, x + 38, y + 9, 0xFFFFFFFF);
                String tag = has && !(t instanceof PlayerEntity) ? "Mob" : "Player";
                Ui.text(ctx, mc, tag, x + w - 8 - Ui.textW(mc, tag), y + 9, Ui.DIM);

                float hp = has ? t.getHealth() + t.getAbsorptionAmount() : 20f;
                float max = has ? t.getMaxHealth() : 20f;
                float ratio = MathHelper.clamp(hp / max, 0f, 1f);
                State.shownRatio += (ratio - State.shownRatio) * 0.15f;
                Ui.text(ctx, mc, "HP / " + (Math.round(hp * 10) / 10.0), x + 38, y + 21, Ui.SUB);

                int bw = w - 16;
                Ui.roundRect(ctx, x + 8, y + 36, bw, 5, 2, 0xFF23232F);
                int fw = Math.max(4, (int) (bw * State.shownRatio));
                Ui.roundRect(ctx, x + 8, y + 36, fw, 5, 2, RenderUtil.gradient(0));

                if (has) {
                    for (int i = 0; i < TARGET_SLOTS.length; i++) {
                        ItemStack st = t.getEquippedStack(TARGET_SLOTS[i]);
                        int ix = x + 8 + i * 20;
                        if (!st.isEmpty()) ctx.drawItem(st, ix, y + 44);
                        durability(ctx, st, ix, y + 61);
                    }
                }
            }
        });

        // ---------------------------------------------------------------- Cooldowns
        ALL.add(new Widget("cooldowns", COOLDOWNS, 0.55f, 0.78f) {
            private final Item[] cd = {Items.ENDER_PEARL, Items.CHORUS_FRUIT, Items.GOLDEN_APPLE,
                    Items.ENCHANTED_GOLDEN_APPLE, Items.SHIELD, Items.WIND_CHARGE};
            private final float[] pr = new float[6];

            @Override public void draw(DrawContext ctx, MinecraftClient mc, int x, int y, boolean edit) {
                if (cdIcons == null) {
                    cdIcons = new ItemStack[cd.length];
                    for (int i = 0; i < cd.length; i++) cdIcons[i] = new ItemStack(cd[i]);
                }
                int n = 0;
                for (int i = 0; i < cd.length; i++) {
                    pr[i] = mc.player.getItemCooldownManager().getCooldownProgress(cdIcons[i], 0f);
                    if (pr[i] > 0f) n++;
                }
                if (n == 0 && !edit) { this.w = 0; this.h = 0; return; }
                this.w = 130;
                this.h = 25 + Math.max(n, 1) * 20 + 2;
                int cy = Ui.panel(ctx, mc, x, y, w, h, "Cooldowns");
                if (n == 0) Ui.text(ctx, mc, "Ready", x + 8, cy + 5, Ui.DIM);
                int row = 0;
                for (int i = 0; i < cd.length; i++) {
                    if (pr[i] <= 0f) continue;
                    int ry = cy + row * 20 + 1;
                    ctx.drawItem(cdIcons[i], x + 6, ry);
                    int bw = w - 70;
                    Ui.roundRect(ctx, x + 28, ry + 6, bw, 4, 2, 0xFF23232F);
                    Ui.roundRect(ctx, x + 28, ry + 6, Math.max(3, (int) (bw * pr[i])), 4, 2, RenderUtil.gradient(row * 4));
                    String pct = (int) (pr[i] * 100) + "%";
                    Ui.text(ctx, mc, pct, x + w - 8 - Ui.textW(mc, pct), ry + 4, Ui.SUB);
                    row++;
                }
            }
        });

        // ---------------------------------------------------------------- Armor
        ALL.add(new Widget("armor", ARMOR_HUD, 0.42f, 0.80f) {
            @Override public void draw(DrawContext ctx, MinecraftClient mc, int x, int y, boolean edit) {
                this.w = 4 * 20 + 6;
                this.h = 28;
                Ui.card(ctx, x, y, w, h, 7);
                for (int i = 0; i < ARMOR.length; i++) {
                    ItemStack st = mc.player.getEquippedStack(ARMOR[i]);
                    int ix = x + 5 + i * 20;
                    if (!st.isEmpty()) ctx.drawItem(st, ix, y + 4);
                    durability(ctx, st, ix, y + 22);
                }
            }
        });

        // ---------------------------------------------------------------- Keystrokes
        ALL.add(new Widget("keys", KEYSTROKES, 0.01f, 0.80f) {
            @Override public void draw(DrawContext ctx, MinecraftClient mc, int x, int y, boolean edit) {
                long handle = mc.getWindow().getHandle();
                boolean l = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
                boolean r = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
                long now = System.currentTimeMillis();
                if (l && !prevL && mc.currentScreen == null && LCLICKS.size() < 64) LCLICKS.add(now);
                if (r && !prevR && mc.currentScreen == null && RCLICKS.size() < 64) RCLICKS.add(now);
                prevL = l;
                prevR = r;

                this.w = 58;
                this.h = 58;
                key(ctx, mc, x + 20, y, 18, "W", mc.options.forwardKey.isPressed());
                key(ctx, mc, x, y + 20, 18, "A", mc.options.leftKey.isPressed());
                key(ctx, mc, x + 20, y + 20, 18, "S", mc.options.backKey.isPressed());
                key(ctx, mc, x + 40, y + 20, 18, "D", mc.options.rightKey.isPressed());
                key(ctx, mc, x, y + 40, 28, cps(LCLICKS) + "", l);
                key(ctx, mc, x + 30, y + 40, 28, cps(RCLICKS) + "", r);
            }
        });

        // ---------------------------------------------------------------- Item counter
        ALL.add(new Widget("items", ITEM_COUNT, 0.01f, 0.45f) {
            @Override public void draw(DrawContext ctx, MinecraftClient mc, int x, int y, boolean edit) {
                Item[] items = {Items.TOTEM_OF_UNDYING, Items.END_CRYSTAL, Items.GOLDEN_APPLE, Items.ENDER_PEARL};
                if (icons == null) {
                    icons = new ItemStack[items.length];
                    for (int i = 0; i < items.length; i++) icons[i] = new ItemStack(items[i]);
                }
                long n = System.currentTimeMillis();
                if (n - countsAt > 500) {
                    for (int i = 0; i < items.length; i++) COUNTS[i] = count(mc.player.getInventory(), items[i]);
                    countsAt = n;
                }
                this.w = 72;
                this.h = 25 + items.length * 18 + 3;
                int cy = Ui.panel(ctx, mc, x, y, w, h, "Items");
                for (int i = 0; i < items.length; i++) {
                    ctx.drawItem(icons[i], x + 7, cy + i * 18);
                    Ui.text(ctx, mc, "x" + COUNTS[i], x + 30, cy + i * 18 + 4, 0xFFE8E8F4);
                }
            }
        });

        // ---------------------------------------------------------------- Radar
        ALL.add(new Widget("radar", RADAR, 0.88f, 0.75f) {
            @Override public void draw(DrawContext ctx, MinecraftClient mc, int x, int y, boolean edit) {
                int size = 84;
                this.w = size;
                this.h = size;
                Ui.card(ctx, x, y, size, size, 9);
                int cx = x + size / 2, cy = y + size / 2;
                ctx.fill(cx, y + 6, cx + 1, y + size - 6, 0x28FFFFFF);
                ctx.fill(x + 6, cy, x + size - 6, cy + 1, 0x28FFFFFF);
                Ui.circle(ctx, cx - 2, cy - 2, 5, 0xFF8F7BFF);

                double yaw = Math.toRadians(mc.player.getYaw());
                double s = Math.sin(yaw), c = Math.cos(yaw);
                double lim = size / 2.0 - 6;
                int drawn = 0;
                for (PlayerEntity p : mc.world.getPlayers()) {
                    if (p == mc.player) continue;
                    if (drawn++ >= 32) break;
                    double dx = p.getX() - mc.player.getX(), dz = p.getZ() - mc.player.getZ();
                    double px = MathHelper.clamp((-dx * c - dz * s) * 1.2, -lim, lim);
                    double py = MathHelper.clamp((dx * s - dz * c) * 1.2, -lim, lim);
                    Ui.circle(ctx, cx + (int) px - 2, cy + (int) py - 2, 4, 0xFFFF5F6D);
                }
            }
        });

        // ---------------------------------------------------------------- Combo
        ALL.add(new Widget("combo", COMBO, 0.46f, 0.58f) {
            @Override public void draw(DrawContext ctx, MinecraftClient mc, int x, int y, boolean edit) {
                if (State.combo < 1 && !edit) { this.w = 0; this.h = 0; return; }
                String t = "Combo  " + State.combo;
                this.w = Ui.textW(mc, t) + 16;
                this.h = 16;
                Ui.card(ctx, x, y, w, h, 8);
                Hud.gradText(ctx, mc, t, x + 8, y + 4, State.combo * 3);
            }
        });
    }

    private Widgets() {}
}
