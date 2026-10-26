package nova.visuals;

import java.util.Properties;

/** Persistent settings for lightweight world visuals. */
public final class VisualSettings {
    /** 0 violet, 1 cyan, 2 pink, 3 green. */
    public static int blockColor = 0;
    public static int blockThickness = 2;
    /** 0 noon, 1 midnight, 2 sunrise, 3 sunset. */
    public static int timePreset = 0;

    static void load(Properties p) {
        blockColor = clamp(integer(p, "blockOverlay.color", 0), 0, 3);
        blockThickness = clamp(integer(p, "blockOverlay.thickness", 2), 1, 4);
        timePreset = clamp(integer(p, "timeChanger.preset", 0), 0, 3);
    }
    static void save(Properties p) {
        p.setProperty("blockOverlay.color", String.valueOf(blockColor));
        p.setProperty("blockOverlay.thickness", String.valueOf(blockThickness));
        p.setProperty("timeChanger.preset", String.valueOf(timePreset));
    }
    public static int blockColorArgb() {
        return switch (blockColor) {
            case 1 -> 0xFF45E8FF;
            case 2 -> 0xFFFF6CB8;
            case 3 -> 0xFF72F5A5;
            default -> 0xFFB18CFF;
        };
    }
    public static long timeOfDay() {
        return switch (timePreset) {
            case 1 -> 18000L;
            case 2 -> 0L;
            case 3 -> 12000L;
            default -> 6000L;
        };
    }
    private static int integer(Properties p, String k, int d) { try { return Integer.parseInt(p.getProperty(k, String.valueOf(d))); } catch (Exception e) { return d; } }
    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    private VisualSettings() {}
}
