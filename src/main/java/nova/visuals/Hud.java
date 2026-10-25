package nova.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
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
import java.util.Comparator;
import java.util.List;

import static nova.visuals.Modules.*;

public final class Hud {
    private static final ArrayDeque<Long> LCLICKS = new ArrayDeque<>();
    private static final ArrayDeque<Long> RCLICKS = new ArrayDeque<>();
    private static boolean prevL, prevR;

    private static List<Module> activeCache;
    private static long activeBuilt;

    private static final int[] COUNTS = new int[4];
    private static long countsAt;
    private static ItemStack[] icons;

    // кэш строк вотермарка (обновляется 4 раза в секунду, а не каждый кадр)
    private static String wmInfo = "", wmXyz = "";
    private static int wmInfoW, wmXyzW, nameW = -1;
    private static long wmAt;

    public static void invalidate() { activeBuilt = 0; }

    public static void render(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();

        if (HUD.enabled)        try { watermark(ctx, mc, sw); }        catch (Throwable t) { Safe.fail(HUD, t); }
        if (ARMOR_HUD.enabled)  try { armor(ctx, mc, sw, sh); }        catch (Throwable t) { Safe.fail(ARMOR_HUD, t); }
        if (POTION_HUD.enabled) try { potions(ctx, mc); }              catch (Throwable t) { Safe.fail(POTION_HUD, t); }
        if (TARGET_HUD.enabled) try { targetHud(ctx, mc, sw, sh); }    catch (Throwable t) { Safe.fail(TARGET_HUD, t); }
        if (KEYSTROKES.enabled) try { keystrokes(ctx, mc, sh); }       catch (Throwable t) { Safe.fail(KEYSTROKES, t); }
        if (ITEM_COUNT.enabled) try { itemCounter(ctx, mc, sh); }      catch (Throwable t) { Safe.fail(ITEM_COUNT, t); }
        if (RADAR.enabled)      try { radar(ctx, mc, sw, sh); }        catch (Throwable t) { Safe.fail(RADAR, t); }
        if (COMBO.enabled)      try { combo(ctx, mc, sw, sh); }        catch (Throwable t) { Safe.fail(COMBO, t); }
    }

    // ------------------------------------------------------------------ gradients (в lite-режиме — один цвет, без лишних draw-вызовов)
    public static void gradBar(DrawContext ctx, int x1, int y, int x2, int h) {
        if (Perf.lite()) {
            ctx.fill(x1, y, x2, y + h, RenderUtil.gradient(0));
            return;
        }
        for (int x = x1; x < x2; x += 6) {
            ctx.fill(x, y, Math.min(x + 6, x2), y + h, RenderUtil.gradient((x - x1) / 6.0));
        }
    }

