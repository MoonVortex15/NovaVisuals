package nova.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.entity.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

import static nova.visuals.Modules.*;

public final class WorldVisuals {
    private record Target(Box box, int fill, int outline) {}

    public static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        boolean any = PLAYER_ESP.enabled || PROJECTILE_ESP.enabled || ITEM_ESP.enabled
                || STORAGE_ESP.enabled || TRACERS.enabled || TRAJECTORIES.enabled;
        if (!any) return;

        MatrixStack ms = ctx.matrixStack();
        if (ms == null) return;
        Matrix4f m = ms.peek().getPositionMatrix();
        Camera cam = ctx.camera();
        Vec3d cp = cam.getPos();
        float td = ctx.tickCounter().getTickDelta(true);

        List<Target> targets = new ArrayList<>();
        List<Target> tracers = new ArrayList<>();
        int i = 0;

        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player) continue;
            int color;
            boolean player = false;

            if (e instanceof PlayerEntity) {
                if (!PLAYER_ESP.enabled && !TRACERS.enabled) continue;
                player = true;
                color = RenderUtil.rainbow(i * 25, 0.65f);
                i++;
            } else if (e instanceof ItemEntity) {
                if (!ITEM_ESP.enabled) continue;
                color = 0xFFFFD54A;
            } else if (e instanceof EndCrystalEntity) {
                if (!PROJECTILE_ESP.enabled) continue;
                color = 0xFF40E0FF;
            } else if (e instanceof ProjectileEntity) {
                if (!PROJECTILE_ESP.enabled || e.getVelocity().lengthSquared() < 1.0E-4) continue;
                color = 0xFFFF6A3D;
            } else continue;

            double x = MathHelper.lerp(td, e.prevX, e.getX());
            double y = MathHelper.lerp(td, e.prevY, e.getY());
            double z = MathHelper.lerp(td, e.prevZ, e.getZ());
            Box b = e.getBoundingBox()
                    .offset(x - e.getX() - cp.x, y - e.getY() - cp.y, z - e.getZ() - cp.z);

            if (!player || PLAYER_ESP.enabled) {
                targets.add(new Target(b, RenderUtil.alpha(color, 0x3C), color));
            }
            if (player && PLAYER_ESP.enabled && e instanceof LivingEntity le) {
                float ratio = MathHelper.clamp((le.getHealth() + le.getAbsorptionAmount()) / le.getMaxHealth(), 0f, 1f);
                Box bg = new Box(b.minX - 0.14, b.minY, b.minZ - 0.14, b.minX - 0.08, b.maxY, b.minZ - 0.08);
                Box fg = new Box(bg.minX, bg.minY, bg.minZ, bg.maxX, bg.minY + (bg.maxY - bg.minY) * ratio, bg.maxZ);
                targets.add(new Target(bg, 0xB0000000, 0));
                targets.add(new Target(fg, MathHelper.hsvToRgb(ratio * 0.33f, 1f, 1f) | 0xFF000000, 0));
            }
            if (player && TRACERS.enabled) tracers.add(new Target(b, 0, color));
        }

        if (STORAGE_ESP.enabled) {
            int cx = mc.player.getChunkPos().x, cz = mc.player.getChunkPos().z;
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    WorldChunk c = mc.world.getChunkManager().getWorldChunk(cx + dx, cz + dz);
                    if (c == null) continue;
                    for (BlockEntity be : c.getBlockEntities().values()) {
                        int col;
                        if (be instanceof EnderChestBlockEntity) col = 0xFFB066FF;
                        else if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity) col = 0xFFFFA500;
                        else if (be instanceof ShulkerBoxBlockEntity) col = 0xFFFF5FD2;
                        else if (be instanceof AbstractFurnaceBlockEntity || be instanceof HopperBlockEntity) col = 0xFF8A8A8A;
                        else continue;
                        targets.add(new Target(new Box(be.getPos()).offset(-cp.x, -cp.y, -cp.z),
                                RenderUtil.alpha(col, 0x3C), col));
                    }
                }
            }
        }

        List<Vec3d> path = TRAJECTORIES.enabled ? predict(mc) : null;

        if (targets.isEmpty() && tracers.isEmpty() && path == null) return;

        RenderUtil.begin();
        for (Target t : targets) RenderUtil.fillBox(m, t.fill, t.box);

        RenderSystem.lineWidth(2f);
        for (Target t : targets) if (t.outline != 0) RenderUtil.outlineBox(m, t.box, t.outline);

        if (!tracers.isEmpty()) {
            Vec3d look = Vec3d.fromPolar(cam.getPitch(), cam.getYaw());
            BufferBuilder b = RenderUtil.lines();
            for (Target t : tracers) {
                Vec3d c = t.box.getCenter();
                RenderUtil.line(b, m, look.x, look.y, look.z, c.x, c.y, c.z, t.outline);
            }
            BufferRenderer.drawWithGlobalProgram(b.end());
        }

        if (path != null && path.size() >= 2) {
            int col = RenderUtil.rainbow(0, 0.5f);
            BufferBuilder b = RenderUtil.lines();
            for (int k = 1; k < path.size(); k++) {
                Vec3d a = path.get(k - 1).subtract(cp), d = path.get(k).subtract(cp);
                RenderUtil.line(b, m, a.x, a.y, a.z, d.x, d.y, d.z, col);
            }
            BufferRenderer.drawWithGlobalProgram(b.end());

            Vec3d end = path.get(path.size() - 1).subtract(cp);
            Box mark = new Box(end.x - 0.2, end.y - 0.2, end.z - 0.2, end.x + 0.2, end.y + 0.2, end.z + 0.2);
            RenderUtil.fillBox(m, RenderUtil.alpha(col, 0x60), mark);
            RenderUtil.outlineBox(m, mark, col);
        }

        RenderSystem.lineWidth(1f);
        RenderUtil.end();
    }

    /** Предсказание траектории лука / перлов / снежков / зелий в руке. */
    private static List<Vec3d> predict(MinecraftClient mc) {
        PlayerEntity p = mc.player;
        ItemStack st = p.getMainHandStack();
        Item it = st.getItem();
        double speed, grav;

        if (it == Items.BOW) {
            if (!p.isUsingItem()) return null;
            float pull = BowItem.getPullProgress(p.getItemUseTime());
            if (pull < 0.1f) return null;
            speed = pull * 3.0;
            grav = 0.05;
        } else if (it == Items.ENDER_PEARL || it == Items.SNOWBALL || it == Items.EGG) {
            speed = 1.5; grav = 0.03;
        } else if (it == Items.EXPERIENCE_BOTTLE) {
            speed = 0.7; grav = 0.07;
        } else if (it == Items.SPLASH_POTION || it == Items.LINGERING_POTION) {
            speed = 0.5; grav = 0.05;
        } else return null;

        Vec3d pos = p.getEyePos().subtract(0, 0.1, 0);
        Vec3d vel = Vec3d.fromPolar(p.getPitch(), p.getYaw()).multiply(speed);
        List<Vec3d> pts = new ArrayList<>();
        pts.add(pos);

        for (int i = 0; i < 150; i++) {
            Vec3d next = pos.add(vel);
            var hit = mc.world.raycast(new RaycastContext(pos, next,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
            if (hit.getType() != HitResult.Type.MISS) {
                pts.add(hit.getPos());
                break;
            }
            pts.add(next);
            pos = next;
            vel = vel.multiply(0.99).subtract(0, grav, 0);
            if (pos.y < mc.world.getBottomY()) break;
        }
        return pts;
    }

    private WorldVisuals() {}
}
