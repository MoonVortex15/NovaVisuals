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
import net.minecraft.util.math.BlockPos;
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
    private record Stored(BlockPos pos, int color) {}

    // списки переиспользуются каждый кадр — меньше мусора для сборщика памяти
    private static final List<Target> TARGETS = new ArrayList<>();
    private static final List<Target> TRACER_LIST = new ArrayList<>();
    private static final List<Stored> STORAGE_CACHE = new ArrayList<>();
    private static final List<Entity> ENTITY_CACHE = new ArrayList<>();
    private static long lastEntityScan;
    private static net.minecraft.world.World cachedWorld;
    private static long lastScan;

    private static List<Vec3d> pathCache;
    private static long pathAt;

    public static void render(WorldRenderContext ctx) {
        try {
            renderInner(ctx);
        } catch (Throwable t) {
            try { RenderUtil.end(); RenderSystem.lineWidth(1f); } catch (Throwable ignored) {}
            Safe.failGroup("WorldVisuals", t, PLAYER_ESP, MOB_ESP, PROJECTILE_ESP, ITEM_ESP, STORAGE_ESP, TRACERS, TRAJECTORIES, TARGET_INDICATOR);
        }
    }

    private static void renderInner(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        boolean any = PLAYER_ESP.enabled || MOB_ESP.enabled || PROJECTILE_ESP.enabled || ITEM_ESP.enabled
                || STORAGE_ESP.enabled || TRACERS.enabled || TRAJECTORIES.enabled || TARGET_INDICATOR.enabled;
        if (!any) return;

        boolean low = Perf.lite();
        if (cachedWorld != mc.world) {
            cachedWorld = mc.world;
            ENTITY_CACHE.clear();
            STORAGE_CACHE.clear();
            lastEntityScan = 0;
            lastScan = 0;
        }
        double range = low ? 36 : 80;
        double rangeSq = range * range;
        int maxTargets = low ? 24 : 64;

        MatrixStack ms = ctx.matrixStack();
        if (ms == null) return;
        Matrix4f m = ms.peek().getPositionMatrix();
        Camera cam = ctx.camera();
        Vec3d cp = cam.getPos();
        float td = ctx.tickCounter().getTickDelta(true);

        TARGETS.clear();
        TRACER_LIST.clear();
        boolean needEntities = PLAYER_ESP.enabled || MOB_ESP.enabled || PROJECTILE_ESP.enabled || ITEM_ESP.enabled || TRACERS.enabled;
        int i = 0;

        if (needEntities) {
            long now = System.currentTimeMillis();
            if (now - lastEntityScan > (low ? 300 : 100)) {
                ENTITY_CACHE.clear();
                for (Entity candidate : mc.world.getEntities()) {
                    if (candidate == mc.player || candidate.squaredDistanceTo(mc.player) > rangeSq) continue;
                    boolean relevant = (candidate instanceof PlayerEntity && (PLAYER_ESP.enabled || TRACERS.enabled))
                            || (candidate instanceof LivingEntity && !(candidate instanceof PlayerEntity) && MOB_ESP.enabled)
                            || (candidate instanceof ItemEntity && ITEM_ESP.enabled)
                            || ((candidate instanceof EndCrystalEntity || candidate instanceof ProjectileEntity) && PROJECTILE_ESP.enabled);
                    if (relevant) ENTITY_CACHE.add(candidate);
                    if (ENTITY_CACHE.size() >= maxTargets * 2) break;
                }
                lastEntityScan = now;
            }
            for (Entity e : ENTITY_CACHE) {
                if (TARGETS.size() >= maxTargets) break;
                if (e.isRemoved() || e == mc.player || e.squaredDistanceTo(mc.player) > rangeSq) continue;
                int color;
                boolean player = false;

                if (e instanceof PlayerEntity) {
                    if (!PLAYER_ESP.enabled && !TRACERS.enabled) continue;
                    player = true;
                    color = RenderUtil.rainbow(i * 25, 0.65f);
                    i++;
                } else if (e instanceof LivingEntity) {
                    if (!MOB_ESP.enabled) continue;
                    color = 0xFFFF7BC8;
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
                    TARGETS.add(new Target(b, low ? 0 : RenderUtil.alpha(color, 0x3C), color));
                }
                if (!low && player && PLAYER_ESP.enabled && e instanceof LivingEntity le) {
                    float ratio = MathHelper.clamp((le.getHealth() + le.getAbsorptionAmount()) / le.getMaxHealth(), 0f, 1f);
                    Box bg = new Box(b.minX - 0.14, b.minY, b.minZ - 0.14, b.minX - 0.08, b.maxY, b.minZ - 0.08);
                    Box fg = new Box(bg.minX, bg.minY, bg.minZ, bg.maxX, bg.minY + (bg.maxY - bg.minY) * ratio, bg.maxZ);
                    TARGETS.add(new Target(bg, 0xB0000000, 0));
                    TARGETS.add(new Target(fg, MathHelper.hsvToRgb(ratio * 0.33f, 1f, 1f) | 0xFF000000, 0));
                }
                if (player && TRACERS.enabled) TRACER_LIST.add(new Target(b, 0, color));
            }
        } else if (!ENTITY_CACHE.isEmpty()) {
            ENTITY_CACHE.clear();
        }

        if (STORAGE_ESP.enabled) {
            long now = System.currentTimeMillis();
            if (now - lastScan > (low ? 3000 : 1000)) {
                lastScan = now;
                STORAGE_CACHE.clear();
                int rad = low ? 2 : 3;
                int cx = mc.player.getChunkPos().x, cz = mc.player.getChunkPos().z;
                for (int dx = -rad; dx <= rad; dx++) {
                    for (int dz = -rad; dz <= rad; dz++) {
                        WorldChunk c = mc.world.getChunkManager().getWorldChunk(cx + dx, cz + dz);
                        if (c == null) continue;
                        for (BlockEntity be : c.getBlockEntities().values()) {
                            if (STORAGE_CACHE.size() >= 96) break;
                            int col;
                            if (be instanceof EnderChestBlockEntity) col = 0xFFB066FF;
                            else if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity) col = 0xFFFFA500;
                            else if (be instanceof ShulkerBoxBlockEntity) col = 0xFFFF5FD2;
                            else if (be instanceof AbstractFurnaceBlockEntity || be instanceof HopperBlockEntity) col = 0xFF8A8A8A;
                            else continue;
                            STORAGE_CACHE.add(new Stored(be.getPos(), col));
                        }
                    }
                }
            }
            int drawn = 0;
            for (Stored s : STORAGE_CACHE) {
                if (drawn >= maxTargets) break;
                double dx = s.pos.getX() + 0.5 - cp.x, dy = s.pos.getY() + 0.5 - cp.y, dz = s.pos.getZ() + 0.5 - cp.z;
                if (dx * dx + dy * dy + dz * dz > rangeSq) continue;
                TARGETS.add(new Target(new Box(s.pos).offset(-cp.x, -cp.y, -cp.z),
                        low ? 0 : RenderUtil.alpha(s.color, 0x3C), s.color));
                drawn++;
            }
        } else if (!STORAGE_CACHE.isEmpty()) {
            STORAGE_CACHE.clear();
        }

        List<Vec3d> path = null;
        if (TRAJECTORIES.enabled) {
            long now = System.currentTimeMillis();
            if (!low || now - pathAt > 66) {
                pathCache = predict(mc, low ? 50 : 100);
                pathAt = now;
            }
            path = pathCache;
        }

        boolean drawTargetMarker = shouldDrawTargetMarker(mc);
        if (TARGETS.isEmpty() && TRACER_LIST.isEmpty() && (path == null || path.size() < 2) && !drawTargetMarker) return;

        RenderUtil.begin();

        boolean hasFill = false;
        for (Target t : TARGETS) if (t.fill != 0) { hasFill = true; break; }
        if (hasFill) {
            BufferBuilder b = RenderUtil.quads();
            for (Target t : TARGETS) if (t.fill != 0) RenderUtil.addFill(b, m, t.box, t.fill);
            BufferRenderer.drawWithGlobalProgram(b.end());
        }

        RenderSystem.lineWidth(low ? 1f : 2f);
        boolean hasLines = !TRACER_LIST.isEmpty();
        for (Target t : TARGETS) if (t.outline != 0) { hasLines = true; break; }
        if (hasLines) {
            BufferBuilder b = RenderUtil.lines();
            for (Target t : TARGETS) if (t.outline != 0) RenderUtil.addOutline(b, m, t.box, t.outline);
            if (!TRACER_LIST.isEmpty()) {
                Vec3d look = Vec3d.fromPolar(cam.getPitch(), cam.getYaw());
                for (Target t : TRACER_LIST) {
                    Vec3d c = t.box.getCenter();
                    RenderUtil.line(b, m, look.x, look.y, look.z, c.x, c.y, c.z, t.outline);
                }
            }
            BufferRenderer.drawWithGlobalProgram(b.end());
        }

        if (path != null && path.size() >= 2) {
            int col = RenderUtil.rainbow(0, 0.5f);
            BufferBuilder b = RenderUtil.lines();
            for (int k = 1; k < path.size(); k++) {
                Vec3d a = path.get(k - 1), d = path.get(k);
                RenderUtil.line(b, m, a.x - cp.x, a.y - cp.y, a.z - cp.z, d.x - cp.x, d.y - cp.y, d.z - cp.z, col);
            }
            BufferRenderer.drawWithGlobalProgram(b.end());

            Vec3d end = path.get(path.size() - 1).subtract(cp);
            Box mark = new Box(end.x - 0.2, end.y - 0.2, end.z - 0.2, end.x + 0.2, end.y + 0.2, end.z + 0.2);
            if (!low) RenderUtil.fillBox(m, mark, RenderUtil.alpha(col, 0x60));
            RenderUtil.outlineBox(m, mark, col);
        }

        if (drawTargetMarker) drawTargetMarker(m, cp, td);

        RenderSystem.lineWidth(1f);
        RenderUtil.end();
    }

    private static boolean shouldDrawTargetMarker(MinecraftClient mc) {
        LivingEntity target = State.target;
        return TARGET_INDICATOR.enabled && target != null && !target.isRemoved()
                && target.getWorld() == mc.world
                && System.currentTimeMillis() - State.targetTime <= TargetIndicatorSettings.durationMs;
    }

    /** Draws a low-cost rotating four-sided frame around the most recently attacked entity. */
    private static void drawTargetMarker(Matrix4f matrix, Vec3d cameraPos, float tick) {
        LivingEntity target = State.target;
        if (target == null) return;
        double x = MathHelper.lerp(tick, target.prevX, target.getX()) - cameraPos.x;
        double y = MathHelper.lerp(tick, target.prevY, target.getY());
        double z = MathHelper.lerp(tick, target.prevZ, target.getZ()) - cameraPos.z;
        double half = TargetIndicatorSettings.size;
        double lowY = y + target.getHeight() * 0.22 - cameraPos.y;
        double highY = y + target.getHeight() * 0.78 - cameraPos.y;
        double angle = System.currentTimeMillis() * 0.001 * TargetIndicatorSettings.speed;
        double[][] points = TargetIndicatorSettings.shape == 0
                ? new double[][]{{-1,-1},{1,-1},{1,1},{-1,1}}
                : new double[][]{{0,-1},{1,0},{0,1},{-1,0}};
        double[][] lower = new double[4][3];
        double[][] upper = new double[4][3];
        double sin = Math.sin(angle), cos = Math.cos(angle);
        for (int i = 0; i < 4; i++) {
            double ox = points[i][0] * half, oz = points[i][1] * half;
            double rx = ox * cos - oz * sin, rz = ox * sin + oz * cos;
            lower[i] = new double[]{x + rx, lowY, z + rz};
            upper[i] = new double[]{x + rx, highY, z + rz};
        }
        int color = switch (TargetIndicatorSettings.colorMode) {
            case 1 -> 0xFF45E8FF;
            case 2 -> 0xFFB18CFF;
            default -> RenderUtil.rainbow(0, 0.8f);
        };
        BufferBuilder b = RenderUtil.lines();
        for (int i = 0; i < 4; i++) {
            int n = (i + 1) % 4;
            RenderUtil.line(b, matrix, lower[i][0], lower[i][1], lower[i][2], lower[n][0], lower[n][1], lower[n][2], color);
            RenderUtil.line(b, matrix, upper[i][0], upper[i][1], upper[i][2], upper[n][0], upper[n][1], upper[n][2], color);
            RenderUtil.line(b, matrix, lower[i][0], lower[i][1], lower[i][2], upper[i][0], upper[i][1], upper[i][2], RenderUtil.alpha(color, 0xB0));
        }
        BufferRenderer.drawWithGlobalProgram(b.end());
    }

    private static List<Vec3d> predict(MinecraftClient mc, int maxSteps) {
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
        List<Vec3d> pts = new ArrayList<>(maxSteps + 2);
        pts.add(pos);

        for (int i = 0; i < maxSteps; i++) {
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
