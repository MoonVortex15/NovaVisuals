package nova.visuals;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/** Защита от вылетов: ошибка в функции не роняет игру, а после 3 ошибок функция сама отключается. */
public final class Safe {
    private static final Logger LOG = LoggerFactory.getLogger("NovaVisuals");
    private static final Map<String, Integer> ERRORS = new HashMap<>();

    public static void fail(Module m, Throwable t) {
        failGroup(m.name, t, m);
    }

    public static void failGroup(String key, Throwable t, Module... mods) {
        int n = ERRORS.merge(key, 1, Integer::sum);
        if (n == 1) LOG.error("Error in " + key, t);
        if (n >= 3) {
            for (Module m : mods) {
                if (m.enabled) {
                    m.enabled = false;
                    try { m.onDisable(); } catch (Throwable ignored) {}
                }
            }
            Config.save();
            Hud.invalidate();
            if (n == 3) LOG.error(key + " disabled after repeated errors");
        }
    }

    private Safe() {}
}
