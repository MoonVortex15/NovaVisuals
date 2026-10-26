package nova.visuals;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class Config {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("novavisuals.properties");

    public static void load() {
        Properties p = new Properties();
        if (Files.exists(FILE)) {
            try (var in = Files.newInputStream(FILE)) { p.load(in); } catch (IOException ignored) {}
        }
        for (Module m : Modules.ALL) {
            String v = p.getProperty(m.name);
            if (v != null) m.enabled = Boolean.parseBoolean(v);
            String key = p.getProperty("key." + m.name);
            if (key != null) { try { m.keyCode = Integer.parseInt(key); } catch (NumberFormatException ignored) {} }
        }
        TargetIndicatorSettings.load(p);
        TrajectorySettings.load(p);
        VisualSettings.load(p);
        VisualEffectsSettings.load(p);
        for (Widget w : Widgets.ALL) {
            String v = p.getProperty("pos." + w.id);
            if (v == null) continue;
            try {
                String[] a = v.split(",");
                w.rx = Float.parseFloat(a[0]);
                w.ry = Float.parseFloat(a[1]);
            } catch (Exception ignored) {}
        }
    }

    public static void save() {
        Properties p = new Properties();
        for (Module m : Modules.ALL) p.setProperty(m.name, String.valueOf(m.enabled));
        for (Module m : Modules.ALL) p.setProperty("key." + m.name, String.valueOf(m.keyCode));
        TargetIndicatorSettings.save(p);
        TrajectorySettings.save(p);
        VisualSettings.save(p);
        VisualEffectsSettings.save(p);
        for (Widget w : Widgets.ALL) p.setProperty("pos." + w.id, w.rx + "," + w.ry);
        try (var out = Files.newOutputStream(FILE)) { p.store(out, "Nova Visuals"); } catch (IOException ignored) {}
    }

    private Config() {}
}
