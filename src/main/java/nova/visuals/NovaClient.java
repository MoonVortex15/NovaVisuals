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
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public class NovaClient implements ClientModInitializer {
    public static KeyBinding openGui;
    public static KeyBinding zoomKey;

    // перетаскивание виджетов в чате
    public static Widget dragging;
    private static double offX, offY;

    @Override
    public void onInitializeClient() {
        try { Config.load(); } catch (Throwable ignored) {}

        openGui = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Nova GUI", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "Nova"));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "Nova"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            try {
                while (openGui.wasPressed()) mc.setScreen(new ModuleScreen());
            } catch (Throwable ignored) {}

            for (Module m : Modules.ALL) {
                if (!m.enabled) continue;
                try { m.onTick(mc); } catch (Throwable t) { Safe.fail(m, t); }
            }

            if (mc.player != null) {
                float hp = mc.player.getHealth();
                if (hp < State.lastHealth) State.combo = 0;
                State.lastHealth = hp;
                if (System.currentTimeMillis() - State.lastHit > 3000) State.combo = 0;
            }
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            try {
                if (world.isClient && entity instanceof LivingEntity le) {
                    State.target = le;
                    State.targetTime = System.currentTimeMillis();
                    State.lastHit = State.targetTime;
                    State.combo++;
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
}