    public static void gradText(DrawContext ctx, MinecraftClient mc, String text, int x, int y, double off) {
        if (Perf.lite()) {
            ctx.drawTextWithShadow(mc.textRenderer, text, x, y, RenderUtil.gradient(off));
            return;
        }
        int cx = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            ctx.drawTextWithShadow(mc.textRenderer, ch, cx, y, RenderUtil.gradient(off + i * 2));
            cx += mc.textRenderer.getWidth(ch);
        }
    }

    // ------------------------------------------------------------------ watermark + arraylist
    private static void updateInfo(MinecraftClient mc) {
        long n = System.currentTimeMillis();
        if (n - wmAt < 250 && nameW >= 0) return;
        wmAt = n;
        int ping = 0;
        if (mc.getNetworkHandler() != null) {
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (e != null) ping = e.getLatency();
        }
        String server = mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "singleplayer";
        wmInfo = " §7| §f" + mc.getCurrentFps() + " fps §7| §f" + ping + " ms §7| §f" + server;
        double bps = Math.hypot(mc.player.getX() - mc.player.prevX, mc.player.getZ() - mc.player.prevZ) * 20;
        wmXyz = "XYZ §f" + (int) mc.player.getX() + " " + (int) mc.player.getY() + " " + (int) mc.player.getZ()
                + " §7| §f" + (Math.round(bps * 10) / 10.0) + " b/s";
        nameW = mc.textRenderer.getWidth(Brand.NAME);
        wmInfoW = mc.textRenderer.getWidth(wmInfo);
        wmXyzW = mc.textRenderer.getWidth(wmXyz);
    }

    private static void watermark(DrawContext ctx, MinecraftClient mc, int sw) {
        updateInfo(mc);
        int x = 4, y = 4, h = 16;
        int w = 20 + nameW + wmInfoW + 6;
        ctx.fill(x, y, x + w, y + h, 0xD00E0E16);
        gradBar(ctx, x, y, x + w, 1);
        ctx.fill(x + 1, y + 1, x + 15, y + h, RenderUtil.alpha(RenderUtil.gradient(0), 0xB0));
        ctx.drawCenteredTextWithShadow(mc.textRenderer, Brand.NAME.substring(0, 1), x + 8, y + 5, 0xFFFFFFFF);
        gradText(ctx, mc, Brand.NAME, x + 20, y + 5, 0);
        ctx.drawTextWithShadow(mc.textRenderer, wmInfo, x + 20 + nameW, y + 5, 0xFFFFFFFF);

        int w2 = wmXyzW + 12;
        ctx.fill(x, y + h + 2, x + w2, y + h + 16, 0xB00E0E16);
        ctx.fill(x, y + h + 2, x + 2, y + h + 16, RenderUtil.gradient(6));
        ctx.drawTextWithShadow(mc.textRenderer, wmXyz, x + 7, y + h + 5, 0xFFB8B8C8);

        long nowMs = System.currentTimeMillis();
        if (activeCache == null || nowMs - activeBuilt > 500) {
            activeCache = ALL.stream()
                    .filter(m -> m.enabled && m != HUD)
                    .sorted(Comparator.comparingInt((Module m) -> -mc.textRenderer.getWidth(m.name)))
                    .toList();
            activeBuilt = nowMs;
        }
        int ay = 4, idx = 0;
        for (Module m : activeCache) {
            int tw = mc.textRenderer.getWidth(m.name);
            int col = RenderUtil.gradient(idx * 4);
            ctx.fill(sw - tw - 9, ay, sw - 2, ay + 11, 0xB00E0E16);
            ctx.fill(sw - 2, ay, sw, ay + 11, col);
            ctx.drawTextWithShadow(mc.textRenderer, m.name, sw - tw - 5, ay + 2, col);
            ay += 11;
            idx++;
        }
    }

    // ------------------------------------------------------------------ armor
    private static final EquipmentSlot[] ARMOR_SLOTS =
            {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private static void armor(DrawContext ctx, MinecraftClient mc, int sw, int sh) {
        int x = sw / 2 - 36, y = sh - 72;
        for (EquipmentSlot s : ARMOR_SLOTS) {
            ItemStack st = mc.player.getEquippedStack(s);
            if (!st.isEmpty()) {
                ctx.drawItem(st, x, y);
                ctx.drawStackOverlay(mc.textRenderer, st, x, y);
            }
            x += 18;
        }
    }

    // ------------------------------------------------------------------ potions
    private static void potions(DrawContext ctx, MinecraftClient mc) {
        int y = 24 + 18;   // ниже плашки с координатами
        int shown = 0;
        for (StatusEffectInstance e : mc.player.getStatusEffects()) {
            if (shown++ >= 8) break;
            String name = e.getEffectType().value().getName().getString() + " " + (e.getAmplifier() + 1);
            String time;
            if (e.isInfinite()) time = "inf";
            else {
                int s = e.getDuration() / 20;
                time = (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
            }
            String line = name + " §7" + time;
            ctx.fill(4, y - 1, 8 + mc.textRenderer.getWidth(line), y + 10, 0x80000000);
            ctx.drawTextWithShadow(mc.textRenderer, line, 6, y + 1,
                    e.getEffectType().value().isBeneficial() ? 0xFF7CFF8A : 0xFFFF6B6B);
            y += 12;
        }
    }

    // ------------------------------------------------------------------ target hud
    private static void targetHud(DrawContext ctx, MinecraftClient mc, int sw, int sh) {
        long now = System.currentTimeMillis();
        LivingEntity t = State.target;
        if (mc.targetedEntity instanceof LivingEntity le) {
            t = le;
            State.target = le;
            State.targetTime = now;
        }
        if (t == null || t.isRemoved() || now - State.targetTime > 5000) {
            State.target = null;
            return;
        }

        int x = sw / 2 + 30, y = sh / 2 + 10, w = 150, h = 48;
        ctx.fill(x, y, x + w, y + h, 0xA0000000);
        ctx.fill(x, y, x + 2, y + h, RenderUtil.gradient(0));

        String head = t.getName().getString() + " §7" + (Math.round(mc.player.distanceTo(t) * 10) / 10.0) + "m";
        ctx.drawTextWithShadow(mc.textRenderer, head, x + 6, y + 4, 0xFFFFFFFF);

        float hp = t.getHealth() + t.getAbsorptionAmount();
        float ratio = MathHelper.clamp(hp / t.getMaxHealth(), 0f, 1f);
        State.shownRatio += (ratio - State.shownRatio) * 0.15f;
        int barW = w - 12;
        ctx.fill(x + 6, y + 16, x + 6 + barW, y + 22, 0xFF202020);
        ctx.fill(x + 6, y + 16, x + 6 + (int) (barW * State.shownRatio), y + 22,
                MathHelper.hsvToRgb(State.shownRatio * 0.33f, 1f, 1f) | 0xFF000000);
        ctx.drawTextWithShadow(mc.textRenderer, (Math.round(hp * 10) / 10.0) + " HP", x + 6 + barW - 40, y + 24, 0xFFDDDDDD);

        EquipmentSlot[] slots = {EquipmentSlot.MAINHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET};
        int ix = x + 6;
        for (EquipmentSlot s : slots) {
            ItemStack st = t.getEquippedStack(s);
            if (!st.isEmpty()) {
                ctx.drawItem(st, ix, y + 28);
                ctx.drawStackOverlay(mc.textRenderer, st, ix, y + 28);
            }
            ix += 18;
        }
    }

    // ------------------------------------------------------------------ keystrokes + cps
    private static int cps(ArrayDeque<Long> q) {
        long n = System.currentTimeMillis();
        while (!q.isEmpty() && n - q.peekFirst() > 1000) q.pollFirst();
        return q.size();
    }

    private static void key(DrawContext ctx, MinecraftClient mc, int x, int y, int w, String label, boolean down) {
        ctx.fill(x, y, x + w, y + 18, down ? 0xB0FFFFFF : 0x80000000);
        ctx.drawCenteredTextWithShadow(mc.textRenderer, label, x + w / 2, y + 5, down ? 0xFF000000 : 0xFFFFFFFF);
    }

    private static void keystrokes(DrawContext ctx, MinecraftClient mc, int sh) {
        long handle = mc.getWindow().getHandle();
        boolean l = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        long now = System.currentTimeMillis();
        if (l && !prevL && mc.currentScreen == null && LCLICKS.size() < 64) LCLICKS.add(now);
        if (r && !prevR && mc.currentScreen == null && RCLICKS.size() < 64) RCLICKS.add(now);
        prevL = l;
        prevR = r;

        int x = 6, y = sh - 66;
        key(ctx, mc, x + 20, y, 18, "W", mc.options.forwardKey.isPressed());
        key(ctx, mc, x, y + 20, 18, "A", mc.options.leftKey.isPressed());
        key(ctx, mc, x + 20, y + 20, 18, "S", mc.options.backKey.isPressed());
        key(ctx, mc, x + 40, y + 20, 18, "D", mc.options.rightKey.isPressed());
        key(ctx, mc, x, y + 40, 29, cps(LCLICKS) + "", l);
        key(ctx, mc, x + 31, y + 40, 27, cps(RCLICKS) + "", r);
    }

    // ------------------------------------------------------------------ item counter
    private static int count(PlayerInventory inv, Item item) {
        int n = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(item)) n += s.getCount();
        }
        return n;
    }

    private static void itemCounter(DrawContext ctx, MinecraftClient mc, int sh) {
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
        int x = 4, y = sh / 2 + 10;
        for (int i = 0; i < items.length; i++) {
            ctx.fill(x, y, x + 40, y + 18, 0x80000000);
            ctx.drawItem(icons[i], x + 1, y + 1);
            ctx.drawTextWithShadow(mc.textRenderer, "x" + COUNTS[i], x + 20, y + 5, 0xFFFFFFFF);
            y += 20;
        }
    }

    // ------------------------------------------------------------------ radar
    private static void radar(DrawContext ctx, MinecraftClient mc, int sw, int sh) {
        int size = 84, x = sw - size - 6, y = sh - size - 6;
        int cx = x + size / 2, cy = y + size / 2;
        int col = RenderUtil.gradient(0);
        ctx.fill(x, y, x + size, y + size, 0x80000000);
        ctx.fill(x, y, x + size, y + 1, col);
        ctx.fill(x, y + size - 1, x + size, y + size, col);
        ctx.fill(x, y, x + 1, y + size, col);
        ctx.fill(x + size - 1, y, x + size, y + size, col);
        ctx.fill(cx, y + 1, cx + 1, y + size - 1, 0x40FFFFFF);
        ctx.fill(x + 1, cy, x + size - 1, cy + 1, 0x40FFFFFF);
        ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFF55FF55);

        double yaw = Math.toRadians(mc.player.getYaw());
        double s = Math.sin(yaw), c = Math.cos(yaw);
        double lim = size / 2.0 - 3;
        int drawn = 0;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            if (drawn++ >= 32) break;
            double dx = p.getX() - mc.player.getX(), dz = p.getZ() - mc.player.getZ();
            double px = MathHelper.clamp((-dx * c - dz * s) * 1.2, -lim, lim);
            double py = MathHelper.clamp((dx * s - dz * c) * 1.2, -lim, lim);
            int ex = cx + (int) px, ey = cy + (int) py;
            ctx.fill(ex - 1, ey - 1, ex + 2, ey + 2, 0xFFFF4040);
        }
    }

    // ------------------------------------------------------------------ combo
    private static void combo(DrawContext ctx, MinecraftClient mc, int sw, int sh) {
        if (State.combo < 1) return;
        ctx.drawCenteredTextWithShadow(mc.textRenderer, "Combo: " + State.combo, sw / 2, sh / 2 + 30,
                RenderUtil.gradient(State.combo * 3));
    }

    private Hud() {}
}
