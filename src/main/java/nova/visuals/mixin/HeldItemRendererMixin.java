package nova.visuals.mixin;

import nova.visuals.Modules;
import nova.visuals.VisualEffectsSettings;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Client-only view-model transform; does not alter item data or server behavior. */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void novavisuals$pushCustomHand(AbstractClientPlayerEntity player, float tickProgress,
            float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress,
            MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (!Modules.CUSTOM_HAND.enabled) return;
        matrices.push();
        matrices.translate(VisualEffectsSettings.handX, VisualEffectsSettings.handY, VisualEffectsSettings.handZ);
        matrices.translate(0.5, 0.5, 0.5);
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(VisualEffectsSettings.handRotX));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(VisualEffectsSettings.handRotY));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(VisualEffectsSettings.handRotZ));
        float scale = VisualEffectsSettings.handScale;
        matrices.scale(scale, scale, scale);
        matrices.translate(-0.5, -0.5, -0.5);
    }

    @ModifyVariable(method = "renderFirstPersonItem", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private float novavisuals$customSwing(float swingProgress) {
        if (Modules.CUSTOM_HAND.enabled && VisualEffectsSettings.handAnimation == 1) return 0.0f;
        return swingProgress;
    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"))
    private void novavisuals$popCustomHand(AbstractClientPlayerEntity player, float tickProgress,
            float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress,
            MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (Modules.CUSTOM_HAND.enabled) matrices.pop();
    }
}
