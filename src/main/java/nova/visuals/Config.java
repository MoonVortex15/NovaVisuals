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
        }
    }

    public static void save() {
        Properties p = new Properties();
        for (Module m : Modules.ALL) p.setProperty(m.name, String.valueOf(m.enabled));
        try (var out = Files.newOutputStream(FILE)) { p.store(out, "Nova Visuals"); } catch (IOException ignored) {}
    }

    private Config() {}
}
