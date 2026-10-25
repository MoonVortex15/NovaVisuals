package nova.visuals;

import net.minecraft.client.MinecraftClient;

/** Авто-режим: если FPS упал ниже 25, мод сам включает облегчённую отрисовку (вернётся при FPS > 45). */
public final class Perf {
    private static boolean fpsLow;
    private static long checked;

    public static boolean lite() {
        long n = System.currentTimeMillis();
        if (n - checked > 1000) {
            checked = n;
            int fps = MinecraftClient.getInstance().getCurrentFps();
            if (fps > 0 && fps < 25) fpsLow = true;
            else if (fps > 45) fpsLow = false;
        }
        return Modules.LOW_END.enabled || fpsLow;
    }

    private Perf() {}
}
