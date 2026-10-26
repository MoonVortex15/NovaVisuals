package nova.visuals;

import java.util.Properties;

/** Persistent tuning for local-only combat particles, movement trails and first-person hand visuals. */
public final class VisualEffectsSettings {
    public static int hitStyle = 0;
    public static int hitCount = 9;
    public static int damageCount = 5;
    public static int criticalStyle = 1;
    public static int criticalCount = 10;
    public static int killStyle = 2;
    public static int killCount = 18;
    public static int trailStyle = 0;
    public static int trailDensity = 2;

    public static float handX = 0.0f;
    public static float handY = 0.0f;
    public static float handZ = 0.0f;
    public static float handScale = 1.0f;
    public static int handRotX = 0;
    public static int handRotY = 0;
    public static int handRotZ = 0;
    /** 0 vanilla swing, 1 suppress item swing. */
    public static int handAnimation = 0;

    static void load(Properties p) {
        hitStyle = clamp(i(p, "effects.hitStyle", 0), 0, 3);
        hitCount = clamp(i(p, "effects.hitCount", 9), 3, 24);
        damageCount = clamp(i(p, "effects.damageCount", 5), 1, 16);
        criticalStyle = clamp(i(p, "effects.criticalStyle", 1), 0, 3);
        criticalCount = clamp(i(p, "effects.criticalCount", 10), 3, 28);
        killStyle = clamp(i(p, "effects.killStyle", 2), 0, 3);
        killCount = clamp(i(p, "effects.killCount", 18), 6, 40);
        trailStyle = clamp(i(p, "trails.style", 0), 0, 3);
        trailDensity = clamp(i(p, "trails.density", 2), 1, 4);
        handX = clamp(f(p, "customHand.x", 0), -0.6f, 0.6f);
        handY = clamp(f(p, "customHand.y", 0), -0.6f, 0.6f);
        handZ = clamp(f(p, "customHand.z", 0), -0.6f, 0.6f);
        handScale = clamp(f(p, "customHand.scale", 1), 0.5f, 1.5f);
        handRotX = clamp(i(p, "customHand.rotX", 0), -45, 45);
        handRotY = clamp(i(p, "customHand.rotY", 0), -45, 45);
        handRotZ = clamp(i(p, "customHand.rotZ", 0), -45, 45);
        handAnimation = clamp(i(p, "customHand.animation", 0), 0, 1);
    }
    static void save(Properties p) {
        p.setProperty("effects.hitStyle", "" + hitStyle);
        p.setProperty("effects.hitCount", "" + hitCount);
        p.setProperty("effects.damageCount", "" + damageCount);
        p.setProperty("effects.criticalStyle", "" + criticalStyle);
        p.setProperty("effects.criticalCount", "" + criticalCount);
        p.setProperty("effects.killStyle", "" + killStyle);
        p.setProperty("effects.killCount", "" + killCount);
        p.setProperty("trails.style", "" + trailStyle);
        p.setProperty("trails.density", "" + trailDensity);
        p.setProperty("customHand.x", "" + handX);
        p.setProperty("customHand.y", "" + handY);
        p.setProperty("customHand.z", "" + handZ);
        p.setProperty("customHand.scale", "" + handScale);
        p.setProperty("customHand.rotX", "" + handRotX);
        p.setProperty("customHand.rotY", "" + handRotY);
        p.setProperty("customHand.rotZ", "" + handRotZ);
        p.setProperty("customHand.animation", "" + handAnimation);
    }
    private static int i(Properties p, String k, int d) { try { return Integer.parseInt(p.getProperty(k, "" + d)); } catch (Exception e) { return d; } }
    private static float f(Properties p, String k, float d) { try { return Float.parseFloat(p.getProperty(k, "" + d)); } catch (Exception e) { return d; } }
    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
    private VisualEffectsSettings() {}
}
