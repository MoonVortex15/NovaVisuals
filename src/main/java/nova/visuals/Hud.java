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

    public static void render(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();

        if (HUD.enabled) watermark(ctx, mc, sw);
        if (ARMOR_HUD.enabled) armor(ctx, mc, sw, sh);
        if (POTION_HUD.enabled) potions(ctx, mc);
        if (TARGET_HUD.enabled) targetHud(ctx, mc, sw, sh);
        if (KEYSTROKES.enabled) keystrokes(ctx, mc, sh);
        if (ITEM_COUNT.enabled) itemCounter(ctx, mc, sh);
        if (RADAR.enabled) radar(ctx, mc, sw, sh);
        if (COMBO.enabled) combo(ctx, mc, sw, sh);
    }

    // ------------------------------------------------------------------ watermark + arraylist
    private static void watermark(DrawContext ctx, MinecraftClient mc, int sw) {
        int ping = 0;
        if (mc.getNetworkHandler() != null) {
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (e != null) ping = e.getLatency();
        }
        String wm = "Nova §7| §f" + mc.getCurrentFps() + " fps §7| §f" + ping + " ms §7| §f"
                + String.format("%.0f %.0f %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
        int w = mc.textRenderer.getWidth(wm) + 8;
        ctx.fill(4, 4, 4 + w, 17, 0x90000000);
        ctx.fill(4, 4, 4 + w, 5, RenderUtil.rainbow(0, 0.7f));
        ctx.drawTextWithShadow(mc.textRenderer, wm, 8, 7, RenderUtil.rainbow(0, 0.5f));

        long nowMs = System.currentTimeMillis();
        if (activeCache == null || nowMs - activeBuilt > 500) {
            activeCache = ALL.stream()
                    .filter(m -> m.enabled && m != HUD)
                    .sorted(Comparator.comparingInt((Module m) -> -mc.textRenderer.getWidth(m.name)))
                    .toList();
            activeBuilt = nowMs;
        }
        List<Module> active = activeCache;
        int y = 4, idx = 0;
        for (Module m : active) {
            int tw = mc.textRenderer.getWidth(m.name);
            int col = RenderUtil.rainbow(idx * 40, 0.6f);
            ctx.fill(sw - tw - 8, y, sw - 2, y + 11, 0x90000000);
            ctx.fill(sw - 2, y, sw, y + 11, col);
            ctx.drawTextWithShadow(mc.textRenderer, m.name, sw - tw - 5, y + 2, col);
            y += 11;
            idx++;
        }
    }

    // ------------------------------------------------------------------ armor
    private static void armor(DrawContext ctx, MinecraftClient mc, int sw, int sh) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        int x = sw / 2 - 36, y = sh - 72;
        for (EquipmentSlot s : slots) {
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
        int y = 24;
        for (StatusEffectInstance e : mc.player.getStatusEffects()) {
            String name = e.getEffectType().value().getName().getString() + " " + (e.getAmplifier() + 1);
            String time;
            if (e.isInfinite()) time = "inf";
            else {
                int s = e.getDuration() / 20;
                time = String.format("%d:%02d", s / 60, s % 60);
            }
            String line = name + " §7" + time;
            ctx.fill(4, y - 1, 8 + mc.textRenderer.getWidth(line), y + 10, 0x80000000);
            ctx.drawTextWithShadow(mc.textRenderer, line, 6, y + 1, e.getEffectType().value().isBeneficial() ? 0xFF7CFF8A : 0xFFFF6B6B);
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
        ctx.fill(x, y, x + 2, y + h, RenderUtil.rainbow(0, 0.7f));

        String head = t.getName().getString() + " §7" + String.format("%.1fm", mc.player.distanceTo(t));
        ctx.drawTextWithShadow(mc.textRenderer, head, x + 6, y + 4, 0xFFFFFFFF);

        float hp = t.getHealth() + t.getAbsorptionAmount();
        float ratio = MathHelper.clamp(hp / t.getMaxHealth(), 0f, 1f);
        State.shownRatio += (ratio - State.shownRatio) * 0.15f;
        int barW = w - 12;
        ctx.fill(x + 6, y + 16, x + 6 + barW, y + 22, 0xFF202020);
        ctx.fill(x + 6, y + 16, x + 6 + (int) (barW * State.shownRatio), y + 22,
                MathHelper.hsvToRgb(State.shownRatio * 0.33f, 1f, 1f) | 0xFF000000);
        ctx.drawTextWithShadow(mc.textRenderer, String.format("%.1f HP", hp), x + 6 + barW - 36, y + 24, 0xFFDDDDDD);

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
        if (l && !prevL && mc.currentScreen == null) LCLICKS.add(now);
        if (r && !prevR && mc.currentScreen == null) RCLICKS.add(now);
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
        int col = RenderUtil.rainbow(0, 0.6f);
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
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
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
                RenderUtil.rainbow(State.combo * 20, 0.7f));
    }

    private Hud() {}
}
