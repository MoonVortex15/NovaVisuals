package nova.visuals.mixin;

import nova.visuals.Modules;
import nova.visuals.VisualSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Overrides time only for the local client world; server time remains unchanged. */
@Mixin(World.class)
public abstract class WorldTimeMixin {
    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void novavisuals$overrideLocalTime(CallbackInfoReturnable<Long> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == (Object) this && Modules.TIME_CHANGER.enabled) {
            cir.setReturnValue(VisualSettings.timeOfDay());
        }
    }
}
