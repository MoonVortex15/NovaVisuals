package nova.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

import static nova.visuals.Module.Category.*;

public final class Modules {
    public static final List<Module> ALL = new ArrayList<>();

    // ---------- RENDER ----------
    public static final Module PLAYER_ESP     = add(new Module("PlayerESP", RENDER, true));
    public static final Module PROJECTILE_ESP = add(new Module("ProjectileESP", RENDER, false));
    public static final Module ITEM_ESP       = add(new Module("ItemESP", RENDER, false));
    public static final Module STORAGE_ESP    = add(new Module("StorageESP", RENDER, false));
    public static final Module TRACERS        = add(new Module("Tracers", RENDER, false));

    public static final Module GLOW = add(new Module("Glow", RENDER, false) {
        @Override public void onTick(MinecraftClient mc) {
            if (mc.world == null) return;
            for (PlayerEntity p : mc.world.getPlayers()) if (p != mc.player) p.setGlowing(true);
        }
        @Override public void onDisable() {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world == null) return;
            for (PlayerEntity p : mc.world.getPlayers()) p.setGlowing(false);
        }
    });

    public static final Module HITBOXES = add(new Module("Hitboxes", RENDER, false) {
        @Override public void onTick(MinecraftClient mc) {
            mc.getEntityRenderDispatcher().setRenderHitboxes(true);
        }
        @Override public void onDisable() {
            MinecraftClient.getInstance().getEntityRenderDispatcher().setRenderHitboxes(false);
        }
    });

    public static final Module FULLBRIGHT = add(new Module("Fullbright", RENDER, false) {
        @Override public void onTick(MinecraftClient mc) {
            if (mc.player == null) return;
            mc.player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.NIGHT_VISION, 420, 0, false, false, false));
        }
        @Override public void onDisable() {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
    });

    // ---------- PVP (visual helpers) ----------
    public static final Module TRAJECTORIES = add(new Module("Trajectories", PVP, true));

    public static final Module NO_HURT_CAM = add(new Module("NoHurtCam", PVP, false) {
        @Override public void onTick(MinecraftClient mc) {
            if (mc.player != null) mc.player.hurtTime = 0;
        }
    });

    public static final Module ZOOM = add(new Module("Zoom (C)", PVP, true) {
        private boolean zooming;
        private int prevFov;
        private double prevSens;

        @Override public void onTick(MinecraftClient mc) {
            boolean want = NovaClient.zoomKey != null && NovaClient.zoomKey.isPressed() && mc.currentScreen == null;
            if (want && !zooming) {
                prevFov = mc.options.getFov().getValue();
                prevSens = mc.options.getMouseSensitivity().getValue();
                mc.options.getFov().setValue(30);
                mc.options.getMouseSensitivity().setValue(prevSens * 0.35);
                zooming = true;
            } else if (!want && zooming) {
                restore(mc);
            }
        }
        @Override public void onDisable() { restore(MinecraftClient.getInstance()); }

        private void restore(MinecraftClient mc) {
            if (!zooming) return;
            mc.options.getFov().setValue(prevFov);
            mc.options.getMouseSensitivity().setValue(prevSens);
            zooming = false;
        }
    });

    // ---------- HUD ----------
    public static final Module HUD         = add(new Module("HUD", HUD_CAT(), true));
    public static final Module ARMOR_HUD   = add(new Module("ArmorHUD", HUD_CAT(), true));
    public static final Module POTION_HUD  = add(new Module("PotionHUD", HUD_CAT(), true));
    public static final Module TARGET_HUD  = add(new Module("TargetHUD", HUD_CAT(), true));
    public static final Module KEYSTROKES  = add(new Module("Keystrokes", HUD_CAT(), true));
    public static final Module ITEM_COUNT  = add(new Module("ItemCounter", HUD_CAT(), true));
    public static final Module RADAR       = add(new Module("Radar", HUD_CAT(), false));
    public static final Module COMBO       = add(new Module("Combo", HUD_CAT(), true));

    // ---------- WORLD ----------
    public static final Module NO_WEATHER = add(new Module("NoWeather", WORLD, false) {
        @Override public void onTick(MinecraftClient mc) {
            if (mc.world == null) return;
            mc.world.setRainGradient(0f);
            mc.world.setThunderGradient(0f);
        }
    });

    private static Module.Category HUD_CAT() { return Module.Category.HUD; }
    private static Module add(Module m) { ALL.add(m); return m; }
    private Modules() {}
}
