package nova.visuals;

import net.minecraft.client.MinecraftClient;

public class Module {
    public enum Category { RENDER, PVP, HUD, WORLD }

    public final String name;
    public final Category category;
    public boolean enabled;

    public Module(String name, Category category, boolean enabled) {
        this.name = name;
        this.category = category;
        this.enabled = enabled;
    }

    public void toggle() {
        enabled = !enabled;
        if (enabled) onEnable(); else onDisable();
        Config.save();
        Hud.invalidate();
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick(MinecraftClient mc) {}
}
