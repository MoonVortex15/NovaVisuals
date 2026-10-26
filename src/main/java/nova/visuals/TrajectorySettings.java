package nova.visuals;

import java.util.Properties;

/** Client-side appearance and per-projectile controls for the trajectory renderer. */
public final class TrajectorySettings {
    public static boolean arrows = true;
    public static boolean pearls = true;
    public static boolean potions = true;
    public static boolean snowballsEggs = true;
    /** 0 gradient, 1 rainbow, 2 solid accent. */
    public static int style = 0;
    /** 0 violet/cyan, 1 cyan, 2 pink, 3 green. */
    public static int color = 0;
    public static int thickness = 2;
    public static boolean dots = true;
    public static boolean impactMarker = true;
    public static boolean endpointGlow = true;

    static void load(Properties p) {
        arrows = bool(p, "trajectory.arrows", true);
        pearls = bool(p, "trajectory.pearls", true);
        potions = bool(p, "trajectory.potions", true);
        snowballsEggs = bool(p, "trajectory.small", true);
        style = clamp(integer(p, "trajectory.style", 0), 0, 2);
        color = clamp(integer(p, "trajectory.color", 0), 0, 3);
        thickness = clamp(integer(p, "trajectory.thickness", 2), 1, 4);
        dots = bool(p, "trajectory.dots", true);
        impactMarker = bool(p, "trajectory.impact", true);
        endpointGlow = bool(p, "trajectory.glow", true);
    }
    static void save(Properties p) {
        p.setProperty("trajectory.arrows", String.valueOf(arrows));
        p.setProperty("trajectory.pearls", String.valueOf(pearls));
        p.setProperty("trajectory.potions", String.valueOf(potions));
        p.setProperty("trajectory.small", String.valueOf(snowballsEggs));
        p.setProperty("trajectory.style", String.valueOf(style));
        p.setProperty("trajectory.color", String.valueOf(color));
        p.setProperty("trajectory.thickness", String.valueOf(thickness));
        p.setProperty("trajectory.dots", String.valueOf(dots));
        p.setProperty("trajectory.impact", String.valueOf(impactMarker));
        p.setProperty("trajectory.glow", String.valueOf(endpointGlow));
    }
    private static boolean bool(Properties p, String k, boolean d) { return Boolean.parseBoolean(p.getProperty(k, String.valueOf(d))); }
    private static int integer(Properties p, String k, int d) { try { return Integer.parseInt(p.getProperty(k, String.valueOf(d))); } catch (Exception e) { return d; } }
    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    private TrajectorySettings() {}
}
