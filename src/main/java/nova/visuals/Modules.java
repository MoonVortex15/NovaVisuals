package nova.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;
import org.lwjgl.glfw.GLFW;

import static nova.visuals.Module.Category.*;

public final class Modules {
    public static final List<Module> ALL = new ArrayList<>();

    // ---------- RENDER ----------
    public static final Module LOW_END        = add(new Module("LowEnd", RENDER, true));
    public static final Module PLAYER_ESP     = add(new Module("PlayerESP", RENDER, true));
    public static final Module MOB_ESP        = add(new Module("MobESP", RENDER, false));
    public static final Module PROJECTILE_ESP = add(new Module("ProjectileESP", RENDER, false));
    public static final Module ITEM_ESP       = add(new Module("ItemESP", RENDER, false));
    public static final Module STORAGE_ESP    = add(new Module("StorageESP", RENDER, false));
    public static final Module TRACERS        = add(new Module("Tracers", RENDER, false));

    public static final Module GLOW = add(new Module("Glow", RENDER, false) {
        @Override public void onTick(MinecraftClient mc) {
            if (mc.world == null) return;
            for (PlayerEntity p : mc.world.getPlayers()) if (p != mc.player && !p.isGlowing()) p.setGlowing(true);
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
            StatusEffectInstance cur = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
            if (cur == null || cur.getDuration() < 220) {
                mc.player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.NIGHT_VISION, 1200, 0, false, false, false));
            }
        }
        @Override public void onDisable() {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
    });

    // ---------- PVP (visual helpers) ----------
    public static final Module TRAJECTORIES = add(new Module("Trajectories", PVP, true));
    public static final Module TARGET_INDICATOR = add(new Module("TargetIndicator", PVP, true));

    public static final Module NO_HURT_CAM = add(new Module("NoHurtCam", PVP, false) {
        @Override public void onTick(MinecraftClient mc) {
            if (mc.player != null) mc.player.hurtTime = 0;
        }
    });

    public static final Module ZOOM = add(new Module("Zoom (C)", PVP, true));

    // ---------- HUD (все элементы можно перетаскивать мышкой при открытом чате) ----------
    public static final Module WATERMARK   = add(new Module("Watermark", HUD_CAT(), true));
    public static final Module COORDS      = add(new Module("Coords", HUD_CAT(), true));
    public static final Module POTION_HUD  = add(new Module("Potions", HUD_CAT(), true));
    public static final Module HOTKEYS     = add(new Module("HotKeys", HUD_CAT(), true));
    public static final Module TARGET_HUD  = add(new Module("TargetHUD", HUD_CAT(), true));
    public static final Module COOLDOWNS   = add(new Module("Cooldowns", HUD_CAT(), true));
    public static final Module ARMOR_HUD   = add(new Module("ArmorHUD", HUD_CAT(), true));
    public static final Module KEYSTROKES  = add(new Module("Keystrokes", HUD_CAT(), false));
    public static final Module ITEM_COUNT  = add(new Module("ItemCounter", HUD_CAT(), false));
    public static final Module RADAR       = add(new Module("Radar", HUD_CAT(), false));
    public static final Module COMBO       = add(new Module("Combo", HUD_CAT(), true));
    public static final Module ARRAY_LIST  = add(new Module("ArrayList", HUD_CAT(), false));

    // ---------- WORLD ----------
    public static final Module NO_WEATHER = add(new Module("NoWeather", WORLD, false) {
        @Override public void onTick(MinecraftClient mc) {
            if (mc.world == null) return;
            mc.world.setRainGradient(0f);
            mc.world.setThunderGradient(0f);
        }
    });

    static { ZOOM.keyCode = GLFW.GLFW_KEY_C; }

    private static Module.Category HUD_CAT() { return Module.Category.HUD; }
    private static Module add(Module m) { ALL.add(m); return m; }
    private Modules() {}
}
