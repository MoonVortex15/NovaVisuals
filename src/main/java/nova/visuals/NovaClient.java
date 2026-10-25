package nova.visuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;

public class NovaClient implements ClientModInitializer {
    public static KeyBinding openGui;
    public static KeyBinding zoomKey;

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

        WorldRenderEvents.LAST.register(WorldVisuals::render);
        HudRenderCallback.EVENT.register(Hud::render);
    }
}
