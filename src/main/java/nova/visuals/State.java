package nova.visuals;

import net.minecraft.entity.LivingEntity;

public final class State {
    public static LivingEntity target;
    public static long targetTime;
    public static int combo;
    public static long lastHit;
    public static float lastHealth = 20f;
    public static float shownRatio = 1f;

    private State() {}
}
