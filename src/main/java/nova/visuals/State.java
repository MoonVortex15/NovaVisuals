package nova.visuals;

import net.minecraft.entity.LivingEntity;

public final class State {
    public static LivingEntity target;
    public static long targetTime;
    public static int combo;
    public static long lastHit;
    public static float lastHealth = 20f;
    public static float shownRatio = 1f;
    public static LivingEntity lastAttackedEntity;
    public static long lastAttackedAt;
    public static long lastKillEffectAt;
    public static int trailTicks;
    public static double lastTrailX = Double.NaN;
    public static double lastTrailZ = Double.NaN;

    private State() {}
}
