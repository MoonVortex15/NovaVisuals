package nova.visuals;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class TargetIndicatorSettings {
    // Метод возвращает игрока, на которого ты смотрит/кого атаковал
    public static Player getCurrentTarget() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.crosshairPickEntity instanceof Player player) {
            return player;
        }
        // Если хочешь привязать к последней атакованной цели — можно расширить логику здесь
        return null;
    }
}
