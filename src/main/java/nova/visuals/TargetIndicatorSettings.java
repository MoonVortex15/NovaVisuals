package nova.visuals;

import java.util.Properties;

/** Lightweight user-adjustable target marker options. */
public final class TargetIndicatorSettings {
    /** 0 = square, 1 = diamond. */
    public static int shape = 1;
    public static float size = 0.9f;
    public static float speed = 1.6f;
    public static int durationMs = 1600;
    /** 0 = rainbow, 1 = cyan, 2 = purple. */
    public static int colorMode = 0;

    static void load(Properties p) {
        try { shape = clamp(Integer.parseInt(p.getProperty("target.shape", "1")), 0, 1); } catch (Exception ignored) {}
        try { size = Math.max(0.35f, Math.min(1.8f, Float.parseFloat(p.getProperty("target.size", "0.9")))); } catch (Exception ignored) {}
        try { speed = Math.max(0.3f, Math.min(4.0f, Float.parseFloat(p.getProperty("target.speed", "1.6")))); } catch (Exception ignored) {}
        try { durationMs = Math.max(300, Math.min(5000, Integer.parseInt(p.getProperty("target.duration", "1600")))); } catch (Exception ignored) {}
        try { colorMode = clamp(Integer.parseInt(p.getProperty("target.color", "0")), 0, 2); } catch (Exception ignored) {}
    }

    static void save(Properties p) {
        p.setProperty("target.shape", String.valueOf(shape));
        p.setProperty("target.size", String.valueOf(size));
        p.setProperty("target.speed", String.valueOf(speed));
        p.setProperty("target.duration", String.valueOf(durationMs));
        p.setProperty("target.color", String.valueOf(colorMode));
    }

    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    private TargetIndicatorSettings() {}
}
