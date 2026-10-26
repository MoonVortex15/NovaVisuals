package nova.visuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public class NovaClient implements ClientModInitializer {
    public static KeyBinding openGui;
    /** Set each client tick; consumed by the GameRenderer mixin while the zoom key is held. */
    public static volatile boolean zoomActive;
    private static final java.util.Map<Module, Boolean> KEY_WAS_DOWN = new java.util.HashMap<>();

    // перетаскивание виджетов в чате
    public static Widget dragging;
    private static double offX, offY;

    public static boolean isKeyDown(int keyCode) {
        if (keyCode < 0) return false;
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        return mc.getWindow() != null && GLFW.glfwGetKey(mc.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
    }

    public static String keyName(int keyCode) {
        if (keyCode < 0) return "Unbound";
        return InputUtil.fromKeyCode(keyCode, 0).getLocalizedText().getString();
    }

    @Override
    public void onInitializeClient() {
        try { Config.load(); } catch (Throwable ignored) {}

        openGui = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Меню Nova Visuals", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "Nova Visuals"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            try {
                if (mc.currentScreen == null) while (openGui.wasPressed()) mc.setScreen(new ModuleScreen());
                else while (openGui.wasPressed()) { /* consume key presses while a screen is open */ }
            } catch (Throwable ignored) {}

            if (mc.currentScreen == null && mc.getWindow() != null) {
                for (Module m : Modules.ALL) {
                    if (m.keyCode < 0 || m == Modules.ZOOM) continue;
                    boolean down = isKeyDown(m.keyCode);
                    boolean was = KEY_WAS_DOWN.getOrDefault(m, false);
                    if (down && !was) m.toggle();
                    KEY_WAS_DOWN.put(m, down);
                }
            } else {
                KEY_WAS_DOWN.replaceAll((m, down) -> false);
            }

            zoomActive = Modules.ZOOM.enabled && mc.currentScreen == null && isKeyDown(Modules.ZOOM.keyCode);

            for (Module m : Modules.ALL) {
                if (!m.enabled) continue;
                try { m.onTick(mc); } catch (Throwable t) { Safe.fail(m, t); }
            }

            if (mc.player != null) {
                float hp = mc.player.getHealth();
                if (hp < State.lastHealth) State.combo = 0;
                State.lastHealth = hp;
                if (System.currentTimeMillis() - State.lastHit > 3000) State.combo = 0;

                // A lightweight local particle trail; no packets are sent to the server.
                if (Modules.TRAILS.enabled && mc.world != null && mc.player.age % 3 == 0
                        && mc.player.getVelocity().horizontalLengthSquared() > 0.006) {
                    mc.world.addParticle(ParticleTypes.END_ROD, mc.player.getX(), mc.player.getY() + 0.12,
                            mc.player.getZ(), 0.0, 0.015, 0.0);
                }
            }

            // Wait for the server-synced health/death state after the attack callback.
            if (mc.world != null && State.lastAttackedEntity != null
                    && System.currentTimeMillis() - State.lastAttackedAt < 1800
                    && State.lastKillEffectAt != State.lastAttackedAt
                    && (State.lastAttackedEntity.getHealth() <= 0.0f)) {
                if (Modules.KILL_EFFECTS.enabled) {
                    spawnBurst(mc, State.lastAttackedEntity, ParticleTypes.TOTEM_OF_UNDYING, 18);
                }
                State.lastKillEffectAt = State.lastAttackedAt;
            }
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            try {
                if (world.isClient && entity instanceof LivingEntity le) {
                    State.target = le;
                    State.targetTime = System.currentTimeMillis();
                    State.lastHit = State.targetTime;
                    State.combo++;
                    State.lastAttackedEntity = le;
                    State.lastAttackedAt = State.targetTime;
                    if (Modules.HIT_PARTICLES.enabled) spawnBurst(net.minecraft.client.MinecraftClient.getInstance(), le, ParticleTypes.CRIT, 9);
                    if (Modules.DAMAGE_PARTICLES.enabled) spawnBurst(net.minecraft.client.MinecraftClient.getInstance(), le, ParticleTypes.DAMAGE_INDICATOR, 5);
                    if (Modules.CRITICAL_EFFECTS.enabled && isLikelyCritical(player)) {
                        spawnBurst(net.minecraft.client.MinecraftClient.getInstance(), le, ParticleTypes.ENCHANTED_HIT, 10);
                    }
                }
            } catch (Throwable ignored) {}
            return ActionResult.PASS;
        });

        // ---- перетаскивание HUD мышкой, пока открыт чат
        ScreenEvents.AFTER_INIT.register((client, screen, scaledW, scaledH) -> {
            if (!(screen instanceof ChatScreen)) return;
            dragging = null;

            ScreenMouseEvents.allowMouseClick(screen).register((s, mx, my, button) -> {
                if (button != 0) return true;
                int w = client.getWindow().getScaledWidth(), h = client.getWindow().getScaledHeight();
                for (int i = Widgets.ALL.size() - 1; i >= 0; i--) {
                    Widget wd = Widgets.ALL.get(i);
                    if (!wd.module.enabled) continue;
                    if (wd.hit(mx, my, w, h)) {
                        dragging = wd;
                        offX = mx - wd.px(w);
                        offY = my - wd.py(h);
                        return false;   // клик не уйдёт в чат
                    }
                }
                return true;
            });

            ScreenEvents.afterRender(screen).register((s, ctx, mx, my, tickDelta) -> {
                try {
                    int w = client.getWindow().getScaledWidth(), h = client.getWindow().getScaledHeight();
                    if (dragging != null) {
                        boolean down = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
                        if (down) {
                            dragging.rx = MathHelper.clamp((float) ((mx - offX) / w), 0f, 1f);
                            dragging.ry = MathHelper.clamp((float) ((my - offY) / h), 0f, 1f);
                        } else {
                            dragging = null;
                            Config.save();
                        }
                    }
                    Hud.editOverlay(ctx, client, mx, my, w, h);
                } catch (Throwable t) {
                    dragging = null;
                }
            });
        });

        WorldRenderEvents.LAST.register(WorldVisuals::render);
        HudRenderCallback.EVENT.register(Hud::render);
    }

    private static boolean isLikelyCritical(net.minecraft.entity.player.PlayerEntity player) {
        return !player.isOnGround() && player.fallDistance > 0.0f && !player.isClimbing()
                && !player.isTouchingWater() && !player.hasVehicle();
    }

    private static void spawnBurst(net.minecraft.client.MinecraftClient mc, LivingEntity target,
                                   net.minecraft.particle.ParticleEffect type, int count) {
        if (mc == null || mc.world == null || target == null || target.isRemoved()) return;
        double x = target.getX();
        double y = target.getBodyY(0.55);
        double z = target.getZ();
        for (int i = 0; i < count; i++) {
            double vx = (Math.random() - 0.5) * 0.16;
            double vy = Math.random() * 0.16;
            double vz = (Math.random() - 0.5) * 0.16;
            mc.world.addParticle(type, x + (Math.random() - 0.5) * target.getWidth(),
                    y + (Math.random() - 0.5) * target.getHeight(),
                    z + (Math.random() - 0.5) * target.getWidth(), vx, vy, vz);
        }
    }
}
