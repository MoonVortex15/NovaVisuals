package nova.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

public final class RenderUtil {
    private RenderUtil() {}

    public static int rainbow(int offset, float sat) {
        float h = ((System.currentTimeMillis() / 18 + offset) % 360) / 360f;
        return MathHelper.hsvToRgb(h, sat, 1f) | 0xFF000000;
    }

    public static int alpha(int argb, int a) {
        return (argb & 0xFFFFFF) | (a << 24);
    }

    public static void begin() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
    }

    public static void end() {
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void v(BufferBuilder b, Matrix4f m, double x, double y, double z, int c) {
        b.vertex(m, (float) x, (float) y, (float) z).color(c);
    }

    public static void fillBox(Matrix4f m, Box bx, int c) {
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        double x1 = bx.minX, y1 = bx.minY, z1 = bx.minZ, x2 = bx.maxX, y2 = bx.maxY, z2 = bx.maxZ;
        // bottom / top
        v(b, m, x1, y1, z1, c); v(b, m, x2, y1, z1, c); v(b, m, x2, y1, z2, c); v(b, m, x1, y1, z2, c);
        v(b, m, x1, y2, z1, c); v(b, m, x1, y2, z2, c); v(b, m, x2, y2, z2, c); v(b, m, x2, y2, z1, c);
        // north / south
        v(b, m, x1, y1, z1, c); v(b, m, x1, y2, z1, c); v(b, m, x2, y2, z1, c); v(b, m, x2, y1, z1, c);
        v(b, m, x1, y1, z2, c); v(b, m, x2, y1, z2, c); v(b, m, x2, y2, z2, c); v(b, m, x1, y2, z2, c);
        // west / east
        v(b, m, x1, y1, z1, c); v(b, m, x1, y1, z2, c); v(b, m, x1, y2, z2, c); v(b, m, x1, y2, z1, c);
        v(b, m, x2, y1, z1, c); v(b, m, x2, y2, z1, c); v(b, m, x2, y2, z2, c); v(b, m, x2, y1, z2, c);
        BufferRenderer.drawWithGlobalProgram(b.end());
    }

    public static void fillBox(Matrix4f m, int c, Box bx) {
        fillBox(m, bx, c);
    }

    public static void outlineBox(Matrix4f m, Box bx, int c) {
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        double x1 = bx.minX, y1 = bx.minY, z1 = bx.minZ, x2 = bx.maxX, y2 = bx.maxY, z2 = bx.maxZ;
        // bottom
        line(b, m, x1, y1, z1, x2, y1, z1, c); line(b, m, x2, y1, z1, x2, y1, z2, c);
        line(b, m, x2, y1, z2, x1, y1, z2, c); line(b, m, x1, y1, z2, x1, y1, z1, c);
        // top
        line(b, m, x1, y2, z1, x2, y2, z1, c); line(b, m, x2, y2, z1, x2, y2, z2, c);
        line(b, m, x2, y2, z2, x1, y2, z2, c); line(b, m, x1, y2, z2, x1, y2, z1, c);
        // verticals
        line(b, m, x1, y1, z1, x1, y2, z1, c); line(b, m, x2, y1, z1, x2, y2, z1, c);
        line(b, m, x2, y1, z2, x2, y2, z2, c); line(b, m, x1, y1, z2, x1, y2, z2, c);
        BufferRenderer.drawWithGlobalProgram(b.end());
    }

    public static void line(BufferBuilder b, Matrix4f m, double x1, double y1, double z1,
                            double x2, double y2, double z2, int c) {
        v(b, m, x1, y1, z1, c);
        v(b, m, x2, y2, z2, c);
    }

    public static BufferBuilder lines() {
        return Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
    }
}
